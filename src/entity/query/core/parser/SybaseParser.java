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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static entity.tool.util.StringUtils.isEmpty;

public class SybaseParser extends SqlParserBase
{

    public SybaseParser( DataSource ds )
    {
        super( ds );
    }

    @Override
    public String getPrefix()
    {
        return "";
    }

    @Override
    public String getSuffix()
    {
        return "";
    }

    @Override
    public String getSelectNow()
    {
        return "select getdate();";
    }

    @Override
    public String getTablesSql()
    {
        return "select name as table_name from sysobjects where type=\"U\"";
    }

    @Override
    public String getColumnInfoListSql( String tablename )
    {
        return String.format("SELECT a.name as column_name, b.name as data_type, '' as column_comment FROM syscolumns a LEFT JOIN systypes b ON a.usertype = b.usertype INNER JOIN sysobjects d ON a.id=d.id AND d.name<>'dtproperties' LEFT JOIN syscomments e ON a.cdefault=e.id WHERE d.name='%s'", tablename);
    }

    @Override
    public String getPrimaryKeySpl(String tablename) {
        return "SELECT '';";
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
                sb.append(" identity ");
            }

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

        return String.format("ALTER TABLE [%s] %s ", tablename, sb.substring(1));
    }

    @Override
    public <T> String getTableExistSql(String tablename) {
        return String.format("select count(*) from user_tables where table_name = '%s'", tablename);
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
                alterString = " MODIFY ";
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
            sb.append(alterString + "`"+ col.getColumnName() + "` " + type + String.format("(%s, 4)", len));
        }

        else {
            sb.append(alterString + "`"+ col.getColumnName() + "` " + type + String.format("(%s)", len));
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
        if(type.getClass().equals(Integer.class)) {
            return "int";
        }
        if(type.getClass().equals(Long.class)) {
            return "BIGINT";
        }
        if(type.getClass().equals(BigDecimal.class)) {
            return "decimal";
        }
        if(type.getClass().equals(Float.class)) {
            return "Real";
        }
        if(type.getClass().equals(Double.class)) {
            return "Float";
        }
        if(type.equals(String.class)) {
            return "NVARCHAR";
        }
        if(type.equals(Blob.class)) {
            return "Image";
        }
        if(type.equals(Date.class)){
            return "Datetime";
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
            return "NVARCHAR";
        }
        if(defaultType.toLowerCase().equals("text")) {
            return "NTEXT";
        }
        if(defaultType.toLowerCase().equals("float")) {
            return "Real";
        }
        if(defaultType.toLowerCase().equals("double")) {
            return "Float";
        }
        if(defaultType.toLowerCase().equals("image")) {
            return "Image";
        }
        if(defaultType.toLowerCase().equals("money")) {
            return "decimal";
        }

        return defaultType;
    }
}
