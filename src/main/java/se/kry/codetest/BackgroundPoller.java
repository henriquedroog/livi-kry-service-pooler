package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class BackgroundPoller {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundPoller.class);

  private final WebClient webClient;

  public BackgroundPoller(Vertx vertx) {
    this.webClient = WebClient.create(vertx, new WebClientOptions().setConnectTimeout(5000));
  }

  public Future<List<String>> pollServices(Map<String, String> services) {

    services.forEach((url, status) -> {
      webClient.getAbs(url).send(result -> {
        if(result.succeeded() && result.result().statusCode() == 200) {
          services.put(url,RequestStatus.OK.name());
        } else {
          LOGGER.debug("Pooling result for service: " + url + " failed", result.cause().getMessage());
          services.put(url, RequestStatus.FAIL.name());
        }
      });
    });
    //TODO
    return Future.succeededFuture();
  }
}
