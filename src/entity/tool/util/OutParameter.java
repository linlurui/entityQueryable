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

public class OutParameter<T>
{
    public OutParameter() {}
    public OutParameter(T data) {
        this.data = data;
    }
    
    private T data;

    public T getData()
    {
        return data;
    }

    public void setData( T data )
    {
        this.data = data;
    }
}
