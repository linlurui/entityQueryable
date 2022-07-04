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


import java.sql.Blob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import entity.query.core.IDataActuator;
import entity.query.core.ISqlParser;
import entity.query.core.executor.DBExecutorAdapter;
import entity.query.enums.CommandMode;
import entity.tool.util.StringUtils;
import io.reactivex.Flowable;

public final class On<T> extends QueryableAction<T> {

	protected On(Class<T> clazz, Object obj, ISqlParser parser, IDataActuator dataActuator) {
		super();
		init(clazz, obj, parser, dataActuator);
	}

	public Where<T> where(String exp) {
		Where<T> clause = new Where<T>();
		clause.init(this.genericType, this.entityObject(), getParser(), this);
		clause.getParser().addWhere(exp);

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

	public <T1> boolean insertTo(Class<T1> clazz) throws Exception {
		Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
		String sql = getParser().toString(this.genericType, "", CommandMode.InsertFrom, this.entityObject(), 0, 0, false, blobMap);
		Integer row = DBExecutorAdapter.createExecutor(this, getGenericType()).execute(sql, blobMap);

		return row!=null && row > 0;
	}

    public <T1> Flowable<Integer> asyncInsertTo(Class<T1> clazz) throws Exception {
        Map<Integer, Blob> blobMap = new HashMap<Integer, Blob>();
        String sql = getParser().toString(this.genericType, "", CommandMode.InsertFrom, this.entityObject(), 0, 0, false, blobMap);
		return DBExecutorAdapter.createExecutor(this, getGenericType()).flowable(sql, blobMap);
    }

	@Override
	@JsonIgnore
	@JSONField(serialize = false)
	public String getExpression() {
		return String.join(", ", this.getParser().getContainer().Join);
	}
}
