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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class RequestUtils
{
    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_PUT = "PUT";

    private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);
    
    public static String get( String serviceUrl ) throws IOException
    {
        return get(serviceUrl, null);
    }
    
    public static String get( String serviceUrl, Map headParams )
            throws IOException
    {
        StringBuffer sb = new StringBuffer();
        sb.append( serviceUrl );

        return request( sb.toString(), null, headParams, "GET" );
    }

    public static String delete( String serviceUrl, String jsonParameter ) throws IOException
    {
        return delete(serviceUrl, jsonParameter, null);
    }
    
    public static String delete( String serviceUrl, String jsonParameter, HashMap<String, String> headParams )
            throws IOException
    {
        StringBuffer sb = new StringBuffer();
        sb.append( serviceUrl );

        if ( !(StringUtils.isEmpty( jsonParameter )) )
        {
            sb.append( "?" );
            sb.append( jsonParameter );
        }

        return request( sb.toString(), jsonParameter, headParams, "DELETE" );
    }

    public static String put( String serviceUrl, String jsonParameter ) throws IOException
    {
        return put(serviceUrl, jsonParameter, null);
    }
    
    public static String put( String serviceUrl, String jsonParameter, HashMap<String, String> headParams )
            throws IOException
    {
        return request( serviceUrl, jsonParameter, headParams, "PUT" );
    }

    public static String post( String serviceUrl, String jsonParameter ) throws IOException
    {
        return post(serviceUrl, jsonParameter, null);
    }
    
    public static String post( String serviceUrl, String jsonParameter, HashMap<String, String> headParams )
            throws IOException
    {
        return request( serviceUrl, jsonParameter, headParams, "POST" );
    }
    
    public static String request( String serviceUrl, String parameterString, Map headParams,
            String restMethod ) throws IOException {
        String method = restMethod.toUpperCase();
        HttpURLConnection conn = null;
        OutputStream outPutStream = null;
        Writer writer = null;
        BufferedReader br = null;
        String str = null;
        InputStreamReader inputStreamReader = null;
        InputStream inputStream = null;
        try
        {
            trustAllHttpsCertificates();  
            HttpsURLConnection.setDefaultHostnameVerifier(hv);  
            
            conn = getURLConnection( serviceUrl, method, headParams );

            if ( ("POST".equals( method )) || ("PUT".equals( method )) )
            {
                outPutStream = conn.getOutputStream();
                writer = new OutputStreamWriter( outPutStream, "UTF-8" );
                if ( !StringUtils.isEmpty( parameterString ) )
                {
                    writer.write( parameterString );
                }
                writer.flush();

            }

            inputStream = conn.getInputStream();
            inputStreamReader = new InputStreamReader( inputStream, "UTF-8" );
            br = new BufferedReader( inputStreamReader );

            StringBuilder sb = getMessageFromReader( br );
            str = sb.toString();
            
        } catch ( Exception e )
        {
        	log.error( String.format("Url: %s, method: %s, postdata: %s", serviceUrl, method, parameterString) );
            log.error( "RequestUtils request error-> " + e.getMessage() );
        } finally
        {
        	if(outPutStream != null) {
        		outPutStream.close();
        	}

            if ( conn != null )
            {
                try
                {
                    conn.disconnect();
                } catch ( Exception ex )
                {
                    conn = null;
                }
            }

            if ( inputStreamReader != null )
            {
                try
                {
                    inputStreamReader.close();
                } catch ( Exception ex )
                {
                    inputStreamReader = null;
                }
            }

            if ( inputStream != null )
            {
                try
                {
                    inputStream.close();
                } catch ( Exception ex )
                {
                    inputStream = null;
                }
            }

            if ( writer != null )
            {
                try
                {
                    writer.close();
                } catch ( Exception ex )
                {
                    writer = null;
                }
            }
        }
        return str;
    }

    public static HttpURLConnection getConnection( String serviceUrl, String parameterString, Map headParams,
                                  String restMethod ) throws IOException {
        String method = restMethod.toUpperCase();
        HttpURLConnection conn = null;
        String str = null;
        try
        {
            trustAllHttpsCertificates();
            HttpsURLConnection.setDefaultHostnameVerifier(hv);

            conn = getURLConnection( serviceUrl, method, headParams );

            return conn;

        } catch ( Exception e ) {
            log.error( String.format("Url: %s, method: %s, postdata: %s", serviceUrl, method, parameterString) );
            log.error( "RequestUtils request error-> " + e.getMessage() );
            if ( conn != null )
            {
                try
                {
                    conn.disconnect();
                } catch ( Exception ex )
                {
                    conn = null;
                }
            }
        }

        return null;
    }

    public static StringBuilder getMessageFromReader( BufferedReader br ) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        String line = br.readLine(); // 读取第一行
        while (line != null) { // 如果 line 为空说明读完了
            sb.append(line); // 将读到的内容添加到 buffer 中
            sb.append("\n"); // 添加换行符
            line = br.readLine(); // 读取下一行
        }

        return sb;
    }

    private static HttpURLConnection getURLConnection( String serviceUrl, String restMethod, Map<String, Object> headParams ) throws IOException
    {
        URL url = new URL( new String(serviceUrl.getBytes(StandardCharsets.UTF_8)) );
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        // 设置URL链接时间和超时时间都为1分钟
        conn.setConnectTimeout( 1000 * 60 * 1 );
        conn.setReadTimeout( 1000 * 60 * 1 );

        conn.setRequestMethod( restMethod );

        if ( ("POST".equals( restMethod )) || ("PUT".equals( restMethod )) )
        {
            conn.setDoOutput( true );
            conn.setDoInput( true );
        }

        if ( !("GET".equals( restMethod )) )
        {
            conn.setUseCaches( false );

        }
        if ( headParams == null )
        {
            //conn.setRequestProperty( "Content-Type", "application/json" );
        } else {
            for ( Map.Entry<String, Object> entry : headParams.entrySet() ) {
                conn.setRequestProperty( entry.getKey(), entry.getValue().toString() );
            }
            
        }
        return conn;
    }
    
    private static HostnameVerifier hv = new HostnameVerifier() {  
        public boolean verify(String urlHostName, SSLSession session) {  
            System.out.println("Warning: URL Host: " + urlHostName + " vs. "  
                               + session.getPeerHost());  
            return true;  
        }  
    };  
      
    private static void trustAllHttpsCertificates() throws Exception {  
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];  
        javax.net.ssl.TrustManager tm = new miTM();  
        trustAllCerts[0] = tm;  
        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext  
                .getInstance("SSL");  
        sc.init(null, trustAllCerts, null);  
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc  
                .getSocketFactory());  
    }  
  
    static class miTM implements javax.net.ssl.TrustManager,  
            javax.net.ssl.X509TrustManager {  
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {  
            return null;  
        }  
  
        public boolean isServerTrusted(  
                java.security.cert.X509Certificate[] certs) {  
            return true;  
        }  
  
        public boolean isClientTrusted(  
                java.security.cert.X509Certificate[] certs) {  
            return true;  
        }  
  
        public void checkServerTrusted(  
                java.security.cert.X509Certificate[] certs, String authType)  
                throws java.security.cert.CertificateException {  
            return;  
        }  
  
        public void checkClientTrusted(  
                java.security.cert.X509Certificate[] certs, String authType)  
                throws java.security.cert.CertificateException {  
            return;  
        }  
    }  
}

