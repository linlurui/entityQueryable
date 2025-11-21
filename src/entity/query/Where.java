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
import entity.query.core.ExpressionValueBinder;
import entity.query.core.executor.DBExecutorAdapter;
import entity.query.enums.CommandMode;
import entity.query.enums.Condition;
import entity.query.enums.JoinMode;
import entity.tool.util.Callback;
import entity.tool.util.DBUtils;
import entity.tool.util.StringUtils;
import entity.tool.util.ThreadUtils;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Where<T> extends QueryableAction<T> {
	private static final Logger log = LoggerFactory.getLogger(Where.class);

	protected Where() {
		super();
	}

	private void bindValues(String exp, Object[] values) {
		if(values == null || values.length == 0) {
			return;
		}
		ExpressionValueBinder.bind(this.genericType, this.entityObject(), exp, values);
	}

	private Object[] mergeValues(Object value, Object... moreValues) {
		int additional = (moreValues == null) ? 0 : moreValues.length;
		Object[] results = new Object[1 + additional];
		results[0] = value;
		if(additional > 0) {
			System.arraycopy(moreValues, 0, results, 1, additional);
		}
		return results;
	}

    public <E> Where<T> where(String exp, List<E> values) {
        return where(exp, values.toArray(), ", ");
    }

    public <E> Where<T> where(String exp, E[] ...values) {
        return where(exp, values, ", ");
    }

	public <E> Where<T> where(String exp, E[] values, String spliter) {
		return where(null, exp, values, spliter);
	}

	public Where<T> where(String exp, Object value, Object... moreValues) {
		bindValues(exp, mergeValues(value, moreValues));
		Where<T> clause = new Where<T>();
		clause.init(this.genericType, this.entityObject(), getParser(), this);
		clause.getParser().addWhere(exp);

		return clause;
	}

   public <E> Where<T> where(Condition condition, String exp, E[] values, String spliter) {
        Where<T> clause = new Where<T>();
        clause.init(this.genericType, this.entityObject(), getParser(), this);
        List<String> args = new ArrayList<String>();
        for(E obj : values) {
            args.add( DBUtils.getSqlInjText( obj ) );
        }

        if(args.size() > 0) {
            String joinedArgs = StringUtils.join( spliter, args );
            exp = exp.replace("%s", joinedArgs);
        }

		if(condition == null) {
			clause.getParser().addWhere(exp);
		}
		else {
			clause.getParser().addWhere(condition, exp);
		}

        return clause;
    }

	public <E> Where<T> or(String exp, List<E> values) {
		return where(Condition.OR, exp, values.toArray(), ", ");
	}

    public <E> Where<T> or(String exp, E[] ...values) {
        return where(Condition.OR, exp, values, ", ");
    }

	public Where<T> or(String exp, Object value, Object... moreValues) {
		bindValues(exp, mergeValues(value, moreValues));
		return where(Condition.OR, exp);
	}

	public Where<T> or(String exp) {
		return where(Condition.OR, exp);
	}

	public <E> Where<T> and(String exp, List<E> values) {
		return where(Condition.AND, exp, values.toArray(), ", ");
	}

	public <E> Where<T> and(String exp, E[] ...values) {
		return where(Condition.AND, exp, values, ", ");
	}

    public Where<T> and(String exp, Object value, Object... moreValues) {
        bindValues(exp, mergeValues(value, moreValues));
        return where(Condition.AND, exp);
    }

    public Where<T> and(String exp) {
        return where(Condition.AND, exp);
    }

	public Where<T> where(Condition condition, String exp) {
		Where<T> clause = new Where<T>();
		clause.init(this.genericType, this.entityObject(), getParser(), this);
		clause.getParser().addWhere(condition, exp);

		return clause;
	}

	public Where<T> where(Condition condition, String exp, Object value, Object... moreValues) {
		bindValues(exp, mergeValues(value, moreValues));
		return where(condition, exp);
	}

	public Select<T> select(String... exp) {
		Select<T> clause = new Select<T>();
		clause.init(this.genericType, this.entityObject(), getParser(), this);

		if(exp != null && exp.length > 0) {
			clause.getParser().addSelect(StringUtils.join(", ", exp));
		}

		return clause;
	}

	public OrderBy<T> orderby(String... exp) {
		OrderBy<T> clause = new OrderBy<T>();
		clause.init(this.genericType, this.entityObject(), getParser(), this);
		clause.getParser().addOrderBy(StringUtils.join(", ", exp));

		return clause;
	}

	public GroupBy<T> groupby(String... exp) {
		GroupBy<T> clause = new GroupBy<T>();
		clause.init(this.genericType, this.entityObject(), getParser(), this);
		clause.getParser().addGroupBy(StringUtils.join(", ", exp));

		return clause;
	}

	public Join<T> join(JoinMode mode, Queryable<?> q, String alias) {
		Join<T> clause = new Join<T>(this.dataSource);
		clause.init(this.genericType, this.entityObject(), getParser());
		clause.getParser().addJoin(mode, q.toString(CommandMode.Select), alias);

		return clause;
	}

	public <T1> boolean insertTo(Class<T1> clazz) throws SQLException {
		try {
			String sql = getParser().toString(this.genericType, "", CommandMode.InsertFrom, this.entityObject(), 0, 0, false, null);
			Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, null);
			return row != null && row.intValue() > 0;
		} catch (Exception e) {
			log.error("InsertTo failed: " + e.getMessage(), e);
			throw new SQLException("InsertTo operation failed", e);
		}
	}

	public boolean delete() throws SQLException {
		try {
			String sql = getParser().toString(this.genericType, "", CommandMode.Delete, this.entityObject(), 0, 0, false, null);
			Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, null);
			return row != null && row.intValue() > 0;
		} catch (Exception e) {
			log.error("Delete failed: " + e.getMessage(), e);
			throw new SQLException("Delete operation failed", e);
		}
	}

	public boolean update(String... exp) throws SQLException {
		StringBuilder expTextBuilder = new StringBuilder();
    	for (int i=0; i<exp.length; i++) {
    		if(i>0) {
    			expTextBuilder.append(", ");
    		}
    		expTextBuilder.append(DBUtils.getSqlInjText(exp[i]));
    	}
		String expText = expTextBuilder.toString();

		try {
			Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
			String sql = getParser().toString(this.genericType, expText, CommandMode.UpdateFrom, this.entityObject(), 0, 0, false, blobMap);
			Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, blobMap);
			return row != null && row.intValue() > 0;
		} catch (Exception e) {
			log.error("Update failed: " + e.getMessage(), e);
			throw new SQLException("Update operation failed", e);
		}
	}

	public boolean update(Map<String, Object> map) throws SQLException {
		StringBuilder expTextBuilder = new StringBuilder();
		int i = 0;
		for (Map.Entry<String, Object> item : map.entrySet()) {
			if(i>0) {
				expTextBuilder.append(", ");
			}
			expTextBuilder.append("[").append(DBUtils.getSqlInjText(item.getKey()))
			              .append("]=").append(DBUtils.getStringValue(item.getValue()));
			i++;
		}
		String expText = expTextBuilder.toString();

		try {
			Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
			String sql = getParser().toString(this.genericType, expText, CommandMode.UpdateFrom, this.entityObject(), 0, 0, false, blobMap);
			Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, blobMap);
			return row != null && row.intValue() > 0;
		} catch (Exception e) {
			log.error("Update failed: " + e.getMessage(), e);
			throw new SQLException("Update operation failed", e);
		}
	}

    public void batchInsert(List<T> list) throws Exception {
        batchInsert(list, null);
    }

    public void batchUpdate(List<T> list, String...exp) throws Exception {
        batchUpdate(list, null, exp);
    }

    public void batchDelete(List<T> list) throws Exception {
        batchDelete(list, null);
    }

	public void batchInsert(List<T> list, Callback<List<T>> call) throws Exception {
		DBUtils.batchTask(list, this.genericType, this, getParser(), CommandMode.InsertFrom, null, call);
	}

	public void batchUpdate(List<T> list, Callback<List<T>> call, String... exp) throws Exception {
		DBUtils.batchTask(list, this.genericType, this, getParser(), CommandMode.UpdateFrom, exp, call);
	}

	public void batchDelete(List<T> list, Callback<List<T>> call) throws Exception {
		DBUtils.batchTask(list, this.genericType, this, getParser(), CommandMode.DeleteFrom, null, call);
	}

	public <T1> Flowable<Integer> asyncInsertTo(Class<T1> clazz) throws Exception {

		String sql = getParser().toString(this.genericType, "", CommandMode.InsertFrom, this.entityObject(), 0, 0, false, null);
		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(sql, null);

		return flowable;
	}

	public Flowable<Integer> asyncDelete() throws Exception {
		String sql = getParser().toString(this.genericType, "", CommandMode.Delete, this.entityObject(), 0, 0, false, null);
		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(sql, null);

		return flowable;
	}

	public Flowable<Integer> asyncUpdate(String... exp) throws Exception {
		StringBuilder expTextBuilder = new StringBuilder();
		for (int i=0; i<exp.length; i++) {
			if(i>0) {
				expTextBuilder.append(", ");
			}
			expTextBuilder.append(DBUtils.getSqlInjText(exp[i]));
		}
		String expText = expTextBuilder.toString();
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(this.genericType, expText, CommandMode.UpdateFrom, this.entityObject(), 0, 0, false, blobMap);
		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(sql, blobMap);

		return flowable;
	}

	public Flowable<Integer> asyncUpdate(Map<String, Object> map) throws Exception {
		StringBuilder expTextBuilder = new StringBuilder();
		int i = 0;
		for (Map.Entry<String, Object> item : map.entrySet()) {
			if(i>0) {
				expTextBuilder.append(", ");
			}

			if(item.getValue() instanceof  Number) {
				expTextBuilder.append(DBUtils.getSqlInjText(item.getKey()))
				              .append("=").append(DBUtils.getStringValue(item.getValue()));
			} else {
				expTextBuilder.append(DBUtils.getSqlInjText(item.getKey()))
				              .append("='").append(DBUtils.getStringValue(item.getValue())).append("'");
			}
			i++;
		}
		String expText = expTextBuilder.toString();
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(this.genericType, expText, CommandMode.UpdateFrom, this.entityObject(), 0, 0, false, blobMap);

		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(sql, blobMap);

		return flowable;
	}

	@Override
	@JsonIgnore
	@JSONField(serialize = false)
	public String getExpression() {
		return this.getParser().getContainer().Where.toString();
	}
}
