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

public class SqLiteParser extends MysqlParser {

	public SqLiteParser(DataSource ds) {
		super(ds);
	}

    @Override
    public String getPrefix() {
        return "[";
    }

    @Override
    public String getSuffix() {
        return "]";
    }

    @Override
    public String getTablesSql()
    {
        return "SELECT name as table_name, type FROM sqlite_master WHERE type='table' or  type='view' ORDER BY name;";
    }

    @Override
    public String getSelectNow() {
        return "select CURRENT_TIMESTAMP;";
    }

    @Override
    public String getColumnInfoListSql(String tablename)
    {
        return String.format("PRAGMA table_info(\"%s\")", tablename);
    }

    @Override
    public String getPrimaryKeySpl(String tablename)
    {
        return String.format("pragma table_info ('%s')", tablename);
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

            if(col.getIsAutoIncrement()) {
                sb.append(" AUTOINCREMENT ");
            }
        }

        List<String> uniqueList = new ArrayList<String>();
        for (ColumnInfo a : columns) {
            if(a.isUnique()) {
                uniqueList.add(a.getColumnName());
            }
        }
        if(uniqueList.size() > 0) {
            sb.append(String.format(", unique(%s)",
                    StringUtils.join(",", uniqueList)));
        }

        return String.format("CREATE  TABLE [%s] (%s);", tablename, sb.substring(1));
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
        List<String> newColumns = new ArrayList<>();
        List<String> oldColumns = new ArrayList<>();
        String tmpTable = String.format("%s_dg_tmp", tablename);
        //sb.append("BEGIN TRANSACTION;");
        sb.append(getCreateTableSql(tmpTable, columns));
        for(ColumnInfo col : columns) {
            if(isEmpty(col.getColumnName()) || col.getAlterMode()==AlterMode.DROP ||
                    (col.getType() == null && StringUtils.isEmpty(col.getDataType()))) {
                continue;
            }
            if(StringUtils.isNotEmpty(col.getColumnNameOld())) {
                oldColumns.add(col.getColumnNameOld());
            }
            else {
                oldColumns.add(col.getColumnName());
            }
            newColumns.add(col.getColumnName());
        }
        sb.append("insert into ["+tmpTable+"] (["+ StringUtils.join("], [", newColumns) +"])" +
                "select ["+ StringUtils.join("], [", oldColumns) +"] from [" + tablename + "];");
        sb.append(String.format("drop table %s;", tablename));
        sb.append(String.format("alter table %s rename to %s;", tmpTable, tablename));
        //sb.append("COMMIT;");

        return sb.toString();
    }

    @Override
    public <T> String getTableExistSql(String tablename) {
        String sql = String.format("select count(*)  from sqlite_master where type='table' and name = '%s';", tablename);

        return sql;
    }

    public void fillBuildColumnString(StringBuffer sb, ColumnInfo col, boolean isAlter) {


        int len = col.getMaxLength() < 64 ? 64 : col.getMaxLength();
        String type = getDbType(col.getType(), col.getDataType());

        String alterString = "";
        String defaultValue = "";
        if(isAlter) {
            if(col.getAlterMode() == null) {
                col.setAlterMode(AlterMode.ADD);
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
        if("decimal".equals(type.toLowerCase()) || "float".equals(type.toLowerCase()) || "double".equals(type.toLowerCase())){
            sb.append(alterString + "["+ col.getColumnName() + "] " + type + String.format("(%s, 4)", len) + defaultValue);
        }

        else if("varchar".equals(type.toLowerCase())) {
            sb.append(alterString + "["+ col.getColumnName() + "] " + type + String.format("(%s)", len) + defaultValue);
        }

        else if("int".equalsIgnoreCase(type)) {
            sb.append(alterString + "["+ col.getColumnName() + "] INTEGER" + defaultValue);
        }
        else {
            sb.append(alterString + "["+ col.getColumnName() + "] " + type + defaultValue);
        }

        if(col.isCanNotNull()) {
            sb.append(" not null ");
        }
    }

    private String getDbType(Type type, String defaultType) {

        if(type == null) {

            if(StringUtils.isNotEmpty(defaultType)) {
                return ensureDefaultType(defaultType);
            }

            return "NTEXT";
        }

        if(type.equals(Boolean.class)) {
            return "BOOLEAN";
        }
        if(type.equals(Integer.class)) {
            return "INTEGER";
        }
        if(type.equals(Long.class)) {
            return "INTEGER";
        }
        if(type.equals(BigDecimal.class)) {
            return "NUMERIC(16,4)";
        }
        if(type.equals(Float.class)) {
            return "FLOAT";
        }
        if(type.equals(Double.class)) {
            return "REAL";
        }
        if(type.equals(Date.class)) {
            return "DATETIME";
        }
        if(type.equals(String.class)) {
            return "NVARCHAR";
        }
        if(type.equals(Blob.class)) {
            return "BLOB";
        }

        if(StringUtils.isNotEmpty(defaultType)) {
            return ensureDefaultType(defaultType);
        }

        return "NTEXT";
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
            return "INTEGER";
        }
        if(defaultType.toLowerCase().equals("boolean")) {
            return "BOOLEAN";
        }
        if(defaultType.toLowerCase().equals("bool")) {
            return "BOOLEAN";
        }
        if(defaultType.toLowerCase().equals("string")) {
            return "NVARCHAR";
        }
        if(defaultType.toLowerCase().equals("text")) {
            return "NVARCHAR";
        }
        if(defaultType.toLowerCase().equals("float")) {
            return "FLOAT";
        }
        if(defaultType.toLowerCase().equals("double")) {
            return "REAL";
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
