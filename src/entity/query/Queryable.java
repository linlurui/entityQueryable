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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.core.*;
import entity.query.core.executor.DBExecutorAdapter;
import entity.query.enums.CommandMode;
import entity.tool.util.Callback;
import entity.tool.util.DBUtils;
import entity.tool.util.JsonUtils;
import io.reactivex.Flowable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static entity.tool.util.DBUtils.*;
import static entity.tool.util.StringUtils.isEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Queryable<T> extends QueryableBase<T> implements Serializable {

	private static final Logger log = LogManager.getLogger(Queryable.class);

	@SuppressWarnings("unchecked")
	public Queryable() {
		super();
		init((Class<T>)this.getClass(), this, null);
	}

	public Queryable(DBTransaction transaction) {
		super(transaction);
		init((Class<T>)this.getClass(), this, null);
	}

	public From<T> from(QueryableAction<T> queryable, String alias) {
		From<T> clause = new From<T>();
		clause.init(getGenericType(), entityObject(), this);
		clause.getParser().addFrom(String.format( "( %s )", queryable.toString(CommandMode.Select) ), alias);

		return clause;
	}

	public <E> From<T> from(String tableName) {
	    From<T> clause = new From<T>();
	    clause.init(getGenericType(), entityObject(), this);
	    clause.getParser().addFrom(tableName, "");

	    return clause;
	}

	/***
	 * to json string
	 * @return
	 */
	@Override
	@JsonIgnore
	@JSONField(serialize = false)
	public String toString() {
		String json = JsonUtils.toJson(this);
		return json;
	}

	/***
	 * to sql string
	 * @param commandMode
	 * @return
	 */
	public String toString(CommandMode commandMode) {
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(getGenericType(), "", commandMode, entityObject(), 0, 0, false, blobMap);

		return sql;
	}

	public Integer insert() throws SQLException {

		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(getGenericType(), "", CommandMode.Insert, entityObject(), 0, 0, false, blobMap);
		Integer id = DBExecutorAdapter.createExecutor(this).execute(sql, blobMap);

		return id==null ? 0 : id;
	}

	public boolean delete() throws SQLException {
		String sql = getParser().toString(getGenericType(), "", CommandMode.Delete, entityObject(), 0, 0, false, null);
		Integer row = DBExecutorAdapter.createExecutor(this).execute(sql, null);

		return row!=null && row.intValue()>0;
	}

	public boolean update() throws SQLException {
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(getGenericType(), "", CommandMode.Update, entityObject(), 0, 0, false, blobMap);
		Integer row = DBExecutorAdapter.createExecutor(this).execute(sql, blobMap);

		return row!=null && row.intValue()>0;
	}

	public boolean update(String... exp) throws SQLException, IllegalAccessException {

		Field[] flds = this.getClass().getDeclaredFields();
		if(flds == null) {
			throw new SQLException("Can not find fields!!!");
		}

		Field primaryKey = null;
		for(Field fld : flds) {
			PrimaryKey primaryKeyAnn = fld.getAnnotation(PrimaryKey.class);
			if(primaryKeyAnn != null) {
				primaryKey = fld;
				break;
			}
		}

		if(primaryKey == null) {
			throw new SQLException("Can not find primary key!!!");
		}

		if(primaryKey.get(this) == null) {
			throw new SQLException("Primary key can not be empty!!!");
		}

		String fieldname = primaryKey.getName();
		Fieldname fieldNameAnn = primaryKey.getAnnotation(Fieldname.class);
		if(fieldNameAnn != null) {
			fieldname = fieldNameAnn.value();
		}

		QueryableAction ac = this.where(String.format("%s=#{%s}", fieldname, primaryKey.getName()));

		String expText = "";
		for (int i=0; i<exp.length; i++) {
			if(i>0) {
				expText = expText + ", ";
			}
			expText = expText + DBUtils.getSqlInjText( exp[i] );
		}

		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = ac.getParser().toString(getGenericType(), expText, CommandMode.UpdateFrom, entityObject(), 0, 0, false, blobMap);
		Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, blobMap);

		return row!=null && row > 0;
	}

	public Flowable<Integer> asyncInsert() throws Exception {

		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(getGenericType(), "", CommandMode.Insert, entityObject(), 0, 0, false, blobMap);

		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this).flowable(sql, blobMap);

		return flowable;
	}

	public Flowable<Integer> asyncDelete() throws Exception {
		String sql = getParser().toString(getGenericType(), "", CommandMode.Delete, entityObject(), 0, 0, false, null);

		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this).flowable(sql, null);

		return flowable;
	}

	public Flowable<Integer> asyncUpdate() throws Exception {
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(getGenericType(), "", CommandMode.Update, entityObject(), 0, 0, false, blobMap);

		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this).flowable(sql, blobMap);

		return flowable;
	}

	@JsonIgnore
	@JSONField(serialize = false)
	public Date selectNow() {
		String sql = getParser().getSelectNow();
		Date now = null;
		try {
			Flowable flowable = DBExecutorAdapter.createExecutor(this).flowable(Date.class, sql);

			now = (Date) flowable.blockingSingle();
			if(now == null) {
				return new Date();
			}

			return new Date(now.getTime());
		} catch (Exception e) {
			return new Date();
		}
	}

    public void batchInsert(List<T> list) throws Exception {
        batchInsert(list, null);
    }

    public void batchUpdate(List<T> list, String...exp) throws Exception {
        batchUpdate(list, null, exp);
    }

    public void batchDelete(List<T> list) throws Exception {
        batchDelete(list, null);
    }

	public void batchInsert(List<T> list, Callback<List<T>> call) throws Exception {
		batchTask(list, getGenericType(), this, getParser(), CommandMode.Insert, null, call);
	}

	public void batchUpdate(List<T> list, Callback<List<T>> call, String... exp) throws Exception {
		batchTask(list, getGenericType(), this, getParser(), CommandMode.Update, exp, call);
	}

	public void batchDelete(List<T> list, Callback<List<T>> call) throws Exception {
		batchTask(list, getGenericType(), this, getParser(), CommandMode.Delete, null, call);
	}

    public static List<String> getTables(String dataSourceId) {

		List<String> result = new ArrayList<String>();
		DataSource dataSource = null;

		try {
			dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);

			String sql = SqlParserFactory.createParser(dataSource).toString(null, "", CommandMode.Tables, null, 0, 0, false, null);

			result = DBExecutorAdapter.createExecutor(dataSource).query(String.class, sql);

		} catch (Exception e) {
			log.error(e);
		}

		return result;
    }

    public static List<ColumnInfo> getColumns(String dataSourceId, String tablename) {

		List<ColumnInfo> result = new ArrayList<ColumnInfo>();
		DataSource dataSource = null;

		try {
			dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);
			String sql = SqlParserFactory.createParser(dataSource).toString(null, tablename, CommandMode.ColumnsInfo, null, 0, 0, false, null);
			result = DBExecutorAdapter.createExecutor(dataSource).query(ColumnInfo.class, sql);
		} catch (Exception e) {
			log.error(e);
		}

		return result;
    }

    public static String getPrimaryKey(String dataSourceId, String tablename) {
		String result = null;

		DataSource dataSource = null;

		try {
			dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);
			String sql = SqlParserFactory.createParser(dataSource).toString(null, tablename, CommandMode.PrimaryKey, null, 0, 0, false, null);

			if( "sqlite".equals(dataSource.getDbType()) ) {
				List<Map> list = DBExecutorAdapter.createExecutor(dataSource).query(Map.class, sql);
				for(Map map : list) {
					if("1".equals(map.get("pk").toString())) {
						result = map.get("name").toString();
						break;
					}
				}
			}

			else {
				result = DBExecutorAdapter.createExecutor(dataSource).first(String.class, sql);
			}
		} catch (Exception e) {
			log.error(e);
		}

		return result;
	}

	public static void createTable(String dataSourceId, String tablename, List<ColumnInfo> columns) throws Exception {

		if(isEmpty(tablename)) {
			throw new Exception("tablename can not be empty!");
		}

		DataSource dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);
		String sql = SqlParserFactory.createParser(dataSource).getCreateTableSql(tablename, columns);
		DBExecutorAdapter.createExecutor(dataSource).execute(sql, null);

	}

	public static void alterTable(String dataSourceId, String tablename, List<ColumnInfo> columns) throws Exception {

		if(isEmpty(tablename)) {
			throw new Exception("tablename can not be empty!");
		}

		DataSource dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);
		String sql = SqlParserFactory.createParser(dataSource).getAlterTableSql(tablename, columns);
		DBExecutorAdapter.createExecutor(dataSource).execute(sql, null);

	}

	public static boolean exist(String dataSourceId, String tablename) throws Exception {

		if(isEmpty(tablename)) {
			throw new Exception("tablename can not be empty!");
		}

		boolean result = false;
		DataSource dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);

		String sql = SqlParserFactory.createParser(dataSource).getTableExistSql(tablename);
		Number num = DBExecutorAdapter.createExecutor(dataSource).first(Number.class, sql);
		result = num != null && num.intValue() > 0;

		return result;
	}
}
