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

import entity.query.Queryable;
import entity.query.annotation.*;
import entity.query.enums.CommandMode;
import entity.query.enums.Condition;
import entity.query.enums.JoinMode;
import entity.tool.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.sql.Blob;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class SqlParserBase implements ISqlParser {

	private static final Logger log = LoggerFactory.getLogger(SqlParserBase.class);

	protected SqlContainer container;

	protected DataSource dataSource;

	public DataSource getDataSource() {
		return dataSource;
	}

	public SqlParserBase(DataSource dataSource) {
		container = new SqlContainer();
		this.dataSource = dataSource;
	}

	@Override
	public String schema() {
		return this.dataSource.getSchema();
	}

	@Override
	public boolean hasGroupBy() {

		if(container.GroupBy == null) {
			return false;
		}

		return StringUtils.isNotEmpty(container.GroupBy.toString());
	}

	@Override
	public void addWhere(String exp) {

		if(StringUtils.isEmpty(exp)) {
			return;
		}

		container.Where.setLength(0);
		this.cleanSelectList();

		container.Where.append(filter(exp));
	}

	@Override
	public void cleanSelectList() {
		container.Select.setLength(0);
	}

	@Override
	public void addWhere(Condition condition, String exp) {

		if(StringUtils.isEmpty(exp)) {
			return;
		}

		if(condition == Condition.AND) {
			container.Where.append(String.format(" AND %s ", filter(exp)));
		}

		else if(condition == Condition.OR) {
			container.Where.append(String.format(" OR %s ", filter(exp)));
		}
	}

	@Override
	public String getWhereString() {
		return container.Where.toString();
	}

	@Override
	public void addSelect(String exp) {
		if(StringUtils.isEmpty(exp)) {
			return;
		}

		if(container.Select.length() > 0) {
			container.Select.append(", ");
		}
		container.Select.append(filter(exp));

	}

	@Override
	public void addOrderBy(String exp) {
		if(StringUtils.isEmpty(exp)) {
			return;
		}

		container.OrderBy.setLength(0);
		container.OrderBy.append(filter(exp));
	}

	@Override
	public void addGroupBy(String exp) {
		if(StringUtils.isEmpty(exp)) {
			return;
		}

		container.GroupBy.setLength(0);
		container.GroupBy.append(filter(exp));
	}

	@Override
	public void addJoin(JoinMode mode, String exp, String alias) {

		if(StringUtils.isEmpty(alias) || StringUtils.isEmpty(exp)) {
			return;
		}
		container.Join.add(String.format(" %s JOIN (%s) AS %s ", mode.toString(), filter(exp.substring(0, exp.length()-2)), alias));
	}

	@Override
	public <T> void addJoin(JoinMode mode, Class<T> clazz) {
		addJoin(mode, clazz, "");

	}


	@Override
	public <T> void addJoin(JoinMode mode, Class<T> clazz, String alias) {

		OutParameter<Class<T>> param = new OutParameter<Class<T>>();
		param.setData(clazz);
		String tablename = getTablename(param);
		clazz = param.getData();

		if(StringUtils.isEmpty(alias)) {
			container.Join.add(String.format(" %s JOIN [%s] ", mode.toString(), tablename));
			return;
		}

		container.Join.add(String.format(" %s JOIN [%s] AS %s ", mode.toString(), tablename, alias));
	}

   @Override
    public <T> void addFrom(Class<T> clazz, String alias) {

        if(StringUtils.isEmpty(alias)) {
            return;
        }
		OutParameter<Class<T>> param = new OutParameter<Class<T>>();
		param.setData(clazz);
		String tablename = getTablename(param);
		clazz = param.getData();

        container.From.append(String.format(" %s AS %s ", tablename, alias.replaceAll( "['\"]", "" )));
    }

	@Override
	public void addFrom(String exp, String alias) {

		if(StringUtils.isEmpty(exp) || StringUtils.isEmpty(alias)) {
			return;
		}

		container.From.append(String.format(" %s AS %s ", filter(exp.substring(0, exp.length() - 1)), alias.replaceAll( "['\"]", "" )));
	}

	@Override
	public void addUnioin(String selectSql) {

		selectSql = selectSql.substring(0, selectSql.length() - 2);
		if(container.Union.length() > 0) {
			container.Union.append(String.format(" UNION (%s)", filter(selectSql)));
		} else {
			container.Union.append(String.format("(%s)", filter(selectSql)));
		}
	}

	@Override
	public void addOn(String exp) {

		if(exp == null || StringUtils.isEmpty(exp)) {
			return;
		}

		container.On.add(filter(exp));
	}

	@Override
	public <T> Object[] getArgs(Class<T> clazz, String sql, Object obj, Map<Integer, Blob> blobMap) {

		if(clazz == null) {
			return null;
		}

	    Pattern p = Pattern.compile(RegexUtils.Args);
	    Pattern p2 = Pattern.compile("'([^']*)'");
	    Matcher m = p.matcher(sql);
	    List<String> result = new ArrayList<String>();
		Field[] flds = clazz.getDeclaredFields();
		int i = 0;
	    while (m.find()) {
	    	String fieldName = m.group(1);
	    	Object value = ReflectionUtils.getFieldValue(clazz, obj, fieldName);
			if(value==null && sql.replace("\n", "").toUpperCase().startsWith("INSERT INTO")) {
				Optional<Field> optional = Arrays.stream(flds).filter(a -> a.getName().equals(fieldName) &&
						String.class.equals(a.getType())).findFirst();
				if(optional.isPresent()) {
					if(optional.get().getAnnotation(PrimaryKey.class)!=null) {
						value = UUID.randomUUID().toString().replace("-", "");
					}
				}
			}
	    	String strValue = "?";
			if(value instanceof Blob) {
				if(blobMap != null) {
					blobMap.put(i, (Blob) value);
					result.add(strValue);
					i++;
				}
				continue;
			}

			strValue = ensureValue(p2, m, fieldName, value);

			result.add(strValue);
	    }

	    return result.toArray();
	}

	private String ensureValue(Pattern pattern, Matcher matcher, String fieldName, Object value) {
		String strValue;
		strValue = DBUtils.getStringValue(value);

		if(value instanceof UUID) {
			strValue = String.format("'%s'", value.toString().replace("-", ""));
		}

		else {
			Matcher m2 = pattern.matcher(strValue);
			if (m2.find()) {
				strValue = String.format("'%s'",
						matcher.group().replace("'", "").replace(String.format("#{%s}", fieldName), m2.group(1)));
			}
		}
		return strValue;
	}

	@Override
	public <T> String getTablename(OutParameter<Class<T>> param) {
		String table = "";
		Class<T> clazz = (Class<T>) getClass(param.getData());
		param.setData(clazz);

		Tablename ann = clazz.getAnnotation(Tablename.class);
		if(ann==null && clazz.getAnnotations()!=null && clazz.getAnnotations().length>0) {
			for (Annotation item : clazz.getAnnotations()) {
				if(StringUtils.isNotEmpty(table)) {
					break;
				}
				List<Annotation> list = Arrays.stream(item.annotationType().getAnnotations()).collect(Collectors.toList());
				for(int i=0; i<list.size(); i++) {
					if (list.get(i) instanceof Tablename) {
						String value = ReflectionUtils.invoke(item.getClass(), item, "table").toString();
						ann = (Tablename) list.get(i);
						//InvocationHandler handler = Proxy.getInvocationHandler(ann);
						//Field field = handler.getClass().getDeclaredField("memberValues");
						// 因为这个字段为 private final修饰所以要打开权限
						//field.setAccessible(true);
						// 获取memberValues
						//Map memberValues = (Map) field.get(handler);
						if(StringUtils.isNotEmpty(value)) {
							// 修改 value 属性值
							//memberValues.put("value", value);
							table = value;
							break;
						}
					}
				}
			}
		}

		if(StringUtils.isEmpty(table)) {
			if (ann == null || StringUtils.isEmpty(ann.value())) {
				table = clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
			} else {
				table = ann.value();
			}
		}

		if(StringUtils.isEmpty(ApplicationConfig.getInstance().get(table, ""))) {
			return getPrefix() + table + getSuffix();
		}

		table = ApplicationConfig.getInstance().get(table, table);

		return getPrefix() + table + getSuffix();
	}

	private <T> Class<? super T> getClass(Class<T> clazz) {

		if(clazz.getSuperclass() == null) {
			return clazz;
		}

		if(clazz.getSuperclass().equals(Queryable.class)) {
			return clazz;
		}

		return getClass(clazz.getSuperclass());
	}

	@Override
	public <T> String getInsertSql(Class<T> clazz) {

		OutParameter<Class<T>> param = new OutParameter<Class<T>>();
		param.setData(clazz);
		String tablename = getTablename(param);
		clazz = param.getData();

		String names = "";
		String values = "";
		Field[] flds = clazz.getDeclaredFields();
		for(Field fld : flds) {
			AutoIncrement ai = fld.getAnnotation(AutoIncrement.class);
			if(ai != null) {
				continue;
			}

			if(Modifier.isStatic(fld.getModifiers())) {
				continue;
			}

			Exclude exclude = fld.getAnnotation(Exclude.class);
			if(exclude != null) {
				continue;
			}

			Fieldname name = fld.getAnnotation(Fieldname.class);
			if(name != null) {
				names += String.format( ", %s%s%s", getPrefix(), ApplicationConfig.getInstance().get(name.value()), getSuffix());
				values += "," + String.format("#{%s}", fld.getName());
			}

			else {
				names += String.format(", %s%s%s", getPrefix(), fld.getName(), getSuffix());
				values += "," + String.format("#{%s}", fld.getName());
			}
		}
		if(StringUtils.isEmpty(names)) {
			return "";
		}

		names = names.substring(1);
		values = values.substring(1);

		return String.format("\nINSERT INTO %s (%s) VALUES (%s)\n", tablename, names, values);
	}

	@Override
	public <T> String getDeleteSql(Class<T> clazz) {

		String where = container.Where.length() > 0 ? String.format("WHERE %s", container.Where.toString()) : "";
		OutParameter<Class<T>> param = new OutParameter<Class<T>>();
		param.setData(clazz);
		String tablename = getTablename(param);
		clazz = param.getData();

		return String.format("\nDELETE FROM %s %s\n", tablename, where);
	}

	@Override
	public <T> String getUpdateSql(Class<T> clazz) {
		OutParameter<Class<T>> param = new OutParameter<Class<T>>();
		param.setData(clazz);
		String tablename = getTablename(param);
		clazz = param.getData();

		String settor = "";
		Field[] flds = clazz.getDeclaredFields();
		for(Field fld : flds) {
			AutoIncrement ai = fld.getAnnotation(AutoIncrement.class);
			if(ai != null) {
				continue;
			}

			if(Modifier.isStatic(fld.getModifiers())) {
				continue;
			}

			Fieldname name = fld.getAnnotation(Fieldname.class);
			if(name != null) {
				settor += String.format(",%s%s%s=#{%s}", getPrefix(), ApplicationConfig.getInstance().get(name.value()), getSuffix(), fld.getName());
			}

			else {
				settor += String.format(",%s%s%s=#{%s}", getPrefix(), fld.getName(), getSuffix(), fld.getName());
			}
		}
		if(StringUtils.isEmpty(settor)) {
			return "";
		}

		settor = settor.substring(1);

		return String.format("\nUPDATE %s SET %s\n", tablename, settor);
	}

	@Override
	public <T> String getUpdateSql(Class<T> clazz, String exp) {
		OutParameter<Class<T>> param = new OutParameter<Class<T>>();
		param.setData(clazz);
		String tablename = getTablename(param);
		clazz = param.getData();

		String where = container.Where.length()>0 ? String.format("WHERE %s", container.Where.toString()) : "";

		return String.format("\nUPDATE %s SET %s %s \n", tablename, filter(exp), where);
	}

	@Override
	public <T> String getInsertToSql(Class<T> clazz) {
		String sql = getSelectSql(clazz);
		if(StringUtils.isEmpty(sql)) {
			return "";
		}

		sql = sql.substring(0, sql.length() - 2);

		OutParameter<Class<T>> param = new OutParameter<Class<T>>();
		param.setData(clazz);
		String tablename = getTablename(param);
		clazz = param.getData();

		String names = "";
		Field[] flds = clazz.getDeclaredFields();
		for(Field fld : flds) {
			AutoIncrement ai = fld.getAnnotation(AutoIncrement.class);
			if(ai != null) {
				continue;
			}

			if(Modifier.isStatic(fld.getModifiers())) {
				continue;
			}

			Fieldname name = fld.getAnnotation(Fieldname.class);
            if(name != null) {
                names += String.format( ", %s%s%s", getPrefix(), ApplicationConfig.getInstance().get(name.value()), getSuffix());
            }

            else {
                names += String.format( ", %s%s%s", getPrefix(), fld.getName(), getSuffix());
            }
		}
		if(StringUtils.isEmpty(names)) {
			return "";
		}

		names = names.substring(1);

		return String.format("\nINSERT INTO %s (%s) %s\n", tablename, names, sql);
	}

	@Override
	public <T> String getSelectSql(Class<T> clazz) {

		return getSelectSql(clazz, 0, 0, false);
	}


	@Override
	public <T> String getSelectSql(Class<T> clazz, int skip, int top, Boolean isCount) {
		Field[] flds = clazz.getDeclaredFields();
		String primaryKey = null;
		for(Field fld : flds) {

			if(Modifier.isStatic(fld.getModifiers())) {
				continue;
			}

			PrimaryKey pk = fld.getAnnotation(PrimaryKey.class);
			if(pk != null) {
				Fieldname name = fld.getAnnotation(Fieldname.class);
				if(name != null) {
					primaryKey = ApplicationConfig.getInstance().get(name.value());
				}

				else {
					primaryKey = fld.getName();
				}

				break;
			}
		}

		String selectText = "*";
		if(isCount) {
			if(container.Join.size() > 0) {
				selectText = "COUNT(*) AS TOTAL";
			}
			else {
				selectText = StringUtils.isEmpty(primaryKey) ? "COUNT(*) AS TOTAL" : String.format("COUNT(%s) AS TOTAL", primaryKey);
			}
		} else {
			selectText = container.Select.length()>0 ? container.Select.toString() : "*";
		}

		String fromText = "";
		if(container.From.length()>0) {
			fromText = container.From.toString();
		}

		else {
			OutParameter<Class<T>> param = new OutParameter<Class<T>>();
			param.setData(clazz);
			String tablename = getTablename(param);
			clazz = param.getData();
			fromText = tablename;
		}

		String alias = "";
		if(container.From.length()>0) {
			String[] arr = container.From.toString().toLowerCase().split("as");
			alias = arr[arr.length - 1];
		}

		else{
			OutParameter<Class<T>> param = new OutParameter<Class<T>>();
			param.setData(clazz);
			alias = getTablename(param);
			clazz = param.getData();
		}
		String whereText = container.Where.length()>0 ? String.format("WHERE %s", container.Where.toString()) : "";
		String joinText = "";
		for(int i=0; i<container.Join.size(); i++) {
			joinText += String.format(" %s ", container.Join.get(i));
			if(container.On.size() - 1 >= i) {
				joinText += String.format(" ON %s", container.On.get(i));
			}
		}
		String groupByText = container.GroupBy.length()>0 ? String.format(" GROUP BY %s", container.GroupBy.toString()) : "";
		String orderByText = container.OrderBy.length()>0 ? String.format(" ORDER BY %s", container.OrderBy.toString()) : "";

		String topText = top>0 ? String.format("TOP %s", top) : "";
		String skipText = skip>0 ? String.format("WHERE ROWNUM>%s", skip) : "";

		String sql = "";
		if(!StringUtils.isEmpty(primaryKey) && skip>0) {
			if(selectText.equals("*")) {
				selectText = fromText + ".*";
			}

			else {
				String regex = "([`\"\\[]?\\s*[\\w\\d_]+[`\"\\]]?\\.[`\"\\[]?[\\w\\d_]+\\s*[`\"\\]]?| AS\\s+[`\"\\[]?\\s*[\\w\\d_]+\\s*[`\"\\]]?|[\\w\\d_]+\\([\\w\\d_]+\\))";
			    Pattern p = Pattern.compile(regex);
			    Matcher m = p.matcher(selectText);
			    List<String> list = new ArrayList<String>();
			    while (m.find()) {
			    	list.add(m.group(0));
			    }
			    selectText = selectText.replaceAll(regex, "?");
			    selectText = selectText.replaceAll("[\\[`\"]?\\s*([\\w\\d_]+)\\s*[\\]`\"]?[^\\.?]\\s*,?", String.format("%s%s%s.%s$1%s", getPrefix(), fromText, getSuffix(), getPrefix(), getSuffix()));
				selectText = selectText.replaceAll("\\?", "%s");
				selectText = String.format(selectText, list.toArray());
				selectText = selectText.replaceAll("\\(\\s*[`\"\\[]?\\s*([^(.)\\s`\"\\]\\[]+)\\s*[`\"\\]]?\\s*\\)", String.format("(%s%s%s.%s$1%s)", getPrefix(), fromText, getSuffix(), getPrefix(), getSuffix()));
			}

			sql = String.format("\nSELECT %s %s, ROW_NUMBER() OVER(%s %s) AS ROWNUM FROM %s %s %s \n", ((top + skip)>0 ? String.format("TOP %s", (top + skip)) : ""), primaryKey, groupByText, orderByText, fromText, joinText, whereText);
			sql = String.format("\nSELECT %s %s FROM %s LEFT JOIN (%s) AS a ON a.%s=%s.%s %s WHERE ROWNUM>%s \n", topText, selectText, fromText, sql, primaryKey, alias, primaryKey, orderByText, skip);
		} else {
			sql = String.format("\nSELECT %s FROM %s %s %s\n", selectText, fromText, joinText, whereText);

			if(skip>0 && top>0) {
				sql = String.format("SELECT %s * FROM (SELECT a.*, ROW_NUMBER() OVER(%s %s) AS ROWNUM FROM (%s) AS a) AS a %s", topText, groupByText, orderByText, sql, skipText);
			}

			else if(skip>0) {
				sql = String.format("SELECT * FROM (SELECT a.*, ROW_NUMBER() OVER(%s %s) AS ROWNUM FROM (%s) AS a) AS a %s", groupByText, orderByText, sql, skipText);
			}

			else if(top>0) {
				sql = String.format("SELECT %s * FROM (%s) AS a %s %s", topText, sql, groupByText, orderByText);
			} else {
				sql = String.format("%s %s %s", sql, groupByText, orderByText);
			}
		}

		if(container.Union.length() > 0) {
			sql = String.format("(%s) \n UNION \n %s", sql, container.Union.toString());
		}

		return String.format("%s", sql);
	}

	@Override
	public <T> String getSelectExistSql(Class<T> clazz) {
		String sql = getSelectSql(clazz, 0, 1, true);
		return sql.substring(0, sql.length() - 2);
	}

	@Override
	public <T> String toString( Class<T> clazz, String exp, CommandMode cmdMode, Object obj, int skip, int top, Boolean isCount, Map<Integer, Blob> blobMap ) {
		String sql = "";
		switch(cmdMode) {
		case Insert:
			sql = getInsertSql(clazz);
			break;
		case Update:
			sql = getUpdateSql(clazz);
			break;
		case InsertFrom:
			sql = getInsertToSql(clazz);
			break;
		case UpdateFrom:
			sql = getUpdateSql(clazz, exp);
			break;
		case Delete:
		case DeleteFrom:
			sql = getDeleteSql(clazz);
			break;
		case Exist:
			sql = getSelectExistSql(clazz);
			break;
		case Tables:
		    return getTablesSql();
		case ColumnsInfo:
		    return getColumnInfoListSql(exp);
		case PrimaryKey:
			return  getPrimaryKeySpl(exp);
		default:
			sql = getSelectSql(clazz, skip, top, isCount);
			break;
		}

		Object[] args = getArgs(clazz, sql, obj, blobMap);

		sql = DBUtils.getSql(sql, args);

		return sql;
	}

	public SqlContainer getContainer() {
		return container;
	}

	public String getPrefix() {
		return "[";
	}

	public String getSuffix() {
		return "]";
	}

	protected String filter(String exp) {
	    Pattern p = Pattern.compile(RegexUtils.PrefixAndSuffixForChar);
	    Matcher m = p.matcher(exp);
	    List<String> list = new ArrayList<String>();
	    while (m.find()) {
	    	list.add(m.group(0));
	    }

		exp = exp.replaceAll(RegexUtils.PrefixAndSuffixForChar, "%s");
		exp = exp.replaceAll(RegexUtils.PrefixAndSuffix, String.format("%s$1%s", getPrefix(), getSuffix()));
		if(list.size() > 0) {
			exp = String.format(exp, list.toArray());
		}

		return exp;
	};

	public abstract String getSelectNow();

	@Override
	public String getDropTableSql(String tablename) {
		return "DROP TABLE " + tablename;
	}
}
