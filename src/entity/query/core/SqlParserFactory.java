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
import entity.query.enums.DBType;
import entity.tool.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlParserFactory<T> {

	private static final Logger log = LoggerFactory.getLogger( SqlParserFactory.class );

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
		String dsname = DataSourceFactory.findDataSourceAnnotation(clazz);
		DBConfig ann = clazz.getAnnotation(DBConfig.class);
		if(StringUtils.isNotEmpty(dsname) && !"default".equals(dsname)) {
			ds = DataSourceFactory.getInstance().getDataSource(dsname);
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
				ds = DataSourceFactory.getInstance().getDataSource(ann);
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

		if(StringUtils.isEmpty(type)) {
			return null;
		}

		ISqlParser parser = createParser(ds);

		return parser;
	}

	public static ISqlParser createParser(DataSource ds) throws Exception {

		String type = getDBType(ds);
		return createParser(type, ds);
	}

	public static ISqlParser createParser(DataSource ds, SqlContainer container) throws Exception {
		ISqlParser parser = createParser(ds);
		if(container != null && parser instanceof SqlParserBase) {
			((SqlParserBase) parser).setContainer(container);
		}
		return parser;
	}

	public static ISqlParser createParser(DBType dbType) throws Exception {
		if(dbType == null) {
			throw new IllegalArgumentException("dbType can not be null");
		}
		DataSource virtualDataSource = new DataSource();
		virtualDataSource.setDbType(dbType.getValue());
		virtualDataSource.setUrl(String.format("jdbc:%s://virtual", dbType.getValue()));
		return createParser(dbType.getValue(), virtualDataSource);
	}

	public static ISqlParser createParser(DBType dbType, SqlContainer container) throws Exception {
		ISqlParser parser = createParser(dbType);
		if(container != null && parser instanceof SqlParserBase) {
			((SqlParserBase) parser).setContainer(container);
		}
		return parser;
	}

	private static ISqlParser createParser(String type, DataSource ds) throws Exception {
		if(StringUtils.isEmpty(type)) {
			throw new Exception("Database type can not be empty!!!");
		}
		String dbType = type.trim();
		ISqlParser parser = null;
		if("mysql".equalsIgnoreCase(dbType)) {
		    parser = new MysqlParser(ds);
		}

		else if("mariadb".equalsIgnoreCase(dbType)) {
			parser = new MariaDBParser(ds);
		}

		else if("oracle".equalsIgnoreCase(dbType)) {
		    parser =  new OracleParser(ds);
		}

		else if("db2".equalsIgnoreCase(dbType)) {
		    parser =  new DB2Parser(ds);
		}

		else if("odbc".equalsIgnoreCase(dbType)) {
		    parser =  new OdbcParser(ds);
		}

		else if("postgresql".equalsIgnoreCase(dbType)) {
		    parser =  new PostgresqlParser(ds);
		}

		else if("sqlserver".equalsIgnoreCase(dbType)) {
		    parser =  new SqlserverParser(ds);
		}

		else if("sybase".equalsIgnoreCase(dbType)) {
		    parser =  new SybaseParser(ds);
		}

		else if("sqlite".equalsIgnoreCase(dbType)) {
		    parser =  new SqLiteParser(ds);
		}

		else if("couchbase".equalsIgnoreCase(dbType)) {
			parser =  new CouchbaseParser(ds);
		}

		else if("derby".equalsIgnoreCase(dbType)) {
			parser =  new DerbyParser(ds);
		}

		else if("hive2".equalsIgnoreCase(dbType)) {
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
		Pattern pattern = Pattern.compile("(?i)(oracle|sqlite|sqlserver|couchbase|derby|hive|mariadb|mysql|postgresql|sybase|db2|odbc)");
		Matcher matcher = pattern.matcher(url);
		if(matcher.find()) {
            type = matcher.group(1);
        }
		return type;
	}
}
