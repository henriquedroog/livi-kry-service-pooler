package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  public static final String URL = "url";
  public static final String NAME = "name";
  public static final String STATUS = "status";
  public static final String CREATED_AT = "created_at";

  private final HashMap<String, String> services = new HashMap<>();
  private DBConnector connector;
  private BackgroundPoller poller;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    poller = new BackgroundPoller(vertx);
    connector = new DBConnector(vertx);
    connector.query(DBQueriesDefinition.CREATE_DATABASE).setHandler(done -> {
      if(done.succeeded()){
        LOGGER.info("Completed db migrations");
      } else {
        LOGGER.info("Could not do the db migrations", done.cause());
      }
    });
  }

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(services));
    populateServices();
    setRoutes(router);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8080, result -> {
          if (result.succeeded()) {
            System.out.println("KRY code test service started");
            startFuture.complete();
          } else {
            startFuture.fail(result.cause());
          }
        });
  }

  private void populateServices() {
    connector.query(DBQueriesDefinition.GET_ALL_SERVICES).setHandler(ar -> {
      if(ar.succeeded()) {
        if(ar.result().getNumRows() > 0){
          ar.result().getRows()
                  .forEach(svc -> {
                    services.put(svc.getString(URL),RequestStatus.UNKNOWN.name());
                  });
        }
      } else {
        LOGGER.error("Could not fetch services", ar.cause());
      }
    });
  }

  private void setRoutes(Router router){
    router.route("/*").handler(StaticHandler.create());
    router.get("/service").handler(this::getServicesHandler);
    router.post("/service").handler(this::createService);
    router.delete("/service").handler(this::deleteService);
  }

  private void deleteService(RoutingContext context) {
    JsonObject jsonBody = context.getBodyAsJson();

    String url = jsonBody.getString(URL);
    connector.query(DBQueriesDefinition.DELETE_SERVICE_BY_URL, new JsonArray().add(url))
            .setHandler(ar -> {
      if(ar.succeeded()) {
        services.remove(url);
        context.response().setStatusCode(204);
        context.response().putHeader("Content-Type", "application/json");
        context.response().end(new JsonObject().put("success", true).encode());
      } else {
        context.response().setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(ar.cause().getMessage());
        LOGGER.error("Could not insert entry into database", ar.cause());
      }
    });

  }

  private void createService(RoutingContext context) {
      LOGGER.info("Creating Services");
    JsonObject jsonBody = context.getBodyAsJson();
      LOGGER.info("Creating Services" + jsonBody);
    JsonArray parameters = new JsonArray()
            .add(jsonBody.getString(URL))
            .add(jsonBody.getString(NAME));
    LOGGER.info("Parameters" + parameters);
    connector.query(DBQueriesDefinition.INSERT_SERVICE, parameters)
            .setHandler(ar -> {
      if(ar.succeeded()) {
        LOGGER.info("Body" + RequestStatus.UNKNOWN.name());
        services.put(jsonBody.getString(URL), RequestStatus.UNKNOWN.name());
        context.response()
                .putHeader("content-type", "text/plain")
                .setStatusCode(200)
                .end(RequestStatus.OK.name());
      } else {
        context.response().setStatusCode(500)
                .putHeader("content-type", "text/plain")
                .end(ar.cause().getMessage());
        LOGGER.error("Could not insert entry into database", ar.cause());
      }
    });
  }

  private void getServicesHandler(RoutingContext context) {
    connector.query(DBQueriesDefinition.GET_ALL_SERVICES).setHandler(ar -> {
      if(ar.succeeded()) {
        List<JsonObject> jsonServices = ar.result().getRows().stream()
                .map(service -> new JsonObject()
                        .put(URL, service.getString(URL))
                        .put(NAME, service.getString(NAME))
                        .put(CREATED_AT, service.getString(CREATED_AT))
                        .put(STATUS, services.get(service.getString(URL))))
                .collect(Collectors.toList());

        context.response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end(new JsonArray(jsonServices).encode());
      } else {
        context.response().setStatusCode(500)
                .putHeader("content-type", "text/plain")
                .end(ar.cause().getMessage());
        LOGGER.error("Could not fetch services", ar.cause());
      }
    });
  }
}



