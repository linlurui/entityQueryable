/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.core;

import entity.query.annotation.DBConfig;
import entity.query.core.parser.*;
import entity.tool.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlParserFactory<T> {
	
	private static final Logger log = LogManager.getLogger( SqlParserFactory.class );
	
	private SqlParserFactory() {
		
	}
	
	@SuppressWarnings("rawtypes")
	private volatile static SqlParserFactory singleton;
	
	@SuppressWarnings("rawtypes")
	public static SqlParserFactory getInstance() {  
		if (singleton == null) {  
			synchronized (DataSourceFactory.class) {
				if (singleton == null) {  
					singleton = new SqlParserFactory();  
				}
			}
		}
		
		return singleton;
	}
	
	public ISqlParser CreateParser(Class<T> clazz) throws Exception {
		
		String type = null;
		DataSource ds = null;
		entity.query.annotation.DataSource dataSourceAnn = clazz.getAnnotation(entity.query.annotation.DataSource.class);
		DBConfig ann = clazz.getAnnotation(DBConfig.class);
		if(dataSourceAnn != null) {
			ds = DataSourceFactory.getInstance().getDataSource(dataSourceAnn.value());
			type = getDBType(ds);
		}
		else if(ann != null) {
			if(StringUtils.isEmpty( ann.dbType() )) {
				if(!StringUtils.isEmpty( ann.id() )) {
					try {
						ds = DataSourceFactory.getInstance().getDataSource(ann);
						type = getDBType(ds);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			} else {
				type = ann.dbType();
			}
		}
		
		else {
			try {
				ds = DataSourceFactory.getInstance().getDataSource();
				type = getDBType(ds);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		if(type.isEmpty()) {
			return null;
		}

		ISqlParser parser = createParser(ds);
		
		return parser;
	}

	public static ISqlParser createParser(DataSource ds) throws Exception {

		String type = getDBType(ds);
		ISqlParser parser = null;
		if("mysql".equalsIgnoreCase(type.trim())) {
		    parser = new MysqlParser(ds);
		}

		else if("oracle".equalsIgnoreCase(type.trim())) {
		    parser =  new OracleParser(ds);
		}

		else if("db2".equalsIgnoreCase(type.trim())) {
		    parser =  new DB2Parser(ds);
		}

		else if("odbc".equalsIgnoreCase(type.trim())) {
		    parser =  new OdbcParser(ds);
		}

		else if("postgresql".equalsIgnoreCase(type.trim())) {
		    parser =  new PostgresqlParser(ds);
		}

		else if("sqlserver".equalsIgnoreCase(type.trim())) {
		    parser =  new SqlserverParser(ds);
		}

		else if("sybase".equalsIgnoreCase(type.trim())) {
		    parser =  new SybaseParser(ds);
		}

		else if("sqlite".equalsIgnoreCase(type.trim())) {
		    parser =  new SqLiteParser(ds);
		}

		else if("couchbase".equalsIgnoreCase(type.trim())) {
			parser =  new CouchbaseParser(ds);
		}

		else if("derby".equalsIgnoreCase(type.trim())) {
			parser =  new DerbyParser(ds);
		}

		else if("hive2".equalsIgnoreCase(type.trim())) {
			parser =  new HiveParser(ds);
		}

		if(parser == null) {
			throw new Exception("Database type can not be empty!!!");
		}

		return parser;
	}

	private static String getDBType(DataSource ds) {
		String type = "";
		String url = ds.getUrl();
		Pattern pattern = Pattern.compile("(?i)(oracle|sqlite|sqlserver|couchbase|derby|hive|mysql|postgresql|sybase|db2|odbc)");
		Matcher matcher = pattern.matcher(url);
		if(matcher.find()) {
            type = matcher.group(1);
        }
		return type;
	}
}
