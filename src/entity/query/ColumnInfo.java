/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query;

import entity.query.annotation.Exclude;
import entity.query.annotation.Fieldname;
import entity.query.enums.AlterMode;
import java.io.Serializable;
import java.util.List;


public class ColumnInfo implements Serializable {

	public ColumnInfo() {

	}

	public ColumnInfo(String name, String dataType, boolean isPk) {
		this.columnName = name;
		this.dataType = dataType;
		this.isPrimaryKey = isPk;
	}

	public AlterMode getAlterMode() {
		return alterMode;
	}

	public void setAlterMode(AlterMode alterMode) {
		this.alterMode = alterMode;
	}

	private AlterMode alterMode;

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public void setName(String columnName) {
		this.columnName = columnName;
	}

	@Fieldname(value="column_name")
	private String columnName;

	public String getColumnComment() {
		return columnComment;
	}

	public void setColumnComment(String columnComment) {
		this.columnComment = columnComment;
	}

	@Fieldname(value="column_comment")
	private String columnComment;

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	@Fieldname(value="data_type")
	private String dataType;

	@Fieldname(value = "isAutoIncrement")
	private Boolean isAutoIncrement;

	public Boolean getIsAutoIncrement() {

		if(isAutoIncrement == null) {
			return false;
		}

		return isAutoIncrement;
	}

	public String getColumnNameOld() {
		return columnNameOld;
	}

	public void setColumnNameOld(String columnNameOld) {
		this.columnNameOld = columnNameOld;
	}

	@Exclude
	private String columnNameOld;

	public void setIsAutoIncrement(Boolean autoIncrement) {
		isAutoIncrement = autoIncrement;
	}

	public List<OptionInfo> getOptionInfos() {
		return optionInfos;
	}

	public void setOptionInfos(List<OptionInfo> optionInfos) {
		this.optionInfos = optionInfos;
	}

	@Exclude
	private List<OptionInfo> optionInfos;

	@Exclude
	private ForeignKeyInfo foreignKeyInfo;

	public ForeignKeyInfo getForeignKeyInfo() {
		return foreignKeyInfo;
	}

	public void setForeignKeyInfo(ForeignKeyInfo foreignKeyInfo) {
		this.foreignKeyInfo = foreignKeyInfo;
	}


	private Boolean isPrimaryKey;

	public Boolean isPrimaryKey() {

        return getIsPrimaryKey();
    }

	public Boolean getIsPrimaryKey() {

		if(isPrimaryKey == null) {
			return false;
		}

		return isPrimaryKey;
	}

	public void setIsPrimaryKey(Boolean primaryKey) {
		isPrimaryKey = primaryKey;
	}

	public Boolean getPk() {

		if(isPrimaryKey == null) {
			return false;
		}

		return isPrimaryKey;
	}

	public void setPk(Boolean primaryKey) {
		isPrimaryKey = primaryKey;
	}

	private int maxLength;

	public Boolean getAutoIncrement() {
		return isAutoIncrement;
	}

	public void setAutoIncrement(Boolean autoIncrement) {
		isAutoIncrement = autoIncrement;
	}

	public Boolean getPrimaryKey() {
		return isPrimaryKey;
	}

	public void setPrimaryKey(Boolean primaryKey) {
		isPrimaryKey = primaryKey;
	}

	public int getNumericScale() {
		return numericScale;
	}

	public void setNumericScale(int numericScale) {
		this.numericScale = numericScale;
	}

	private int numericScale;

	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	private Class<?> type;

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public void setType(String dataType) {
		this.dataType = dataType;
	}

	private boolean canNotNull;

	public boolean isCanNotNull() {
		return canNotNull;
	}

	public boolean getNotnull() {
		return canNotNull;
	}

	public void setCanNotNull(boolean canNotNull) {
		this.canNotNull = canNotNull;
	}

	public void setNotnull(Boolean canNotNull) {
		this.canNotNull = canNotNull;
	}

	private String defaultValue;

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDfltValue() {
		return defaultValue;
	}

	public void setDfltValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	private  String alias;

	public boolean isUnique() {
		return unique;
	}

	public boolean getUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	private boolean unique;
}
