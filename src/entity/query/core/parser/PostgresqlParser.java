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
import entity.query.core.DataSource;
import entity.query.enums.AlterMode;
import entity.tool.util.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static entity.tool.util.StringUtils.isEmpty;

public class PostgresqlParser extends MysqlParser {

	public PostgresqlParser(DataSource ds) {
		super(ds);
	}

	@Override
	public String getPrefix() {
		return "\"";
	}

	@Override
	public String getSuffix() {
		return "\"";
	}

	@Override
	public String getTablesSql()
	{
		return "select relname as table_name, 'table' as type from pg_class c where relkind = 'r' and relname not like 'pg_%' and relname not like 'sql_%' order by relname";
	}

	@Override
	public String getColumnInfoListSql(String tablename)
	{
		return String.format( "SELECT col_description(a.attrelid,a.attnum) as column_comment,format_type(a.atttypid,a.atttypmod) as data_type,a.attname as column_name, a.attnotnull as notnull FROM pg_class as c inner join pg_attribute as a on c.relname='%s' and a.attrelid = c.oid and a.attnum>0;", tablename );
	}

	@Override
	public String getPrimaryKeySpl(String tablename) {
		return String.format( "SELECT A.COLUMN_NAME FROM information_schema.COLUMNS A\n" +
				"LEFT JOIN (\n" +
				"    SELECT pg_attribute.attname FROM pg_index, pg_class, pg_attribute \n" +
				"    WHERE pg_class.oid = '%s' :: regclass AND pg_index.indrelid = pg_class.oid AND \n" +
				"    pg_attribute.attrelid = pg_class.oid AND pg_attribute.attnum = ANY (pg_index.indkey) \n" +
				") B ON A.column_name = b.attname\n" +
				"WHERE A.table_schema = 'public' AND A.table_name='%s' AND length(B.attname)>0", tablename, tablename );
	}

	@Override
	public <T> String getCreateTableSql(String tablename, List<ColumnInfo> columns) {

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

            if(col.isPrimaryKey() && col.getDataType() != null &&
                    ("INTEGER".equals(col.getDataType().toUpperCase()) ||
                            "LONG".equals(col.getDataType().toUpperCase()) ||
                            "INT".equals(col.getDataType().toUpperCase()) ||
                            "BIGINT".equals(col.getDataType().toUpperCase()) ) ) {
				col.setIsAutoIncrement(true);
			}

			fillBuildColumnString(sb, col, false);

			if(col.isPrimaryKey()) {
				sb.append(" PRIMARY KEY ");
			}
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

		return String.format("CREATE TABLE \"%s\" (%s);", tablename, sb.substring(1));
	}

	public void fillBuildColumnString(StringBuffer sb, ColumnInfo col, boolean isAlter) {
		int len = col.getMaxLength() < 64 ? 64 : col.getMaxLength();
		String type = col.getIsAutoIncrement() ? "SERIAL" : getDbType(col.getType(), len, col.getDataType());

		String alterString = "";
		String left = "";
		String right = "";
		String defaultValue = "";
		if(isAlter) {
			if(col.getAlterMode() == null) {
				col.setAlterMode(AlterMode.CHANGE);
			}
			alterString = col.getAlterMode().getValue();
		}
		else {
			if(StringUtils.isNotEmpty(col.getDefaultValue())) {
				if(col.getDataType() != null &&
						("INTEGER".equals(col.getDataType().toUpperCase()) ||
								"LONG".equals(col.getDataType().toUpperCase()) ||
								"INT".equals(col.getDataType().toUpperCase()) ||
								"BIGINT".equals(col.getDataType().toUpperCase()) ) ) {
					defaultValue = String.format(" default %s ", col.getDefaultValue());
				}
				else {
					defaultValue = String.format(" default '%s' ", col.getDefaultValue());
				}
			}
		}

		sb.append(",");
		sb.append(alterString + "\""+ col.getColumnName() + "\" " + type +defaultValue);


		if(col.isCanNotNull()) {
			sb.append(" not null ");
		}


	}

	@Override
	public <T> String getAlterTableSql(String tablename, List<ColumnInfo> columns) {

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

		return String.format("ALTER TABLE \"%s\" %s ", tablename, sb.substring(1));
	}

	@Override
	public <T> String getTableExistSql(String tablename) {
		String sql = String.format("select count(*) from pg_class where relname = '%s';", tablename);

		return sql;
	}

	@Override
	protected String getSkipAndTop(int skip, int top) {

		String result = "";
		if(skip < 1 && top < 1){
			return result;
		}

		if(top > 1) {
			result += String.format(" LIMIT %s", top);
		}

		if(skip > 1) {
			result += String.format(" OFFSET %s", skip);
		}

		return result;
	}

	private String getDbType(Type type, int len, String defaultType) {

	    if(type == null) {

            if(StringUtils.isNotEmpty(defaultType)) {
                return ensureDefaultType(defaultType);
            }

            return "TEXT";
        }

		if(type.equals(Boolean.class)) {
			return "char(1)";
		}
		if(type.equals(Integer.class)) {
			return "integer";
		}
		if(type.equals(Long.class)) {
			return "bigint";
		}
		if(type.equals(BigDecimal.class)) {
			return "decimal";
		}
		if(type.equals(Float.class)) {
			return "real";
		}
		if(type.equals(Double.class)) {
			return "double";
		}
		if(type.equals(Date.class)) {
			return "date";
		}
		if(type.equals(String.class)) {
			return "VARCHAR";
		}
		if(type.equals(Blob.class)) {
			return "bytea";
		}

        if(StringUtils.isNotEmpty(defaultType)) {
            return ensureDefaultType(defaultType);
        }

		return "TEXT";
	}

    private String ensureDefaultType(String defaultType) {

        if(StringUtils.isEmpty(defaultType)) {
            return "VARCHAR";
        }
        if(defaultType.toLowerCase().equals("date")){
            return "DATE";
        }
        if(defaultType.toLowerCase().equals("datetime")){
            return "DATE";
        }
        if(defaultType.toLowerCase().equals("time")){
            return "DATE";
        }
        if(defaultType.toLowerCase().equals("integer")) {
            return "INTEGER";
        }
        if(defaultType.toLowerCase().equals("long")) {
            return "BIGINT";
        }
        if(defaultType.toLowerCase().equals("boolean")) {
            return "CHAR(1)";
        }
        if(defaultType.toLowerCase().equals("bool")) {
            return "CHAR(1)";
        }
        if(defaultType.toLowerCase().equals("nvarchar")) {
            return "varchar";
        }
        if(defaultType.toLowerCase().equals("string")) {
            return "VARCHAR";
        }
        if(defaultType.toLowerCase().equals("text")) {
            return "VARCHAR";
        }
        if(defaultType.toLowerCase().equals("float")) {
            return "REAL";
        }
        if(defaultType.toLowerCase().equals("double")) {
            return "DOUBLE";
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
