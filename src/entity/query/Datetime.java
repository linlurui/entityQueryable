/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */


package entity.query;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import entity.tool.util.OutParameter;
import entity.tool.util.StringUtils;

public final class Datetime extends Queryable<Datetime> {

	private static Datetime instance;
	private static String timeZone = "Asia/Shanghai";

	static {
		instance = new Datetime();
	}

	public static Date now() {
		return instance.selectNow();
	}
	public static Date addSecond(Date date, int value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, value);
        return calendar.getTime();
	}

	public static Date addDays(Date date, int value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, value);
        return calendar.getTime();
	}

	public static Date addMonth(Date date, int value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, value);
        return calendar.getTime();
	}

	public static Date addYear(Date date, int value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.YEAR, value);
        return calendar.getTime();
	}

    public static Date addMinute(Date date, int value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, value);
        return calendar.getTime();
    }

    public static Date addHour(Date date, int value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, value);
        return calendar.getTime();
    }

    public static int getDayOfYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.get(Calendar.DAY_OF_YEAR);
    }

    public static int getDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int getDayOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.get(Calendar.YEAR);
    }

    public static int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.get(Calendar.MONTH);
    }

    public static String format(Date date, String formatString) {

        if(StringUtils.isEmpty(formatString)){
            formatString = "yyyy-MM-dd HH:mm:ss";
        }

        return (new SimpleDateFormat(formatString)).format(date);
    }

    public static Date getMinimumOnDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY,
                calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE,
                calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND,
                calendar.getActualMinimum(Calendar.SECOND));

        return date;
    }

    public static Date getMinimumOnSecond(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.SECOND,
                calendar.getActualMinimum(Calendar.SECOND));

        return date;
    }

    public static Date getMaximumOnDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY,
                calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE,
                calendar.getActualMaximum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND,
                calendar.getActualMaximum(Calendar.SECOND));
        date = calendar.getTime();

        return date;
    }

    public static Date getTime() {
    	Calendar calendar = Calendar.getInstance();
    	return calendar.getTime();
    }

    public static Date getDate(Date value, String timeZone) {

        SimpleDateFormat myFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone timeZoneChina = TimeZone.getTimeZone(timeZone);//获取时区
        myFmt.setTimeZone(timeZoneChina);//设置系统时区

        String strDate =  myFmt.format(value);

        return parse(strDate);
    }

    public static boolean tryParse(String dateStr, OutParameter<Date> resultSet) {
        try {
            resultSet.setData( parse(dateStr) );

            return true;
        }

        catch (Exception e)
        {
            return false;
        }
    }

    public static Date parse(String dateStr) {

    	if(StringUtils.isEmpty(dateStr)) {
    		return null;
    	}

    	dateStr = dateStr.trim();

        return parse(dateStr, getFormatString(dateStr));
    }

    public static String getFormatString(String dateStr) {

        String result = "yyyy-MM-dd HH:mm:ss";
        if(StringUtils.isEmpty(dateStr)) {
            result = result;
        }

        else if(Pattern.compile("^\\d{4}\\-\\d{2}\\-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}$").matcher(dateStr).find()) {
            result = "yyyy-MM-dd HH:mm:ss";
        }

        else if(Pattern.compile("^\\d{4}\\-\\d{2}\\-\\d{2}$").matcher(dateStr).find()) {
            result = "yyyy-MM-dd";
        }

        else if(Pattern.compile("^\\d{2}:\\d{2}:\\d{2}$").matcher(dateStr).find()) {
            result = "HH:mm:ss";
        }

        else if(Pattern.compile("^\\d{4}/\\d{2}/\\d{2}$").matcher(dateStr).find()) {
            result = "yyyy/MM/dd";
        }

        else if(Pattern.compile("^\\d{4}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}$").matcher(dateStr).find()) {
            result = "yyyy/MM/dd HH:mm:ss";
        }

        else if(Pattern.compile("^\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}$").matcher(dateStr).find()) {
            result = "yy/MM/dd HH:mm:ss";
        }

        else if(Pattern.compile("^\\d{2}\\-\\d{2}\\-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}$").matcher(dateStr).find()) {
            result = "yy-MM-dd HH:mm:ss";
        }

        else if(Pattern.compile("^\\d{4}\\-\\d{2}\\-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$").matcher(dateStr).find()) {
            result = "yy-MM-dd HH:mm:ss.SSS";
        }

        return result;
    }

    public static Date parse(String dateStr, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        //String formattedDate = formatter.format(dateStr);
        Date result = null;
        try
        {
            result = formatter.parse(dateStr.toString());
        } catch ( ParseException e )
        {
            e.printStackTrace();
        }

        return result;
    }

	public static String getTimeZone() {
		return timeZone;
	}

	public static void setTimeZone(String timeZone) {
		Datetime.timeZone = timeZone;
	}

	public static Date getTime(long millis) {
        Date d = new Date();
        d.setTime(millis);

        return d;
	}

    @Override
    public String getExpression() {
        return format(now(), "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 当前时间是否在指定区间内
     * @param startTime 开始时间，格式HH:mm
     * @param endTime 结束时间，格式HH:mm
     */
    public static boolean isEffectiveDate(String startTime, String endTime) throws ParseException {
        Date now = now();
        String startTimeFormat = "HH:mm";
        String endTimeFormat = "HH:mm";
        List<String> arr = StringUtils.splitString2List(startTime, ":");
        if(arr.size()==3) {
            startTimeFormat = "HH:mm:ss";
        }
        else if(arr.size()==1) {
            startTimeFormat = "HH";
        }
        arr = StringUtils.splitString2List(endTime, ":");
        if(arr.size()==3) {
            endTimeFormat = "HH:mm:ss";
        }
        else if(arr.size()==1) {
            endTimeFormat = "HH";
        }
        String dateString = format(now, "yyyy-MM-dd ");
        Date begin = new SimpleDateFormat("yyyy-MM-dd " + startTimeFormat).parse(dateString + startTime);
        Date end = new SimpleDateFormat("yyyy-MM-dd " + endTimeFormat).parse(dateString + endTime);
        return isEffectiveDate(now, begin, end);
    }

    /**
     * 判断当前时间是否在[startTime, endTime]区间，注意时间格式要一致
     *
     * @param nowTime 当前时间
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    public static boolean isEffectiveDate(Date nowTime, Date startTime, Date endTime) {
        if (nowTime.getTime() == startTime.getTime()
                || nowTime.getTime() == endTime.getTime()) {
            return true;
        }

        Calendar date = Calendar.getInstance();
        date.setTime(nowTime);

        Calendar begin = Calendar.getInstance();
        begin.setTime(startTime);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);

        if (date.after(begin) && date.before(end)) {
            return true;
        } else {
            return false;
        }
    }
}
