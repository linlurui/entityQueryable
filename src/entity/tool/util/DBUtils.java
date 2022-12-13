/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.tool.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import entity.query.*;
import entity.query.core.*;
import entity.query.core.executor.BatchExecutor;
import entity.query.enums.CommandMode;
import org.davidmoten.rx.jdbc.exceptions.SQLRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;


public class DBUtils
{
    private static final Logger log = LoggerFactory.getLogger( DBUtils.class );

    public static String getSql( String sql, Object... args )
    {

        if ( args == null || args.length < 1 )
        {
            return sql;
        }

        sql = sql.replaceAll( RegexUtils.ArgsReplacement, "%s" );

        sql = String.format( sql, args );

        return sql;
    }

    public static String getStringValue(Object arg) {

        if ( arg == null )
        {
            return "null";
        }

        if ( arg instanceof String ) {
            if(arg == null) {
                return "null";
            }

            if(Pattern.matches("^\\{\"date\":\\d+,\"hours\":\\d+,\"seconds\":\\d+,\"month\":\\d+,\"timezoneOffset\":[\\-\\d+]+,\"year\":\\d+,\"minutes\":\\d+,\"time\":\\d+,\"day\":\\d+\\}$", arg.toString())) {
                try {
                    Map map = JsonUtils.parse(arg.toString(), Map.class);
                    String date = Datetime.format(new Date(Long.parseLong(map.get("time").toString())), "yyyy-MM-dd HH:mm:ss.SSS");
                    return String.format( "'%s'", date );

                } catch (JsonParseException e) {
                    e.printStackTrace();
                } catch (JsonMappingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return String.format( "'%s'", getSqlInjText( arg ) );
        }

        if ( arg instanceof Boolean )
        {
            if ( (Boolean)arg )
            {
                return "1";
            }

            else
            {
                return "0";
            }
        }

        if ( arg instanceof Date )
        {
            String val = (new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" )).format( (java.util.Date)arg );
            return String.format( "'%s'", val );
        }

        if ( arg instanceof UUID) {
            return arg.toString().replace("-", "");
        }

        if ( arg instanceof Blob) {
            return String.format("'%s'", blobToString((Blob)arg));
        }

        if ( arg instanceof Map) {
            if(arg == null) {
                return "''";
            }
            arg = JsonUtils.toJson(arg);
            if(arg == null) {
                return "''";
            }
            return String.format("'%s'", JsonUtils.toJson(arg));
        }

        if(arg.getClass().isEnum()) {
            try {
                Method getValueMethod = arg.getClass().getMethod("getValue");
                if(getValueMethod == null) {
                    return String.format( "'%s'", getSqlInjText( arg ) );
                }
                else {
                    return String.format( "'%s'", arg.getClass().getMethod("getValue").invoke(arg).toString() );
                }
            } catch (InvocationTargetException e) {
                log.warn(e.getMessage());
            } catch (IllegalAccessException e) {
                log.warn(e.getMessage());
            } catch (NoSuchMethodException e) {
                log.warn(e.getMessage());
            }
        }

        return getSqlInjText( arg );
    }

    /**
     * Blob字段的通用转换
     * 注意可能出现乱码
     * @return 转好的字符串，
     * **/
    public static String blobToString(Blob blob){
        StringBuffer str=new StringBuffer();
        //使用StringBuffer进行拼接
        InputStream in=null;//输入字节流
        try {
            in = blob.getBinaryStream();
            //一般接下来是把in的字节流写入一个文件中,但这里直接放进字符串
            byte[] buff=new byte[(int) blob.length()];
            //      byte[] buff=new byte[1024];
            //    byte[] b = new byte[blob.getBufferSize()];
            for(int i=0;(i=in.read(buff))>0;){
                str=str.append(new String(buff));
            }
            return str.toString();


        }catch (Exception e) {
            e.printStackTrace();
        } finally{
            try{
                in.close();
            }catch(Exception e){
                System.out.println("转换异常");
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 把Blob类型转换为byte数组类型
     *
     * @param blob
     * @return
     */
    public static byte[] blobToBytes(Blob blob) {
        BufferedInputStream is = null;
        try {
            is = new BufferedInputStream(blob.getBinaryStream());
            byte[] bytes = new byte[(int) blob.length()];
            int len = bytes.length;
            int offset = 0;
            int read = 0;
            while (offset < len
                    && (read = is.read(bytes, offset, len - offset)) >= 0) {
                offset += read;
            }
            return bytes;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                is.close();
                is = null;
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static String getSqlInjText(Object arg )
    {
        if ( arg == null )
        {
            return "";
        }
        return arg.toString()
                .replaceAll( "'", "" )
                .replaceAll( "\"", "“" )
                //.replaceAll("%", "％")
                .replaceAll("\\*", "×")
                //.replaceAll(";", "；")
                .replaceAll("<", "＜")
                .replaceAll(">", "＞");
    }

    public static <T> void batchTask(List<T> list, final Class<T> clazz, IDataActuator dataActuator, final ISqlParser iParser,
                                     final CommandMode mode, final String[] exps, final Callback<List<T>> callback ) throws Exception {
        if ( list == null || list.size() < 1 )
        {
            return;
        }
        int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        CopyOnWriteArrayList<T> v = new CopyOnWriteArrayList<T>();
        v.addAll( list );
        Callable<List<T>> call = new BatchExecutor( v, dataActuator, clazz, iParser, mode, exps, true );
        ThreadUtils.execute( ThreadUtils.DontWaitting, threadCount, callback, call );
    }

    public static Map<String, Object> batchQuery( Map<String, Class> typeMap, QueryableAction... queryables ) throws Exception
    {
        if(queryables == null || typeMap == null) {
            throw new Exception( "Params can not be empty" );
        }

        String[] keys = typeMap.keySet().toArray( new String[typeMap.size()] );
        if ( keys.length != queryables.length )
        {
            throw new Exception( "Inconsistent length" );
        }

        Map<String, Object> resultMap = new HashMap<String, Object>();

        List<Callable<List<Object>>> callas = new CopyOnWriteArrayList<Callable<List<Object>>>();
        for ( final QueryableAction query : queryables )
        {
            callas.add( new Callable<List<Object>>()
            {
                @SuppressWarnings( "unchecked" )
                @Override
                public List<Object> call() throws Exception
                {
                    return query.query( Map.class );
                }
            } );
        }

        List<Object> result = ThreadUtils.async( callas );
        if(result != null  && result.size() == 1) {

            for ( Map.Entry<String, Class> item : typeMap.entrySet() ) {
                Map m = (Map)result.get( 0 );
                if(m.containsKey( item.getKey()) && resultMap.containsKey( item.getKey() )) {
                    resultMap.put( item.getKey(), m.get( item.getKey() ) );
                }
            }
        }

        return resultMap;
    }

    @SuppressWarnings( {"rawtypes"} )
    public static List<Object> batchQuery( QueryableAction... queryables )
    {

        List<Object> result = new ArrayList<Object>();
        if ( queryables == null || queryables.length < 1 )
        {
            return result;
        }

        List<Callable<List<Object>>> callas = new CopyOnWriteArrayList<Callable<List<Object>>>();
        for ( final QueryableAction query : queryables )
        {

            callas.add( new Callable<List<Object>>()
            {
                @SuppressWarnings( "unchecked" )
                @Override
                public List<Object> call() throws Exception
                {
                    return query.query();
                }
            } );
        }

        result = ThreadUtils.async( callas );

        return result;
    }

    public static <T> List<T> query( Class<T> clazz, String sql ) throws SQLException
    {
        return query( clazz, clazz, sql, false, null, null );
    }

    public static <T> List<T> query( Class<T> clazz, String sql, boolean isScalar ) throws SQLException
    {
        return query( clazz, clazz, sql, isScalar, null, null );
    }

    public static <T> List<T> query( Class<T> clazz, String sql, boolean isScalar, Connection conn ) throws SQLException
    {
        return query( clazz, clazz, sql, isScalar, conn, null );
    }

    @SuppressWarnings( "unchecked" )
    public static <T, E> List<E> query(Class<T> clazz, Class<E> returnType, String sql, boolean isScalar, Connection conn, DataSource datasource) throws SQLException {
        PreparedStatement preparedstatement = null;
        ResultSet rs = null;
        List<E> list = new ArrayList<E>();

        try
        {
            if(datasource == null) {
                if(!clazz.isPrimitive() && !clazz.isEnum() &&
                        (clazz.getSuperclass().equals(Queryable.class) ||
                                clazz.getSuperclass().equals(QueryableBase.class) ||
                                clazz.getSuperclass().equals(QueryableAction.class))){
                    datasource = getDatasource(clazz);
                }
                else {
                    datasource = getDatasource(null);
                }
            }

            if ( conn == null ) {
                conn = datasource.getConnection();
            }

            preparedstatement = getPrepareStatement(datasource, conn, sql);
            rs = preparedstatement.executeQuery();

            ResultSetMetaData rsmd = rs.getMetaData();
            while ( rs.next() )
            {
                E result = DBUtils.getMetaData(rs, rsmd, returnType);
                list.add( result );
            }
            rs.close();
        } catch ( Exception e )
        {
            try
            {
                if ( preparedstatement != null && !preparedstatement.isClosed() )
                {
                    preparedstatement.close();
                }
            } catch ( Exception ex )
            {
                log.warn( ex.getMessage() );
            }

            if("true".equals(ApplicationConfig.getInstance().get("${entity.debug}", ""))) {
                if(!e.getMessage().equals("timeouted!") &&
                !"connection holder is null".equals(e.getMessage()) &&
                !"Operation not allowed after ResultSet closed".equals(e.getMessage()) &&
                !"ResultSet already requested".equals(e.getMessage()) &&
                !"statement is not executing".equals(e.getMessage())) {
                    String logmsg = String.format( "EntityQueryable Error:\n%s\n%s", sql, e.getMessage() );
                    log.error("entityQueryable Sql=======================>>>");
                    log.error( logmsg );
                    log.error( e.getMessage(), e );
                }
                else {
                    log.warn(e.getMessage());
                }
            }
            if(conn != null) {
                if (conn.isClosed()) {
                    conn = datasource.getConnection();
                    return query(clazz, returnType, sql, isScalar, conn, datasource);
                }
                else {
                    datasource.close(conn);
                    conn = datasource.getConnection();
                }

                if ("ResultSet already requested".equals(e.getMessage())) { //For sqlite
                    return query(clazz, returnType, sql, isScalar, conn, datasource);
                }

                if ("statement is not executing".equals(e.getMessage())) { //For sqlite
                    return query(clazz, returnType, sql, isScalar, conn, datasource);
                }

                if ("Operation not allowed after ResultSet closed".equals(e.getMessage())) {
                    return query(clazz, returnType, sql, isScalar, conn, datasource);
                }

                if ("connection holder is null".equals(e.getMessage())) {
                    return query(clazz, returnType, sql, isScalar, conn, datasource);
                }

                if ("Communications link failure".equals(e.getMessage())) {
                    return query(clazz, returnType, sql, isScalar, conn, datasource);
                }
            }
        }

        return list;
    }

    public static <T> List<Map<String, Object>> query( Class<T> clazz, String sql, Connection conn, DataSource datasource ) throws SQLRuntimeException
    {
        PreparedStatement preparedstatement = null;
        ResultSet rs = null;
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        try {
            if(datasource == null) {
                if(!clazz.isPrimitive() && !clazz.isEnum() &&
                        (clazz.getSuperclass().equals(Queryable.class) ||
                                clazz.getSuperclass().equals(QueryableBase.class) ||
                                clazz.getSuperclass().equals(QueryableAction.class))){
                    datasource = getDatasource(clazz);
                }
                else {
                    datasource = getDatasource(null);
                }
            }

            if ( conn == null ) {
                conn = datasource.getConnection();
            }

            preparedstatement = getPrepareStatement(datasource, conn, sql);

            rs = preparedstatement.executeQuery();

            ResultSetMetaData rsmd = rs.getMetaData();
            while (rs.next()) {
                Map result = DBUtils.getMetaData(rs, rsmd, Map.class);
                list.add(result);
            }
            rs.close();

        } catch ( Exception e )
        {
            if("true".equals(ApplicationConfig.getInstance().get("${entity.debug}", ""))) {
                if(!e.getMessage().equals("timeouted!")) {
                    String logmsg = String.format( "EntityQueryable Error:\n%s\n%s", sql, e.getMessage() );
                    log.error("entityQueryable Sql=======================>>>");
                    log.error( logmsg );
                    log.error( e.getMessage(), e );
                }
            }
        }

        try
        {
            if ( preparedstatement != null && !preparedstatement.isClosed() )
            {
                preparedstatement.close();
            }
        } catch ( Exception e )
        {
            log.error( e.getMessage(), e );
        }

        return list;
    }

    public static <T> Integer execute( Class<T> clazz, String sql) throws Exception
    {
        return execute(clazz, sql, null, null, null);
    }

    public static <T> Integer execute( Class<T> clazz, String sql, Connection conn) throws Exception
    {
        return execute(clazz, sql, null, conn, null);
    }

    public static <T> Integer execute( Class<T> clazz, String sql, Map<Integer, Blob> blobMap ) throws Exception
    {
        return execute(clazz, sql, blobMap, null, null);
    }

    public static <T> Integer execute( Class<T> clazz, String sql, Map<Integer, Blob> blobMap, Connection conn, DataSource datasource) throws Exception
    {
        PreparedStatement preparedstatement = null;
        int rs = 0;
        boolean autoCommit = true;

        try {
            if(datasource == null) {
                if(!clazz.isPrimitive() && !clazz.isEnum() &&
                        (clazz.getSuperclass().equals(Queryable.class) ||
                                clazz.getSuperclass().equals(QueryableBase.class) ||
                                clazz.getSuperclass().equals(QueryableAction.class))){
                    datasource = getDatasource(clazz);
                }
                else {
                    datasource = getDatasource(null);
                }
            }
            if ( conn == null ) {
                conn = datasource.getConnection();
            }
            else {
                autoCommit = conn.getAutoCommit();
            }

            if(conn.getMetaData().getDatabaseProductName().toLowerCase().equals("oracle")) {
                String[] keysName = {"id"};
                preparedstatement = getPrepareStatement(datasource, conn, sql, keysName, true);
            }

            else {
                preparedstatement = getPrepareStatement(datasource, conn, sql, null, true);
            }

            if(blobMap != null) {
                for(Integer i : blobMap.keySet()) {
                    preparedstatement.setBlob(i, blobMap.get(i));
                }
            }

            rs = preparedstatement.executeUpdate();

            if(sql.toLowerCase().trim().startsWith("insert")) {
                ResultSet generatedKeys = preparedstatement.getGeneratedKeys();
                while (generatedKeys.next()) {
                    rs = generatedKeys.getInt(1);
                }
            }

            if(autoCommit) {
                datasource.commit(conn);
            }

            return rs;

        } catch ( Exception e )
        {
            try
            {
                if ( preparedstatement != null && !preparedstatement.isClosed() )
                {
                    preparedstatement.close();
                }
            } catch ( Exception ex )
            {
                log.warn( ex.getMessage() );
            }

            if("true".equals(ApplicationConfig.getInstance().get("${entity.debug}", ""))) {
                String logmsg = String.format( "EntityQueryable Error:\n%s\n%s", sql, e.getMessage() );
                if(!e.getMessage().equals("timeouted!")) {
                    log.error("entityQueryable Sql=======================>>>");
                    log.error( logmsg );
                    log.error( e.getMessage(), e );
                }
                throw new SQLException( logmsg );
            }

            if(conn != null && conn.getAutoCommit()) {
                if (conn.isClosed()) {
                    conn = datasource.getConnection();
                    return execute( clazz, sql, blobMap, conn, datasource);
                }
                else {
                    datasource.close(conn);
                    conn = datasource.getConnection();
                }

                if ("ResultSet already requested".equals(e.getMessage())) { //For sqlite
                    return execute( clazz, sql, blobMap, conn, datasource);
                }

                if ("statement is not executing".equals(e.getMessage())) { //For sqlite
                    return execute( clazz, sql, blobMap, conn, datasource);
                }

                if ("Operation not allowed after ResultSet closed".equals(e.getMessage())) {
                    return execute( clazz, sql, blobMap, conn, datasource);
                }

                if ("connection holder is null".equals(e.getMessage())) {
                    return execute( clazz, sql, blobMap, conn, datasource);
                }

                if ("Communications link failure".equals(e.getMessage())) {
                    return execute( clazz, sql, blobMap, conn, datasource);
                }

                if("No operations allowed after statement closed.".equals(e.getMessage())) {
                    return execute( clazz, sql, blobMap, conn, datasource);
                }

                if("ResultSet is from UPDATE. No Data.".equals(e.getMessage())) {
                    return execute( clazz, sql, blobMap, conn, datasource);
                }

                datasource.commit(conn);
            }
            else {
                datasource.rollback(conn);
            }
            throw e;
        } finally
        {
            try
            {
                if ( preparedstatement != null && !preparedstatement.isClosed() )
                {
                    preparedstatement.close();
                }
            } catch ( Exception e )
            {
                log.warn( e.getMessage() );
            }
        }
    }

    /**
     * 判断一个类是JAVA类型还是用户定义类型
     * @param clz
     * @return
     */
    public static boolean isJavaClass(Class<?> clz) {
        return clz != null && clz.getClassLoader() == null;
    }

    public static PreparedStatement getPrepareStatement(DataSource datasource, Connection conn, String sql) throws SQLException {
        return getPrepareStatement(datasource, conn, sql, null, false);
    }

    public static PreparedStatement getPrepareStatement(DataSource datasource, Connection conn, String sql, String[] keys, boolean generatedKeys) throws SQLException {
        if(generatedKeys) {
            if(keys != null && keys.length>0) {
                return conn.prepareStatement(sql, keys);
            }
            return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        }
        return conn.prepareStatement(sql);
    }

    public static <E> E getMetaData(ResultSet rs, ResultSetMetaData rsmd, Class<E> returnType) throws SQLException {
        int columnCount = rsmd.getColumnCount();
        E result = null;
        Map<String, Object> mapMetaData = new HashMap<String, Object>();

        if(!isJavaClass(returnType)) {
            result = ReflectionUtils.getInstance(returnType);
        }

        for ( int i = 0; i < columnCount; i++ )
        {
            try
            {
                String columnName = rsmd.getColumnLabel( i + 1 );
                Object columnValue = null;
                try{
                    columnValue = rs.getObject( columnName );
                } catch (SQLException ex){}
                if(returnType.isPrimitive()
                        || returnType == Date.class
                        || returnType == String.class
                        || returnType == BigDecimal.class
                        || returnType == Integer.class
                        || returnType == Long.class
                        || returnType == Float.class
                        || returnType == Double.class
                        || returnType == Number.class) {

                    if(returnType == String.class) {
                        result = (E)StringUtils.cast(returnType, columnValue.toString());
                    }

                    else {
                        result = (E) columnValue;
                    }
                }
                else if(returnType == UUID.class) {

                    result = (E)UUID.fromString(columnValue.toString());
                }
                else if(returnType == Map.class) {
                    mapMetaData.put(columnName, columnValue);
                }
                else {
                    if(ColumnInfo.class.equals(returnType)) {
                        columnName = ensureColumnName(columnName);
                    }
                    ReflectionUtils.setFieldValue(returnType, result, columnName, columnValue);
                }
            }

            catch ( Exception e )
            {
                if("true".equals(ApplicationConfig.getInstance().get("${entity.debug}", ""))) {
                    log.error( String.format( "error field=====> %s", rsmd.getColumnLabel( i + 1 ) ) );
                    log.error(e.getMessage());
                }
            }
        }

        if(returnType == Map.class) {
            result = (E) mapMetaData;
        }

        return result;
    }

    private static String ensureColumnName(String columnName) {
        if(columnName.indexOf("_") == -1) {
            return columnName;
        }
        String swap = "";
        String[] text = columnName.split("_");
        for(String str : text) {
            swap += str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }
        columnName = swap.substring(0, 1).toLowerCase() + swap.substring(1);

        return columnName;
    }

    private static <T> DataSource getDatasource(Class<T> clazz) throws SQLException {
        String ds = DataSourceFactory.findDataSourceAnnotation(clazz);
        if(StringUtils.isNotEmpty(ds)) {
            return DataSourceFactory.getInstance().getDataSource(ds);
        }

        return DataSourceFactory.getInstance().getDataSource();
    }
}
