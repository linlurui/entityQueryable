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
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public interface IDBExecutor {

    DataSource getDatasource() throws SQLException;

    Connection getConnection() throws SQLException;

    Integer execute(String sql ) throws SQLException;

    Integer execute( String sql, Map<Integer, Blob> blobMap ) throws SQLException;

    <E> List<E> query(Class<E> returnType, String sql) throws SQLException ;

    Integer count(String sql ) throws SQLException ;

    <E> E first(Class<E> returnType, String sql) throws SQLException;

    <E> List<?> batchQuery(List<String> sqls, Class<E> returnType ) throws SQLException;

    <E> List<E> batchExecute( List<String> sqls, List<Map<Integer, Blob>> blobList ) throws SQLException;

    <E> Single<E> single(Class<E> returnType, String sql);

    <E> Maybe<E> maybe(Class<E> returnType, String sql);

    <E> Flowable<E> flowable(Class<E> returnType, String sql);

    Flowable<Integer> flowable(String sql, Map<Integer, Blob> blobMap);
}
