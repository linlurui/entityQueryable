
/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.core.lifecycle;

import entity.query.core.DataSource;
import entity.query.enums.CommandMode;

public final class EntityLifecycleContext {

    private final CommandMode commandMode;
    private final DataSource dataSource;
    private final String sql;
    private final String traceId;
    private final boolean cacheHit;
    private final long startedAt;
    private final long finishedAt;

    private EntityLifecycleContext(CommandMode commandMode,
                                   DataSource dataSource,
                                   String sql,
                                   String traceId,
                                   boolean cacheHit,
                                   long startedAt,
                                   long finishedAt) {
        this.commandMode = commandMode;
        this.dataSource = dataSource;
        this.sql = sql;
        this.traceId = traceId;
        this.cacheHit = cacheHit;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public static EntityLifecycleContext create(CommandMode commandMode,
                                                DataSource dataSource,
                                                String sql,
                                                String traceId,
                                                boolean cacheHit,
                                                long startedAt,
                                                long finishedAt) {
        return new EntityLifecycleContext(commandMode, dataSource, sql, traceId, cacheHit, startedAt, finishedAt);
    }

    public CommandMode getCommandMode() {
        return commandMode;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getSql() {
        return sql;
    }

    public String getTraceId() {
        return traceId;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public long getDurationMillis() {
        if(finishedAt <= startedAt) {
            return 0L;
        }
        return finishedAt - startedAt;
    }
}

