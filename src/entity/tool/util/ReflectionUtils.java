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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import entity.query.Datetime;
import entity.query.annotation.Fieldname;
import entity.query.core.ApplicationConfig;


public class ReflectionUtils {

    private volatile static HashMap<String, MethodAccess> methodMap = new HashMap<String, MethodAccess>();
    private volatile static HashMap<String, ConstructorAccess<?>> objMap = new HashMap<String, ConstructorAccess<?>>();
    public static <T> MethodAccess getMethodAccess(Class<T> clazz) {

        if (!methodMap.containsKey(clazz.getName())) {
            synchronized (methodMap) {
                if (!methodMap.containsKey(clazz.getName())) {
                    methodMap.put(clazz.getName(), MethodAccess.get(clazz));
                }
            }
        }

        MethodAccess result = methodMap.get(clazz.getName());

        if(result == null) {
            return MethodAccess.get(clazz);
        }

        return result;
    }


    @SuppressWarnings("unchecked")
    public static <T> ConstructorAccess<T> getConstructorAccess(Class<T> clazz){
        if (!objMap.containsKey(clazz.getName())) {
            synchronized (objMap) {
                if (!objMap.containsKey(clazz.getName())) {
                    objMap.put(clazz.getName(), ConstructorAccess.get(clazz));
                }
            }
        }

        ConstructorAccess<T> result = (ConstructorAccess<T>) objMap.get(clazz.getName());

        if(result == null) {
            return ConstructorAccess.get(clazz);
        }

        return result;
    }

    public static <T> T getInstance(Class<T> clazz) {
        ConstructorAccess<T> access = getConstructorAccess(clazz);
        return access.newInstance();
    }

    private static String getMethodName(String fieldname, String method) {
        StringBuffer result = new StringBuffer();
        String[] arr = fieldname.split("_");
        result.append(method);
        for(String item : arr) {
            result.append( item.substring(0, 1).toUpperCase() );
            result.append( item.substring(1) );
        }

        return result.toString();
    }

    public static <T> Object invoke(Class<?> clazz, Object obj, String method, T value) {

        Object result = null;
        MethodAccess access = getMethodAccess(clazz);
        OutParameter<String> out = new OutParameter<String>();
        out.setData( method );
        if(hasMethod(out, access)) {

            if(value != null) {
                if (access.getReturnTypes().length == 1 && access.getIndex(method)>-1 && access.getReturnTypes()[access.getIndex(method)].equals(UUID.class)) {
                    String str = value.toString();
                    if (StringUtils.isNotEmpty(str) && str.length() == 32) {
                        value = (T) UUID.fromString(str.substring(0, 8) + "-" +
                                str.substring(8, 12) + "-" +
                                str.substring(12, 16) + "-" +
                                str.substring(16, 20) + "-" + str.substring(20));
                    }
                }
            }

            if(value == null && access.getParameterTypes()[access.getIndex(method)].length == 1) {
                if ("java.lang.String".equals(access.getParameterTypes()[access.getIndex(method)][0].getName())) {
                    result = invokeSetter(obj, method, "");
                }
            }
            else {
                result = invokeSetter(obj, method, value);
            }
        }

        return result;
    }

    public static Object invoke(Class<?> clazz, Object obj, String method) {

        Object result = null;
        MethodAccess access = getMethodAccess(clazz);
        OutParameter<String> out = new OutParameter<String>();
        out.setData( method );
        if(hasMethod(out, access)) {
            method = out.getData();
            result = invokeGetter(obj, method);
            if(result == null && access.getIndex(method)>-1 && access.getReturnTypes()[access.getIndex(method)].equals(UUID.class)) {
                result = UUID.randomUUID();
            }
        }

        return result;
    }

