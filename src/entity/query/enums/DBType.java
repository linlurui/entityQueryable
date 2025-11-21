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

public enum DBType {

    Mysql("mysql"),
    MariaDB("mariadb"),
    Oracle("oracle"),
    DB2("db2"),
    Odbc("odbc"),
    PostgreSQL("postgresql"),
    SqlServer("sqlserver"),
    Sybase("sybase"),
    Sqlite("sqlite"),
    Couchbase("couchbase"),
    Derby("derby"),
    Hive2("hive2");

    private final String value;

    DBType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

