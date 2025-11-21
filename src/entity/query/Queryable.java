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
import entity.query.core.cache.QueryCacheManager;
import entity.query.core.executor.DBExecutorAdapter;
import entity.query.core.lifecycle.EntityLifecycleContext;
import entity.query.core.lifecycle.EntityLifecycleEventType;
import entity.query.core.lifecycle.EntityLifecycleManager;
import entity.query.enums.CommandMode;
import entity.query.enums.DBType;
import entity.tool.util.*;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import static entity.tool.util.DBUtils.*;
import static entity.tool.util.StringUtils.isEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Queryable<T> extends QueryableBase<T> implements Serializable {

	private static final Logger log = LoggerFactory.getLogger(Queryable.class);

	@SuppressWarnings("unchecked")
	public Queryable() {
		super();
		init(getTypeClass(), this, null);
	}

	private Class getTypeClass() {
		Pattern regex = Pattern.compile("\\$\\d+$");
		if(regex.matcher(this.getClass().getName()).find()) {
			return this.getClass().getSuperclass();
		}
		return this.getClass();
	}

	public Queryable(DBTransaction transaction) {
		super(transaction);
		init(getTypeClass(), this, null);
	}

	/**
	 * 静态链式查询入口，自动根据实体继承规则推导表名。
	 * 等价于：new User().from(user.tablename())
	 */
	public static <T extends Queryable<T>> From<T> from(Class<T> clazz) {
		if(clazz == null) {
			throw new IllegalArgumentException("Class can not be null");
		}

		T entity = ReflectionUtils.getInstance(clazz);
		String tableName = entity.tablename();
		return entity.from(tableName);
	}

	public static <T extends Queryable<T>> From<T> from(Class<T> clazz, String tableName) {
		return from(clazz, tableName, null);
	}

	public static <T extends Queryable<T>> From<T> from(Class<T> clazz, String tableName, String alias) {
		if(clazz == null) {
			throw new IllegalArgumentException("Class can not be null");
		}
		T entity = ReflectionUtils.getInstance(clazz);
		return entity.from(tableName, alias);
	}

	public From<T> from(QueryableAction<T> queryable, String alias) {
		From<T> clause = new From<T>();
		clause.init(this.genericType, this.entityObject(), this);
		StringBuilder sb = new StringBuilder("( ");
		sb.append(queryable.toString(CommandMode.Select)).append(" )");
		clause.getParser().addFrom(sb.toString(), alias);

		return clause;
	}

	public <E> From<T> from(String tableName) {
		return from(tableName, null);
	}

	public <E> From<T> from(String tableName, String alias) {
	    From<T> clause = new From<T>();
	    clause.init(this.genericType, this.entityObject(), this);
	    clause.getParser().addFrom(tableName, alias);

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

	public String getViewDefined() throws SQLException {
		String sql = this.toString(CommandMode.GetViewSql);
		Map<String, String> result = DBExecutorAdapter.createExecutor(this, getGenericType()).first(Map.class, sql);
		if(result!=null && result.containsKey("sql")) {
			return result.get("sql");
		}
		return null;
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

	public PreparedSql toPreparedSql() {
		return toPreparedSql(CommandMode.Select);
	}

	public PreparedSql toPreparedSql(CommandMode commandMode) {
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		return getParser().getPreparedSql(this.genericType, "", commandMode, this.entityObject(), 0, 0, false, blobMap);
	}

	public String toString(CommandMode commandMode, DBType dbType) {
		if(dbType == null) {
			return toString(commandMode);
		}
		if(!(getParser() instanceof SqlParserBase)) {
			throw new IllegalStateException("Current parser does not support DBType overrides.");
		}
		try {
			SqlContainer clone = cloneContainer(((SqlParserBase) getParser()).getContainer());
			ISqlParser overrideParser = SqlParserFactory.createParser(dbType, clone);
			Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
			return overrideParser.toString(this.genericType, "", commandMode, this.entityObject(), 0, 0, false, blobMap);
		} catch (Exception e) {
			log.error("Failed to create parser for {}: {}", dbType, e.getMessage(), e);
			throw new IllegalStateException(String.format("Unsupported DBType %s", dbType), e);
		}
	}

	private SqlContainer cloneContainer(SqlContainer source) {
		if(source == null) {
			return new SqlContainer();
		}
		SqlContainer target = new SqlContainer();
		target.Where.append(source.Where.toString());
		target.OrderBy.append(source.OrderBy.toString());
		target.GroupBy.append(source.GroupBy.toString());
		target.Select.append(source.Select.toString());
		target.Union.append(source.Union.toString());
		target.From.append(source.From.toString());
		target.Join.addAll(source.Join);
		target.On.addAll(source.On);
		return target;
	}

	public Integer insert() throws SQLException {
		try {
			Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
			String sql = getParser().toString(this.genericType, "", CommandMode.Insert, this.entityObject(), 0, 0, false, blobMap);
            EntityLifecycleContext context = buildLifecycleContext(CommandMode.Insert, sql);
            EntityLifecycleManager.fire(EntityLifecycleEventType.PRE_PERSIST, this, context);
			Integer id = DBExecutorAdapter.createExecutor(this).execute(sql, blobMap);
            if(id != null && id > 0) {
                QueryCacheManager.clearAll();
                EntityLifecycleManager.fire(EntityLifecycleEventType.POST_PERSIST, this, context);
            }
            return (id == null) ? 0 : id;
		} catch (Exception e) {
			log.error("Insert failed: " + e.getMessage(), e);
			throw new SQLException("Insert operation failed", e);
		}
	}

	public boolean delete() throws SQLException {
		try {
			String sql = getParser().toString(this.genericType, "", CommandMode.Delete, this.entityObject(), 0, 0, false, null);
            EntityLifecycleContext context = buildLifecycleContext(CommandMode.Delete, sql);
            EntityLifecycleManager.fire(EntityLifecycleEventType.PRE_REMOVE, this, context);
			Integer row = DBExecutorAdapter.createExecutor(this).execute(sql, null);
            boolean success = row != null && row.intValue() > 0;
            if(success) {
                QueryCacheManager.clearAll();
                EntityLifecycleManager.fire(EntityLifecycleEventType.POST_REMOVE, this, context);
            }
            return success;
		} catch (Exception e) {
			log.error("Delete failed: " + e.getMessage(), e);
			throw new SQLException("Delete operation failed", e);
		}
	}

	public boolean update() throws SQLException {
		try {
			Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
			String sql = getParser().toString(this.genericType, "", CommandMode.Update, this.entityObject(), 0, 0, false, blobMap);
            EntityLifecycleContext context = buildLifecycleContext(CommandMode.Update, sql);
            EntityLifecycleManager.fire(EntityLifecycleEventType.PRE_UPDATE, this, context);
			Integer row = DBExecutorAdapter.createExecutor(this).execute(sql, blobMap);
            boolean success = row != null && row.intValue() > 0;
            if(success) {
                QueryCacheManager.clearAll();
                EntityLifecycleManager.fire(EntityLifecycleEventType.POST_UPDATE, this, context);
            }
            return success;
		} catch (Exception e) {
			log.error("Update failed: " + e.getMessage(), e);
			throw new SQLException("Update operation failed", e);
		}
	}

	public boolean update(String... exp) throws SQLException, IllegalAccessException {

		Field[] flds = entity.tool.util.FieldCache.getCachedDeclaredFields(getTypeClass());
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

		StringBuilder expTextBuilder = new StringBuilder();
		for (int i=0; i<exp.length; i++) {
			if(i>0) {
				expTextBuilder.append(", ");
			}
			expTextBuilder.append(DBUtils.getSqlInjText(exp[i]));
		}
		String expText = expTextBuilder.toString();

		try {
			Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
			String sql = ac.getParser().toString(this.genericType, expText, CommandMode.UpdateFrom, this.entityObject(), 0, 0, false, blobMap);
            EntityLifecycleContext context = buildLifecycleContext(CommandMode.Update, sql);
            EntityLifecycleManager.fire(EntityLifecycleEventType.PRE_UPDATE, this, context);
			Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, blobMap);
            boolean success = row != null && row.intValue() > 0;
            if(success) {
                QueryCacheManager.clearAll();
                EntityLifecycleManager.fire(EntityLifecycleEventType.POST_UPDATE, this, context);
            }
            return success;
		} catch (Exception e) {
			log.error("Update failed: " + e.getMessage(), e);
			throw new SQLException("Update operation failed", e);
		}
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
        QueryCacheManager.clearAll();
	}

	public void batchUpdate(List<T> list, Callback<List<T>> call, String... exp) throws Exception {
        batchTask(list, this.genericType, this, getParser(), CommandMode.Update, exp, call);
        QueryCacheManager.clearAll();
	}

	public void batchDelete(List<T> list, Callback<List<T>> call) throws Exception {
        batchTask(list, this.genericType, this, getParser(), CommandMode.Delete, null, call);
        QueryCacheManager.clearAll();
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
		if(StringUtils.isNotEmpty(sql)) {
			DBExecutorAdapter.createExecutor(dataSource).execute(sql);
		}

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

    private EntityLifecycleContext buildLifecycleContext(CommandMode mode, String sql) {
        long now = System.currentTimeMillis();
        return EntityLifecycleContext.create(mode, dataSource(), sql, null, false, now, now);
    }
}
