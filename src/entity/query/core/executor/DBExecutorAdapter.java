/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */

package entity.query.core.executor;

import com.alibaba.druid.pool.DruidDataSource;
import entity.query.Queryable;
import entity.query.core.DataActuatorFactory;
import entity.query.core.DataSource;
import entity.query.core.DataSourceFactory;
import entity.query.core.IDataActuator;

import java.sql.SQLException;

public final class DBExecutorAdapter {

    public static <T> IDBExecutor createExecutor(Class<T> clazz) throws Exception {
        DataSource dataSource = DataSourceFactory.getInstance().getDataSource(clazz);
        return createExecutor(dataSource);
    }

    public static <T> IDBExecutor createExecutor(String dataSourceId) throws Exception {
        DataSource dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);
        return createExecutor(dataSource);
    }

    public static <T> IDBExecutor createExecutor(DataSource dataSource) throws Exception {
        IDataActuator dataActuator = DataActuatorFactory.getInstance().createActuator(dataSource);
        return createExecutor(dataActuator);
    }

    public static <T> IDBExecutor createExecutor(IDataActuator dataActuator) throws SQLException {
        return createExecutor(dataActuator, null);
    }

    public static <T> IDBExecutor createExecutor(IDataActuator dataActuator, Class<T> clazz) throws SQLException {

        if(dataActuator == null) {
            return null;
        }

        if(dataActuator.dataSource().getTransaction() != null) {
            return new JdbcExecutor(dataActuator);
        }

        if(clazz == null || (dataActuator.getConnection() != null && !dataActuator.getConnection().getAutoCommit())) {
            return new JdbcExecutor(dataActuator);
        }

        if(dataActuator.dataSource().isRxjava2()) {
            return new RxJava2JdbcExecutor(dataActuator);
        }

        return new JdbcExecutor(dataActuator);
    }
}
