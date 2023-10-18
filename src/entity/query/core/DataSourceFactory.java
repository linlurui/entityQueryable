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
import entity.tool.util.JsonUtils;
import entity.tool.util.NumberUtils;
import entity.tool.util.ReflectionUtils;
import entity.tool.util.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.annotation.Annotation;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DataSourceFactory {

	private static final Logger log = LoggerFactory.getLogger( DataSourceFactory.class );

	private static final String regexUrl = "jdbc:(mysql|mariadb|sqlserver|db2|oracle|couchbase|derby|hive2|postgresql|sybase|sqlite|microsoft:sqlserver)(:\\w+:)?@?:?(//|\\(|:)?([\\w+\\.\\-_]+|\\w:)(:\\d+)?(/|;\\s*DatabaseName=|:|[\\w=\\(\\)\\s]+)([\\w\\-_\\.]+)";

	private static boolean hasLoadedXmlConfig = false;
	private static boolean hasLoadedYmlConfig = false;
	private String default_id = "entityQueryable.default.id";
	private volatile static Hashtable<String, DataSource> dataSourceMap = new Hashtable<String, DataSource>();
//	private ThreadLocal<Hashtable<String, DataSource>> threadLocal;

	public DataSourceFactory() {}

	private volatile static DataSourceFactory singleton;

	public static DataSourceFactory getInstance() {
		if (singleton == null) {
			synchronized (DataSourceFactory.class) {
				if (singleton == null) {
					singleton = new DataSourceFactory();
				}
			}
		}

		return singleton;
	}


	public Collection<DataSource> getAllDataSource() throws Exception {
		return getAllDataSource("");
	}

	public Collection<DataSource> getAllDataSource(String xmlFileName) throws Exception {
		if(dataSourceMap == null || dataSourceMap.size() < 1) {
			fillDatasourceByYaml();
			loadXmlConfig(xmlFileName);
		}

		return dataSourceMap.values();
	}

	public <T> DataSource getDataSource(Class<T> clazz) throws Exception {
		DataSourceFactory conf = DataSourceFactory.getInstance();
		DataSource ds;
		String dsname = findDataSourceAnnotation(clazz);
		DBConfig configAnn = clazz.getAnnotation(DBConfig.class);
		if(StringUtils.isNotEmpty(dsname)) {
			ds = conf.getDataSource(dsname);
		} else if(configAnn != null) {
			ds = conf.getDataSource(configAnn);
		}

		else {
			ds = conf.getDataSource();
		}

		if(ds == null) {
			throw new SQLException("can not find the data source!!!");
		}

		return ds;
	}

	public static <T> String findDataSourceAnnotation(Class<T> clazz) {
		entity.query.annotation.DataSource ann = clazz.getAnnotation(entity.query.annotation.DataSource.class);
		String ds = "";
		if(ann==null && clazz.getAnnotations()!=null && clazz.getAnnotations().length>0) {
			for (Annotation item : clazz.getAnnotations()) {
				if(ann!=null) {
					break;
				}
				List<Annotation> list = new ArrayList<Annotation>();
				if(item.annotationType().getAnnotations() != null) {
					for (Annotation a : item.annotationType().getAnnotations()) {
						list.add(a);
					}
				}
				for(int i=0; i<list.size(); i++) {
					if(StringUtils.isNotEmpty(ds)) {
						break;
					}
					if (list.get(i) instanceof entity.query.annotation.DataSource) {
						String value = ReflectionUtils.invoke(item.getClass(), item, "dataSource").toString();
						ann = (entity.query.annotation.DataSource) list.get(i);
						//InvocationHandler handler = Proxy.getInvocationHandler(ann1);
						//Field field = handler.getClass().getDeclaredField("memberValues");
						// 因为这个字段为 private final修饰所以要打开权限
						//field.setAccessible(true);
						// 获取memberValues
						//Map memberValues = (Map) field.get(handler);
						if(StringUtils.isNotEmpty(value)) {
							// 修改 value 属性值
							//memberValues.put("value", value);
							ds = value;
							break;
						}
					}
				}
			}
		}

		if(StringUtils.isNotEmpty(ds)) {
			return ds;
		}

		if(ann == null) {
			return "";
		}

		return ann.value();
	}

	private void fillDatasourceByYaml() throws IOException {

		if(hasLoadedYmlConfig && dataSourceMap.size() > 0) {
			return;
		}
		Object map = ApplicationConfig.getInstance().getMap("entity.datasource.environments");
		if(!ApplicationConfig.getInstance().containsKey("entity.datasource.environments")) {
			if(ApplicationConfig.getInstance().containsKey("spring.datasource.druid")) {
				map = ApplicationConfig.getInstance().getMap("spring.datasource.druid");
			}
			else {
				map = ApplicationConfig.getInstance().getMap("spring.datasource");
			}
		}

		if(map==null) {
			return;
		}

		if(map instanceof Map) {
			Map<String, Object> root = (Map<String, Object>)map;
			if(root.size() < 1) {
				return;
			}
			fillDataSourceFromEntityMap(root);
			hasLoadedYmlConfig = true;
		}
	}

	private void fillDataSourceFromEntityMap(Map<String, Object> params) throws IOException {
		Map<String, Object> root = JsonUtils.convert(params, Map.class);
		for(Map.Entry<String, Object> obj : root.entrySet()) {

			if(dataSourceMap.containsKey(obj.getKey())) {
				continue;
			}

			boolean isMap = obj.getValue() instanceof Map;
			if(!isMap) {
				if(obj.getValue() instanceof String) {
					Map<String, Object> temp = ApplicationConfig.getInstance().getMap(obj.getValue().toString());
					if(temp==null) {

					}
				}
				continue;
			}

			Map<String, Object> item = (Map<String, Object>) obj.getValue();
			if((!item.containsKey("driver") || item.get("driver")==null) &&
					item.containsKey("driverClassName") && item.get("driverClassName") != null &&
					StringUtils.isNotEmpty(item.get("driverClassName").toString())) {
				item.put("driver", item.get("driverClassName"));
				item.remove("driverClassName");
			}

			if(!item.containsKey("driver") || item.get("driver") == null) {
				continue;
			}

			if(!item.containsKey("url") || item.get("url") == null) {
				continue;
			}

			if(!item.containsKey("type") || item.get("type") == null) {
				item.put("type", "jdbc");
			}

			DataSource dataSource = new DataSource();

			dataSource.setId(obj.getKey());
			if(item.containsKey("classScope") && item.get("classScope")!=null) {
				dataSource.setClassScope(StringUtils.toString(item.get("classScope")));
			}

			if(item.containsKey("rxjava2") && item.get("rxjava2")!=null) {
				dataSource.setRxjava2("true".equals(item.get("rxjava2")));
			}

			dataSource.setDbType(StringUtils.toLowerCase(item.get("type")));
			dataSource.setDriverClassName(StringUtils.toString(item.get("driver")));
			String url = StringUtils.replaceAll(item.get("url"), "&amp;", "&");

			initDatasourceConfig(dataSource);

			if (url.toLowerCase().indexOf("autoreconnect=true") > 0) {
				dataSource.setAutoReconnect(true);
			}

			else {
				dataSource.setAutoReconnect(false);
			}

			dataSource.setSchema(getSchema(url));
			dataSource.setUrl(url);
			String dbtype = getDbType(url);
			dataSource.setValidationQuery(getVaildationQuery(dbtype));
			dataSource.setDbType(dbtype.toLowerCase());

			dataSource.setUsername(StringUtils.toString(item.get("username")));
			dataSource.setPassword(StringUtils.toString(item.get("password")));
			if (item.containsKey("initialSize") && item.get("initialSize") != null) {
				if (NumberUtils.parseInt(item.get("initialSize")) != null) {
					dataSource.setInitialSize(NumberUtils.parseInt(item.get("initialSize")));
				}
			}

			if (item.containsKey("maxActive") && item.get("maxActive") != null) {
				if (NumberUtils.parseInt(NumberUtils.parseInt(item.get("maxActive"))) != null) {
					dataSource.setMaxActive(NumberUtils.parseInt(item.get("maxActive")));
				}
			}

			if (item.containsKey("minIdle") && item.get("minIdle") != null) {
				if (NumberUtils.parseInt(NumberUtils.parseInt(item.get("minIdle"))) != null) {
					dataSource.setMinIdle(NumberUtils.parseInt(item.get("minIdle")));
				}
			}

			if (item.containsKey("maxWait") && item.get("maxWait") != null) {
				if (NumberUtils.parseInt(NumberUtils.parseInt(item.get("maxWait"))) != null) {
					dataSource.setMaxWait(NumberUtils.parseInt(item.get("maxWait")));
				}
			}

			if (item.containsKey("useUnfairLock") && item.get("useUnfairLock") != null) {
				if (item.get("useUnfairLock") != null) {
					dataSource.setUseUnfairLock(Boolean.parseBoolean(StringUtils.toString(item.get("useUnfairLock"))));
				}
			}

			if (item.containsKey("maxOpenPreparedStatements") && item.get("maxOpenPreparedStatements") != null) {
				dataSource.setPoolPreparedStatements(false);
				int value = NumberUtils.parseInt(StringUtils.toString(item.get("maxOpenPreparedStatements")));
				dataSource.setMaxOpenPreparedStatements(value);
				if (value > 0) {
					dataSource.setPoolPreparedStatements(true);
				}
			}

			if (item.containsKey("validationQuery") && item.get("validationQuery") != null) {
				dataSource.setValidationQuery(StringUtils.toString(item.get("validationQuery")));
			}

			if (item.containsKey("validationQueryTimeout") && item.get("validationQueryTimeout") != null) {
				dataSource.setValidationQueryTimeout(NumberUtils.parseInt(item.get("validationQueryTimeout")));
			}
			else if("sqlserver".equals(dataSource.getDbType())) {
				dataSource.setValidationQueryTimeout(10);
			}

			if (item.containsKey("minEvictableIdleTimeMillis") && item.get("minEvictableIdleTimeMillis") != null) {
				dataSource.setMinEvictableIdleTimeMillis(Long.parseLong(StringUtils.toString(item.get("minEvictableIdleTimeMillis"))));
			}

			if (item.containsKey("maxEvictableIdleTimeMillis") && item.get("maxEvictableIdleTimeMillis") != null) {
				dataSource.setMaxEvictableIdleTimeMillis(Long.parseLong(StringUtils.toString(item.get("maxEvictableIdleTimeMillis"))));
			}

			if (item.containsKey("timeBetweenEvictionRunsMillis") && item.get("timeBetweenEvictionRunsMillis") != null) {
				dataSource.setTimeBetweenEvictionRunsMillis(Long.parseLong(StringUtils.toString(item.get("timeBetweenEvictionRunsMillis"))));
			}

			if (item.containsKey("filters") && item.get("filters") != null) {
				if (StringUtils.isNotEmpty(StringUtils.toString(item.get("filters")))) {
					try {
						dataSource.setFilters(StringUtils.toString(item.get("filters")));
					} catch (SQLException e) {
						log.error(e.getMessage(), e);
					}
				}
			}

			dataSource.setDefault(Boolean.TRUE.equals(item.get("default")));
			if(dataSource.isDefault()) {
				default_id = obj.getKey();
			}

			dataSource.setId(obj.getKey());

			dataSourceMap.put(obj.getKey(), dataSource);
//				if(!dataSource.isSingle()) {
//					setToThreadLocal(dataSourceMap);
//				}
		}

		boolean hasDefault = false;
		for(Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
			if(entry.getValue().isDefault()) {
				hasDefault = true;
				break;
			}
		}
		if(!hasDefault) {
			for(Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
				entry.getValue().setDefault(true);
				break;
			}
		}
	}

//	private void setToThreadLocal(Hashtable<String, DataSource> dataSourceMap) {
//		if(threadLocal == null) {
//			threadLocal = new ThreadLocal<Hashtable<String, DataSource>>();
//		}
//		threadLocal.set(dataSourceMap);
//	}

	private void initDatasourceConfig(DataSource dataSource) {

		dataSource.setValidationQuery(getVaildationQuery(dataSource.getDbType()));
		dataSource.setTestOnBorrow(false);
		dataSource.setTestOnReturn(false);
		dataSource.setTestWhileIdle(true);
		dataSource.setTimeBetweenLogStatsMillis(120000);

		if("true".equals(ApplicationConfig.getInstance().get("${entity.debug}", ""))) {
			dataSource.setRemoveAbandoned(true);
			dataSource.setRemoveAbandonedTimeout(1800);
			dataSource.setLogAbandoned(true);
		}

		if(StringUtils.isNotEmpty(dataSource.getDbType()) && "|couchbase|derby|hive|mysql|mariadb|postgresql|sqlite|".contains(dataSource.getDbType())) {
			dataSource.setPoolPreparedStatements(false);
			dataSource.setMaxPoolPreparedStatementPerConnectionSize(-1);
		}
		else {
			dataSource.setPoolPreparedStatements(true);
			dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
		}
		dataSource.setTimeBetweenEvictionRunsMillis(60000);
		dataSource.setKeepAlive(false);
		dataSource.setInitialSize(5);
		dataSource.setMaxActive(100);
		dataSource.setMinIdle(5);
		dataSource.setMaxWait(60000);
		dataSource.setMinEvictableIdleTimeMillis(180000);
	}

	private void loadXmlConfig(String xmlFileName) throws SQLException, IOException, JDOMException {

		if(hasLoadedXmlConfig) {
			if(dataSourceMap.size() > 0 && StringUtils.isEmpty(xmlFileName)) {
				return;
			}
		}

		if (StringUtils.isEmpty(xmlFileName)) {
			xmlFileName = "db-config.xml";
		}

		synchronized (dataSourceMap) {

			InputStream configStream = getDBConfigStream(xmlFileName);

			if(configStream == null) {
				if(dataSourceMap.size() < 1) {
					throw new SQLException("Can not find xml file for database config!!!");
				}
				hasLoadedXmlConfig = true;
				return;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len = 0;
			while ((len = configStream.read(buffer)) != -1) {
				baos.write(buffer, 0, len);
			}
			configStream.close();
			String strXml = baos.toString();
			baos.close();
			strXml = strXml.replaceAll("\\s*<![^<>]+>", "");

			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(new ByteArrayInputStream(strXml.getBytes()));
			Element root = document.getRootElement();

			Element environments = root.getChild("environments");
			default_id = environments.getAttribute("default").getValue();
			List<Element> list = environments.getChildren();

			for (Element item : list) {

			    DataSource dataSource = new DataSource();

				String key = item.getAttribute("id").getValue();
				if(dataSourceMap.containsKey(key)) {
					continue;
				}

				if(item.getAttribute("classScope")!=null) {
					dataSource.setClassScope(item.getAttribute("classScope").getValue());
				}

				initDatasourceConfig(dataSource);

				Element dataSourceEle = item.getChild("dataSource");
				List<Element> datas = dataSourceEle.getChildren();

				for (Element child : datas) {
					if ("driver".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String driver = child.getAttribute("value").getValue();
						dataSource.setDriverClassName(driver);
						continue;
					}

					if("rxjava2".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						dataSource.setRxjava2("true".equals(child.getAttribute("value").getValue()));
						continue;
					}

					if ("url".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String url = child.getAttribute("value").getValue();
						if (StringUtils.isEmpty(url)) {
							throw new SQLException("Connection string can not be empty!");
						}
						url = url.replaceAll("&amp;", "&");
						if (url.toLowerCase().indexOf("autoreconnect=true") > 0) {
						    dataSource.setAutoReconnect(true);
						}

						else {
						    dataSource.setAutoReconnect(false);
						}

						dataSource.setSchema(getSchema(url));

						dataSource.setUrl(url);

						String dbtype = getDbType(url);
						dataSource.setValidationQuery(getVaildationQuery(dbtype));
						dataSource.setDbType(dbtype.toLowerCase());
						if(StringUtils.isNotEmpty(dataSource.getDbType()) && "|couchbase|derby|hive|mysql|mariadb|postgresql|sqlite|".contains(dataSource.getDbType())) {
							dataSource.setPoolPreparedStatements(false);
							dataSource.setMaxPoolPreparedStatementPerConnectionSize(-1);
						}
						else {
							dataSource.setPoolPreparedStatements(true);
							dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
						}
						continue;
					}

					if ("username".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String username = child.getAttribute("value").getValue();
						dataSource.setUsername(username);
						continue;
					}

					if ("password".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String password = child.getAttribute("value").getValue();
						dataSource.setPassword(password);
						continue;
					}

					if ("initialSize".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String poolMaximumIdleConnections = child.getAttribute("value").getValue();
						dataSource.setInitialSize(NumberUtils.parseInt(poolMaximumIdleConnections));
						continue;
					}

					if ("maxActive".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String poolMaximumActiveConnections = child.getAttribute("value").getValue();
						if (StringUtils.isNotEmpty(poolMaximumActiveConnections)) {
							dataSource.setMaxActive(NumberUtils.parseInt(poolMaximumActiveConnections));
						}
						continue;
					}

					if ("minIdle".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String minIdle = child.getAttribute("value").getValue();
						if (StringUtils.isNotEmpty(minIdle)) {
						    dataSource.setMinIdle(NumberUtils.parseInt(minIdle));
						}
						continue;
					}

					if ("maxWait".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String maxWait = child.getAttribute("value").getValue();
						if (StringUtils.isNotEmpty(maxWait)) {
						    dataSource.setMaxWait(NumberUtils.parseInt(maxWait));
						}
						continue;
					}

					if ("useUnfairLock".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String useUnfairLock = child.getAttribute("value").getValue();
						if (StringUtils.isNotEmpty(useUnfairLock)) {
						    dataSource.setUseUnfairLock(Boolean.valueOf(useUnfairLock));
						}
						continue;
					}

					if ("maxOpenPreparedStatements".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String maxOpenPreparedStatements = child.getAttribute("value").getValue();
						dataSource.setPoolPreparedStatements(false);
						if (StringUtils.isNotEmpty(maxOpenPreparedStatements)) {
							int value = NumberUtils.parseInt(maxOpenPreparedStatements);
							dataSource.setMaxOpenPreparedStatements(value);
							if (value > 0) {
							    dataSource.setPoolPreparedStatements(true);
							}
						}
						continue;
					}

					if ("validationQuery".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String validationQuery = child.getAttribute("value").getValue();
						if (StringUtils.isNotEmpty(validationQuery)) {
						    dataSource.setValidationQuery(validationQuery);
						}

						continue;
					}

					if ("validationQueryTimeout".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String validationQueryTimeout = child.getAttribute("value").getValue();
						if (StringUtils.isNotEmpty(validationQueryTimeout)) {
						    dataSource.setValidationQueryTimeout(NumberUtils.parseInt(validationQueryTimeout));
						}
						continue;
					}

					if ("minEvictableIdleTimeMillis".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String minEvictableIdleTimeMillis = child.getAttribute("value").getValue();
						if (StringUtils.isNotEmpty(minEvictableIdleTimeMillis)) {
							long value = NumberUtils.parseInt(minEvictableIdleTimeMillis);
							dataSource.setMinEvictableIdleTimeMillis(value);
						}
						continue;
					}

					if ("maxEvictableIdleTimeMillis".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String maxEvictableIdleTimeMillis = child.getAttribute("value").getValue();
						if (StringUtils.isNotEmpty(maxEvictableIdleTimeMillis)) {
							long value = NumberUtils.parseInt(maxEvictableIdleTimeMillis);
							dataSource.setMaxEvictableIdleTimeMillis(value);
						}
						continue;
					}

					if ("timeBetweenEvictionRunsMillis".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String timeBetweenEvictionRunsMillis = child.getAttribute("value").getValue();
						if (StringUtils.isNotEmpty(timeBetweenEvictionRunsMillis)) {
							long value = NumberUtils.parseInt(timeBetweenEvictionRunsMillis);
							dataSource.setTimeBetweenEvictionRunsMillis(value);
						}
						continue;
					}

					if ("filters".equalsIgnoreCase(child.getAttribute("name").getValue())) {
						String filters = child.getAttribute("value").getValue();
						if (StringUtils.isNotEmpty(filters)) {
						    dataSource.setFilters(filters);
						}
						continue;
					}
				}

				dataSource.setDefault(key.equals(default_id));

				dataSource.setId(key);

				dataSourceMap.put(key, dataSource);
//				if(!dataSource.isSingle()) {
//					setToThreadLocal(dataSourceMap);
//				}
			}
			hasLoadedXmlConfig = true;
		}
	}

	private InputStream getDBConfigStream(String xmlFileName) throws FileNotFoundException {

		//tomcat路径
		String property = System.getProperty("catalina.home");
		String path =property+ File.separator + "conf" + File.separator  + xmlFileName;
		File file = new File(path);

		if(file.exists()) {
			return new FileInputStream(file);
		}
		else {
			file = new File(System.getProperty("user.dir") + File.separator + "conf" + File.separator + xmlFileName);
		}

		if(file.exists()) {
			return new FileInputStream(file);
		}
		else {
			file = new File(System.getProperty("user.dir") + File.separator + "config" + File.separator + xmlFileName);
		}

		if(file.exists()) {
			return new FileInputStream(file);
		}
		else {
			file = new File(System.getProperty("user.dir") + File.separator + xmlFileName);
		}

		if(file.exists()) {
			return new FileInputStream(file);
		}
		else {
			file = new File(System.getProperty("user.dir") + File.separator + "resources" + File.separator + xmlFileName);
		}

		if(file.exists()) {
			return new FileInputStream(file);
		}

		return Thread.currentThread().getContextClassLoader().getResourceAsStream(xmlFileName);
	}

	private String getVaildationQuery(String dbtype) {
		if(StringUtils.isEmpty(dbtype)) {
			return "select 1";
		}
		if ("ORACLE".equals(dbtype.toUpperCase())) {
			return "select 1 from dual";
		} else if ("HSQLDB".equals(dbtype.toUpperCase())) {
			return "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS";
		} else if ("DB2".equals(dbtype.toUpperCase())) {
			return "select 1 from sysibm.sysdummy1";
		} else if ("DERBY".equals(dbtype.toUpperCase())) {
			return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
		} else if ("INFORMIX".equals(dbtype.toUpperCase())) {
			return "select count(*) from systables";
		}
		return "select 1";
	}

	private String getSchema(String url) {

		Pattern p = Pattern.compile(regexUrl);
		Matcher m = p.matcher(url);
		if(m.find() && m.groupCount()>=7) {
			return m.group(7);
		}

		return "";
	}

	private String getDbType(String url) {

		Pattern p = Pattern.compile(regexUrl);
		Matcher m = p.matcher(url);
		if(m.find()) {
			return m.group(1);
		}

		return "";
	}

	public DataSource getDataSource(DBConfig ann) throws Exception {

		if(StringUtils.isNotEmpty(ann.id())) {
			if(dataSourceMap.containsKey(ann.id())) {
				return dataSourceMap.get(ann.id());
			}

			if(
					StringUtils.isNotEmpty(ann.driver()) &&
					StringUtils.isNotEmpty(ann.db()) &&
					StringUtils.isNotEmpty(ann.driverType()) &&
					StringUtils.isNotEmpty(ann.dbType())
			) {


				if(dataSourceMap.containsKey(ann.id())) {
					return dataSourceMap.get(ann.id());
				}

				String url = getConnectionUrl(ann.driverType(), ann.dbType(), ann.server(), ann.port(), ann.db());
				DataSource dataSource = new DataSource();

				dataSource.setDbType(ann.dbType().toLowerCase());
				dataSource.setDriverClassName(ann.driver());
				dataSource.setAutoReconnect(ann.autoReconnect());
				dataSource.setUrl(url);
				dataSource.setUsername(ann.uid());
				dataSource.setPassword(ann.pwd());
				initDatasourceConfig(dataSource);

				if(ann.maxOpenPreparedStatements() > 0) {
					dataSource.setPoolPreparedStatements(true);
				}

				if(ann.maxOpenPreparedStatements() > 0) {
					dataSource.setMaxOpenPreparedStatements(ann.maxOpenPreparedStatements());
				}

				if(StringUtils.isNotEmpty(ann.validationQuery())){
					dataSource.setValidationQuery(ann.validationQuery());
				}

				if(ann.minEvictableIdleTimeMillis() > 0) {
					dataSource.setMinEvictableIdleTimeMillis(ann.minEvictableIdleTimeMillis());
				}

				if(StringUtils.isNotEmpty(ann.filters())) {
					dataSource.setFilters(ann.filters());
				}

				if(StringUtils.isNotEmpty(ann.validationQuery())) {
					dataSource.setValidationQuery(ann.validationQuery());
				}

				dataSource.setRxjava2(ann.rxjava2());

				dataSource.setSchema(ann.db());
				dataSourceMap.put(ann.id(), dataSource);

				return dataSource;
			}

			else if(StringUtils.isNotEmpty(ann.path())) {
				return getDataSource(ann.id(), ann.path());
			}
		}

		return getDataSource();
	}

	public String getConnectionUrl(String driverType, String dbType, String server, int port, String db) {

		if("mysql".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s://%s:%d/%s", driverType, dbType, server, port, db);
		}

		else if("mariadb".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s://%s:%d/%s", driverType, dbType, server, port, db);
		}

		else if("oracle".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s:thin:@//%s:%s/%s", driverType, dbType, server, port, db);
		}

		else if("db2".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s://%s:%s/%s", driverType, dbType, server, port, db);
		}

		else if("odbc".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s://%s:%d;Database=%s", driverType, dbType, server, port, db);
		}

		else if("postgresql".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s://%s:%s/%s", driverType, dbType, server, port, db);
		}

		else if("sqlserver".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s://%s:%d;Database=%s", driverType, dbType, server, port, db);
		}

		else if("sybase".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s:Tds:%s:%s/%s", driverType, dbType, server, port, db);
		}

		else if("sqlite".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s:%s", driverType, dbType, db);
		}

		else if("couchbase".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s://%s:%s", driverType, dbType, server, port);
		}

		else if("derby".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s://%s:%s/%s", driverType, dbType, server, port, db);
		}

		else if("hive2".equalsIgnoreCase(dbType.trim())) {
			return String.format("%s:%s://%s:%s/%s", driverType, dbType, server, port, db);
		}

		return "";
	}

	public DataSource getDataSource() throws SQLException {
		return getDataSource("");
	}

	public DataSource getDataSource(String id) throws SQLException {
		return getDataSource(id, "");
	}

	public DataSource getDataSource(String id, String path) throws SQLException {

		boolean flag = false;
		if (StringUtils.isEmpty(id) || id == "default") {
			id = default_id;
			flag = true;
		}

		if(dataSourceMap.size() > 0 && hasLoadedXmlConfig) {
			return dataSourceMap.get(id);
		}

		try {
			fillDatasourceByYaml();
			loadXmlConfig(path);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} catch (JDOMException e) {
			log.error(e.getMessage(), e);
		}

//		waittingDataSource(500);

		if(flag) {
			id = default_id;
		}

		return dataSourceMap.get(id);
	}

	private synchronized void waittingDataSource(long millis) {
		synchronized (dataSourceMap) {
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {}
		}
	}

	public static Properties loadConfigureFileXml(String configFileName) throws IOException {
		Properties props = new Properties();
		InputStream inputStream = null;
		try {
			inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName);
			props.loadFromXML(inputStream);
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
		return props;
	}

	public static Properties loadConfigureFilePropertie(String configFileName) throws IOException {
		Properties props = new Properties();
		InputStream inputStream = null;
		try {
			inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName);
			props.load(inputStream);
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
		return props;
	}
}
