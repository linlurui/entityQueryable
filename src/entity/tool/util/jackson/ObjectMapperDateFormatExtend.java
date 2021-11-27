package entity.tool.util.jackson;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ObjectMapperDateFormatExtend extends DateFormat {

    private static final long serialVersionUID = 1L;
    private DateFormat dateFormat;
    private static final List<String> formarts = new ArrayList<>(4);

    static {
        formarts.add("yyyy-MM");
        formarts.add("yyyy-MM-dd");
        formarts.add("yyyy-MM-dd HH:mm");
        formarts.add("yyyy-MM-dd HH:mm:ss");
    }

    public ObjectMapperDateFormatExtend(DateFormat dateFormat) {//构造函数传入objectmapper默认的dateformat
        this.dateFormat = dateFormat;
    }
    //序列化时会执行这个方法
    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        return dateFormat.format(date,toAppendTo,fieldPosition);
    }

    //反序列化时执行此方法，我们先让他执行我们自己的format，如果异常则执执行他的
    @Override
    public Date parse(String source, ParsePosition pos) {
        Date date;

        try {
            date = convert(source);
        } catch (Exception e) {
            date = dateFormat.parse(source, pos);
        }
        return date;
    }
    //此方法在objectmapper 默认的dateformat里边用到，这里也要重写
    @Override
    public Object clone() {
        DateFormat dateFormat = (DateFormat) this.dateFormat.clone();
        return new ObjectMapperDateFormatExtend(dateFormat);
    }

    public static Date convert(String source) {
        String value = source.trim();
        if ("".equals(value)) {
            return null;
        }
        if (source.matches("^\\d{4}-\\d{1,2}$")) {
            return parseDate(source, formarts.get(0));
        } else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            return parseDate(source, formarts.get(1));
        } else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}$")) {
            return parseDate(source, formarts.get(2));
        } else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$")) {
            return parseDate(source, formarts.get(3));
        } else {
            throw new IllegalArgumentException("Invalid date value '" + source + "'");
        }
    }

    /**
     * 格式化日期
     *
     * @param dateStr String 字符型日期
     * @param format  String 格式
     * @return Date 日期
     */
    public static Date parseDate(String dateStr, String format) {
        Date date = null;
        try {
            DateFormat dateFormat = new SimpleDateFormat(format);
            date = dateFormat.parse(dateStr);
        } catch (Exception e) {
            System.out.println(String.format("Error:parseDate(dateStr:{},format:{})", dateStr, format));
        }
        return date;
    }
}
