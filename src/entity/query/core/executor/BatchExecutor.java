
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
import entity.query.core.ISqlParser;
import entity.query.enums.CommandMode;
import entity.tool.util.ReflectionUtils;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class BatchExecutor<T> implements Callable<List<T>>
{

    private static final Logger log = LoggerFactory.getLogger( BatchExecutor.class );

    private List<T> data;
    private IDataActuator dataActuator;
    private ISqlParser iParser;
    private CommandMode mode;
    private Class<T> clazz;
    private String exp;
    private boolean autoCommit = true;

    public BatchExecutor(final List<T> data, IDataActuator dataActuator, Class<T> clazz, ISqlParser iParser, CommandMode mode,
                         String[] exps, boolean autoCommit )
    {
        this.data = data;
        this.dataActuator = dataActuator;
        this.iParser = iParser;
        this.mode = mode;
        this.clazz = clazz;
        this.exp = StringUtils.join( ", ", exps );
        this.autoCommit = autoCommit;
    }

    @Override
    public List<T> call()
    {
        if ( data == null || data.size() < 1 )
        {
            return null;
        }

        String sql = "";
        List<String> sqls = new ArrayList<String>();
        List<Map<Integer, Blob>> blobList = new ArrayList<Map<Integer, Blob>>();
        int count = 0;

        try {

            synchronized ( data )
            {
                for ( T item : data )
                {
                    count++;
                    if ( count > 10 )
                    {
                    	execute(sqls, this.dataActuator, blobList);
                        sqls.clear();
                        count = 1;
                    }

                    DataSource ds = this.dataActuator.dataSource();
                    if ( ds != null )
                    {
                        ReflectionUtils.setFieldValue( clazz, item, "dataSource", ds );
                    }
                    Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
                    sql = iParser.toString( clazz, exp, mode, ReflectionUtils.invoke( clazz, item, "entityObject" ), 0, 0, false, blobMap );
                    sqls.add( sql );
                    blobList.add(blobMap);
                }
            }

            if ( sqls.size() > 0 )
            {
            	execute(sqls, this.dataActuator, blobList);
            }
        }
        catch(Exception e) {
        	log.error(e.getMessage(), e);
        }

        return data;
    }

    private void execute(List<String> sqls, IDataActuator dataActuator, List<Map<Integer, Blob>> blobList) throws SQLException {
    	execute(sqls, dataActuator, blobList, 2000);
    }

	private void execute(List<String> sqls, IDataActuator dataActuator, List<Map<Integer, Blob>> blobList, long timeout) throws SQLException {
		try {
		    DBExecutorAdapter.createExecutor(dataActuator).batchExecute(sqls, blobList);
		} catch (SQLException e) {
        	if(e.getMessage().equals("timeouted!")) {
        		try {
        			log.info("Database connection has been timeouted!");
        			if(timeout >= 5000) {
        				log.info(String.format("Timeouted %s millis!", timeout));
        				throw e;
        			}

					Thread.sleep(timeout);
				} catch (InterruptedException e1) {}
        		execute(sqls, dataActuator, blobList, timeout + 1000);
        	}

        	else {
        		throw e;
        	}
		}
	}
}
