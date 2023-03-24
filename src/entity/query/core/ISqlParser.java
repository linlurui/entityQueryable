/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.core;


import entity.query.ColumnInfo;
import entity.query.enums.CommandMode;
import entity.query.enums.Condition;
import entity.query.enums.JoinMode;
import entity.tool.util.OutParameter;

import java.sql.Blob;
import java.util.List;
import java.util.Map;

public interface ISqlParser {

	void addWhere(String exp);

	void addWhere(Condition condition, String exp);

	void cleanSelectList();

	void addSelect(String exp);

	void addOrderBy(String exp);

	void addGroupBy(String exp);

	void addJoin(JoinMode mode, String exp, String alias);

	<T> void addJoin(JoinMode mode, Class<T> clazz);

	<T> void addJoin(JoinMode mode, Class<T> clazz, String alias);

	void addOn(String exp);

	void addFrom(String string, String alias);

	<T> void addFrom(Class<T> clazz, String alias);

	void addUnioin(String selectSql);

	<T> Object[] getArgs(Class<T> genericType, String sql, Object obj, Map<Integer, Blob> blobMap);

	<T> String getInsertSql(Class<T> genericType);

	<T> String getDeleteSql(Class<T> genericType);

	<T> String getUpdateSql(Class<T> genericType);

	<T> String getUpdateSql(Class<T> genericType, String exp);

	<T> String getInsertToSql(Class<T> clazz);

	<T> String getSelectSql(Class<T> clazz);

	<T> String getSelectSql(Class<T> clazz, int skip, int top, Boolean isCount);

	<T> String getSelectExistSql(Class<T> clazz);

	<T> String toString(Class<T> clazz, String exp, CommandMode cmdMode, Object obj, int skip, int top, Boolean isCount, Map<Integer, Blob> blobMap);

	String getSelectNow();

	boolean hasGroupBy();

	String getTablesSql();

	String getColumnInfoListSql(String tablename);

	String getPrimaryKeySpl(String tablename);

	String schema();

	<T> String getCreateTableSql(String tablename, List<ColumnInfo> columns);

	<T> String getAlterTableSql(String tablename, List<ColumnInfo> columns, List<ColumnInfo> storedColumns);

	<T> String getTableExistSql(String tablename);

	<T> String getDropTableSql(String tablename);

	String getWhereString();

    <T> String getTablename(OutParameter<Class<T>> param);

	SqlContainer getContainer();
}
