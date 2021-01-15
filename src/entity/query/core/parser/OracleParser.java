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
import entity.query.core.SqlParserBase;
import entity.query.enums.AlterMode;
import entity.tool.util.OutParameter;
import entity.tool.util.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Blob;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static entity.tool.util.StringUtils.isEmpty;
import static entity.tool.util.StringUtils.isNotEmpty;

public class OracleParser extends SqlParserBase {

	public OracleParser(DataSource ds) {
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
	public <T> String getDeleteSql(Class<T> genericType) {
		
		String where = container.Where.length() > 0 ? String.format("WHERE %s", container.Where.toString()) : "";
		OutParameter<Class<T>> param = new OutParameter<Class<T>>();
		param.setData(genericType);
		String tablename = getTablename(param);
		genericType = param.getData();
		
		return String.format("\nDELETE %s %s\n", tablename, where);
	}
	
	@Override
	public <T> String getSelectExistSql(Class<T> clazz) {
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
		
		String whereText = container.Where.length()>0 ? String.format("WHERE %s", container.Where.toString()) : "";
		
		return String.format("\nSELECT COUNT(1) FROM %s %s LIMIT 0,1 \n", fromText, whereText);
	}

	@Override
	public String getSelectNow() {
		return "select sysdate()";
	}

    @Override
    public String getTablesSql()
    {
        return "select table_name from user_tables where TABLESPACE_NAME is not null";
    }

    @Override
    public String getColumnInfoListSql( String tablename )
    {
        return String.format( "SELECT b.column_name column_name,b.data_type data_type,a.comments column_comment FROM user_col_comments a,all_tab_columns b WHERE a.table_name = b.table_name and a.table_name='%s'", tablename.toUpperCase() );
    }

	@Override
	public String getPrimaryKeySpl(String tablename) {
		return String.format( "select col.column_name from user_constraints con,  user_cons_columns col where con.constraint_name = col.constraint_name and con.constraint_type='P' and col.table_name='%s'", tablename.toUpperCase() );
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

            if(col.isPrimaryKey() && col.getDataType() != null &&
                    ("INTEGER".equals(col.getDataType().toUpperCase()) ||
                            "LONG".equals(col.getDataType().toUpperCase()) ||
                            "INT".equals(col.getDataType().toUpperCase()) ||
                            "BIGINT".equals(col.getDataType().toUpperCase()) ) ) {
				col.setIsAutoIncrement(true);
			}

			fillBuildColumnString(sb, col, false);

			if(col.isPrimaryKey()) {
				pk = col.getColumnName();
			}
		}

		if(isNotEmpty(pk)) {
			sb.append(String.format(",constraint PK_%s primary key (%s)", tablename, pk));
		}

		List<String> uniqueList = columns.stream().filter(a -> a.isUnique()).map(b -> b.getColumnName()).collect(Collectors.toList());
		if(uniqueList.size() > 0) {
			sb.append(String.format(",constraint %s unique(%s)",
					StringUtils.join("_", uniqueList),
					StringUtils.join(",", uniqueList)));
		}

		return String.format("CREATE  TABLE %s (%s)", tablename, sb.substring(1));
	}

	public void fillBuildColumnString(StringBuffer sb, ColumnInfo col, boolean isAlter) {
		int len = col.getMaxLength() < 64 ? 64 : col.getMaxLength();
		String type = getDbType(col.getType(), len, col.getDataType());

		String alterString = "";
		String left = "";
		String right = "";

		if(isAlter) {
            if(col.getAlterMode() == null) {
                col.setAlterMode(AlterMode.CHANGE);
            }

			if(col.getAlterMode() == AlterMode.CHANGE){
				alterString = " MODIFY ";
			}

			else if(col.getAlterMode() == AlterMode.RENAME){
				alterString = " RENAME COLUMN ";
			}

			else {
				alterString = col.getAlterMode().getValue();
			}
			left = "(";
			right = ")";
		}

		sb.append(",");
		if(col.getAlterMode() == AlterMode.RENAME) {
			sb.append(alterString + left + col.getColumnName() + right);
		}
		else {
			sb.append(alterString + left + col.getColumnName() + " " + type);
			if(col.isCanNotNull()) {
				sb.append(" not null ");
			}
			sb.append(right);
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

		return String.format("ALTER TABLE %s (%s)", tablename, sb.substring(1));
	}


	@Override
	public <T> String getTableExistSql(String tablename) {
		String sql = String.format("select count(*) from user_tables where table_name = '%s'", tablename.toUpperCase());

		return sql;
	}


	private String getDbType(Type type, int len, String defaultType) {

        if(type == null) {

            if(StringUtils.isNotEmpty(defaultType)) {
                return ensureDefaultType(defaultType);
            }

            return "TEXT";
        }

        if(type.equals(Boolean.class)) {
			return "CHAR(1)";
		}
		if(type.equals(Integer.class)) {
			return "INTEGER";
		}
		if(type.equals(Long.class)) {
			return "NUMBER";
		}
		if(type.equals(BigDecimal.class)) {
			return "DECIMAL(12, 4)";
		}
		if(type.equals(Float.class)) {
			return "NUMBER(38)";
		}
		if(type.equals(Double.class)) {
			return "NUMBER(63)";
		}
		if(type.equals(Date.class)) {
			return "DATE";
		}
		if(type.equals(String.class)) {
			return "NVARCHAR2";
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
            return "NVARCHAR2";
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
            return "NUMBER";
        }
        if(defaultType.toLowerCase().equals("boolean")) {
            return "CHAR(1)";
        }
        if(defaultType.toLowerCase().equals("bool")) {
            return "CHAR(1)";
        }
        if(defaultType.toLowerCase().equals("nvarchar")) {
            return "NVARCHAR2";
        }
        if(defaultType.toLowerCase().equals("string")) {
            return "NVARCHAR2";
        }
        if(defaultType.toLowerCase().equals("text")) {
            return "NVARCHAR2";
        }
        if(defaultType.toLowerCase().equals("float")) {
            return "NUMBER(38)";
        }
        if(defaultType.toLowerCase().equals("double")) {
            return "NUMBER(63)";
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

