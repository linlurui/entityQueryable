/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */

package entity.tool.util;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段缓存工具类，用于缓存类的反射字段信息，提升性能
 */
public class FieldCache {
    
    private static final ConcurrentHashMap<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 获取类的声明字段，带缓存
     * @param clazz 类对象
     * @return 字段数组
     */
    public static Field[] getCachedDeclaredFields(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return FIELD_CACHE.computeIfAbsent(clazz, Class::getDeclaredFields);
    }
    
    /**
     * 清除缓存（用于测试或特殊场景）
     */
    public static void clearCache() {
        FIELD_CACHE.clear();
    }
}


