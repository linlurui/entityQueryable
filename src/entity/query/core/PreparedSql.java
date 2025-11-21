/**
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 */

package entity.query.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a parameterized SQL statement along with its bound parameters.
 */
public class PreparedSql {

    private final String sql;
    private final List<Object> parameters;

    public PreparedSql(String sql, List<Object> parameters) {
        this.sql = sql;
        if (parameters == null || parameters.isEmpty()) {
            this.parameters = Collections.emptyList();
        } else {
            this.parameters = Collections.unmodifiableList(new ArrayList<Object>(parameters));
        }
    }

    public String getSql() {
        return sql;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return String.format("%s %s", sql, parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sql, parameters);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PreparedSql)) {
            return false;
        }
        PreparedSql other = (PreparedSql) obj;
        return Objects.equals(this.sql, other.sql) && Objects.equals(this.parameters, other.parameters);
    }
}

