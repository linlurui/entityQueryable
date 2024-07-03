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
import entity.query.enums.CommandMode;
import entity.query.enums.JoinMode;
import entity.tool.util.DBUtils;
import entity.tool.util.OutParameter;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Blob;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class QueryableBase<T> implements IDataActuator {

	private static final Logger log = LoggerFactory.getLogger(Queryable.class);

	@SuppressWarnings("unchecked")
	protected QueryableBase() {
	}

	protected QueryableBase(DBTransaction transaction) {
		this.transaction = transaction;
	}

	protected void init(Class<T> clazz, Object obj, IDataActuator dataActuator) {
		genericType = clazz;
		SqlParserFactory factory = SqlParserFactory.getInstance();
		try {
			parser = factory.CreateParser(getGenericType());

			entityObject = obj;
			if (dataActuator != null) {
				this.dataSource = dataActuator.dataSource();
				this.connection = dataActuator.getConnection();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
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
	private DBTransaction transaction;
	public DBTransaction getTransaction() {
		return this.transaction;
	}

	public void setTransaction(DBTransaction conn) {
		this.transaction = conn;
	}

	@ExcelIgnore
	@JsonIgnore
	@JSONField(serialize = false)
	private Connection connection;
	public Connection getConnection() {
		return this.connection;
	}

	public void setConnection(Connection conn) {
		this.connection = conn;
	}

	@ExcelIgnore
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
	protected ISqlParser getParser() {
		return parser;
	}

	@ExcelIgnore
	@JsonIgnore
	@JSONField(serialize = false)
	protected Class<T> genericType;
	protected Class<T> getGenericType(){
		return genericType;
	}

	protected void finalize() {
		parser = null;
		entityObject = null;
		genericType = null;
		connection = null;
		transaction = null;
		dataSource = null;
	}

	public String tablename() {
		OutParameter<Class<T>> param = new OutParameter<Class<T>>();
		param.setData(getGenericType());
		String tablename = parser.getTablename(param);

		return tablename;
	}

	public <E> Where<T> where(String exp, List<E> values) {
        return where(exp, values.toArray(), ", ");
    }

	public <E> Where<T> where(String exp, E[] values) {
	    return where(exp, values, ", ");
	}

   public <E> Where<T> where(String exp, E[] values, String spliter) {
        Where<T> clause = new Where<T>();
        clause.init(getGenericType(), entityObject(), getParser(), this);
        if(values == null || values.length < 1) {
        	clause.getParser().addWhere(exp);

        	return clause;
		}

        List<String> args = new ArrayList<String>();
        for(E obj : values) {
            args.add( DBUtils.getSqlInjText( obj ) );
        }

        if(args.size() > 0) {
        	if(values[0] instanceof String) {
				exp = String.format(exp, "'" + StringUtils.join("'" + spliter + "'", args) + "'");
			}

        	else {
				exp = String.format(exp, StringUtils.join(spliter, args));
			}
        }

        clause.getParser().addWhere(exp);

        return clause;
    }

	public Where<T> where(String exp) {
		Where<T> clause = new Where<T>();
		clause.init(getGenericType(), entityObject(), getParser(), this);
		clause.getParser().addWhere(exp);

		return clause;
	}

	public Select<T> select(String... exp) {
		Select<T> clause = new Select<T>();
		clause.init(getGenericType(), entityObject(), getParser(), this);
		if(exp != null && exp.length > 0) {
			clause.getParser().addSelect(StringUtils.join(", ", exp));
		}

		return clause;
	}

	public OrderBy<T> orderby(String... exp) {
		OrderBy<T> clause = new OrderBy<T>();
		clause.init(getGenericType(), entityObject(), getParser(), this);
		clause.getParser().addOrderBy(StringUtils.join(", ", exp));

		return clause;
	}

	public GroupBy<T> groupby(String... exp) {
		GroupBy<T> clause = new GroupBy<T>();
		clause.init(getGenericType(), entityObject(), getParser(), this);
		clause.getParser().addGroupBy(StringUtils.join(", ", exp));

		return clause;
	}

    public <E> Join<T> inner(Class<E> clazz) {
        return join(JoinMode.Inner, clazz, null);
    }

    public <E> Join<T> inner(Class<E> clazz, String alias) {
        Join<T> clause = new Join<T>(this.dataSource);
        clause.init(getGenericType(), entityObject(), getParser());
        clause.getParser().addJoin(JoinMode.Inner, clazz, alias);

        return clause;
    }

    public Join<T> inner(Queryable<?> q, String alias) {
        Join<T> clause = new Join<T>(this.dataSource);
        clause.init(getGenericType(), entityObject(), getParser());
        clause.getParser().addJoin(JoinMode.Inner, q.toString(CommandMode.Select), alias);

        return clause;
    }

    public <E> Join<T> outer(Class<E> clazz) {
        return join(JoinMode.Outer, clazz, null);
    }

    public <E> Join<T> outer(Class<E> clazz, String alias) {
        Join<T> clause = new Join<T>(this.dataSource);
        clause.init(getGenericType(), entityObject(), getParser());
        clause.getParser().addJoin(JoinMode.Outer, clazz, alias);

        return clause;
    }

    public Join<T> outer(Queryable<?> q, String alias) {
        Join<T> clause = new Join<T>(this.dataSource);
        clause.init(getGenericType(), entityObject(), getParser());
        clause.getParser().addJoin(JoinMode.Outer, q.toString(CommandMode.Select), alias);

        return clause;
    }

    public <E> Join<T> cross(Class<E> clazz) {
        return join(JoinMode.Cross, clazz, null);
    }

    public <E> Join<T> cross(Class<E> clazz, String alias) {
        Join<T> clause = new Join<T>(this.dataSource);
        clause.init(getGenericType(), entityObject(), getParser());
        clause.getParser().addJoin(JoinMode.Cross, clazz, alias);

        return clause;
    }

    public Join<T> cross(Queryable<?> q, String alias) {
        Join<T> clause = new Join<T>(this.dataSource);
        clause.init(getGenericType(), entityObject(), getParser());
        clause.getParser().addJoin(JoinMode.Cross, q.toString(CommandMode.Select), alias);

        return clause;
    }

	public <E> Join<T> right(Class<E> clazz) {
	    return join(JoinMode.Right, clazz, null);
	}

	public <E> Join<T> right(Class<E> clazz, String alias) {
	    Join<T> clause = new Join<T>(this.dataSource);
	    clause.init(getGenericType(), entityObject(), getParser());
	    clause.getParser().addJoin(JoinMode.Right, clazz, alias);

	    return clause;
	}

	public Join<T> right(Queryable<?> q, String alias) {
	    Join<T> clause = new Join<T>(this.dataSource);
	    clause.init(getGenericType(), entityObject(), getParser());
	    clause.getParser().addJoin(JoinMode.Right, q.toString(CommandMode.Select), alias);

	    return clause;
	}

	public <E> Join<T> left(Class<E> clazz) {
	    return join(JoinMode.Left, clazz, null);
	}

	public <E> Join<T> left(Class<E> clazz, String alias) {
	    Join<T> clause = new Join<T>(this.dataSource);
	    clause.init(getGenericType(), entityObject(), getParser());
	    clause.getParser().addJoin(JoinMode.Left, clazz, alias);

	    return clause;
	}

	public Join<T> left(Queryable<?> q, String alias) {
	    Join<T> clause = new Join<T>(this.dataSource);
	    clause.init(getGenericType(), entityObject(), getParser());
	    clause.getParser().addJoin(JoinMode.Left, q.toString(CommandMode.Select), alias);

	    return clause;
	}

	public <E> Join<T> join(JoinMode mode, Class<E> clazz) {
		return join(mode, clazz, null);
	}

	public <E> Join<T> join(JoinMode mode, Class<E> clazz, String alias) {
		Join<T> clause = new Join<T>(this.dataSource);
		clause.init(getGenericType(), entityObject(), getParser());
		clause.getParser().addJoin(mode, clazz, alias);

		return clause;
	}

	public Join<T> join(JoinMode mode, Queryable<?> q, String alias) {
		Join<T> clause = new Join<T>(this.dataSource);
		clause.init(getGenericType(), entityObject(), getParser());
		clause.getParser().addJoin(mode, q.toString(CommandMode.Select), alias);

		return clause;
	}

    public <E> From<T> as(String alias) {
        From<T> clause = new From<T>();
        clause.init(getGenericType(), entityObject(), this);
        clause.getParser().addFrom(getGenericType(), alias);

        return clause;
    }

    @Override
	@JsonIgnore
	@JSONField(serialize = false)
	public String toString() {
		return toString(CommandMode.Select);
	}

	public String toString(CommandMode commandMode) {
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(getGenericType(), "", commandMode, entityObject(), 0, 0, false, blobMap);

		return sql;
	}

	@Override
	@JsonIgnore
	@JSONField(serialize = false)
	public String getExpression() {
		return null;
	}
}
