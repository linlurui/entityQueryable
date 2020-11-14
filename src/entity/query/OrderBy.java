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
import entity.tool.util.StringUtils;

public final class OrderBy<T> extends QueryableAction<T> {

	protected OrderBy() {

	}

	public Select<T> select(String... exp) {
		Select<T> clause = new Select<T>();
		clause.init(getGenericType(), entityObject(), getParser(), this);

		if(exp != null && exp.length > 0) {
			clause.getParser().addSelect(StringUtils.join(", ", exp));
		}

		return clause;
	}

	@Override
	@JsonIgnore
	@JSONField(serialize = false)
	public String getExpression() {
		return this.getParser().getContainer().OrderBy.toString();
	}
}
