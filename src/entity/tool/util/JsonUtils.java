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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.tool.util.jackson.ObjectMapperDateFormatExtend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtils
{
	private final static ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger( JsonUtils.class );

    /**
     * json得到对象
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    public static <T> T parse( String jsonString, Class<T> pojoClass ) throws JsonParseException, JsonMappingException, IOException
    {
        T pojo = null;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //mapper.setDateFormat(new ObjectMapperDateFormatExtend(mapper.getDateFormat()));
        pojo = mapper.readValue( jsonString, pojoClass );

        return pojo;
    }

    /**
     * json得到对象集合
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> List<T> toList( String json, Class<T> cls ) throws JsonParseException, JsonMappingException, IOException
    {
        ArrayList<T> mList = new ArrayList<T>();
        List array = mapper.readValue( json, List.class );
        for ( int i = 0; i < array.size(); i++ )
        {
        	if(array.get( i ).getClass().equals(cls)) {
        		mList.add( (T)array.get( i ) );
        	}

        	else {
        		mList.add(JsonUtils.convert(array.get(i), cls));
        	}
        }

        return mList;
    }

    /**
     * 获取请求体中的Json
     */
    public static byte[] readBytes( InputStream is, int contentLen ) throws IOException
    {
        if ( contentLen > 0 )
        {
            int readLen = 0;
            int readLengthThisTime = 0;
            byte[] message = new byte[contentLen];

            while ( readLen != contentLen )
            {
                readLengthThisTime = is.read( message, readLen, contentLen - readLen );
                if ( readLengthThisTime == -1 )
                {
                    break;
                }
                readLen += readLengthThisTime;
            }

            return message;

        }
        return new byte[] {};
    }

    /**
     * obj to json
     */
    public static String toJson( Object src )
    {
        try {
			return mapper.writeValueAsString( src );
		} catch (JsonProcessingException e) {}

        return "";
    }

    public static <T, S> T convert(S src, Class<T> pojoClass) throws IOException {
        if(src instanceof String) {
            return parse((String) src, pojoClass);
        }

        if(src.getClass().equals(pojoClass)) {
            return (T) src;
        }

        String json = toJson(src);
        T target = null;
        try {
            target = parse(json, pojoClass);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return target;
    }
}
