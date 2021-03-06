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
import entity.query.enums.Condition;
import entity.query.enums.JoinMode;
import entity.tool.util.Callback;
import entity.tool.util.DBUtils;
import entity.tool.util.StringUtils;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Where<T> extends QueryableAction<T> {

	protected Where() {
		super();
	}

    public <E> Where<T> where(String exp, List<E> values) {
        return where(exp, values.toArray(), ", ");
    }

    public <E> Where<T> where(String exp, E[] values) {
        return where(exp, values, ", ");
    }

   public <E> Where<T> where(String exp, E[] values, String spliter) {
        Where<T> clause = new Where<T>();
        clause.init(getGenericType(), entityObject(), getParser(), this);
        List<String> args = new ArrayList<String>();
        for(E obj : values) {
            args.add( DBUtils.getSqlInjText( obj ) );
        }

        if(args.size() > 0) {
            exp = String.format( exp, StringUtils.join( spliter, args ) );
        }

        clause.getParser().addWhere(exp);

        return clause;
    }

    public Where<T> or(String exp) {
        return where(Condition.OR, exp);
    }

    public Where<T> and(String exp) {
        return where(Condition.AND, exp);
    }

	public Where<T> where(Condition condition, String exp) {
		Where<T> clause = new Where<T>();
		clause.init(getGenericType(), entityObject(), getParser(), this);
		clause.getParser().addWhere(condition, exp);

		return clause;
	}

	public Select<T> select(String... exp) {
		Select<T> clause = new Select<T>();
		clause.init(getGenericType(), entityObject(), getParser(), this);

		if(exp != null && exp.length > 0) {
			clause.getParser().addSelect(StringUtils.join(", ", exp));
		}

		return clause;
	}

	public OrderBy<T> orderby(String... exp) {
		OrderBy<T> clause = new OrderBy<T>();
		clause.init(getGenericType(), entityObject(), getParser(), this);
		clause.getParser().addOrderBy(StringUtils.join(", ", exp));

		return clause;
	}

	public GroupBy<T> groupby(String... exp) {
		GroupBy<T> clause = new GroupBy<T>();
		clause.init(getGenericType(), entityObject(), getParser(), this);
		clause.getParser().addGroupBy(StringUtils.join(", ", exp));

		return clause;
	}

	public Join<T> join(JoinMode mode, Queryable<?> q, String alias) {
		Join<T> clause = new Join<T>(this.dataSource);
		clause.init(getGenericType(), entityObject(), getParser());
		clause.getParser().addJoin(mode, q.toString(CommandMode.Select), alias);

		return clause;
	}

	public <T1> boolean insertTo(Class<T1> clazz) throws SQLException {

		String sql = getParser().toString(getGenericType(), "", CommandMode.InsertFrom, entityObject(), 0, 0, false, null);
		Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, null);

		return row!=null && row > 0;
	}

	public boolean delete() throws SQLException {
		String sql = getParser().toString(getGenericType(), "", CommandMode.Delete, entityObject(), 0, 0, false, null);
		Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, null);

		return row!=null && row > 0;
	}

	public boolean update(String... exp) throws SQLException {
		String expText = "";
    	for (int i=0; i<exp.length; i++) {
    		if(i>0) {
    			expText = expText + ", ";
    		}
    		expText = expText + DBUtils.getSqlInjText( exp[i] );
    	}
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(getGenericType(), expText, CommandMode.UpdateFrom, entityObject(), 0, 0, false, blobMap);
		Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, blobMap);

		return row!=null && row > 0;
	}

	public boolean update(Map<String, Object> map) throws SQLException {
		String expText = "";
		int i = 0;
		for (Map.Entry<String, Object> item : map.entrySet()) {
			if(i>0) {
				expText = expText + ", ";
			}

			expText = expText + String.format("[%s]=%s", DBUtils.getSqlInjText( item.getKey() ), DBUtils.getStringValue( item.getValue() ));
			i++;
		}
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(getGenericType(), expText, CommandMode.UpdateFrom, entityObject(), 0, 0, false, blobMap);
		Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, blobMap);

		return row!=null && row > 0;
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
		DBUtils.batchTask(list, getGenericType(), this, getParser(), CommandMode.InsertFrom, null, call);
	}

	public void batchUpdate(List<T> list, Callback<List<T>> call, String... exp) throws Exception {
		DBUtils.batchTask(list, getGenericType(), this, getParser(), CommandMode.UpdateFrom, exp, call);
	}

	public void batchDelete(List<T> list, Callback<List<T>> call) throws Exception {
		DBUtils.batchTask(list, getGenericType(), this, getParser(), CommandMode.DeleteFrom, null, call);
	}

	public <T1> Flowable<Integer> asyncInsertTo(Class<T1> clazz) throws Exception {

		String sql = getParser().toString(getGenericType(), "", CommandMode.InsertFrom, entityObject(), 0, 0, false, null);
		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(sql, null);

		return flowable;
	}

	public Flowable<Integer> asyncDelete() throws Exception {
		String sql = getParser().toString(getGenericType(), "", CommandMode.Delete, entityObject(), 0, 0, false, null);
		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(sql, null);

		return flowable;
	}

	public Flowable<Integer> asyncUpdate(String... exp) throws Exception {
		String expText = "";
		for (int i=0; i<exp.length; i++) {
			if(i>0) {
				expText = expText + ", ";
			}
			expText = expText + DBUtils.getSqlInjText( exp[i] );
		}
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(getGenericType(), expText, CommandMode.UpdateFrom, entityObject(), 0, 0, false, blobMap);
		Flowable<Integer> flowable = DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(sql, blobMap);

		return flowable;
	}

	public Flowable<Integer> asyncUpdate(Map<String, Object> map) throws Exception {
		String expText = "";
		int i = 0;
		for (Map.Entry<String, Object> item : map.entrySet()) {
			if(i>0) {
				expText = expText + ", ";
			}

			if(item.getValue() instanceof  Number) {
				expText = expText + String.format("%s=%s", DBUtils.getSqlInjText( item.getKey() ), DBUtils.getStringValue( item.getValue() ));
			}

			else {
				expText = expText + String.format("%s='%s'", DBUtils.getSqlInjText( item.getKey() ), DBUtils.getStringValue( item.getValue() ));
			}
			i++;
		}
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(getGenericType(), expText, CommandMode.UpdateFrom, entityObject(), 0, 0, false, blobMap);

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
