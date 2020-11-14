package entity.tool.util;

public class NumberUtils {
    public static Integer parseInt(Object value) {

        if(value == null) {
            return null;
        }

        if(value instanceof Integer) {
            return (Integer) value;
        }

        return Integer.parseInt(value.toString());
    }
}
