package entity.tool.util;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONLibDataFormatSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * fastjson工具类
 * @version:1.0.0
 */
public class FastJsonUtils {

    private static final SerializeConfig config;
    private static final Logger log = LogManager.getLogger(FastJsonUtils.class);

    static {
        config = new SerializeConfig();
        config.put(java.util.Date.class, new JSONLibDataFormatSerializer()); // 使用和json-lib兼容的日期输出格式
        config.put(java.sql.Date.class, new JSONLibDataFormatSerializer()); // 使用和json-lib兼容的日期输出格式
    }

    private static final SerializerFeature[] features = {SerializerFeature.WriteMapNullValue, // 输出空置字段
            SerializerFeature.WriteNullListAsEmpty, // list字段如果为null，输出为[]，而不是null
            SerializerFeature.WriteNullNumberAsZero, // 数值字段如果为null，输出为0，而不是null
            SerializerFeature.WriteNullBooleanAsFalse, // Boolean字段如果为null，输出为false，而不是null
            SerializerFeature.WriteNullStringAsEmpty // 字符类型字段如果为null，输出为""，而不是null
    };

    public static String toJson(Object object) {

        if(object == null) {
            return null;
        }

        try {
            if (object instanceof String) {
                return object.toString();
            }

            return JSON.toJSONString(object, config, features);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static String toJSONNoFeatures(Object object) {
        return JSON.toJSONString(object, config);
    }

    public static Object toBean(String text) {
        try {
            return JSON.parse(text);
        }
        catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    public static <T> T toBean(String text, Class<T> clazz) {

        try {
            return JSON.parseObject(text, clazz);
        }
        catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    // 转换为数组
    public static <T> T[] toArray(String text) {
        return toArray(text, null);
    }

    // 转换为数组
    public static <T> T[] toArray(String text, Class<T> clazz) {
        try {
            return (T[]) toList(text, clazz).toArray();
        }
        catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    // 转换为List
    public static <T> List<T> toList(String text, Class<T> clazz) {
        List<T> list = new ArrayList<T>();

        Object[] arr = JSON.parseArray(text, Map.class).toArray();
        if (arr == null) {
            return null;
        }

        for (Object item : arr) {
            list.add(toBean(toJson(item), clazz));
        }

        return list;
    }

    /**
     * json字符串转化为map
     * @param s
     * @return
     */
    public static <K, V> Map<K, V>  stringToCollect(String s) {
        Map<K, V> m = (Map<K, V>) JSONObject.parseObject(s);
        return m;
    }

    /**
     * 转换JSON字符串为对象
     * @param jsonData
     * @param clazz
     * @return
     */
    public static <T> T parse(String jsonData, Class<?> clazz) {

        try {
            if (StringUtils.isEmpty(jsonData)) {
                return null;
            }

            if (List.class.equals(clazz)) {
                return (T) toList(jsonData, Map.class);
            }
        }
        catch (Exception e) {
            log.error(e);
        }

        return (T) toBean(jsonData, clazz);
    }

    /**
     * 转换JSON字符串为对象
     * @param obj
     * @param clazz
     * @return
     */
    public static <T> T convert(Object obj, Class<?> clazz) {
        return parse(toJson(obj), clazz);
    }

    /**
     * 将map转化为string
     * @param m
     * @return
     */
    public static <K, V> String collectToString(Map<K, V> m) {
        String s = JSONObject.toJSONString(m);
        return s;
    }

}
