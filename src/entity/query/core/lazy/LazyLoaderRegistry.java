
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

import entity.query.core.lifecycle.EntityLifecycleContext;
import entity.tool.util.ReflectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LazyLoaderRegistry {

    private static final Map<Class<?>, List<LazyBinding>> REGISTRY = new ConcurrentHashMap<Class<?>, List<LazyBinding>>();

    private LazyLoaderRegistry() {
    }

    public static void register(Class<?> entityType, String property, LazyLoader loader) {
        if(entityType == null || property == null || loader == null) {
            return;
        }
        REGISTRY.computeIfAbsent(entityType, key -> new CopyOnWriteArrayList<LazyBinding>())
                .add(new LazyBinding(property, loader));
    }

    public static void attach(Object entity, EntityLifecycleContext context) {
        if(entity == null) {
            return;
        }
        List<LazyBinding> bindings = REGISTRY.get(entity.getClass());
        if(bindings == null || bindings.isEmpty()) {
            return;
        }
        for(LazyBinding binding : bindings) {
            try {
                LazyReference<Object> reference = LazyReference.from(() -> binding.loader.load(entity, context));
                ReflectionUtils.setFieldValue(entity.getClass(), entity, binding.property, reference);
            } catch (Exception ignored) {
            }
        }
    }

    private static final class LazyBinding {
        private final String property;
        private final LazyLoader loader;

        private LazyBinding(String property, LazyLoader loader) {
            this.property = property;
            this.loader = loader;
        }
    }
}

