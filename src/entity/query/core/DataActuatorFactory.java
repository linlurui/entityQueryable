package entity.query.core;


import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.sql.Connection;
import java.sql.SQLException;

public class DataActuatorFactory {

    private volatile static DataActuatorFactory singleton;

    public static DataActuatorFactory getInstance() {
        if (singleton == null) {
            synchronized (DataSourceFactory.class) {
                if (singleton == null) {
                    singleton = new DataActuatorFactory();
                }
            }
        }

        return singleton;
    }

    public IDataActuator createActuator(DataSource dataSource) throws SQLException {
        return new DataActuator(dataSource);
    }

    private final class DataActuator implements IDataActuator {

        DataActuator(DataSource dataSource) throws SQLException {

            if(dataSource == null) {
                return;
            }

            this.dataSource = dataSource;
            this.connection = dataSource.getConnection();
        }

        @JsonIgnore
        @JSONField(serialize = false)
        private Connection connection;

        @JsonIgnore
        @JSONField(serialize = false)
        private DataSource dataSource;

        @Override
        public Connection getConnection() {
            return this.connection;
        }

        @Override
        public void setConnection(Connection conn) {
            this.connection = conn;
        }

        @Override
        public DataSource dataSource() {
            return this.dataSource;
        }

        @Override
        @JsonIgnore
        @JSONField(serialize = false)
        public String getExpression() {
            return null;
        }
    }
}
