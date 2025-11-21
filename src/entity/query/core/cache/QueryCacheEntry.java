
/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.core.cache;

import entity.query.core.lazy.LazyReference;
import entity.tool.util.JsonUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class QueryCacheEntry {

    private final Object value;
    private final long createdAt;
    private final long ttlMillis;
    private final Class<?> resultType;

    QueryCacheEntry(Object value, long ttlMillis, Class<?> resultType) {
        this.value = value;
        this.ttlMillis = ttlMillis;
        this.resultType = resultType;
        this.createdAt = System.currentTimeMillis();
    }

    boolean isExpired() {
        if(ttlMillis <= 0) {
            return false;
        }
        return System.currentTimeMillis() - createdAt > ttlMillis;
    }

    @SuppressWarnings("unchecked")
    <T> T materialize() {
        return (T) cloneValue(value);
    }

    private Object cloneValue(Object source) {
        if(source == null) {
            return null;
        }
        if(source instanceof List) {
            List<?> list = (List<?>) source;
            List<Object> cloned = new ArrayList<Object>(list.size());
            for(Object item : list) {
                cloned.add(cloneValue(item));
            }
            return cloned;
        }
        if(source instanceof Map) {
            return new HashMap<Object, Object>((Map<?, ?>) source);
        }
        if(source instanceof LazyReference) {
            return source;
        }
        Class<?> type = source.getClass();
        if(isImmutable(type)) {
            if(source instanceof Date) {
                return new Date(((Date) source).getTime());
            }
            return source;
        }
        try {
            return JsonUtils.convert(source, type);
        } catch (Exception e) {
            return source;
        }
    }

    private boolean isImmutable(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || String.class.equals(type)
                || Boolean.class.equals(type)
                || Date.class.equals(type);
    }

    Class<?> getResultType() {
        return resultType;
    }
}

