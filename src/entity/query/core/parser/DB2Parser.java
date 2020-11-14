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
import static entity.tool.util.StringUtils.isNotEmpty;

public class DB2Parser extends SqlParserBase {

	public DB2Parser(DataSource ds) {
		super(ds);
	}

    @Override
	public String getSelectNow() {
		return "SELECT current timestamp FROM sysibm.sysdummy1;";
	}

    @Override
    public String getTablesSql()
    {
        return "select name as table_name from sysibm.systables where type = 'T'";
    }

    @Override
    public String getColumnInfoListSql( String tablename )
    {
        return String.format( "SELECT a.name column_name, a.coltype data_type ,COALESCE(a.remarks, '') column_comment FROM sysibm.syscolumns a INNER JOIN sysibm.systables d on a.tbname=d.name LEFT JOIN sysibm.sysindexes n on n.tbname=d.name and SUBSTR(colnames,2)=a.name where d.type='T'and d.tbspace='%s'", tablename );
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

        String pk = null;
        StringBuffer sb = new StringBuffer();
        for(ColumnInfo col : columns) {
            if(isEmpty(col.getColumnName()) || (col.getType() == null && StringUtils.isEmpty(col.getDataType()))) {
                continue;
            }

            if(col.getPrimaryKey() && col.getDataType() != null &&
                    ("INTEGER".equals(col.getDataType().toUpperCase()) ||
                            "LONG".equals(col.getDataType().toUpperCase()) ||
                            "INT".equals(col.getDataType().toUpperCase()) ||
                            "BIGINT".equals(col.getDataType().toUpperCase()) ) ) {
                col.setIsAutoIncrement(true);
            }

            fillBuildColumnString(sb, col, false);

            if(col.getIsAutoIncrement()) {
                sb.append(" GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1 ) ");
            }

            if(col.getPrimaryKey()) {
                pk = col.getColumnName();
            }
        }

        if(isNotEmpty(pk)) {
            sb.append(String.format(",PRIMARY KEY (%s)", pk));
        }

        List<String> uniqueList = columns.stream().filter(a -> a.isUnique()).map(b -> b.getColumnName()).collect(Collectors.toList());
        if(uniqueList.size() > 0) {
            sb.append(String.format(",constraint %s unique(%s)",
                    StringUtils.join("_", uniqueList),
                    StringUtils.join(",", uniqueList)));
        }

        return String.format("CREATE  TABLE %s (%s)", tablename, sb.substring(1));
    }

    private void fillBuildColumnString(StringBuffer sb, ColumnInfo col, boolean isAlter) {

        int len = col.getMaxLength() < 64 ? 64 : col.getMaxLength();
        String type = getDbType(col.getType(), col.getDataType());

        String alterString = "";
        String set = " ";

        if(isAlter) {
            if(col.getAlterMode() == null) {
                col.setAlterMode(AlterMode.CHANGE);
            }

            alterString = col.getAlterMode().getValue();
            set = " SET ";
        }

        sb.append(",");
        if("decimal".equals(type.toLowerCase())){
            sb.append(alterString + col.getColumnName() + set + type + String.format("(%s, 4)", len));
        }

        else if("varchar".equals(type.toLowerCase())) {
            sb.append(alterString + col.getColumnName() + set + type + String.format("(%s)", len));
        }

        else {
            sb.append(alterString + col.getColumnName() + set + type);
        }

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

        return String.format("ALTER TABLE %s %s ", tablename, sb.substring(1));
    }

    @Override
    public <T> String getTableExistSql(String tablename) {
        String sql = String.format("SELECT count(*) FROM SYSIBM.SYSTABLES WHERE TID<>0 AND Name='%s' AND Creator='DB2INST1';", tablename);

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
            return "char";
        }
        if(type.equals(Integer.class)) {
            return "INT";
        }
        if(type.equals(Long.class)) {
            return "BIGINT";
        }
        if(type.equals(BigDecimal.class)) {
            return "DECIMAL";
        }
        if(type.equals(Float.class)) {
            return "REAL";
        }
        if(type.equals(Double.class)) {
            return "DOUBLE";
        }
        if(type.equals(Date.class)) {
            return "timestamp";
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
            return "timestamp";
        }
        if(defaultType.toLowerCase().equals("datetime")){
            return "timestamp";
        }
        if(defaultType.toLowerCase().equals("time")){
            return "timestamp";
        }
        if(defaultType.toLowerCase().equals("integer")) {
            return "int";
        }
        if(defaultType.toLowerCase().equals("long")) {
            return "bigint";
        }
        if(defaultType.toLowerCase().equals("boolean")) {
            return "char";
        }
        if(defaultType.toLowerCase().equals("bool")) {
            return "char";
        }
        if(defaultType.toLowerCase().equals("nvarchar")) {
            return "varchar";
        }
        if(defaultType.toLowerCase().equals("string")) {
            return "varchar";
        }
        if(defaultType.toLowerCase().equals("text")) {
            return "varchar";
        }
        if(defaultType.toLowerCase().equals("image")) {
            return "BLOB";
        }
        return defaultType;
    }
}
