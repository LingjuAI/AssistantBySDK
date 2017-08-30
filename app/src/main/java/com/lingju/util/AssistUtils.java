package com.lingju.util;

import com.lingju.context.entity.Scheduler;
import com.lingju.model.AlarmClock;
import com.lingju.model.Remind;
import com.lingju.model.SimpleDate;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Ken on 2016/11/24.
 */
public class AssistUtils {

    public final static String WEEKS[] = new String[]{"日", "一", "二", "三", "四", "五", "六"};
    public final static String DETAILWEEKS[] = new String[]{"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
    public static String[] RemindFrType = new String[11];
    private static DecimalFormat adf = new DecimalFormat("#,##0.00");

    static {
        RemindFrType[0] = "仅一次";
        RemindFrType[1] = "每周一";
        RemindFrType[2] = "每周二";
        RemindFrType[3] = "每周三";
        RemindFrType[4] = "每周四";
        RemindFrType[5] = "每周五";
        RemindFrType[6] = "每周六";
        RemindFrType[7] = "每周日";
        RemindFrType[8] = "每天";
        RemindFrType[9] = "每月今日";
        RemindFrType[10] = "每年今日";
    }

    /**
     * 闹钟周期转换星期文本
     **/
    public static String transalteWeekDayString(int value) {
        StringBuilder sb = new StringBuilder();
        if (value < 8) {
            sb.append(value == 0 ? "仅一次" : WEEKS[value % 7]);
        } else {
            int l = Integer.toOctalString(value).length();
            if (l == 7) {
                sb.append("每天");
            } else {
                for (int i = 0; i < l; i++) {
                    sb.append(WEEKS[((value >> ((l - i - 1) * 3)) % 8) % 7]).append(" ");
                }
                sb.setLength(sb.length() - 1);
            }
        }

        return sb.toString();
    }

    public static String translateRemindFrequency(int f) {
        return translateRemindFrequency(f, Calendar.getInstance());
    }

    /**
     * 提醒周期转换
     **/
    public static String translateRemindFrequency(int f, Calendar date) {
        if (f < 0)
            return "";
        if (f < 9) {
            return RemindFrType[f];
        } else {
            if (f == 9) {
                return date != null ? "每月的" + date.get(Calendar.DAY_OF_MONTH) + "日" : RemindFrType[9];
            } else {
                SimpleDateFormat sf = new SimpleDateFormat("MM月dd日");
                return date != null ? "每年的" + sf.format(new Date(date.getTimeInMillis())) : RemindFrType[10];
            }
        }
    }

    /**
     * 将闹钟周期转换成int数组
     **/
    public static int[] transalteWeekDays(int value) {
        if (value < 8)
            return new int[]{value};
        int l = Integer.toOctalString(value).length();
        int rs[] = new int[l];
        for (int i = 0; i < l; i++) {
            rs[i] = (value >> ((l - i - 1) * 3)) % 8;
        }
        return rs;
    }

    /**
     * 将闹钟周期数组转换换为闹钟对象的周期属性
     **/
    public static int array2AlarmFr(int[] arr) {
        int fr = 0;
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                fr <<= 3;
                fr += arr[i];
            }
        }
        return fr;
    }

    /**
     * 获取今天对应的星期几文本
     **/
    public static String getWeekDay() {
        Calendar cl = Calendar.getInstance();
        int week = cl.get(Calendar.DAY_OF_WEEK);
        return DETAILWEEKS[week - 1];
    }

    /**
     * 格式化账单金额
     **/
    public static String formatAmount(double money) {
        return adf.format(money);
    }

    /**
     * 解析账单文本
     **/
    public static double parseAmount(String amount) {
        double money = 0;
        try {
            money = (double) adf.parse(amount);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return money;
    }

    public static boolean isNumber(String str) {
        if (str != null) {
            String reg = "^(-)?[0-9]+(.[0-9]+)?$";
            return str.matches(reg);
        }
        return false;
    }

    /**
     * 设置提醒周期
     **/
    public static void setRemindFr(Remind remind, Scheduler scheduler) {
        Long interval = scheduler.getInterval();
        String unit = scheduler.getUnit();
        int fr = 0;
        if (interval == 0) {
            fr = 0;
        } else if (interval == 1) {
            if ("D".equals(unit)) {
                fr = 8;
            } else if ("M".equals(unit)) {
                fr = 9;
            } else if ("Y".equals(unit)) {
                fr = 10;
            }
        } else if (interval == 7) {
            Calendar cl = Calendar.getInstance();
            cl.setTimeInMillis(scheduler.getWhen());
            int week = cl.get(Calendar.DAY_OF_WEEK);
            fr = week - 1 == 0 ? 7 : week - 1;
        }
        remind.setFrequency(fr);
    }

    /**
     * 设置闹钟周期相关（周期，响铃时间，是否重复）
     **/
    public static void setAlarmFr(AlarmClock alarm, List<Scheduler> schedulers) {
        Scheduler scheduler = schedulers.get(0);
        alarm.setRtime(TimeUtils.getTimeValue(new Date(scheduler.getWhen())));
        if (scheduler.getInterval() == 0) {
            alarm.setRepeat(false);
            alarm.setFrequency(0);
            alarm.setRdate(new Date(scheduler.getWhen()));
        } else {
            alarm.setRepeat(true);
            alarm.setRdate(new Date(scheduler.getWhen()));
            int frArr[];
            if (scheduler.getInterval() == 1) {
                frArr = new int[]{1, 2, 3, 4, 5, 6, 7};
            } else {
                frArr = new int[schedulers.size()];
                for (int i = 0; i < schedulers.size(); i++) {
                    scheduler = schedulers.get(i);
                    Calendar cl = Calendar.getInstance();
                    cl.setTimeInMillis(scheduler.getWhen());
                    int week = cl.get(Calendar.DAY_OF_WEEK);
                    week = week - 1 == 0 ? 7 : week - 1;
                    frArr[i] = week;
                }
            }

            alarm.setFrequency(array2AlarmFr(frArr));
        }
    }

    /**
     * 设置闹钟响铃日期（方便转换成Scheduler）
     **/
    public static void setAlarmRdate(AlarmClock alarm) {
        int frequency = alarm.getFrequency();
        if (frequency == 0) {
            if (alarm.getRtime() > TimeUtils.getTimeValue(new Date())) {
                alarm.setRdate(new SimpleDate(alarm.getRtime()).getDateByTime(TimeUtils.getTodayDate()));
            } else {
                alarm.setRdate(new SimpleDate(alarm.getRtime()).getDateByTime(TimeUtils.getTomorrow()));
            }
        } else {
            int[] weekDays = transalteWeekDays(frequency);
            Date rdate = new SimpleDate(alarm.getRtime()).getDateByTime(TimeUtils.getTodayDate());
            Calendar cl = Calendar.getInstance();
            int week = cl.get(Calendar.DAY_OF_WEEK);
            week = week - 1 == 0 ? 7 : week - 1;
            rdate = new Date((rdate.getTime() + (weekDays[0] - week) * TimeUtils.DAY));
            alarm.setRdate(rdate);
        }
    }
}
