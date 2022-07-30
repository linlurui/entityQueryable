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


import entity.query.core.DataSource;
import entity.query.core.IDataActuator;
import entity.tool.util.DBUtils;
import entity.tool.util.StringUtils;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.davidmoten.rx.jdbc.*;
import org.davidmoten.rx.jdbc.pool.NonBlockingConnectionPool;
import org.davidmoten.rx.jdbc.pool.Pools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static entity.tool.util.DBUtils.blobToBytes;

public class RxJava2JdbcExecutor implements IDBExecutor {

    private Database db;

    private static final Logger log = LoggerFactory.getLogger( RxJava2JdbcExecutor.class );

    private IDataActuator dataActuator;

    @Override
    public DataSource getDatasource() throws SQLException {
        return dataActuator.dataSource();
    }

    public RxJava2JdbcExecutor(IDataActuator dataActuator) {
        try {
            this.dataActuator = dataActuator;
            NonBlockingConnectionPool pool =
                    Pools.nonBlocking()
                            .connectionProvider(dataActuator.dataSource())
                            .build();

            this.db = Database.from(pool);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {

        if(dataActuator.dataSource().getTransaction() != null) {
            return dataActuator.dataSource().getTransaction().getConnection();
        }

        if(dataActuator.getConnection() != null) {
            return dataActuator.getConnection();
        }

        return dataActuator.dataSource().getConnection();
    }

    @Override
    public Integer execute(String sql ) throws SQLException
    {
        return execute(sql, null);
    }

    @Override
    public Integer execute( String sql, Map<Integer, Blob> blobMap ) throws SQLException
    {
        return getValue(flowable(sql, blobMap));
    }

    @Override
    public <E> List<E> query( Class<E> returnType, String sql ) throws SQLException {
        List<E> result = getList(flowable(returnType, sql));

        return result;
    }

    @Override
    public Integer count(String sql ) throws SQLException {

        if(StringUtils.isEmpty(sql)) {
            throw new SQLException("Sql statement can not be empty!!!");
        }

        Flowable<Integer> flowable =
                db.select(sql).getAs(Integer.class);

        return getValue(flowable);
    }

    @Override
    public <E> E first(Class<E> returnType, String sql) throws SQLException {

        return getValue(flowable(returnType, sql));
    }

    @Override
    public <E> List<?> batchQuery(List<String> sqls, Class<E> returnType ) throws SQLException
    {
        if(returnType.equals(Map.class)) {

            List<List> results = new ArrayList<List>();
            for(String sql : sqls) {
                results.add(getList(flowable(returnType, sql)));
            }

            return results;
        }

        List<E> results2 = new ArrayList<E>();
        for(String sql : sqls) {
            List<E> list = getList(flowable(returnType, sql));
            if(list==null) {
                continue;
            }

            for (E item : list) {
                results2.add(item);
            }
        }

        return results2;
    }

    @Override
    public <E> List<E> batchExecute( List<String> sqls, List<Map<Integer, Blob>> blobList ) throws SQLException
    {
        List<E> results = new ArrayList<E>();
        for(int i=0; i < sqls.size(); i++) {

            Map<Integer, Blob> blobMap = null;
            if(i < blobList.size()) {
                blobMap = blobList.get(i);
            }
            String sql = sqls.get(i).toLowerCase();
            if("ORACLE".equals(dataActuator.dataSource().getDbType())) {
                Matcher m = Pattern.compile("insert\\s+into\\s*((\\w[\\w\\d]*)\\s*\\([,\"\\w]*(id)\\s*[,\\)])").matcher(sql);
                if(m.find()){
                    sql = m.replaceAll("INSERT IGNORE_ROW_ON_DUPKEY_INDEX($2($3)) INTO $1");
                }
            }
            else if("MARIADB".equalsIgnoreCase(dataActuator.dataSource().getDbType()) || "MYSQL".equalsIgnoreCase(dataActuator.dataSource().getDbType())) {
                sql = sql.replaceAll("insert\\s+into", "insert\\s+ignore\\s+into");
            }
            else if("SQLITE".equalsIgnoreCase(dataActuator.dataSource().getDbType()) || "MYSQL".equalsIgnoreCase(dataActuator.dataSource().getDbType())) {
                sql = sql.replaceAll("insert\\s+or\\s+into", "insert\\s+ignore\\s+into");
            }
            try {
                results.add((E)execute(sql, blobMap));
            }
            catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }

        return results;
    }

    @Override
    public <E> Single<E> single(Class<E> returnType, String sql) {
        E e = null;
        return flowable(returnType, sql).single(e);
    }

    @Override
    public <E> Maybe<E> maybe(Class<E> returnType, String sql) {
        E e = null;
        return flowable(returnType, sql).single(e).toMaybe();
    }

    @Override
    public <E> Flowable<E> flowable(final Class<E> returnType, String sql) {

        if(StringUtils.isEmpty(sql)) {
            return Flowable.empty();
        }

        if(returnType.isInterface() && !returnType.equals(Map.class)) {
            return db.select(sql).autoMap(returnType);
        }

        else if(returnType.equals(Map.class)) {
            Flowable<HashMap> flowable =
                    db.select(sql).get(new ResultSetMapper<HashMap>() {
                        @Override
                        public HashMap apply(@Nonnull ResultSet rs) throws SQLException {
                            ResultSetMetaData rsmd = rs.getMetaData();
                            int columnCount = rsmd.getColumnCount();

                            HashMap<String, Object> result = new HashMap<String, Object>();
                            for ( int i = 0; i < columnCount; i++ ) {
                                String columnName = rsmd.getColumnLabel( i + 1 );
                                Object columnValue = null;
                                try{
                                    columnValue = rs.getObject( columnName );
                                } catch (SQLException ex){}
                                result.put( columnName, columnValue );
                            }

                            return result;
                        }
                    });

            return (Flowable<E>) flowable;
        }

        Flowable<E> flowable =
                db.select(sql).get(new ResultSetMapper<E>() {
                    @Override
                    public E apply(@Nonnull ResultSet rs) throws SQLException {
                        return DBUtils.getMetaData(rs, rs.getMetaData(), returnType);
                    }
                });


        return flowable;
    }

    @Override
    public Flowable<Integer> flowable(String sql, Map<Integer, Blob> blobMap) {
        if(StringUtils.isEmpty(sql)) {
            return Flowable.empty();
        }

        UpdateBuilder queryable = db.update(sql);
        if(blobMap != null) {
            for(Blob param : blobMap.values()) {
                queryable = queryable.parameters(Database.blob(blobToBytes(param)));
            }
        }

        if(sql.toLowerCase().contains("insert")) {
            return queryable.returnGeneratedKeys().getAs(Integer.class);
        }

        return queryable.counts();
    }

    protected <E> List<E> getList(Flowable<E> flowable) {
        return flowable.toList().blockingGet();
    }

    protected <E> E getValue(Flowable<E> flowable) {
        return flowable.firstElement().blockingGet();
    }
}
