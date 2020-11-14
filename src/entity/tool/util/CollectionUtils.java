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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class CollectionUtils
{
    public static boolean isNotEmpty( final Collection<?> coll )
    {
        return (coll != null && coll.size() > 0);
    }

    public static boolean isEmpty( final Collection<?> coll )
    {
        return !isNotEmpty( coll );
    }
    
    public static <S, T> List<T> cast2(List<S> list, Class<T> t) throws JsonParseException, JsonMappingException, IOException
    {
        List<T> result = new ArrayList<T>();
        for ( S map : list )
        {
            String json = JsonUtils.toJson(map);
            result.add( JsonUtils.parse(json, t) );
        }
        
        return result;
    }
    
    public static List<Character> distinct(char[] arr)
    {
        Vector<Character> v = new Vector<Character>();
        for(int i=0; i<arr.length; i++){
            v.add(arr[i]);
        }
        
        return distinct(v);
    }
    
    public static <T> List<T> distinct(T... arg)
    {
        return distinct(toList(arg));
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> List<T> distinct(Collection<T> coll)
    {
    	List<T> result = new ArrayList<T>();
        HashSet h = new HashSet(coll);     
        result.addAll(h);
        
        return result;  
    }

    public static <T> List<T> sort(T... arg)
    {
        return sort(false, arg);
    }
    
    public static <T> List<T> sort(boolean isDesc, T... arg)
    {
        List<T> list = toList(arg);
        
        Comparator<T> c = null;
        
        if(isDesc){
        	c = new Comparator<T>() {

                 @Override
                 public int compare(T o1, T o2) {
                     
                     return String.valueOf(o2).compareTo(String.valueOf(o1));
                 }
             };
        }
        
        else {
        	c = new Comparator<T>() {

                @Override
                public int compare(T o1, T o2) {
                    
                    return String.valueOf(o1).compareTo(String.valueOf(o2));
                }
            };
        }
        
        Collections.sort(list, c);
        
        return list;
    }
    
    public static <T> List<T> sort(Collection<T> coll)
    {
        return sort(false, coll);
    }
    
    @SuppressWarnings("unchecked")
	public static <T> List<T> sort(boolean isDesc, Collection<T> coll)
    {
        List<T> result = new ArrayList<T>();
        if(coll == null || coll.size() < 1) {
            return result;
        }

		T[] arg = (T[]) coll.toArray();
        
        return sort(isDesc, arg);
    }
    
    public static <T> List<T>  toList(T... arg)
    {
        Vector<T> v = new Vector<T>();
        if(arg == null){
            return v;
        }
        
        for(int i=0; i<arg.length; i++){
            v.add(arg[i]);
        }
        
        return v;
    }
    
    public static <T> List<String> getValuesByFieldname(List<T> list, String fieldname) {
        List<String> result = new ArrayList<String>();
        for(T obj : list) {
            Object value = ReflectionUtils.getFieldValue( obj.getClass(), obj, fieldname );
            result.add( String.valueOf( value ) );
        }
        
        return result;
    }
    
    
    public static <T> List<String> getValuesByMethodName(List<T> list, String methodname) {
        List<String> result = new ArrayList<String>();
        for(T obj : list) {
            Object value = ReflectionUtils.invoke( obj.getClass(), obj, methodname );
            result.add( String.valueOf( value ) );
        }
        
        return result;
    }
}
