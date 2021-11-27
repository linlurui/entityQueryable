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
import entity.tool.util.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Blob;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static entity.tool.util.StringUtils.isEmpty;

public class SqlserverParser extends SqlParserBase
{

    public SqlserverParser( DataSource ds )
    {
        super( ds );
    }

    @Override
    protected String filter( String exp )
    {
        return exp;
    }

    @Override
    public String getSelectNow()
    {
        return "select GETDATE();";
    }

    @Override
    public String getTablesSql() {
        return "select name as table_name from sysobjects where xtype='u' OR xtype='v'";
    }

    @Override
    public String getColumnInfoListSql( String tablename ) {
        return String.format( "select COLUMN_NAME AS column_name,'' column_comment,DATA_TYPE AS data_type, (SELECT COLUMNPROPERTY( OBJECT_ID('%s'),column_name,'IsIdentity')) as isAutoIncrement from information_schema.COLUMNS where TABLE_NAME='%s'", tablename, tablename );
    }

    @Override
    public String getPrimaryKeySpl(String tablename) {
        return String.format( "SELECT name FROM SysColumns WHERE id=Object_Id('%s') and colid=(select top 1 colid from sysindexkeys where id=Object_Id('%s'))", tablename, tablename );
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

            if(col.getIsAutoIncrement()) {
                sb.append(" IDENTITY (1,1) ");
            }

            if(col.isPrimaryKey()) {
                sb.append(" PRIMARY KEY ");
            }
        }

        List<String> uniqueList = columns.stream().filter(a -> a.isUnique()).map(b -> b.getColumnName()).collect(Collectors.toList());
        if(uniqueList.size() > 0) {
            sb.append(String.format(",constraint %s unique(%s)",
                    StringUtils.join("_", uniqueList),
                    getPrefix() + StringUtils.join(getSuffix() + "," + getPrefix(), uniqueList) + getSuffix()));
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
        for(ColumnInfo col : columns) {
            if(isEmpty(col.getColumnName()) || (col.getType() == null && StringUtils.isEmpty(col.getDataType()))) {
                continue;
            }

            fillBuildColumnString(sb, col, true);
        }

        return String.format("ALTER TABLE [%s] %s;", tablename, sb.substring(1));
    }

    @Override
    public <T> String getTableExistSql(String tablename) {
        return String.format("select * from sysobjects where id = object_id(N'[%s]') and OBJECTPROPERTY(id, N'IsUserTable') = 1", tablename);
    }

    public void fillBuildColumnString(StringBuffer sb, ColumnInfo col, boolean isAlter) {
        int len = col.getMaxLength() < 64 ? 64 : col.getMaxLength();
        String type = getDbType(col.getType(), col.getDataType());

        String alterString = "";
        if(isAlter) {
            if(col.getAlterMode() == null) {
                col.setAlterMode(AlterMode.CHANGE);
            }

            if(col.getAlterMode() == AlterMode.CHANGE){
                alterString = " ALTER COLUMN ";
            }
            else if(col.getAlterMode() == AlterMode.DROP){
                alterString = " DROP COLUMN ";
            }
            else {
                alterString = col.getAlterMode().getValue();
            }
        }
        else {
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
        }

        sb.append(",");
        if("decimal".equals(type.toLowerCase())){
            sb.append(alterString + "["+ col.getColumnName() + "] " + type + String.format("(%s, 4)", len));
        }

        else if("varchar".equals(type.toLowerCase()) || "nvarchar".equals(type.toLowerCase())){
            sb.append(alterString + "["+ col.getColumnName() + "] " + type + String.format("(%s)", len));
        }

        else {
            sb.append(alterString + "["+ col.getColumnName() + "] " + type );
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
            return "NVARCHAR";
        }
        if(type.equals(Blob.class)) {
            return "BINARY";
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
        if(defaultType.toLowerCase().equals("string")) {
            return "VARCHAR";
        }
        if(defaultType.toLowerCase().equals("text")) {
            return "NVARCHAR";
        }
        if(defaultType.toLowerCase().equals("float")) {
            return "float";
        }
        if(defaultType.toLowerCase().equals("double")) {
            return "double";
        }
        if(defaultType.toLowerCase().equals("image")) {
            return "BINARY";
        }
        if(defaultType.toLowerCase().equals("money")) {
            return "decimal";
        }

        return defaultType;
    }
}
