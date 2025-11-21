
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

final class LightweightQueryCache implements QueryCacheProvider {

    private final Map<String, QueryCacheEntry> store = new ConcurrentHashMap<String, QueryCacheEntry>();
    private final ConcurrentLinkedQueue<String> fifo = new ConcurrentLinkedQueue<String>();
    private final QueryCacheOptions options;

    LightweightQueryCache(QueryCacheOptions options) {
        this.options = options;
    }

    @Override
    public <T> T get(String key) {
        QueryCacheEntry entry = store.get(key);
        if(entry == null) {
            return null;
        }
        if(entry.isExpired()) {
            store.remove(key);
            fifo.remove(key);
            return null;
        }
        return entry.materialize();
    }

    @Override
    public void put(String key, Object value, Class<?> resultType) {
        if(value == null) {
            return;
        }
        Object payload = clonePayload(value);
        store.put(key, new QueryCacheEntry(payload, options.getTtlMillis(), resultType));
        fifo.offer(key);
        trimToSize();
    }

    @Override
    public void evict(String key) {
        if(key == null) {
            return;
        }
        store.remove(key);
        fifo.remove(key);
    }

    @Override
    public void clear() {
        store.clear();
        fifo.clear();
    }

    @Override
    public void trimToSize() {
        int maxEntries = options.getMaxEntries();
        if(maxEntries <= 0) {
            return;
        }
        while(store.size() > maxEntries) {
            String key = fifo.poll();
            if(key == null) {
                break;
            }
            store.remove(key);
        }
    }

    private Object clonePayload(Object value) {
        if(value == null) {
            return null;
        }
        if(value instanceof java.util.List) {
            return new java.util.ArrayList<Object>((java.util.List<?>) value);
        }
        if(value instanceof java.util.Map) {
            return new java.util.HashMap<Object, Object>((java.util.Map<?, ?>) value);
        }
        return value;
    }
}

