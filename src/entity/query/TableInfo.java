package entity.query;

import entity.query.annotation.Fieldname;

public class TableInfo {
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Fieldname(value = "table_name")
    private String tableName;
    private String type;
    private String description;
}
