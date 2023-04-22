/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.core.parser;


import entity.query.ColumnInfo;
import entity.query.annotation.Fieldname;
import entity.query.annotation.PrimaryKey;
import entity.query.core.ApplicationConfig;
import entity.query.core.DataSource;
import entity.query.core.SqlParserBase;
import entity.query.enums.AlterMode;
import entity.tool.util.OutParameter;
import entity.tool.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static entity.tool.util.StringUtils.isEmpty;
import static entity.tool.util.StringUtils.isNotEmpty;

public class MysqlParser extends SqlParserBase {

	public MysqlParser(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public String getPrefix() {
		return "`";
	}

	@Override
	public String getSuffix() {
		return "`";
	}

	@Override
	public <T> String getSelectSql(Class<T> clazz, int skip, int top, Boolean isCount) {
		Field[] flds = clazz.getDeclaredFields();
		String primaryKey = "";
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
				selectText = "COUNT(*)";
			}
			else {
				selectText = StringUtils.isEmpty(primaryKey) ? "COUNT(*)" : String.format("COUNT(%s)", primaryKey);
			}
		} else {
			selectText = container.Select.length()>0 ? container.Select.toString() : "*";
		}

		String fromText = "";
		OutParameter<Class<T>> param = new OutParameter<Class<T>>();
		param.setData(clazz);
		String tablename = getTablename(param);
		clazz = param.getData();
		fromText = tablename;

		String alias = "";
		param = new OutParameter<Class<T>>();
		param.setData(clazz);
		 tablename = getTablename(param);
		alias = tablename;

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

