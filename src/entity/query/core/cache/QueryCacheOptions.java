
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

/**
 * Lightweight cache options controlled via application.yml.
 */
public final class QueryCacheOptions {

    private final boolean enabled;
    private final long ttlMillis;
    private final int maxEntries;

    public QueryCacheOptions(boolean enabled, long ttlMillis, int maxEntries) {
        this.enabled = enabled;
        this.ttlMillis = ttlMillis;
        this.maxEntries = maxEntries;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    public int getMaxEntries() {
        return maxEntries;
    }
}

