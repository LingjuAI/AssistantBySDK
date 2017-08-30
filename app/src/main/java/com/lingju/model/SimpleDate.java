package com.lingju.model;

import java.util.Calendar;
import java.util.Date;

public class SimpleDate {

    private int hour;
    private int minute;

    public SimpleDate() {
        Calendar cl = Calendar.getInstance();
        hour = cl.get(Calendar.HOUR_OF_DAY);
        minute = cl.get(Calendar.MINUTE);
    }

    public SimpleDate(String date) {
        try {
            if (date.indexOf(':') != -1) {
                String ds[] = date.split(":");
                hour = Integer.parseInt(ds[0]);
                minute = Integer.parseInt(ds[1]);
            }
        } catch (Exception e) {

        }
    }

    public void setValue(int dateValue) {
        if (dateValue <= 0 || dateValue > 1439) {
            hour = 0;
            minute = 0;
            return;
        }
        hour = dateValue / 60;
        minute = dateValue % 60;
    }

    public SimpleDate(int dateValue) {
        if (dateValue <= 0 || dateValue > 1439) {
            hour = 0;
            minute = 0;
            return;
        }
        hour = dateValue / 60;
        minute = dateValue % 60;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int toValue() {
        return hour * 60 + minute;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(hour < 10 ? "0" + hour : hour);
        sb.append(':');
        sb.append(minute < 10 ? "0" + minute : minute);
        return sb.toString();
    }

    public boolean lt(SimpleDate sd) {
        if (sd == null)
            return false;
        if (hour < sd.getHour())
            return true;
        if (hour == sd.getHour() && minute < sd.getMinute())
            return true;
        return false;
    }

    public boolean gt(SimpleDate sd) {
        if (sd == null)
            return true;
        if (hour > sd.getHour())
            return true;
        if (hour == sd.getHour() && minute > sd.getMinute())
            return true;
        return false;
    }

    public boolean equals(SimpleDate o) {
        if (o == null)
            return false;
        return hour == o.getHour() && minute == o.getMinute();
    }

    public Date getDateByTime(Date date) {
        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(date.getTime());
        cl.set(Calendar.HOUR_OF_DAY, hour);
        cl.set(Calendar.MINUTE, minute);
        return cl.getTime();
    }

    public String formDuration(int duration) {
        hour = duration / 60;
        minute = duration % 60;
        return toString();
    }
}
