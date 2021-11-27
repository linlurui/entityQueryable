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

import entity.query.Datetime;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public final class StringUtils
{
    public static String concat(Object[] arr)
    {
        String result = "";
        for(Object item : arr){
            result.concat(String.valueOf(item));
        }

        return result;
    }

    public static boolean isEmpty(String value) {
        if(value == null){
            return true;
        }

        if("".equals( value )) {
            return true;
        }

        if("".equals( value.trim() )) {
            return true;
        }

        return value.isEmpty();
    }


    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }


    /**
     * 将字符串分割成Int列表
     *
     * @param parameter
     * @param splitStr
     * @return
     */
    public static List<Integer> splitString2IntList( String parameter, String splitStr )
    {
        List<Integer> ret = new ArrayList<Integer>();

        if ( parameter == null || StringUtils.isEmpty(parameter) )
        {
            return ret;
        }

        if(StringUtils.isEmpty(splitStr)) {
        	splitStr = "\\|";
        }

        else if(splitStr.trim().equals("?")) {
        	splitStr = "\\?";
        }

        else if(splitStr.trim().equals("*")) {
        	splitStr = "\\*";
        }

        String[] splits = parameter.split( splitStr );

        try
        {
            for ( String split : splits )
            {
                ret.add( Integer.valueOf( split.trim() ) );
            }
        } catch ( NumberFormatException e )
        {
            return new ArrayList<Integer>();
        }

        return ret;
    }

    /**
     * 将字符串分割成字符串列表
     *
     * @param parameter
     * @param splitStr
     * @return
     */
    public static List<String> splitString2List( String parameter, String splitStr ) {
        return splitString2List(parameter, splitStr, null);
    }

    public static List<String> splitString2List( String parameter, String splitStr, String format )
    {
        List<String> ret = new ArrayList<String>();

        if ( parameter == null || StringUtils.isEmpty(parameter) )
        {
            return ret;
        }

        if(StringUtils.isEmpty(splitStr)) {
        	splitStr = "\\|";
        }

        else if(splitStr.trim().equals("?")) {
        	splitStr = "\\?";
        }

        else if(splitStr.trim().equals("*")) {
        	splitStr = "\\*";
        }

        String[] splits = parameter.split( splitStr );

        try
        {
            for ( String split : splits )
            {
                if(StringUtils.isEmpty(format)) {
                    ret.add(split.trim());
                }

                else {
                    ret.add(String.format(format, split.trim()));
                }
            }
        } catch ( NumberFormatException e )
        {
            return new ArrayList<String>();
        }

        return ret;
    }

    public static <T> String join(String spliter, List<T> list) {

    	StringBuilder sb = new StringBuilder();
    	boolean isFirst = true;
    	for(T item : list) {
    		if(!isFirst) {
    			sb.append(spliter);
    		}
    		sb.append(String.valueOf(item));
    		isFirst = false;
    	}

    	return sb.toString();
    }

    public static <T> String join(String spliter, T[] list) {

    	if(list == null) {
    		return "";
    	}
    	StringBuilder sb = new StringBuilder();
    	boolean isFirst = true;
    	for(T item : list) {
    		if(!isFirst) {
    			sb.append(spliter);
    		}
    		sb.append(String.valueOf(item));
    		isFirst = false;
    	}

    	return sb.toString();
    }

    public static Class<?> getClass(Type type) {
        Class clz = null;
        if (type instanceof ParameterizedType) {

            ParameterizedType pt = (ParameterizedType) type;

            clz = ((Class) pt.getRawType());

        } else if (type instanceof TypeVariable) {

            TypeVariable tType = (TypeVariable) type;

        } else {

            clz = (Class) type;
        }

        return  clz;
    }

    public static <T> Object cast(Type type, String value) {

        Class<?> clazz = getClass(type);

        return cast(clazz, value);
    }

    public static <T> Object cast(Class<T> type, String value) {

        if(type.isEnum()) {
            try {
                boolean isNumber = Pattern.compile("^\\d+$").matcher(value).find();
                Object instance = Class.forName(type.getName());
                String name = value;
                T[] enums = type.getEnumConstants();
                if (isNumber) {
                    Method m = type.getMethod("values");
                    Object[] items = (Object[]) m.invoke(instance);
                    for(int i=0; i<items.length; i++) {
                        try {
                            if (value.equals(ReflectionUtils.invoke(items[i].getClass(), items[i], "getValue").toString())) {
                                return enums[i];
                            }
                        }
                        catch (Exception e) {}

                        try {
                            if (value.equals(ReflectionUtils.invoke(items[i].getClass(), items[i], "getCode").toString())) {
                                return enums[i];
                            }
                        }
                        catch (Exception e) {}
                    }
                }
                else {
                    for (T enumObj : enums) {
                        if (value.toLowerCase().equals(String.valueOf(enumObj).toLowerCase())) {
                            return enumObj;
                        }
                    }
                }

                Method m = type.getMethod("valueOf", String.class);
                return m.invoke(instance, name);
            }
            catch (Exception e) {
                return null;
            }
        }

        if(type.equals(Integer.class) || type.equals(int.class)) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return 0;
            }
        }

        if(type.equals(Long.class) || type.equals(long.class)) {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                return 0L;
            }
        }

        if(type.equals(Double.class) || type.equals(double.class)) {
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                return Double.valueOf(0);
            }
        }

        if(type.equals(Float.class) || type.equals(float.class)) {
            try {
                return Float.parseFloat(value);
            } catch (Exception e) {
                return Float.valueOf(0);
            }
        }

        if(type.equals(BigDecimal.class)) {
            try {
                return BigDecimal.valueOf(Double.parseDouble(value));
            } catch (Exception e) {
                return BigDecimal.valueOf(Double.valueOf(0));
            }
        }

        if(type.equals(Date.class)) {
            try {
                if(Pattern.matches("^\\d+$", value)) {
                    return Datetime.getTime(Long.parseLong(value));
                }
                return Datetime.parse(value);
            } catch (Exception e) {
                return Datetime.getTime();
            }
        }

        if(type.equals(UUID.class)) {
            try {
                if(value.length() == 32) {
                    return UUID.fromString(value.substring(0, 8) + "-" +
                            value.substring(8, 12) + "-" +
                            value.substring(12, 16) + "-" +
                            value.substring(16, 20) + "-" + value.substring(20));
                }

                if(value.length() == 36) {
                    return UUID.fromString(value);
                }

                return value;
            } catch (Exception e) {
                return UUID.randomUUID();
            }
        }

        if(type.equals(Boolean.class) || type.equals(boolean.class)) {
            try {

                if(value != null && value.trim().equals("1")) {
                    value = "true";
                }

                return Boolean.parseBoolean(value);
            } catch (Exception e) {
                return false;
            }
        }

        if(type.equals(Byte.class) || type.equals(byte.class)) {
            try {
                return Byte.parseByte(value);
            } catch (Exception e) {
                return Byte.valueOf("");
            }
        }

        if(type.equals(Character[].class) || type.equals(char.class)) {
            try {
                return value.toCharArray();
            } catch (Exception e) {
                return null;
            }
        }

        if(type.equals(Byte[].class) || type.equals(byte[].class)) {
            try {
                return value.getBytes();
            } catch (Exception e) {
                return null;
            }
        }

        if(type.equals(Blob.class)) {
            try {
                return new SerialBlob(value.getBytes());
            } catch (SQLException e) {
                return null;
            }
        }

        if(type.equals(Clob.class)) {
            try {
                return new SerialClob(value.toCharArray());
            } catch (SQLException e) {
                return null;
            }
        }

        if(type.equals(String.class)) {
            try {
                return value.toString();
            } catch (Exception e) {
                return "";
            }
        }

        return value;
    }

    public static String toString(Object value) {
        if(value == null) {
            return "";
        }

        return value.toString();
    }

    public static String toLowerCase(Object value) {
        return toString(value).toLowerCase();
    }

    public static String toUpperCase(Object value) {
        return toString(value).toUpperCase();
    }

    public static String replaceAll(Object value, String oldChar, String newChar) {
        return toString(value).replaceAll(oldChar, newChar);
    }
}
