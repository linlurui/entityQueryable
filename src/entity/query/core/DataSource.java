/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.core;


import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import org.davidmoten.rx.jdbc.pool.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource extends DruidDataSource {

	private static final Logger log = LoggerFactory.getLogger( DataSource.class );
	private ThreadLocal<DBTransaction> transactionSet = new ThreadLocal<DBTransaction>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClassScope() {
		return classScope;
	}

	public void setClassScope(String classScope) {
		this.classScope = classScope;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

//	private volatile int activedCount = 0;

	private String id;
	private static final long serialVersionUID = 1L;
	private boolean autoReconnect;
	private String classScope;
	private String schema;
	private boolean isDefault;
	private Connection connection;
	private boolean rxjava2;

	public boolean isAutoReconnect() {
		return autoReconnect;
	}

	public void setAutoReconnect(boolean autoreconnect) {
		this.autoReconnect = autoreconnect;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean aDefault) {
		isDefault = aDefault;
	}

	public DBTransaction getTransaction() {
		return transactionSet.get();
	}

	public boolean isRxjava2() {
		return rxjava2;
	}

	public void setRxjava2(boolean rxjava2) {
		this.rxjava2 = rxjava2;
	}

	public void commit() {
		if(transactionSet.get() != null) {
			commit(transactionSet.get().getConnection());
			return;
		}
		this.commit(this.connection);
	}

	public void commit(Connection conn) {

		if(this.transactionSet.get() == null) {
			return;
		}

		if(conn == null) {
			return;
		}

		try {
			if (conn.getAutoCommit()) {
				conn.setAutoCommit(false);
			}
			conn.commit();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		close(conn);

		transactionSet.remove();
	}

	public void rollback() {
		if(transactionSet.get() != null) {
			rollback(transactionSet.get().getConnection());
			return;
		}
		rollback(this.connection);
	}

	public void rollback(Connection conn) {

		if(this.transactionSet.get() == null) {
			return;
		}

		if(conn==null) {
			return;
		}
		try {
			if (conn.getAutoCommit()) {
				conn.setAutoCommit(false);
			}
			conn.rollback();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		close(conn);

		transactionSet.remove();
	}

	public void close(Connection conn) {
		try {
			if(this.transactionSet.get() == null) {
				return;
			}

			if(!conn.isClosed()) {
				conn.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		finally {
			this.transactionSet.set(null);
		}
	}

	public Connection getConnection(boolean autoCommit) throws SQLException {

		if(transactionSet.get() != null) {
			return transactionSet.get().getConnection();
		}

		if(connection != null) {
			if (!connection.isClosed()) {
				synchronized (connection) {
					try {
						if (!connection.isClosed() && autoCommit && connection.getAutoCommit()) {
							//activedCount++;
							return connection;
						}
					}
					catch(Exception e) {
						log.warn(e.getMessage());
					}
				}
			}
		}

		connection = super.getConnection();
		if(!connection.isClosed() && !autoCommit && connection.getAutoCommit()) {
			log.info("open connection transaction!");
			connection.setAutoCommit(autoCommit);
		}

		return connection;
	}

	public DBTransaction beginTransaction() throws ClassNotFoundException, SQLException {
		//Class.forName(getDriverClassName());
		//Connection conn = DriverManager.getConnection(getUrl(), getUsername(), getPassword());
		//conn.setAutoCommit(false);
		Connection conn = getConnection(false);
		DBTransaction transaction = new DBTransaction();
		transaction.setConnection(conn);
		this.transactionSet.set(transaction);

		return this.transactionSet.get();
	}

	@Override
	public DruidPooledConnection getConnection() throws SQLException {

		return (DruidPooledConnection) getConnection(true);
	}

	public static DatabaseType getHealthCheck(String dbtype) {
		switch (dbtype.toUpperCase()) {
			case "ORACLE":
				return DatabaseType.ORACLE;
			case "HSQLDB":
				return DatabaseType.HSQLDB;
			case "DB2":
				return DatabaseType.DB2;
			case "DERBY":
				return DatabaseType.DERBY;
			case "INFORMIX":
				return DatabaseType.INFORMIX;
			case "MARIADB":
			case "MYSQL":
				return DatabaseType.MYSQL;
			case "SQLSERVER":
				return DatabaseType.SQL_SERVER;
			case "SQLITE":
				return DatabaseType.SQLITE;
			default:
				return DatabaseType.OTHER;
		}
	}
}
