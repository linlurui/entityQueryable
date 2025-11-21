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
import entity.query.core.cache.QueryCacheManager;
import entity.query.core.diagnostics.QueryDiagnostics;
import entity.query.core.executor.DBExecutorAdapter;
import entity.query.core.xml.XmlSqlLoader;
import entity.query.core.lifecycle.EntityLifecycleContext;
import entity.query.core.lifecycle.EntityLifecycleEventType;
import entity.query.core.lifecycle.EntityLifecycleManager;
import entity.query.core.lazy.LazyLoaderRegistry;
import entity.query.core.lazy.LazyReference;
import entity.query.enums.CommandMode;
import entity.query.enums.DBType;
import entity.tool.util.DBUtils;
import entity.tool.util.JsonUtils;
import entity.tool.util.StringUtils;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class QueryableAction<T> implements IDataActuator
{
	private static final Logger log = LoggerFactory.getLogger(Queryable.class);

    protected QueryableAction(){
    }

    protected QueryableAction(DBTransaction transaction){
        this.transaction = transaction;
    }

    protected void init( Class<T> clazz, Object obj, ISqlParser ps, IDataActuator iDataActuator ) {
        genericType = clazz;
        parser = ps;
        entityObject = obj;
        if ( iDataActuator != null )
        {
            this.dataSource = iDataActuator.dataSource();
            this.connection = iDataActuator.getConnection();
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
    private Object entityObject;

    public Object entityObject()
    {
        return entityObject;
    }

    @SuppressWarnings("unchecked")
	public <T1> T1 entityObject(Class<T1> clazz)
    {
    	return (T1) entityObject();
    }

    public <T1> QueryableAction<T> entityObject(T1 obj)
    {
    	entityObject = obj;
    	return this;
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
    private ISqlParser parser;

    protected ISqlParser getParser()
    {
        return parser;
    }

    @ExcelIgnore
    @JsonIgnore
    @JSONField(serialize = false)
    protected Class<T> genericType;

    protected Class<T> getGenericType()
    {
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

    public <E> From<T> as( String alias )
    {
        From<T> clause = new From<T>();
        clause.init( getGenericType(), entityObject(), this );
        clause.getParser().addFrom( getGenericType(), alias );

        return clause;
    }

    public List<T> query() throws SQLException {
        return executeListQuery(getGenericType(), CommandMode.Select, 0, 0);
    }

    public <E> List<E> query( Class<E> type ) throws SQLException {
        return executeListQuery(type, CommandMode.Select, 0, 0);
    }

    public List<T> query( int skip, int top ) throws SQLException {
        return executeListQuery(getGenericType(), CommandMode.Select, skip, top);
    }

    public <E> List<E> query( Class<E> type, int skip, int top ) throws SQLException {
        return executeListQuery(type, CommandMode.Select, skip, top);
    }

    public long count() throws SQLException {
        Number result = executeSingleQuery(Number.class, CommandMode.SelectCount, 0, 0);
        return null==result ? 0 : result.longValue();
    }

    public T first() throws SQLException {
        return executeSingleQuery(getGenericType(), CommandMode.Select, 0, 1);
    }

    public <E> E first( Class<E> type ) throws SQLException {
        return executeSingleQuery(type, CommandMode.Select, 0, 1);
    }

    public List<T> top( int count ) throws SQLException {
        return executeListQuery(getGenericType(), CommandMode.Select, 0, count);
    }

    public <E> List<E> top( Class<E> type, int count ) throws SQLException {
        return executeListQuery(type, CommandMode.Select, 0, count);
    }

    public boolean exist() throws SQLException {
        T result = executeSingleQuery(getGenericType(), CommandMode.Select, 0, 1);
        return ( result == null? false: true );
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
        sql = sql.substring( 0, sql.length() - 1 );

        return sql;
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

    public PreparedSql getPreparedSql() {
        return getPreparedSql(CommandMode.Select);
    }

    public PreparedSql getPreparedSql(CommandMode commandMode) {
        Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
        return getParser().getPreparedSql(this.genericType, "", commandMode, this.entityObject(), 0, 0, false, blobMap);
    }

    private <E> List<E> executeListQuery(final Class<E> type, final CommandMode commandMode, final int skip, final int top) throws SQLException {
        final String sql = getParser().toString(this.genericType, "", commandMode, this.entityObject, skip, top, false, null);
        return executeWithFeatures(type, commandMode, sql, new SqlExecutor<List<E>>() {
            @Override
            public List<E> apply() throws SQLException {
                return DBExecutorAdapter.createExecutor(QueryableAction.this, getGenericType()).query(type, sql);
            }
        });
    }

    private <E> E executeSingleQuery(final Class<E> type, final CommandMode commandMode, final int skip, final int top) throws SQLException {
        final boolean useCountFlag = CommandMode.SelectCount.equals(commandMode);
        final String sql = getParser().toString(this.genericType, "", commandMode, this.entityObject, skip, top, useCountFlag, null);
        return executeWithFeatures(type, commandMode, sql, new SqlExecutor<E>() {
            @Override
            public E apply() throws SQLException {
                return DBExecutorAdapter.createExecutor(QueryableAction.this, getGenericType()).first(type, sql);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <R> R executeWithFeatures(final Class<?> cacheType,
                                      final CommandMode commandMode,
                                      final String sql,
                                      final SqlExecutor<R> executor) throws SQLException {
        boolean cacheHit = false;
        String cacheKey = null;
        R result;
        QueryDiagnostics.Trace trace = QueryDiagnostics.start(sql);
        try {
            if(QueryCacheManager.isEnabled(commandMode)) {
                cacheKey = QueryCacheManager.buildKey(getCurrentDataSourceId(), cacheType, commandMode, sql);
                Object cached = QueryCacheManager.get(cacheKey);
                if(cached != null) {
                    cacheHit = true;
                    result = (R) cached;
                } else {
                    result = executor.apply();
                    QueryCacheManager.put(cacheKey, cloneResultObject(result), cacheType);
                }
            } else {
                result = executor.apply();
            }
        } catch (SQLException e) {
            trace.finish(cacheHit);
            throw e;
        } catch (RuntimeException e) {
            trace.finish(cacheHit);
            throw e;
        }
        long finishedAt = System.currentTimeMillis();
        trace.finish(cacheHit);
        EntityLifecycleContext context = EntityLifecycleContext.create(
                commandMode,
                dataSource(),
                sql,
                trace.getTraceId(),
                cacheHit,
                trace.getStartedAt(),
                finishedAt);
        dispatchPostLoadHooks(result, context);
        return result;
    }

    private void dispatchPostLoadHooks(Object result, EntityLifecycleContext context) {
        dispatchPostLoadHooksStatic(result, context);
    }

    private static void dispatchPostLoadHooksStatic(Object result, EntityLifecycleContext context) {
        if(result == null) {
            return;
        }
        if(result instanceof List) {
            List<?> list = (List<?>) result;
            for(Object item : list) {
                firePostLoadStatic(item, context);
            }
            return;
        }
        firePostLoadStatic(result, context);
    }

    private static void firePostLoadStatic(Object entity, EntityLifecycleContext context) {
        if(entity == null) {
            return;
        }
        EntityLifecycleManager.fire(EntityLifecycleEventType.POST_LOAD, entity, context);
        LazyLoaderRegistry.attach(entity, context);
    }

    private String getCurrentDataSourceId() {
        DataSource ds = dataSource();
        if(ds == null) {
            return "default";
        }
        if(StringUtils.isNotEmpty(ds.getId())) {
            return ds.getId();
        }
        if(StringUtils.isNotEmpty(ds.getUrl())) {
            return ds.getUrl();
        }
        return "default";
    }

    private static Object cloneResultObject(Object source) {
        if(source == null) {
            return null;
        }
        if(source instanceof List) {
            List<?> list = (List<?>) source;
            List<Object> cloned = new ArrayList<Object>(list.size());
            for(Object item : list) {
                cloned.add(cloneEntity(item));
            }
            return cloned;
        }
        return cloneEntity(source);
    }

    private static Object cloneEntity(Object source) {
        if(source == null) {
            return null;
        }
        if(isImmutableType(source.getClass())) {
            if(source instanceof Date) {
                return new Date(((Date) source).getTime());
            }
            return source;
        }
        if(source instanceof LazyReference) {
            return source;
        }
        if(source instanceof Map) {
            return new HashMap<Object, Object>((Map<?, ?>) source);
        }
        try {
            return JsonUtils.convert(source, source.getClass());
        } catch (Exception e) {
            return source;
        }
    }

    private static boolean isImmutableType(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || String.class.equals(type)
                || Boolean.class.equals(type)
                || Date.class.equals(type);
    }

    private static String buildParameterSignature(Object[] parameters) {
        if(parameters == null || parameters.length == 0) {
            return "";
        }
        StringBuilder signature = new StringBuilder();
        for(int i = 0; i < parameters.length; i++) {
            if(i > 0) {
                signature.append("|");
            }
            Object parameter = parameters[i];
            signature.append(parameter == null ? "null" : parameter.toString());
        }
        return signature.toString();
    }

    private static String getDataSourceId(DataSource dataSource) {
        if(dataSource == null) {
            return "default";
        }
        if(StringUtils.isNotEmpty(dataSource.getId())) {
            return dataSource.getId();
        }
        if(StringUtils.isNotEmpty(dataSource.getUrl())) {
            return dataSource.getUrl();
        }
        return "default";
    }

    private interface SqlExecutor<R> {
        R apply() throws SQLException;
    }

    public static <E> List<E> executeSql(String sql, Class<E> resultType, Object... parameters) throws SQLException {
        return executeSql((String) null, sql, resultType, parameters);
    }

    public static <E> List<E> executeSql(String dataSourceId, String sql, Class<E> resultType, Object... parameters) throws SQLException {
        DataSource targetDataSource = resolveDataSource(dataSourceId, resultType);
        return executeSql(targetDataSource, sql, resultType, parameters);
    }

    public static <E> List<E> executeSql(DataSource dataSource, String sql, Class<E> resultType, Object... parameters) throws SQLException {
        return executeSqlInternal(dataSource, sql, resultType, parameters);
    }

    public static <E> List<E> executeSqlFromXml(String statementId, Class<E> resultType, Object parameterObject) throws SQLException {
        return executeSqlFromXml((String) null, statementId, resultType, parameterObject);
    }

    public static <E> List<E> executeSqlFromXml(String dataSourceId, String statementId, Class<E> resultType, Object parameterObject) throws SQLException {
        DataSource targetDataSource = resolveDataSource(dataSourceId, resultType);
        return executeSqlFromXml(targetDataSource, statementId, resultType, parameterObject);
    }

    public static <E> List<E> executeSqlFromXml(DataSource dataSource, String statementId, Class<E> resultType, Object parameterObject) throws SQLException {
        if(StringUtils.isEmpty(statementId)) {
            throw new IllegalArgumentException("Statement id can not be empty");
        }
        if(resultType == null) {
            throw new IllegalArgumentException("Return type can not be null");
        }
        if(dataSource == null) {
            throw new IllegalArgumentException("Data source can not be null");
        }
        PreparedSql preparedSql = XmlSqlLoader.getPreparedSql(statementId, parameterObject);
        List<Object> parameters = preparedSql.getParameters();
        Object[] args = parameters == null ? new Object[0] : parameters.toArray(new Object[parameters.size()]);
        return executeSqlInternal(dataSource, preparedSql.getSql(), resultType, args);
    }

    public String toString( CommandMode mode, int skip, int top )
    {
        String sql = getParser().toString( this.genericType, "", mode, this.entityObject, skip, top, false, null );

        return sql.substring( 0, sql.length() - 1 );
    }


    public Flowable<T> asyncQuery() throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, 0, false, null );
        Flowable<T> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(getGenericType(), sql);
        return flowable;
    }

    public <E> Flowable<E> asyncQuery(Class<E> type ) throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, 0, false, null );
        Flowable<E> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(type, sql);
        return flowable;
    }

    public Flowable<T> asyncQuery(int skip, int top ) throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, skip, top, false, null );
        Flowable<T> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(getGenericType(), sql);
        return flowable;
    }

    public <E> Flowable<E> asyncQuery(Class<E> type, int skip, int top ) throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, skip, top, false, null );
        Flowable<E> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(type, sql);
        return flowable;
    }

    public Single<Long> asyncCount() throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.SelectCount, this.entityObject, 0, 0, true, null );
        Flowable<Long> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(Long.class, sql);
        return flowable.first(Long.valueOf(0));
    }

    public Maybe<T> asyncFirst() throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, 1, false, null );
        return DBExecutorAdapter.createExecutor(this, getGenericType()).maybe(getGenericType(), sql);
    }

    public <E> Maybe<E> asyncFirst(Class<E> type ) throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, 1, false, null );
        return DBExecutorAdapter.createExecutor(this, getGenericType()).maybe(type, sql);
    }

    public Flowable<T> asyncTop( int count ) throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, count, false, null );
        Flowable<T> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(getGenericType(), sql);
        return flowable;
    }

    public <E> Flowable<E> asyncTop(Class<E> type, int count ) throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, count, false, null );
        Flowable<E> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(type, sql);
        return flowable;
    }

    public Single<T> asyncExist() throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, 1, false, null );
        Flowable<T> result = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(getGenericType(), sql);
        return result.singleOrError();
    }

    public Single<Boolean> isEmpty() throws Exception {
        String sql = getParser().toString( this.genericType, "", CommandMode.Exist, this.entityObject, 0, 1, false, null );
        Flowable<T> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(getGenericType(), sql);
        return flowable.isEmpty();
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

    private static void bindParameters(PreparedStatement statement, Object[] parameters) throws SQLException {
        if(parameters == null || parameters.length == 0) {
            return;
        }
        for(int i = 0; i < parameters.length; i++) {
            Object parameter = parameters[i];
            int index = i + 1;
            if(parameter == null) {
                statement.setNull(index, Types.NULL);
                continue;
            }
            if(parameter instanceof Date && !(parameter instanceof java.sql.Date) &&
                    !(parameter instanceof Timestamp) && !(parameter instanceof Time)) {
                statement.setTimestamp(index, new Timestamp(((Date) parameter).getTime()));
                continue;
            }
            if(parameter instanceof Blob) {
                statement.setBlob(index, (Blob) parameter);
                continue;
            }
            if(parameter instanceof byte[]) {
                statement.setBytes(index, (byte[]) parameter);
                continue;
            }
            if(parameter.getClass().isEnum()) {
                statement.setObject(index, parameter.toString());
                continue;
            }
            statement.setObject(index, parameter);
        }
    }

    private static DataSource resolveDataSource(String dataSourceId, Class<?> resultType) throws SQLException {
        try {
            DataSourceFactory factory = DataSourceFactory.getInstance();
            if(StringUtils.isNotEmpty(dataSourceId)) {
                return factory.getDataSource(dataSourceId);
            }
            if(resultType != null) {
                return factory.getDataSource(resultType);
            }
            return factory.getDataSource();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Unable to resolve data source", e);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if(closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignore) {
            log.debug("Ignore close failure: {}", ignore.getMessage());
        }
    }

    private static <E> List<E> executeSqlInternal(DataSource dataSource, String sql, Class<E> resultType, Object... parameters) throws SQLException {
        if(StringUtils.isEmpty(sql)) {
            throw new IllegalArgumentException("Sql statement can not be empty");
        }
        if(resultType == null) {
            throw new IllegalArgumentException("Return type can not be null");
        }
        if(dataSource == null) {
            throw new IllegalArgumentException("Data source can not be null");
        }
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        boolean cacheHit = false;
        String cacheKey = null;
        QueryDiagnostics.Trace trace = QueryDiagnostics.start(sql);
        if(QueryCacheManager.isEnabled(CommandMode.Select)) {
            cacheKey = QueryCacheManager.buildKey(getDataSourceId(dataSource), resultType, CommandMode.Select, sql + buildParameterSignature(parameters));
            List<E> cached = QueryCacheManager.get(cacheKey);
            if(cached != null) {
                cacheHit = true;
                long finishedAt = System.currentTimeMillis();
                trace.finish(true);
                EntityLifecycleContext cachedContext = EntityLifecycleContext.create(CommandMode.Select, dataSource, sql, trace.getTraceId(), true, trace.getStartedAt(), finishedAt);
                dispatchPostLoadHooksStatic(cached, cachedContext);
                return cached;
            }
        }
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(sql);
            bindParameters(statement, parameters);
            resultSet = statement.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            List<E> results = new ArrayList<E>();
            while(resultSet.next()) {
                results.add(DBUtils.getMetaData(resultSet, metaData, resultType));
            }
            if(cacheKey != null) {
                QueryCacheManager.put(cacheKey, cloneResultObject(results), resultType);
            }
            long finishedAt = System.currentTimeMillis();
            trace.finish(cacheHit);
            EntityLifecycleContext context = EntityLifecycleContext.create(CommandMode.Select, dataSource, sql, trace.getTraceId(), cacheHit, trace.getStartedAt(), finishedAt);
            dispatchPostLoadHooksStatic(results, context);
            return results;
        } catch (SQLException e) {
            trace.finish(cacheHit);
            log.error("Failed to execute sql: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            trace.finish(cacheHit);
            log.error("Failed to execute sql: {}", e.getMessage(), e);
            throw new SQLException("Failed to execute sql", e);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

}
