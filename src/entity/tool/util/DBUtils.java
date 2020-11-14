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
import entity.query.ColumnInfo;
import entity.query.Datetime;
import entity.query.QueryableAction;
import entity.query.core.*;
import entity.query.core.executor.BatchExecutor;
import entity.query.enums.CommandMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.davidmoten.rx.jdbc.exceptions.SQLRuntimeException;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private static final Logger log = LogManager.getLogger( DBUtils.class );

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

        if ( arg instanceof String )
        {
            if(arg == null) {
                return "";
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
                .replaceAll( "'", "''" )
                .replaceAll("%", "％")
                .replaceAll("[\\*]", "×")
                .replaceAll(";", "；")
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
        return query( clazz, clazz, sql, false, null );
    }

    public static <T> List<T> query( Class<T> clazz, String sql, boolean isScalar ) throws SQLException
    {
        return query( clazz, clazz, sql, isScalar, null );
    }

    public static <T> List<T> query( Class<T> clazz, String sql, boolean isScalar, Connection conn ) throws SQLException
    {
        return query( clazz, clazz, sql, isScalar, conn );
    }

    @SuppressWarnings( "unchecked" )
    public static <T, E> List<E> query( Class<T> clazz, Class<E> returnType, String sql, boolean isScalar, Connection conn ) throws SQLException
    {
        PreparedStatement preparedstatement = null;
        ResultSet rs = null;
        List<E> list = new ArrayList<E>();

        try
        {
            if ( conn == null )
            {
                conn = DataSourceFactory.getInstance().getDataSource().getConnection();
            }

            preparedstatement = conn.prepareStatement(sql);
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
            if(conn != null && conn.isClosed()) {
                conn = DataSourceFactory.getInstance().getDataSource().getConnection();
                return query( clazz, returnType, sql, isScalar, conn);
            }
            if(!e.getMessage().equals("timeouted!")) {
                String logmsg = String.format( "EntityQueryable Error:\n%s\n%s", sql, e.getMessage() );
                log.error("entityQueryable Sql=======================>>>");
                log.error( logmsg );
                log.error( e.getMessage(), e );
            }
        }

        try
        {
            if ( preparedstatement != null )
            {
                preparedstatement.close();
            }
        } catch ( Exception e )
        {
            log.error( e.getMessage(), e );
        }

        return list;
    }

    public static <T> List<Map<String, Object>> query( Class<T> clazz, String sql, Connection conn ) throws SQLRuntimeException
    {

        PreparedStatement preparedstatement = null;
        ResultSet rs = null;
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        try
        {
            if ( conn == null )
            {
                conn = DataSourceFactory.getInstance().getDataSource().getConnection();
            }

            preparedstatement = conn.prepareStatement( sql );

            rs = preparedstatement.executeQuery();

            ResultSetMetaData rsmd = rs.getMetaData();
            while ( rs.next() )
            {
                Map result = DBUtils.getMetaData(rs, rsmd, Map.class);
                list.add( result );
            }
            rs.close();

        } catch ( Exception e )
        {
            if(!e.getMessage().equals("timeouted!")) {
                String logmsg = String.format( "EntityQueryable Error:\n%s\n%s", sql, e.getMessage() );
                log.error("entityQueryable Sql=======================>>>");
                log.error( logmsg );
                log.error( e.getMessage(), e );
            }
        }

        try
        {
            if ( preparedstatement != null )
            {
                preparedstatement.close();
            }
        } catch ( Exception e )
        {
            log.error( e.getMessage(), e );
        }

        return list;
    }

    public static <T> Integer execute( Class<T> clazz, String sql) throws SQLException
    {
        return execute(clazz, sql, null, null);
    }

    public static <T> Integer execute( Class<T> clazz, String sql, Connection conn) throws SQLException
    {
        return execute(clazz, sql, null, null);
    }

    public static <T> Integer execute( Class<T> clazz, String sql, Map<Integer, Blob> blobMap ) throws SQLException
    {
        return execute(clazz, sql, blobMap, null);
    }

    public static <T> Integer execute( Class<T> clazz, String sql, Map<Integer, Blob> blobMap, Connection conn) throws SQLException
    {
        PreparedStatement preparedstatement = null;
        int rs = 0;

        DataSource datasource = null;
        boolean autoCommit = true;

        try
        {
            begin:

            datasource = DataSourceFactory.getInstance().getDataSource();
            if ( conn == null )
            {
                conn = datasource.getConnection();
            }
            else {
                autoCommit = conn.getAutoCommit();
            }

            if(conn.getMetaData().getDatabaseProductName().toLowerCase().equals("oracle")) {
                String[] keysName = {"id"};
                preparedstatement = conn.prepareStatement(sql, keysName);
            }

            else {
                preparedstatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
            if(conn.isClosed()) {
                conn = datasource.getConnection();
                return execute( clazz, sql, blobMap, conn);
            }
            if(autoCommit) {
                datasource.rollback(conn);
            }
            String logmsg = String.format( "EntityQueryable Error:\n%s\n%s", sql, e.getMessage() );
            if(!e.getMessage().equals("timeouted!")) {
                log.error("entityQueryable Sql=======================>>>");
                log.error( logmsg );
                log.error( e.getMessage(), e );
            }

            if("true".equals(ApplicationConfig.getInstance().get("${entity.debug}", ""))) {
                throw new SQLException( logmsg );
            }
            else {
                throw new SQLException( e.getMessage() );
            }
        } finally
        {
            try
            {
                if ( preparedstatement != null )
                {
                    preparedstatement.close();
                }
            } catch ( Exception e )
            {
                log.error( e.getMessage(), e );
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
                log.error( String.format( "error field=====> %s", rsmd.getColumnLabel( i + 1 ) ) );
                log.error(e);
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
}
