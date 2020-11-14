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

public enum CommandMode
{
    Select,
    Insert,
    InsertFrom,
    Update,
    UpdateFrom,
    Delete,
    DeleteFrom,
    Exist, 
    SelectCount,
    Tables,
    ColumnsInfo,
    PrimaryKey,
}
