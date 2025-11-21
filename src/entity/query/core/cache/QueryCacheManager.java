
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

import entity.query.core.ApplicationConfig;
import entity.query.enums.CommandMode;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton cache facade. Default implementation is an in-memory FIFO cache.
 */
public final class QueryCacheManager {

    private static final QueryCacheOptions OPTIONS = loadOptions();
    private static volatile QueryCacheProvider provider = new LightweightQueryCache(OPTIONS);
    private static final AtomicBoolean WARNED = new AtomicBoolean(false);

    private QueryCacheManager() {
    }

    private static QueryCacheOptions loadOptions() {
        ApplicationConfig config = ApplicationConfig.getInstance();
        boolean enabled = Boolean.parseBoolean(config.get("${entity.query.cache.enabled}", "false"));
        long ttl = Long.parseLong(config.get("${entity.query.cache.ttl}", "60000"));
        int max = Integer.parseInt(config.get("${entity.query.cache.max}", "1000"));
        return new QueryCacheOptions(enabled, ttl, max);
    }

    public static boolean isEnabled(CommandMode mode) {
        if(mode == null) {
            return false;
        }
        return OPTIONS.isEnabled() && (CommandMode.Select.equals(mode) || CommandMode.SelectCount.equals(mode));
    }

    public static <T> T get(String key) {
        if(!OPTIONS.isEnabled()) {
            return null;
        }
        return provider.get(key);
    }

    public static void put(String key, Object value, Class<?> type) {
        if(!OPTIONS.isEnabled()) {
            return;
        }
        provider.put(key, value, type);
    }

    public static void evict(String key) {
        provider.evict(key);
    }

    public static void clearAll() {
        provider.clear();
    }

    public static QueryCacheOptions getOptions() {
        return OPTIONS;
    }

    public static String buildKey(String dataSourceId, Class<?> resultType, CommandMode commandMode, String sql) {

        if(sql == null) {
            sql = "";
        }
        String typeName = resultType == null ? "unknown" : resultType.getName();
        String ds = dataSourceId == null ? "default" : dataSourceId;
        String mode = commandMode == null ? "none" : commandMode.name();
        return String.format("%s|%s|%s|%s", ds, typeName, mode, sql.trim());
    }

    public static void setProvider(QueryCacheProvider newProvider) {
        if(newProvider == null) {
            if(WARNED.compareAndSet(false, true)) {
                throw new IllegalArgumentException("Query cache provider can not be null");
            }
            return;
        }
        provider = newProvider;
    }
}

