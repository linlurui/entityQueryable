
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EntityLifecycleManager {

    private static final List<EntityLifecycleListener> GLOBAL_LISTENERS = new CopyOnWriteArrayList<EntityLifecycleListener>();
    private static final Map<Class<?>, List<EntityLifecycleListener>> LOCAL_LISTENERS = new ConcurrentHashMap<Class<?>, List<EntityLifecycleListener>>();

    private EntityLifecycleManager() {
    }

    public static void registerGlobal(EntityLifecycleListener listener) {
        if(listener == null) {
            return;
        }
        GLOBAL_LISTENERS.add(listener);
    }

    public static void register(Class<?> entityType, EntityLifecycleListener listener) {
        if(entityType == null || listener == null) {
            return;
        }
        LOCAL_LISTENERS.computeIfAbsent(entityType, key -> new CopyOnWriteArrayList<EntityLifecycleListener>())
                .add(listener);
    }

    public static void fire(EntityLifecycleEventType type, Object entity, EntityLifecycleContext context) {
        if(entity == null) {
            return;
        }
        List<EntityLifecycleListener> listeners = new ArrayList<EntityLifecycleListener>();
        listeners.addAll(GLOBAL_LISTENERS);
        List<EntityLifecycleListener> scoped = LOCAL_LISTENERS.get(entity.getClass());
        if(scoped != null) {
            listeners.addAll(scoped);
        }
        for(EntityLifecycleListener listener : listeners) {
            try {
                listener.onEvent(type, entity, context);
            } catch (Exception ignore) {
                // keep lifecycle optional
            }
        }
    }
}

