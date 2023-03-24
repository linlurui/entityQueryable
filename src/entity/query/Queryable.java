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
import entity.tool.util.*;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;

import static entity.tool.util.DBUtils.*;
import static entity.tool.util.StringUtils.isEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Queryable<T> extends QueryableBase<T> implements Serializable {

	private static final Logger log = LoggerFactory.getLogger(Queryable.class);

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
		clause.init(this.genericType, this.entityObject(), this);
		clause.getParser().addFrom(String.format( "( %s )", queryable.toString(CommandMode.Select) ), alias);

		return clause;
	}

	public <E> From<T> from(String tableName) {
	    From<T> clause = new From<T>();
	    clause.init(this.genericType, this.entityObject(), this);
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
		String sql = getParser().toString(this.genericType, "", commandMode, this.entityObject(), 0, 0, false, blobMap);

		return sql;
	}

	public Integer insert() throws SQLException {
		final Integer[] id = {0};
		final Queryable queryable = this;
		final Class<T> clazz = this.genericType;
		final Object obj = this.entityObject();
		if("SQLITE".equalsIgnoreCase(this.dataSource.getDbType())) {
			try {
				Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
				String sql = getParser().toString(clazz, "", CommandMode.Insert, obj, 0, 0, false, blobMap);
				id[0] = DBExecutorAdapter.createExecutor(queryable).execute(sql, blobMap);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return (id==null || id[0]==null) ? 0 : id[0];
		}
		ThreadUtils.onec(new Runnable() {
			@Override
			public void run() {
				try {
					Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
					String sql = getParser().toString(clazz, "", CommandMode.Insert, obj, 0, 0, false, blobMap);
					id[0] = DBExecutorAdapter.createExecutor(queryable).execute(sql, blobMap);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});
		return (id==null || id[0]==null) ? 0 : id[0];
	}

	public boolean delete() throws SQLException {
		final Integer[] row = {0};
		final Queryable queryable = this;
		final Class<T> clazz = this.genericType;
		final Object obj = this.entityObject();
		ThreadUtils.onec(new Runnable(){
			@Override
			public void run() {
				try {
					String sql = getParser().toString(clazz, "", CommandMode.Delete, obj, 0, 0, false, null);
					row[0] = DBExecutorAdapter.createExecutor(queryable).execute(sql, null);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});

		return row[0] !=null && row[0].intValue()>0;
	}

	public boolean update() throws SQLException {
		final Integer[] row = {0};
		final Queryable queryable = this;
		final Class<T> clazz = this.genericType;
		final Object obj = this.entityObject();

		ThreadUtils.onec(new Runnable(){
			@Override
			public void run() {
				try {
					Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
					String sql = getParser().toString(clazz, "", CommandMode.Update, obj, 0, 0, false, blobMap);
					row[0] = DBExecutorAdapter.createExecutor(queryable).execute(sql, blobMap);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});

		return row[0] !=null && row[0].intValue()>0;
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

		final QueryableAction ac = this.where(String.format("%s=#{%s}", fieldname, primaryKey.getName()));

		String expText = "";
		for (int i=0; i<exp.length; i++) {
			if(i>0) {
				expText = expText + ", ";
			}
			expText = expText + DBUtils.getSqlInjText( exp[i] );
		}

		final Integer[] row = {0};
		final Queryable queryable = this;
		final Class<T> clazz = this.genericType;
		final Object obj = this.entityObject();

		final String finalExpText = expText;
		ThreadUtils.onec(new Runnable(){
			@Override
			public void run() {
				try {
					Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
					String sql = ac.getParser().toString(clazz, finalExpText, CommandMode.UpdateFrom, obj, 0, 0, false, blobMap);
					row[0] = DBExecutorAdapter.createExecutor(queryable, getGenericType()).execute(sql, blobMap);
					sql = null;
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});
		expText = null;

		return row[0] !=null && row[0].intValue()>0;
	}

	public Flowable<Integer> asyncInsert() throws Exception {

		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(this.genericType, "", CommandMode.Insert, this.entityObject(), 0, 0, false, blobMap);

		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this).flowable(sql, blobMap);

		return flowable;
	}

	public Flowable<Integer> asyncDelete() throws Exception {
		String sql = getParser().toString(this.genericType, "", CommandMode.Delete, this.entityObject(), 0, 0, false, null);

		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this).flowable(sql, null);

		return flowable;
	}

	public Flowable<Integer> asyncUpdate() throws Exception {
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(this.genericType, "", CommandMode.Update, this.entityObject(), 0, 0, false, blobMap);

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
		batchTask(list, this.genericType, this, getParser(), CommandMode.Insert, null, call);
	}

	public void batchUpdate(List<T> list, Callback<List<T>> call, String... exp) throws Exception {
		batchTask(list, this.genericType, this, getParser(), CommandMode.Update, exp, call);
	}

	public void batchDelete(List<T> list, Callback<List<T>> call) throws Exception {
		batchTask(list, this.genericType, this, getParser(), CommandMode.Delete, null, call);
	}

	public static List<TableInfo> getTables(String dataSourceId) {
		return getTables(dataSourceId, TableInfo.class);
	}

    public static <S> List getTables(String dataSourceId, Class<S> clazz) {

		List<S> result = new ArrayList<S>();
		DataSource dataSource = null;

		try {
			dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);

			String sql = SqlParserFactory.createParser(dataSource).toString(null, "", CommandMode.Tables, null, 0, 0, false, null);

			result = DBExecutorAdapter.createExecutor(dataSource).query(clazz, sql);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
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
			log.error(e.getMessage(), e);
		}

		return result;
    }

    public static String getPrimaryKey(String dataSourceId, String tablename) {
		String result = null;

		DataSource dataSource = null;

		try {
			dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);
			String sql = SqlParserFactory.createParser(dataSource).toString(null, tablename, CommandMode.PrimaryKey, null, 0, 0, false, null);

			if( "sqlite".equalsIgnoreCase(dataSource.getDbType()) ) {
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
			log.error(e.getMessage(), e);
		}

		return result;
	}

	public static void createTable(String dataSourceId, String tablename, List<ColumnInfo> columns) throws Exception {

		if(isEmpty(tablename)) {
			throw new Exception("tablename can not be empty!");
		}

		DataSource dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);
		String sql = SqlParserFactory.createParser(dataSource).getCreateTableSql(tablename, columns);
		DBExecutorAdapter.createExecutor(dataSource).execute(sql);

	}

	public static void alterTable(String dataSourceId, String tablename, List<ColumnInfo> columns) throws Exception {

		if(isEmpty(tablename)) {
			throw new Exception("tablename can not be empty!");
		}

		DataSource dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);
		DBTransaction tran = dataSource.beginTransaction();
		String columnListSql = SqlParserFactory.createParser(dataSource).getColumnInfoListSql(tablename);
		List<ColumnInfo> oldColumns = new ArrayList<>();
		if("sqlite".equals(dataSource.getDbType())) {
			oldColumns = DBExecutorAdapter.createExecutor(dataSource).query(ColumnInfo.class, columnListSql);
		}
		String sql = SqlParserFactory.createParser(dataSource).getAlterTableSql(tablename, columns, oldColumns);
		List<String> sqlList = StringUtils.splitString2List(sql, ";");
		try {
			for (String stm : sqlList) {
				DBExecutorAdapter.createExecutor(dataSource).execute(String.format("%s;", stm));
			}
			dataSource.commit(tran.getConnection());
		}
		catch (Exception e) {
			dataSource.rollback();
			log.error(e.getMessage(), e);
		}
	}

	public static void dropTable(String dataSourceId, String tablename) throws Exception {

		if(isEmpty(tablename)) {
			throw new Exception("tablename can not be empty!");
		}

		DataSource dataSource = DataSourceFactory.getInstance().getDataSource(dataSourceId);
		String sql = SqlParserFactory.createParser(dataSource).getDropTableSql(tablename);
		DBExecutorAdapter.createExecutor(dataSource).execute(sql);

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