		String sql = "";
		if(!StringUtils.isEmpty(primaryKey) && container.From.length()==0 && skip>0) {
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
				selectText = selectText.replaceAll("[\\[`\"]?\\s*([\\w\\d_]+)\\s*[\\]`\"]?\\s*(,?)", String.format("%s%s%s.%s$1%s$2", getPrefix(), fromText, getSuffix(), getPrefix(), getSuffix()));
				selectText = selectText.replaceAll("\\?", "%s");
				selectText = String.format(selectText, list.toArray());
				selectText = selectText.replaceAll("\\(\\s*[`\"\\[]?\\s*([^(.)\\s`\"\\]\\[]+)\\s*[`\"\\]]?\\s*\\)", String.format("(%s%s%s.%s$1%s)", getPrefix(), fromText, getSuffix(), getPrefix(), getSuffix()));
			}
			sql = String.format("\nSELECT %s FROM %s %s %s %s %s \n", primaryKey, fromText, joinText, whereText, groupByText, getSkipAndTop(skip, top));
			sql = String.format("\nSELECT %s FROM %s INNER JOIN (%s) AS a ON a.%s=%s.%s %s\n", selectText, fromText, sql, primaryKey, alias, primaryKey, orderByText);
		} else {
			sql = String.format("\nSELECT %s FROM %s %s %s %s %s", selectText, fromText, joinText, whereText, groupByText, orderByText);
			if(skip > 0 || top > 0) {
				sql += getSkipAndTop(skip, top);
			}
		}

		if(container.Union.length() > 0) {
			sql = String.format("(%s) \n UNION \n %s", sql, container.Union.toString());
		}

		return String.format("%s;\n", sql);
	}

	protected String getSkipAndTop(int skip, int top) {
		return String.format(" LIMIT %s,%s", skip, top);
	}

	@Override
	public String getSelectNow() {
		return "select now();";
	}

    @Override
    public String getTablesSql()
    {
//        return String.format( "%s UNION %s;",
//                String.format( "SELECT table_name, 'table' as type FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='%s'", schema() ),
//                String.format( "SELECT table_name, 'view' as type FROM INFORMATION_SCHEMA.views WHERE TABLE_SCHEMA = '%s';", schema() )
//        );
		return String.format( "SELECT table_name, (case TABLE_TYPE when 'VIEW' then 'view' else 'table' end) as type, TABLE_COMMENT as description FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='%s'", schema() );
    }

	@Override
	public String getColumnInfoListSql(String tablename)
	{
		return String.format( "select COLUMN_NAME as column_name,COLUMN_COMMENT as column_comment,DATA_TYPE as data_type, case EXTRA when 'auto_increment' then 1 else 0 end as isAutoIncrement, CHARACTER_MAXIMUM_LENGTH as maxLength, NUMERIC_SCALE as `numericScale`, COLUMN_COMMENT as columnComment, (case IS_NULLABLE when 'YES' then 0 else 1 end) as canNotNull, COLUMN_DEFAULT as defaultValue, case COLUMN_KEY when 'PRI' then 1 else 0 end as isPrimaryKey from information_schema.COLUMNS where TABLE_NAME='%s' and TABLE_SCHEMA='%s'", tablename, schema() );
	}

	@Override
	public String getPrimaryKeySpl(String tablename) {
		return String.format( "SELECT COLUMN_NAME FROM information_schema.TABLE_CONSTRAINTS t JOIN information_schema.KEY_COLUMN_USAGE k USING (CONSTRAINT_NAME,TABLE_SCHEMA,TABLE_NAME) WHERE t.CONSTRAINT_TYPE='PRIMARY KEY' AND t.TABLE_NAME='%s' AND t.TABLE_SCHEMA='%s'", tablename, schema() );
	}


	@Override
	public <T> String getCreateTableSql(String tablename, List<ColumnInfo> columns) {

		if(isEmpty(tablename)) {
			return null;
		}

		if(columns == null || columns.size() < 1) {
			return null;
		}

		String pk = null;
		StringBuffer sb = new StringBuffer();
		for(ColumnInfo col : columns) {
            if(isEmpty(col.getColumnName()) || (col.getType() == null && StringUtils.isEmpty(col.getDataType()))) {
				continue;
			}

			String pkColumn = fillBuildColumnString(sb, col, false);
			if(StringUtils.isEmpty(pk)) {
				pk = pkColumn;
			}
		}

		if(isNotEmpty(pk)) {
			sb.append(String.format(",PRIMARY KEY (`%s`)", pk));
		}

		List<String> uniqueList = new ArrayList<String>();
		for (ColumnInfo a : columns) {
			if(a.isUnique()) {
				uniqueList.add(a.getColumnName());
			}
		}
		if(uniqueList.size() > 0) {
			sb.append(String.format(",constraint %s unique(%s)",
					StringUtils.join("_", uniqueList),
					getPrefix() + StringUtils.join(getSuffix() + "," + getPrefix(), uniqueList) + getSuffix()));
		}

		return String.format("CREATE TABLE `%s` (%s) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;", tablename, sb.substring(1));
	}

	private String fillBuildColumnString(StringBuffer sb, ColumnInfo col, boolean isAlter) {

		String pk = "";
		int len = col.getMaxLength() < 64 ? 64 : col.getMaxLength();
		String type = getDbType(col.getType(), col.getDataType());

		String alterString = "";
		if(isAlter) {
            if(col.getAlterMode() == null) {
                col.setAlterMode(AlterMode.CHANGE);
            }

			switch (col.getAlterMode()) {
				case CHANGE:
					alterString = " MODIFY ";
					break;
				case DROP:
					alterString = " DROP ";
					break;
				case ADD:
					alterString = " ADD ";
					break;
				case RENAME:
					alterString = " RENAME ";
					break;
			}
		}

		sb.append(",");

		if(col.getAlterMode() == AlterMode.DROP || col.getAlterMode() == AlterMode.RENAME) {
			col.setCanNotNull(false);
			sb.append(alterString + "`"+ col.getColumnName() + "` ");
		}

		else if("decimal".equals(type.toLowerCase()) || "float".equals(type.toLowerCase()) || "double".equals(type.toLowerCase())){
            sb.append(alterString + "`"+ col.getColumnName() + "` " + type + String.format("(%s, 4)", len));
        }

        else if("varchar".equals(type.toLowerCase())) {
            sb.append(alterString + "`"+ col.getColumnName() + "` " + type + String.format("(%s)", len));
        }

        else {
            sb.append(alterString + "`"+ col.getColumnName() + "` " + type);
        }

		if(col.isCanNotNull()) {
            sb.append(" not null ");
        }

        if(!isAlter) {

			if(StringUtils.isNotEmpty(col.getDefaultValue())) {
				if(col.getDataType() != null &&
						("INTEGER".equals(col.getDataType().toUpperCase()) ||
								"LONG".equals(col.getDataType().toUpperCase()) ||
								"INT".equals(col.getDataType().toUpperCase()) ||
								"BIGINT".equals(col.getDataType().toUpperCase()) ) ) {
					sb.append(String.format(" default %s ", col.getDefaultValue()));
				}
				else {
					sb.append(String.format(" default '%s' ", col.getDefaultValue()));
				}
			}

			if(col.isPrimaryKey() && col.getDataType() != null &&
					("INTEGER".equals(col.getDataType().toUpperCase()) ||
							"LONG".equals(col.getDataType().toUpperCase()) ||
							"INT".equals(col.getDataType().toUpperCase()) ||
							"BIGINT".equals(col.getDataType().toUpperCase()) ) ) {
				col.setIsAutoIncrement(true);
			}

			if (col.getIsAutoIncrement()) {
				sb.append(" AUTO_INCREMENT ");
			}

			if (col.isPrimaryKey()) {
				pk = col.getColumnName();
			}

			if (col.getIsAutoIncrement() && !col.getColumnName().equals(pk)) {
				sb.append(String.format(",UNIQUE KEY `%s` (`%s`)", col.getColumnName(), col.getColumnName()));
			}
		}
        else {
			if (col.getIsAutoIncrement()) {
				sb.append(" AUTO_INCREMENT ");
			}
		}

		return pk;
	}

	@Override
	public <T> String getAlterTableSql(String tablename, List<ColumnInfo> columns, List<ColumnInfo> storedColumns) {

		if(isEmpty(tablename)) {
			return null;
		}

		if(columns == null || columns.size() < 1) {
			return null;
		}

		StringBuffer sb = new StringBuffer();
		for(ColumnInfo col : columns) {
            if(isEmpty(col.getColumnName()) || (col.getType() == null && StringUtils.isEmpty(col.getDataType()))) {
				continue;
			}

			fillBuildColumnString(sb, col, true);
		}

		return String.format("ALTER TABLE `%s` %s ", tablename, sb.substring(1));
	}

	@Override
	public <T> String getTableExistSql(String tablename) {
		String sql = String.format("select count(TABLE_NAME) from information_schema.TABLES where TABLE_SCHEMA='%s' and TABLE_NAME='%s';", this.dataSource.getId(), tablename);

		return sql;
	}

	private String getDbType(Type type, String defaultType) {

        if(type == null) {

            if(StringUtils.isNotEmpty(defaultType)) {
                return ensureDefaultType(defaultType);
            }

            return "TEXT";
        }

        if(type.equals(Boolean.class)) {
			return "bit";
		}
		if(type.equals(Integer.class)) {
			return "int";
		}
		if(type.equals(Long.class)) {
			return "bigint";
		}
		if(type.equals(BigDecimal.class)) {
			return "decimal";
		}
		if(type.equals(Float.class)) {
			return "float";
		}
		if(type.equals(Double.class)) {
			return "double";
		}
		if(type.equals(Date.class)) {
			return "datetime";
		}
		if(type.equals(String.class)) {
			return "VARCHAR";
		}
		if(type.equals(Blob.class)) {
			return "BLOB";
		}

		if(StringUtils.isNotEmpty(defaultType)) {
			return ensureDefaultType(defaultType);
		}

		return "TEXT";
	}

	private String ensureDefaultType(String defaultType) {

		if(StringUtils.isEmpty(defaultType)) {
			return "varchar";
		}
		if(defaultType.toLowerCase().equals("date")){
			return "datetime";
		}
		if(defaultType.toLowerCase().equals("datetime")){
			return "datetime";
		}
		if(defaultType.toLowerCase().equals("time")){
			return "datetime";
		}
		if(defaultType.toLowerCase().equals("integer")) {
            return "int";
        }
		if(defaultType.toLowerCase().equals("long")) {
            return "bigint";
        }
		if(defaultType.toLowerCase().equals("boolean")) {
            return "bit";
        }
		if(defaultType.toLowerCase().equals("bool")) {
            return "bit";
        }
		if(defaultType.toLowerCase().equals("nvarchar")) {
			return "varchar";
		}
		if(defaultType.toLowerCase().equals("string")) {
            return "varchar";
        }
		if(defaultType.toLowerCase().equals("text")) {
            return "text";
        }
		if(defaultType.toLowerCase().equals("image")) {
			return "BLOB";
		}
		if(defaultType.toLowerCase().equals("money")) {
			return "decimal";
		}
		return defaultType;
	}
}
