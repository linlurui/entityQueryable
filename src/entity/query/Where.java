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

    public <E> Where<T> where(String exp, List<E> values) {
        return where(exp, values.toArray(), ", ");
    }

    public <E> Where<T> where(String exp, E[] ...values) {
        return where(exp, values, ", ");
    }

	public <E> Where<T> where(String exp, E[] values, String spliter) {
		return where(null, exp, values, spliter);
	}

   public <E> Where<T> where(Condition condition, String exp, E[] values, String spliter) {
        Where<T> clause = new Where<T>();
        clause.init(this.genericType, this.entityObject(), getParser(), this);
        List<String> args = new ArrayList<String>();
        for(E obj : values) {
            args.add( DBUtils.getSqlInjText( obj ) );
        }

        if(args.size() > 0) {
            exp = String.format( exp, StringUtils.join( spliter, args ) );
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

	public Where<T> or(String exp) {
		return where(Condition.OR, exp);
	}

	public <E> Where<T> and(String exp, List<E> values) {
		return where(Condition.AND, exp, values.toArray(), ", ");
	}

	public <E> Where<T> and(String exp, E[] ...values) {
		return where(Condition.AND, exp, values, ", ");
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
		final Integer[] row = {0};
		final Where queryable = this;
		final Class<T> genericType = this.genericType;
		final Object obj = this.entityObject();
		if("SQLITE".equalsIgnoreCase(this.dataSource.getDbType())) {
			try {
				String sql = getParser().toString(genericType, "", CommandMode.InsertFrom, obj, 0, 0, false, null);
				row[0] = DBExecutorAdapter.createExecutor(queryable, getGenericType()).execute(sql, null);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return row[0] !=null && row[0].intValue()>0;
		}
		ThreadUtils.onec(new Runnable() {
			@Override
			public void run() {
				try {
					String sql = getParser().toString(genericType, "", CommandMode.InsertFrom, obj, 0, 0, false, null);
					row[0] = DBExecutorAdapter.createExecutor(queryable, getGenericType()).execute(sql, null);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});

		return row[0] !=null && row[0].intValue()>0;
	}

	public boolean delete() throws SQLException {
		final Integer[] row = {0};
		final Where queryable = this;
		final Class<T> clazz = this.genericType;
		final Object obj = this.entityObject();
		if("SQLITE".equalsIgnoreCase(this.dataSource.getDbType())) {
			try {
				String sql = getParser().toString(clazz, "", CommandMode.Delete, obj, 0, 0, false, null);
				row[0] = DBExecutorAdapter.createExecutor(queryable, getGenericType()).execute(sql, null);
				sql = null;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return row[0] !=null && row[0].intValue()>0;
		}
		ThreadUtils.onec(new Runnable() {
			@Override
			public void run() {
				try {
					String sql = getParser().toString(clazz, "", CommandMode.Delete, obj, 0, 0, false, null);
					row[0] = DBExecutorAdapter.createExecutor(queryable, getGenericType()).execute(sql, null);
					sql = null;
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});

		return row[0] !=null && row[0].intValue()>0;
	}

	public boolean update(String... exp) throws SQLException {
		String expText = "";
    	for (int i=0; i<exp.length; i++) {
    		if(i>0) {
    			expText = expText + ", ";
    		}
    		expText = expText + DBUtils.getSqlInjText( exp[i] );
    	}

		final Integer[] row = {0};
		final Where queryable = this;
		final Class<T> clazz = this.genericType;
		final Object obj = this.entityObject();
		final String finalExpText = expText;
		if("SQLITE".equalsIgnoreCase(this.dataSource.getDbType())) {
			try {
				Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
				String sql = getParser().toString(clazz, finalExpText, CommandMode.UpdateFrom, obj, 0, 0, false, blobMap);
				row[0] = DBExecutorAdapter.createExecutor(queryable, getGenericType()).execute(sql, blobMap);
				sql = null;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return row[0] !=null && row[0].intValue()>0;
		}
		ThreadUtils.onec(new Runnable() {
			@Override
			public void run() {
				try {
					Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
					String sql = getParser().toString(clazz, finalExpText, CommandMode.UpdateFrom, obj, 0, 0, false, blobMap);
					row[0] = DBExecutorAdapter.createExecutor(queryable, getGenericType()).execute(sql, blobMap);
					sql = null;
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});
		expText = null;

		return row[0] !=null && row[0].intValue()>0;
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

		final Integer[] row = {0};
		final Where queryable = this;
		final Class<T> clazz = this.genericType;
		final Object obj = this.entityObject();
		final String finalExpText = expText;
		if("SQLITE".equalsIgnoreCase(this.dataSource.getDbType())) {
			try {
				Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
				String sql = getParser().toString(clazz, finalExpText, CommandMode.UpdateFrom, obj, 0, 0, false, blobMap);
				row[0] = DBExecutorAdapter.createExecutor(queryable, getGenericType()).execute(sql, blobMap);
				sql = null;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return row[0] !=null && row[0].intValue()>0;
		}
		ThreadUtils.onec(new Runnable() {
			@Override
			public void run() {
				try {
					Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
					String sql = getParser().toString(clazz, finalExpText, CommandMode.UpdateFrom, obj, 0, 0, false, blobMap);
					row[0] = DBExecutorAdapter.createExecutor(queryable, getGenericType()).execute(sql, blobMap);
					sql = null;
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});
		expText = null;

		return row[0] !=null && row[0].intValue()>0;
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
		String expText = "";
		for (int i=0; i<exp.length; i++) {
			if(i>0) {
				expText = expText + ", ";
			}
			expText = expText + DBUtils.getSqlInjText( exp[i] );
		}
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(this.genericType, expText, CommandMode.UpdateFrom, this.entityObject(), 0, 0, false, blobMap);
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
