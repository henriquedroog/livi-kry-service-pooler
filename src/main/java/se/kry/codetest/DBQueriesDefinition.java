package se.kry.codetest;

/**
 * @author Henrique Droog
 * @since 2020-04-23
 */
public interface DBQueriesDefinition {
    String CREATE_DATABASE = "CREATE TABLE IF NOT EXISTS service (url VARCHAR(128) NOT NULL UNIQUE, name text NOT NULL, created_at " +
            "TEXT DEFAULT CURRENT_TIMESTAMP)";

    String GET_ALL_SERVICES = "SELECT * FROM service";
    String DELETE_SERVICE_BY_URL = "DELETE FROM service WHERE url = ?";
    String INSERT_SERVICE ="INSERT INTO service (url, name) VALUES (?, ?)";
}
