package com.lingju.util;

import android.app.AlarmManager;

import com.lingju.model.SimpleDate;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    public static final long DAY = 86400000;
    private static TimeUtils timeUtils;
    /**
     * 查询客户属于哪个年龄阶段用的参数
     */
    private static final String ALL_PEOPLE = "青年";
    private static final String YOUTH_PEOPLE = "中年";
    private static final String MIDDLE_PEOPLE = "老年";
    private static final String OLD_PEOPLE = "所有人群";
    private static final int[] AGES_AROUND = new int[]{0, 30, 55};

    public static TimeUtils getInstance() {
        if (null == timeUtils)
            timeUtils = new TimeUtils();
        return timeUtils;
    }


    /**
     * 获取明天的时间(Date)，该时间类型的时间只有年月日，时分秒毫秒均为0
     *
     * @return
     */
    public static Date getTomorrow() {
        Calendar cl = Calendar.getInstance();
        cl.setTime(new Date(System.currentTimeMillis() + 24 * 3600000));
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        return cl.getTime();
    }

    //获取今天的日期，字符串格式
    public String getTodayString() {
        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(System.currentTimeMillis());
        int month = cl.get(Calendar.MONTH) + 1;
        return cl.get(Calendar.YEAR) + "-" + addZeroBefore(month)
                + "-" + addZeroBefore(cl.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * 获取今天的日期，字符串格式
     *
     * @param bidUp 天数加上
     * @return
     */
    public String getBidUpTodayString(int bidUp) {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.DATE, cl.get(Calendar.DATE) + bidUp);
        int month = cl.get(Calendar.MONTH) + 1;
        return cl.get(Calendar.YEAR) + "-" + addZeroBefore(month)
                + "-" + addZeroBefore(cl.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * 取今天的日期
     *
     * @return
     */
    //获取今天的日期，没有时分秒
    public static Date getTodayDate() {
        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(System.currentTimeMillis());
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        return cl.getTime();
    }

    //获取明天的日期
    public static Date getDateTomorrow(Date date) {
        Calendar cl = Calendar.getInstance();
        cl.setTime(new Date(date.getTime() + 24 * 3600000));
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        return cl.getTime();
    }

    /**
     * 获取日期对应的毫秒值（没有时分秒）
     **/
    public static long getDateMills(Date date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(sdf.format(date)).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取时间对应的SimpleDate类型的int value值
     **/
    public static int getTimeValue(Date date) {
        String time = new SimpleDateFormat("HH:mm").format(date);
        return new SimpleDate(time).toValue();
    }

    public static String getTime(Date date) {
        return new SimpleDateFormat("HH:mm").format(date);
    }

    /**
     * 获取本日的n个月前的日期，如本日=2015-03-01，n=3，则输出2014-12-01
     *
     * @param n
     * @return
     */
    public static Date getSomeMonthsBefore(int n) {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);

        int year = cl.get(Calendar.YEAR);
        int month = cl.get(Calendar.MONTH);
        int day = cl.get(Calendar.DAY_OF_MONTH);
        int by = n / 12;
        int bm = n % 12;
        if (by > 0) {
            year -= by;
        }
        if (bm > month) {
            year--;
        }
        month = (12 + month - bm) % 12;
        cl.set(Calendar.YEAR, year);
        cl.set(Calendar.MONTH, month);
        cl.set(Calendar.DAY_OF_MONTH, 1);
        if (day > cl.getActualMaximum(Calendar.DAY_OF_MONTH)) {//cl为n个月前的1号，如果该月的最大日数比现在的日数小，则取当月的最大日数
            cl.set(Calendar.DAY_OF_MONTH, cl.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else {
            cl.set(Calendar.DAY_OF_MONTH, day);
        }
        return cl.getTime();
    }


    public static Date getFirstDayOnSomeMonthsBefore(int n) {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);

        int year = cl.get(Calendar.YEAR);
        int month = cl.get(Calendar.MONTH);
        int by = n / 12;
        int bm = n % 12;
        if (by > 0) {
            year -= by;
        }
        if (bm > month) {
            year--;
        }
        month = (12 + month - bm) % 12;
        cl.set(Calendar.YEAR, year);
        cl.set(Calendar.MONTH, month);
        cl.set(Calendar.DAY_OF_MONTH, 1);
        return cl.getTime();
    }

    //把字符串装换成日期

    /**
     * 一个月前的今天0点0时0分0秒0毫秒
     *
     * @return
     */
    public static Date lastMonthBefore() {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);

        int month = cl.get(Calendar.MONTH);
        int tm = Math.abs(month + 11) % 12;
        if (tm > month) {
            cl.set(Calendar.YEAR, cl.get(Calendar.YEAR) - 1);
        }
        cl.set(Calendar.MONTH, tm);
        return cl.getTime();
    }

    /**
     * 一个星期前的今天0点0时0分0秒0毫秒
     *
     * @return
     */
    public static Date lastWeekBefore() {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        return new Date(cl.getTimeInMillis() - DAY * 7);
    }

    public static Date thisMonthFirstDay() {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        cl.set(Calendar.DAY_OF_MONTH, 1);
        return cl.getTime();
    }

    /**
     * 三个月前的今天0点0时0分0秒0毫秒
     *
     * @return
     */
    public static Date threeMonthBefore() {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        int month = cl.get(Calendar.MONTH);
        int tm = Math.abs(month + 9) % 12;
        if (tm > month) {
            cl.set(Calendar.YEAR, cl.get(Calendar.YEAR) - 1);
        }
        cl.set(Calendar.MONTH, tm);
        return cl.getTime();
    }

    /**
     * 一年前的今天0点0时0分0秒0毫秒
     *
     * @return
     */
    public static Date lastYearBefore() {
        Calendar cl = Calendar.getInstance();
        //cl.setTime(getDate("2012-02-29"));
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        cl.set(Calendar.YEAR, cl.get(Calendar.YEAR) - 1);
        return cl.getTime();
    }

    /**
     * 今年的一月一日0点0时0分0秒0毫秒
     *
     * @return
     */
    public static Date thisYear() {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        cl.set(Calendar.MONTH, Calendar.JANUARY);
        cl.set(Calendar.DAY_OF_MONTH, 1);
        return cl.getTime();
    }

    /**
     * 指定年份的一月一日0点0时0分0秒0毫秒
     *
     * @return
     */
    public static Date getYearStart(int year) {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        cl.set(Calendar.YEAR, year);
        cl.set(Calendar.MONTH, Calendar.JANUARY);
        cl.set(Calendar.DAY_OF_MONTH, 1);
        return cl.getTime();
    }

    /**
     * 获取当前月份第一天开始的日期对象
     **/
    public static Date thisMonth() {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        cl.set(Calendar.DAY_OF_MONTH, 1);
        return cl.getTime();
    }

    /**
     * 把文本转换成yyyy-MM-dd格式的时间
     *
     * @param str
     * @return
     */
    public static Date getDate(String str) {
        try {
            return sdf.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    //把日期转换成常用字符串格式
    public static String getDateString(Date date) {
        return sdf.format(date);
    }

    /**
     * 把一个时间转换成timestamp格式(yyyy-MM-dd)
     *
     * @param str
     * @return
     */
    public static Timestamp convertTimestamp(String str) {
        if (str == null || str.length() == 0)
            return null;
        try {
            return new Timestamp((new SimpleDateFormat("yyyy-MM-dd")).parse(str).getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    //把一个时间转换成timestamp格式
    public static Timestamp convertTimestamp(String str, String format) {
        if (str == null || str.length() == 0)
            return null;
        try {
            return new Timestamp((new SimpleDateFormat(format)).parse(str).getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    //计算两个时间相差多少天
    public int differDay(Date start, Date now) {
        long differ = now.getTime() - start.getTime();
        if (differ <= 0)
            return 0;
        return (int) (differ / AlarmManager.INTERVAL_DAY);
    }

    //计算两个时间相差了多少年
    public int differYear(Date start, Date now) {
        long differ = now.getTime() - start.getTime();
        if (differ <= 0)
            return 0;
        long year = 24 * 60 * 60 * 1000 * 365l;
        return (int) (differ / year) + 1;
    }

    //时分秒如果只有一位，则前面给他加上一个零   Note：    SQLite 根据查询时间查询必须是YYYY-00-00格式
    public String addZeroBefore(Object o) {
        if ((o + "").length() == 1)
            return "0" + (o + "");
        return o.toString();
    }

    //把一个时间试过时分秒都是只有一位数的时候，前面加上零。
    public String addZeroByTime(String time) {
        String[] times = time.split(":");
        times[0] = addZeroBefore(times[0]);
        times[1] = addZeroBefore(times[1]);
        times[2] = addZeroBefore(times[2]);
        return times[0] + ":" + times[1] + ":" + times[2];
    }

    //把一个时间试过时分都是只有一位数的时候，前面加上零。
    public String addZeroByTimeDetailToMinute(String time) {
        String[] times = time.split(":");
        times[0] = addZeroBefore(times[0]);
        times[1] = addZeroBefore(times[1]);
        return times[0] + ":" + times[1];
    }

    //返回此时刻的时分
    public String getNowTimeString() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return hour + ":" + timeUtils.addZeroBefore(minute);
    }

    //返回1第一个比第二个大，返回2第二个比第一个大，返回0，两个相等  这里比较的是时间
    public int compareToTime(String first, String two) {
        String[] firsts = first.split(":");
        String[] twos = two.split(":");
        if (Integer.parseInt(firsts[0]) > Integer.parseInt(twos[0])) {
            return 1;
        } else if (Integer.parseInt(firsts[0]) == Integer.parseInt(twos[0])) {
            if (Integer.parseInt(firsts[1]) > Integer.parseInt(twos[1])) {
                return 1;
            } else if (Integer.parseInt(firsts[1]) == Integer.parseInt(twos[1])) {
                return 0;
            } else {
                return 2;
            }
        } else {
            return 2;
        }
    }

    //返回1第一个比第二个大，返回2第二个比第一个大，返回0，两个相等   这里比较的是日期     Note:卧槽   SB啊 这样写法，用Calendar的毫秒数比较就好啦---》这样效率好吗
    public int compareToDate(String first, String two) {
        String[] firsts = first.split("-");
        String[] twos = two.split("-");
        if (Integer.parseInt(firsts[0]) > Integer.parseInt(twos[0])) {
            return 1;
        } else if (Integer.parseInt(firsts[0]) == Integer.parseInt(twos[0])) {
            if (Integer.parseInt(firsts[1]) > Integer.parseInt(twos[1]))
                return 1;
            else if (Integer.parseInt(firsts[1]) == Integer.parseInt(twos[1])) {
                if (Integer.parseInt(firsts[2]) > Integer.parseInt(twos[2]))
                    return 1;
                else if (Integer.parseInt(firsts[2]) == Integer.parseInt(twos[2]))
                    return 0;
                else
                    return 2;
            } else
                return 2;
        } else {
            return 2;
        }
    }

    //返回当天两个时间点相差的毫秒数，计算的时候值计算时分秒，不考虑到秒和毫秒
    public int getMillisecondDiffer(String start, String end) {
        Calendar cl1 = Calendar.getInstance();
        cl1.set(Calendar.HOUR_OF_DAY, Integer.parseInt(start.split(":")[0]));
        cl1.set(Calendar.MINUTE, Integer.parseInt(start.split(":")[1]));
        cl1.set(Calendar.SECOND, 0);
        cl1.set(Calendar.MILLISECOND, 0);
        Calendar cl2 = Calendar.getInstance();
        cl2.set(Calendar.HOUR_OF_DAY, Integer.parseInt(end.split(":")[0]));
        cl2.set(Calendar.MINUTE, Integer.parseInt(end.split(":")[1]));
        cl2.set(Calendar.SECOND, 0);
        cl2.set(Calendar.MILLISECOND, 0);
        return (int) (cl2.getTimeInMillis() - cl1.getTimeInMillis());
    }

    //返回当天两个时间点相差的毫秒数，计算的时候值计算时分秒，不考虑到秒和毫秒,endBidUp结束时间加上endBidUp这么多天

    /**
     * @param start
     * @param end
     * @param endBidUp
     * @return
     */
    public int getMillisecondDiffer(String start, String end, int endBidUp) {
        Calendar cl1 = Calendar.getInstance();
        cl1.set(Calendar.HOUR_OF_DAY, Integer.parseInt(start.split(":")[0]));
        cl1.set(Calendar.MINUTE, Integer.parseInt(start.split(":")[1]));
        cl1.set(Calendar.SECOND, 0);
        cl1.set(Calendar.MILLISECOND, 0);
        Calendar cl2 = Calendar.getInstance();
        cl2.set(Calendar.HOUR_OF_DAY, Integer.parseInt(end.split(":")[0]) + endBidUp * 24);
        cl2.set(Calendar.MINUTE, Integer.parseInt(end.split(":")[1]));
        cl2.set(Calendar.SECOND, 0);
        cl2.set(Calendar.MILLISECOND, 0);
        return (int) (cl2.getTimeInMillis() - cl1.getTimeInMillis());
    }

    //返回当天的时间，再修改时分后的毫秒数
    public long getMillisecondByTime(String target) {
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.HOUR_OF_DAY, Integer.parseInt(target.split(":")[0]));
        cl.set(Calendar.MINUTE, Integer.parseInt(target.split(":")[1]));
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        //System.out.println("CurrentTime:"+System.currentTimeMillis());
        //System.out.println("timeUtil:"+cl.getTimeInMillis());
        return cl.getTimeInMillis();
    }

    //返回当前时间添加了叠加的毫秒数后，返回当前的时分
    public String getTimeBidUpMilliSecond(long bidUp) {
        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(System.currentTimeMillis() + bidUp);

        return cl.get(Calendar.HOUR_OF_DAY) + ":" + cl.get(Calendar.MINUTE);
    }

    //返回当前时间添加了叠加的毫秒数后，返回当前的时分
    public String getTimeByMilliSecond(long mililis) {
        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(mililis);
        return cl.get(Calendar.HOUR_OF_DAY) + ":" + cl.get(Calendar.MINUTE);
    }


    //返回当前时间是属于哪个时间段
    public String getTimesDescription(String time) {
        String[] times = time.split(":");
        if (Integer.parseInt(times[0]) < 11)
            return "早";
        else if (Integer.parseInt(times[0]) < 13)
            return "中";
        else
            return "晚";
    }

    //返回当前时间是属于哪个时间段
    public String getTimesDescriptionFormat2(String time) {
        String[] times = time.split(":");
        if (Integer.parseInt(times[0]) < 11)
            return "早上";
        else if (Integer.parseInt(times[0]) < 13)
            return "中午";
        else
            return "下午";
    }

    //返回第一个日期格式
    public String firstMatcherDateFormat(String str) {
        Pattern p = Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}");
        Matcher matcher = p.matcher(str);
        if (matcher.find())
            return str.substring(matcher.start(), matcher.end());
        else
            return "";
    }

    /**
     * 根据index的值添加或者减少month的值
     *
     * @param date
     * @param index
     * @return
     */
    public static String dateMonthChangeByIndex(String date, int index) {
        Calendar cl = Calendar.getInstance();
        String[] dates = date.split("-");
        cl.set(Calendar.YEAR, Integer.parseInt(dates[0]));
        cl.set(Calendar.MONTH, Integer.parseInt(dates[1]) + index - 1);
        cl.set(Calendar.DATE, Integer.parseInt(dates[2]));
        Date afterDate = new Date(cl.getTimeInMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(afterDate);
    }

    /**
     * 获取今天和今天的时分时间---只详细到分
     */
    public String getToDayDateAndTimeDetailToMinute() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new Date());
    }

    /**
     * 查询客户属于哪个年龄阶段
     *
     * @param birthday
     * @return
     */
    public String findAgeAround(Timestamp birthday) {
        Date birDate = new Date(birthday.getTime());
        int differAge = differYear(birDate, new Date());
        if (differAge >= AGES_AROUND[0] && differAge <= AGES_AROUND[1])
            return YOUTH_PEOPLE;
        else if (differAge >= AGES_AROUND[1] && differAge <= AGES_AROUND[2])
            return MIDDLE_PEOPLE;
        else if (differAge > AGES_AROUND[2])
            return OLD_PEOPLE;
        return ALL_PEOPLE;
    }

    /**
     * 获取今天此时此刻的日期和时间
     *
     * @return
     */
    public String getToDayTimes() {
        SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//24小时制
        return sdformat.format(new Date());
    }

    /**
     * 将时间转换成 yyyy年MM月dd日 格式的文本
     **/
    public static String format(Date time) {
        return new SimpleDateFormat("yyyy年MM月dd日").format(time);
    }

    public static String formatDate(Date time) {
        return new SimpleDateFormat("MM月dd日").format(time);
    }

    /**
     * 将 yyyy年MM月dd日 格式时间文本转换为Date对象
     **/
    public static Date parse(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将时间转换成 yyyy年MM月dd日 HH:ss 格式的文本
     **/
    public static String formatDateTime(Date time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        return sdf.format(time);
    }

    public static Date parseDateTime(String dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        try {
            return sdf.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Date转化为“yyyy-MM-dd HH:mm:ss”格式的字符串
     *
     * @param time
     * @return
     */
    public static String time2String(Date time) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (time != null) {
            return df.format(time);
        }
        return null;
    }

    public static Timestamp string2Timestamp(String time) {
        if (time != null) {
            SimpleDateFormat df = time.length() == 10 ? new SimpleDateFormat("yyyy-MM-dd") : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (time != null && !"".equals(time)) {
                try {
                    return new Timestamp(df.parse(time).getTime());
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
