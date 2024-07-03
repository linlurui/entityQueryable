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


import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import entity.query.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

public final class Join<T> implements IDataActuator {

	private static final Logger log = LoggerFactory.getLogger(Queryable.class);
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
	@ExcelIgnore
	@JsonIgnore
	@JSONField(serialize = false)
	private Object entityObject;
	public Object entityObject() {
		return entityObject;
	}

	@ExcelIgnore
	@JsonIgnore
	@JSONField(serialize = false)
	private ISqlParser parser;
	public ISqlParser getParser() {
		return parser;
	}

	@ExcelIgnore
	@JsonIgnore
	@JSONField(serialize = false)
	protected Class<T> genericType;
	public Class<T> getGenericType(){
		return genericType;
	}

	protected void finalize() {
		parser = null;
		entityObject = null;
		genericType = null;
		connection = null;
		dataSource = null;
	}

	public On<T> on(String exp) {
		On<T> clause = new On<T>(getGenericType(), entityObject(), getParser(), this);
		clause.init(getGenericType(), entityObject(), getParser(), this);
		clause.getParser().addOn(exp);

		return clause;
	}
}
