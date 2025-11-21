
/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query.core.lazy;

import java.io.Serializable;
import java.util.concurrent.Callable;

public final class LazyReference<T> implements Serializable {

    private final Callable<T> resolver;
    private transient volatile T value;
    private transient volatile boolean initialized;

    public static <T> LazyReference<T> from(Callable<T> resolver) {
        return new LazyReference<T>(resolver);
    }

    private LazyReference(Callable<T> resolver) {
        this.resolver = resolver;
    }

    public T get() {
        if(initialized) {
            return value;
        }
        synchronized (this) {
            if(initialized) {
                return value;
            }
            try {
                value = resolver.call();
            } catch (Exception e) {
                throw new IllegalStateException("Lazy reference resolve failed: " + e.getMessage(), e);
            }
            initialized = true;
        }
        return value;
    }

    public boolean isInitialized() {
        return initialized;
    }
}

