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

import java.io.ByteArrayInputStream;  
import java.io.ByteArrayOutputStream;  
import java.io.FileNotFoundException;  
import java.io.FileOutputStream;  
import java.io.IOException;  
import java.io.ObjectInputStream;  
import java.io.ObjectOutputStream;  
import java.util.Locale;  
  
public class SerializeUtils {  
  
    public static String getBase64String(Object object)  
            throws FileNotFoundException, IOException {  
          
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);  
        byte[] bytes = baos.toByteArray();  
        oos.close();   
        baos.close();  
        return bytesToHexString(bytes);  
  
    }
    
    public static String serialize(Object object, String strFile) throws FileNotFoundException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);  
        byte[] bytes = baos.toByteArray();
        ObjectOutputStream oos2 = new ObjectOutputStream(new FileOutputStream(strFile));
        oos2.writeObject(object);  
        oos.close();  
        oos2.close();  
        baos.close();  
        return bytesToHexString(bytes);  
    }

    public static Object getObject(String hexString) throws IOException,  
            ClassNotFoundException {  
        byte[] bytes = hexStringToBytes(hexString);  
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);  
        ObjectInputStream ois = new ObjectInputStream(bais);  
        return ois.readObject();  
    }  
 static String bytesToHexString(byte[] src) {  
        StringBuilder stringBuilder = new StringBuilder("");  
        if (src == null || src.length <= 0) {  
            return null;  
        }  
        for (int i = 0; i < src.length; i++) {  
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {  
                stringBuilder.append(0);  
            }  
            stringBuilder.append(hv);  
        }  
        return stringBuilder.toString();  
    }  

    private static byte[] hexStringToBytes(String hexString) {  
        if (hexString == null || hexString.equals("")) {  
            return null;  
        }  
        hexString = hexString.toUpperCase(Locale.getDefault());  
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();  
        byte[] d = new byte[length];  
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));  
        }  
        return d;  
    }  

    private static byte charToByte(char c) {  
        return (byte) "0123456789ABCDEF".indexOf(c);  
    }  
  
}  