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
import io.reactivex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdbcExecutor implements IDBExecutor {

    private static final Logger log = LoggerFactory.getLogger( JdbcExecutor.class );

    private IDataActuator dataActuator;

    public JdbcExecutor(IDataActuator dataActuator) {
        this.dataActuator = dataActuator;
    }

    @Override
    public DataSource getDatasource() throws SQLException {
        return dataActuator.dataSource();
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
        return DBUtils.execute(Integer.class, sql, blobMap, getConnection(), getDatasource());
    }

    @Override
    public <E> List<E> query( Class<E> returnType, String sql ) throws SQLException {
        List<E> result = DBUtils.query(returnType, returnType, sql, false, getConnection(), getDatasource());

        return result;
    }

    @Override
    public Integer count(String sql ) throws SQLException {

        if(StringUtils.isEmpty(sql)) {
            throw new SQLException("Sql statement can not be empty!!!");
        }

        List<Integer> result = DBUtils.query(Integer.class, Integer.class, sql, true, getConnection(), getDatasource());
        if(result==null || result.size()<1) {
            return 0;
        }

        return result.get(0);
    }

    @Override
    public <E> E first(Class<E> returnType, String sql) throws SQLException {
        List<E> result = DBUtils.query(returnType, returnType, sql, false, getConnection(), getDatasource());
        if(result==null || result.size()<1) {
            return null;
        }

        return result.get(0);
    }

    @Override
    public <E> List<?> batchQuery(List<String> sqls, Class<E> returnType ) throws SQLException
    {
        if(returnType.equals(Map.class)) {

            List<List> results = new ArrayList<List>();
            for(String sql : sqls) {
                results.add(DBUtils.query(returnType, returnType, sql, false, getConnection(), getDatasource()));
            }

            return results;
        }

        List<E> results2 = new ArrayList<E>();
        for(String sql : sqls) {
            List<E> list = DBUtils.query(returnType, returnType, sql, false, getConnection(), getDatasource());
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
            switch (dataActuator.dataSource().getDbType()) {
                case "ORACLE":
                    Matcher m = Pattern.compile("insert\\s+into\\s*((\\w[\\w\\d]*)\\s*\\([,\"\\w]*(id)\\s*[,\\)])").matcher(sql);
                    if(m.find()){
                        sql = m.replaceAll("INSERT IGNORE_ROW_ON_DUPKEY_INDEX($2($3)) INTO $1");
                    }
                    break;
                case "MARIADB":
                case "MYSQL":
                    sql = sql.replaceAll("insert\\s+into", "insert\\s+ignore\\s+into");
                    break;
                case "SQLITE":
                    sql = sql.replaceAll("insert\\s+or\\s+into", "insert\\s+ignore\\s+into");
                    break;
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
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return first(returnType, sql);
            }
        });
    }

    @Override
    public <E> Maybe<E> maybe(Class<E> returnType, String sql) {
        return Maybe.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return first(returnType, sql);
            }
        });
    }

    @Override
    public <E> Flowable<E> flowable(Class<E> returnType, String sql) {
        return Flowable
                .create(new FlowableOnSubscribe<E>() {
                    @Override
                    public void subscribe(FlowableEmitter<E> e) throws Exception {
                        List<E> result = DBUtils.query(returnType, returnType, sql, false, getConnection(), getDatasource());
                        if(result == null) {
                            return;
                        }

                        for(E item : result) {
                            e.onNext(item);
                        }
                        e.onComplete();
                    }
                }, BackpressureStrategy.BUFFER);
    }

    @Override
    public Flowable<Integer> flowable(String sql, Map<Integer, Blob> blobMap) {
        return Flowable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return DBUtils.execute(Integer.class, sql, getConnection());
            }
        });
    }
}
