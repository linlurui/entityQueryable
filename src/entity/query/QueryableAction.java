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
import entity.query.core.executor.DBExecutorAdapter;
import entity.query.enums.CommandMode;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
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

    private ISqlParser parser;

    protected ISqlParser getParser()
    {
        return parser;
    }

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
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, 0, false, null );
        List<T> result = DBExecutorAdapter.createExecutor(this, getGenericType()).query(getGenericType(), sql);
        return result;
    }

    public <E> List<E> query( Class<E> type ) throws SQLException {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, 0, false, null );
        List<E> result = DBExecutorAdapter.createExecutor(this, getGenericType()).query(type, sql);
        return result;
    }

    public List<T> query( int skip, int top ) throws SQLException {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, skip, top, false, null );
        List<T> result = DBExecutorAdapter.createExecutor(this, getGenericType()).query(getGenericType(), sql);
        return result;
    }

    public <E> List<E> query( Class<E> type, int skip, int top ) throws SQLException {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, skip, top, false, null );
        List<E> result = DBExecutorAdapter.createExecutor(this, getGenericType()).query(type, sql);
        return result;
    }

    public long count() throws SQLException {
        String sql = getParser().toString( this.genericType, "", CommandMode.SelectCount, this.entityObject, 0, 0, true, null );
        Number result = DBExecutorAdapter.createExecutor(this, getGenericType()).first(Number.class, sql);
        return null==result ? 0 : result.longValue();
    }

    public T first() throws SQLException {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, 1, false, null );
        T result = DBExecutorAdapter.createExecutor(this, getGenericType()).first(getGenericType(), sql);
        return result;
    }

    public <E> E first( Class<E> type ) throws SQLException {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, 1, false, null );
        E result = DBExecutorAdapter.createExecutor(this, getGenericType()).first(type, sql);
        return result;
    }

    public List<T> top( int count ) throws SQLException {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, count, false, null );
        List<T> result = DBExecutorAdapter.createExecutor(this, getGenericType()).query(getGenericType(), sql);
        return result;
    }

    public <E> List<E> top( Class<E> type, int count ) throws SQLException {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, count, false, null );
        List<E> result = DBExecutorAdapter.createExecutor(this, getGenericType()).query(type, sql);
        return result;
    }

    public boolean exist() throws SQLException {
        String sql = getParser().toString( this.genericType, "", CommandMode.Select, this.entityObject, 0, 1, false, null );
        T result = DBExecutorAdapter.createExecutor(this, getGenericType()).first(getGenericType(), sql);
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
}
