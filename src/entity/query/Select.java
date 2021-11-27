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

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import entity.query.core.executor.DBExecutorAdapter;
import entity.query.enums.CommandMode;
import entity.query.enums.JoinMode;
import entity.tool.util.StringUtils;
import io.reactivex.Flowable;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class Select<T> extends QueryableAction<T> {

	protected Select() {
	}

	public Join<T> join(JoinMode mode, Queryable<?> q, String alias) {
		Join<T> clause = new Join<T>(this.dataSource);
		clause.init(this.genericType, this.entityObject(), getParser());
		clause.getParser().addJoin(mode, q.toString(CommandMode.Select), alias);

		return clause;
	}

	public <E> Select<E> union(Select<E> clause) {

		clause.getParser().addUnioin(getParser().toString(this.genericType, "", CommandMode.Select, this.entityObject(), 0, 0, false, null));

		return clause;
	}

	public void clean() {
		getParser().cleanSelectList();
	}

	public void createView(String viewName) throws SQLException {

		if(StringUtils.isEmpty(viewName)) {
			throw new SQLException("view name can not bet empty!!!");
		}

		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(this.genericType, null, CommandMode.Select, this.entityObject(), 0, 0, false, blobMap);
		sql = String.format("CREATE VIEW [%s] AS %s", viewName, sql);
		DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, blobMap);
	}

	public void alterView(String viewName) throws SQLException {

		if(StringUtils.isEmpty(viewName)) {
			throw new SQLException("view name can not bet empty!!!");
		}

		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(this.genericType, null, CommandMode.Select, this.entityObject(), 0, 0, false, blobMap);
		sql = String.format("ALTER VIEW [%s] AS %s", viewName, sql);
		DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, blobMap);
	}

	@Override
	@JsonIgnore
	@JSONField(serialize = false)
	public String getExpression() {
		return this.getParser().getContainer().Select.toString();
	}
}
