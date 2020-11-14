/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query;


import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import entity.query.core.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

public final class Join<T> implements IDataActuator {

	private static final Logger log = LogManager.getLogger(Queryable.class);
	protected Join(DataSource ds) {
		dataSource = ds;
	}

	@JsonIgnore
	@JSONField(serialize = false)
	private Connection connection;
	public Connection getConnection() {
		return this.connection;
	}

	public void setConnection(Connection conn) {
		this.connection = conn;
	}

	@JsonIgnore
	@JSONField(serialize = false)
	protected DataSource dataSource;

	public DataSource dataSource() {

		if (dataSource == null) {
			try {
				this.dataSource = DataSourceFactory.getInstance().getDataSource(getGenericType());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		return dataSource;
	}

	@Override
	@JsonIgnore
	@JSONField(serialize = false)
	public String getExpression() {
		return String.join(",", this.getParser().getContainer().Join);
	}

	protected void init(Class<T> clazz, Object obj, ISqlParser ps) {
		genericType = clazz;
		entityObject = obj;
		parser = ps;
        if(dataSource == null && parser != null) {
            SqlParserBase parserBase = (SqlParserBase) parser;
            if(parserBase.getDataSource() != null) {
                this.dataSource = parserBase.getDataSource();
            }
        }
	}

	private Object entityObject;
	public Object entityObject() {
		return entityObject;
	}

	private ISqlParser parser;
	public ISqlParser getParser() {
		return parser;
	}

	protected Class<T> genericType;
	public Class<T> getGenericType(){
		return genericType;
	}

	public On<T> on(String exp) {
		On<T> clause = new On<T>(getGenericType(), entityObject(), getParser(), this);
		clause.init(getGenericType(), entityObject(), getParser(), this);
		clause.getParser().addOn(exp);

		return clause;
	}
}
