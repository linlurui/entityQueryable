package entity.query.core;

import java.sql.Connection;

public class DBTransaction {
    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    private Connection connection;
}
