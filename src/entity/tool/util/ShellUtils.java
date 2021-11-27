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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShellUtils
{
    private static final Logger log = LoggerFactory.getLogger( ShellUtils.class );
    
    public static String execShell( String cmd ) {
        
        if(StringUtils.isEmpty( cmd )) {
            return null;
        }
        
        cmd = cmd.replaceAll( "\\s+", " " );
        
        List<String> params = StringUtils.splitString2List( " ", cmd );
        if(params == null || params.size() < 1) {
            return null;
        }
        String scriptPath = params.get( 0 );
        if(params.size() == 1) {
            return execShell(scriptPath);
        }
        
        params.remove( 0 );
        
        return execShell(scriptPath, params);
    }
    
    /**
     * 解决了 参数中包含 空格和脚本没有执行权限的问题
     * 
     * @param scriptPath
     *            脚本路径
     * @param params
     *            参数数组
     */
    public static String execShell( String scriptPath, String... params ) {
        return execShell(scriptPath, Arrays.asList( params ));
    }
    
    public static String execShell( String scriptPath, List<String> params )
    {
        try
        {
            List<String> cmd = new ArrayList<String>();
            cmd.add( scriptPath );
            
            // 为了解决参数中包含空格
            cmd.addAll( params );

            // 解决脚本没有执行权限
            ProcessBuilder builder = new ProcessBuilder( "/bin/chmod", "755", scriptPath );
            Process process = builder.start();
            process.waitFor();

            Process ps = Runtime.getRuntime().exec( cmd.toArray( new String[cmd.size()] ) );
            ps.waitFor();

            BufferedReader br = new BufferedReader( new InputStreamReader( ps.getInputStream() ) );
            StringBuffer sb = new StringBuffer();
            String line;
            while ( (line = br.readLine()) != null )
            {
                sb.append( line ).append( "\n" );
            }
            // 执行结果
            return sb.toString();

        } catch ( Exception e )
        {
            log.error( e.getMessage(), e );
        }
        
        return null;
    }
}
