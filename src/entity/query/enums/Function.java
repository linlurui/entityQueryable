/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.enums;

public enum Function
{
    NONE(0, ""),
    MAX(1, "MAX"),
    MIN(2,"MIN"),
    UPPER(3,"UPPER"),
    LOWER(4,"LOWER"),
    LENGTH(5,"LENGTH"),
    AVG(6,"AVG"),
    COUNT(7,"COUNT"),
    COUNT_BIG(8,"COUNT_BIG"),
    SUM(9,"SUM"),
    GROUPING(10,"GROUPING"),
    BINARY_CHECKSUM(11,"BINARY_CHECKSUM"),
    CHECKSUM_AGG(12,"CHECKSUM_AGG"),
    CHECKSUM(13,"CHECKSUM"),
    STDEV(14,"STDEV"),
    VAR(15,"VAR"),
    VARP(16,"VARP"),
    GROUP_CONCAT(17, "GROUP_CONCAT"),
    VAR_POP(18, "var_pop"),
    STDDEV_POP(19, "stddev_pop"),
    COVAR_POP(20, "covar_pop"),
    COLLECT_SET(21, "collect_set"),
    FLOOR(22, "floor"),
    CEIL(23, "ceil"),
    RAND(24, "rand"),
    ROUND(25, "round"),
    EXP(26, "exp"),
    LOG(27, "log"),
    POW(28, "pow"),
    SQRT(29, "sqrt"),
    ABS(30, "abs"),
    PMOD(31, "pmod"),
    SIN(32, "sin"),
    COS(33, "cos"),
    TAN(34, "tan"),
    ASIN(35, "asin"),
    ACOS(36, "acos"),
    ATAN(37, "atan"),
    PI(38, "pi"),
    CAST(39, "cast"),
    BINARY(40, "binary"),
    ENCODE(41, "encode"),
    DECODE(42, "decode"),
    JSON2TUPLE(43, "json_tuple"),
    INITCAP(44, "initcap"),
    TRIM(45, "trim"),
    SUBSTR(46, "substr"),
    PARSEURL(47, "parse_url"),
    PARSERURL2TUPLE(48, "parse_url_tuple"),
    EXPLODE(49, "explode"),
    STACK(50, "stack")
    ;

    private int code;
    private String value;

    Function(int code, String value) {
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
