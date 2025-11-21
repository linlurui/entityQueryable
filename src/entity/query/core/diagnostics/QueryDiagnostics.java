
/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.core.diagnostics;

import entity.query.core.ApplicationConfig;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class QueryDiagnostics {

    private static final AtomicLong TOTAL_QUERIES = new AtomicLong(0);
    private static final AtomicLong CACHE_HITS = new AtomicLong(0);
    private static final AtomicLong TOTAL_DURATION = new AtomicLong(0);
    private static final boolean ENABLED = loadEnabled();

    private QueryDiagnostics() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static Trace start(String sql) {
        if(!ENABLED) {
            return Trace.disabled();
        }
        TOTAL_QUERIES.incrementAndGet();
        return Trace.enabled(sql);
    }

    private static boolean loadEnabled() {
        try {
            ApplicationConfig config = ApplicationConfig.getInstance();
            return Boolean.parseBoolean(config.get("${entity.query.diagnostics.enabled}", "true"));
        } catch (Exception ignore) {
            return true;
        }
    }

    public static long getTotalQueries() {
        return TOTAL_QUERIES.get();
    }

    public static long getCacheHits() {
        return CACHE_HITS.get();
    }

    public static long getAverageDurationMillis() {
        long count = TOTAL_QUERIES.get();
        if(count == 0) {
            return 0;
        }
        return TOTAL_DURATION.get() / count;
    }

    public static class Trace implements AutoCloseable {
        private static final Trace DISABLED_TRACE = new Trace(false, null);

        private final boolean enabled;
        private final String traceId;
        private final String sql;
        private final long startedAt;
        private boolean closed;

        private Trace(boolean enabled, String sql) {
            this.enabled = enabled;
            if(enabled) {
                this.traceId = UUID.randomUUID().toString();
                this.sql = sql;
                this.startedAt = System.currentTimeMillis();
            } else {
                this.traceId = null;
                this.sql = sql;
                this.startedAt = 0L;
            }
        }

        private static Trace enabled(String sql) {
            return new Trace(true, sql);
        }

        private static Trace disabled() {
            return DISABLED_TRACE;
        }

        public String getTraceId() {
            return traceId;
        }

        public void finish(boolean cacheHit) {
            if(!enabled || closed) {
                return;
            }
            closed = true;
            if(cacheHit) {
                CACHE_HITS.incrementAndGet();
            }
            TOTAL_DURATION.addAndGet(System.currentTimeMillis() - startedAt);
        }

        @Override
        public void close() {
            finish(false);
        }

        public String getSql() {
            return sql;
        }

        public long getStartedAt() {
            return startedAt;
        }
    }
}

