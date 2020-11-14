package entity.query.enums;

public enum AlterMode {

    NONE(0, ""),
    ADD(1, " ADD COLUMN "),
    CHANGE(2, " CHANGE COLUMN "),
    DROP(3, " DROP COLUMN "),
    RENAME(4, " RENAME "),
    ;

    private int code;
    private String value;

    AlterMode(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