    private static Object invokeGetter(Object obj, String method) {
        if(obj == null || StringUtils.isEmpty(method)) {
            return null;
        }

        if(obj.getClass().getClassLoader() instanceof MemoryClassLoader) {
            try {
                return obj.getClass().getMethod(method).invoke(obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        MethodAccess access = getMethodAccess(obj.getClass());
        return access.invoke(obj, method);
    }

    private static Object invokeSetter(Object obj, String method, Object ...args) {
        if(obj == null || StringUtils.isEmpty(method) || args==null || args.length==0) {
            return null;
        }
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        if(obj.getClass().getClassLoader() instanceof MemoryClassLoader) {
            try {
                return obj.getClass().getMethod(method, types).invoke(obj, args);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        MethodAccess access = getMethodAccess(obj.getClass());

        return access.invoke(obj, method, types, args);
    }

    private static Boolean hasMethod(OutParameter<String> out, MethodAccess access) {
        String method = out.getData();
        String[] names = access.getMethodNames();
        Boolean hasMethod = false;
        for(String name : names) {
            if(name.toLowerCase().equals(method.toLowerCase())) {
                out.setData( name );
                hasMethod = true;
                break;
            }
        }
        return hasMethod;
    }

    public static Object getFieldValue(Class<?> clazz, Object obj, String fieldname) {

        fieldname = ensureFieldname(clazz, fieldname);

        String method = getMethodName(fieldname, "get");

        for(Method a : clazz.getDeclaredMethods()) {
            if(a.getName().equals(method)) {
                method = "get"+ fieldname.substring(0, 1).toUpperCase()+ fieldname.substring(1);
                break;
            }
        }

        return invoke(clazz, obj, method);
    }

    public static void setFieldValue(Class<?> clazz, Object obj, String fieldname, Object value) {

        fieldname = ensureFieldname(clazz, fieldname);

        String method = getMethodName(fieldname, "set");
        for(Method a : clazz.getDeclaredMethods()) {
            if(a.getName().equals(method)) {
                method = "set"+ fieldname.substring(0, 1).toUpperCase()+ fieldname.substring(1);
                break;
            }
        }

        if(value != null) {
            boolean hasValue = false;

            try{
                if(!hasValue && !clazz.getDeclaredField(fieldname).getGenericType().equals(value.getClass())) {
                    if(Date.class.equals(value.getClass()) && !Date.class.equals(clazz.getDeclaredField(fieldname).getGenericType())) {
                        value = Datetime.format((Date) value, "yyyy-MM-dd HH:mm:ss.SSS");
                    }
                    else {
                        value = StringUtils.cast(clazz.getDeclaredField(fieldname).getGenericType(), value.toString());
                    }
                    hasValue = true;
                }
            }
            catch(Exception e){}

            if(!hasValue) {
                try {
                    if (clazz.getMethod(method, value.getClass()) != null) {
                        if(Date.class.equals(value.getClass())) {
                            value = StringUtils.cast(value.getClass(), Long.valueOf(((Date) value).getTime()).toString());
                        }
                        else {
                            value = StringUtils.cast(value.getClass(), value.toString());
                        }
                        hasValue = true;
                    }
                } catch (Exception e) {}
            }

            if(!hasValue) {
                try {
                    String getter = getMethodName(fieldname, "get");
                    for(Method a : clazz.getDeclaredMethods()) {
                        if(a.getName().equals(getter)) {
                            getter = "get"+ fieldname.substring(0, 1).toUpperCase()+ fieldname.substring(1);
                            break;
                        }
                    }

                    if (clazz.getMethod(getter) != null) {
                        value = StringUtils.cast(clazz.getMethod(getter).getReturnType(), value.toString());
                        hasValue = true;
                    }
                } catch (Exception e) {}
            }
        }

        invoke(clazz, obj, method, value);
    }

    private static String ensureFieldname(Class clazz, String field) {
        for(Field item : clazz.getDeclaredFields()) {
            Fieldname fieldname = item.getAnnotation(Fieldname.class);
            if(fieldname != null) {
                String value = ApplicationConfig.getInstance().get(fieldname.value());
                if(StringUtils.isNotEmpty(value) && value.equals(field)) {
                    field = item.getName();
                    break;
                }
            }
        }

        return field;
    }

    public static Object getFieldValue(Object obj, String fieldName){
        Field field=getDeclaredField(obj, fieldName);
        if(field==null){
            throw new IllegalArgumentException("Could not find field["+
                    fieldName+"] on target ["+obj+"]");
        }

        makeAccessiable(field);

        Object result = null;
        try{
            result = field.get(obj);
        }catch(IllegalAccessException e){
            System.out.println("ReflectionUtil Error:" + e.getMessage());
        }
        return result;
    }

    public static void setFieldValue(Object obj, String fieldName, Object value){
        Field field=getDeclaredField(obj, fieldName);
        if(field==null){
            throw new IllegalArgumentException("Could not find field["+
                    fieldName+"] on target ["+obj+"]");
        }

        makeAccessiable(field);
        try{
            field.set(obj, value);
        }catch(IllegalAccessException e){
            System.out.println("ReflectionUtil Error:" + e.getMessage());
        }

    }

    public static void makeAccessiable(Field field){
        if(!Modifier.isPublic(field.getModifiers())){
            field.setAccessible(true);
        }
    }

    public static Field getDeclaredField(Object obj, String fieldName){

        for(Class<?> clazz=obj.getClass(); clazz!=Object.class; clazz=clazz.getSuperclass()){
            try{
                return clazz.getDeclaredField(fieldName);
            }catch(Exception e){

            }
        }
        return null;
    }
}
