package entity.query.core;

import java.sql.Connection;
import java.sql.SQLException;

public interface IDataActuator {

    Connection getConnection();

    void setConnection(Connection conn);

    DataSource dataSource();

    String getExpression();
}
