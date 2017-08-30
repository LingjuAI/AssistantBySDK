package com.lingju.assistant.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.lingju.assistant.R;
import com.lingju.assistant.view.wheel.widget.OnWheelChangedListener;
import com.lingju.assistant.view.wheel.widget.WheelView;
import com.lingju.assistant.view.wheel.widget.adapters.ArrayWheelAdapter;

import java.util.Calendar;

public class DatePicker extends LinearLayout {
    private WheelView yearView;
    private WheelView monthView;
    private WheelView dayView;

    private ArrayWheelAdapter<String> yearAdapter;
    private ArrayWheelAdapter<String> monthAdapter;
    private DayArrayWheelAdapter<String> dayAdapter;

    private String years[] = new String[51];
    private String months[] = new String[]{"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月",};
    private String days[];

    private Calendar defaultDate = Calendar.getInstance();
    private OnchangedListener onChangedListener;
    private boolean showMonth = true;
    private boolean showDay = true;

    public DatePicker(Context context, Calendar defaultDate/*,OnResultListener defaultListener*/) {
        super(context);
        this.defaultDate = defaultDate;
        //this.defaultListener=defaultListener;
        init(context);
    }

    public DatePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.DatePicker);
        showMonth = attributes.getBoolean(R.styleable.DatePicker_isShowMonth, true);
        showDay = attributes.getBoolean(R.styleable.DatePicker_isShowDay, true);
        attributes.recycle();
        init(context);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void setYears() {
        int b = 2000;
        for (int i = 0; i < 51; i++) {
            years[i] = Integer.toString(b++);
        }
    }

    public void setDefaultDate(long defaultTime) {
        this.defaultDate.setTimeInMillis(defaultTime);
        yearView.setCurrentItem(defaultDate.get(Calendar.YEAR) - 2000);
        monthView.setCurrentItem(defaultDate.get(Calendar.MONTH));
        dayView.setCurrentItem(defaultDate.get(Calendar.DAY_OF_MONTH) - 1);
    }

    public void setOnChangedListener(OnchangedListener onChangedListener) {
        this.onChangedListener = onChangedListener;
    }

	/*public void setDefaultOnResultListener(OnResultListener defaultListener) {
        this.defaultListener = defaultListener;
	}*/

    private void resetDays(int yearItem, int monthItem) {
        int maxDays = defaultDate.getActualMaximum(Calendar.DAY_OF_MONTH);
        days = new String[maxDays];
        for (int i = 0; i < maxDays; i++) {
            if (i < 9) {
                days[i] = "0" + (i + 1);
            } else {
                days[i] = "" + (i + 1);
            }
        }
    }

    public Calendar getSelectedDate() {
        return defaultDate;
    }

    private void init(Context context) {
        setYears();
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.date_picker, this);
        yearView = (WheelView) findViewById(R.id.dp_year);
        monthView = (WheelView) findViewById(R.id.dp_month);
        dayView = (WheelView) findViewById(R.id.dp_day);
        if (!showMonth)
            monthView.setVisibility(GONE);
        if (!showDay)
            dayView.setVisibility(GONE);

        yearAdapter = new ArrayWheelAdapter<>(context, years);
        yearAdapter.setItemResource(R.layout.wheel_text_item);
        yearAdapter.setItemTextResource(R.id.text);
        yearView.setViewAdapter(yearAdapter);
        yearView.setCyclic(true);
        yearView.setCurrentItem(defaultDate.get(Calendar.YEAR) - 2000);
        yearView.addChangingListener(new OnWheelChangedListener() {

            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                if (newValue != oldValue) {
                    defaultDate.set(Calendar.YEAR, newValue + 2000);
                    resetDays(newValue, defaultDate.get(Calendar.MONTH));
                    dayAdapter.notifyDataChanged(days);
                    if (onChangedListener != null) {
                        onChangedListener.onChanged(defaultDate);
                    }
                }
            }
        });

        monthAdapter = new ArrayWheelAdapter<>(context, months);
        monthAdapter.setItemResource(R.layout.wheel_text_item);
        monthAdapter.setItemTextResource(R.id.text);
        monthView.setViewAdapter(monthAdapter);
        monthView.setCyclic(true);
        monthView.setCurrentItem(defaultDate.get(Calendar.MONTH));
        monthView.addChangingListener(new OnWheelChangedListener() {

            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                if (newValue != oldValue) {
                    defaultDate.set(Calendar.MONTH, newValue);
                    resetDays(defaultDate.get(Calendar.YEAR) - 2000, newValue);
                    dayAdapter.notifyDataChanged(days);
                    if (onChangedListener != null) {
                        onChangedListener.onChanged(defaultDate);
                    }
                }
            }
        });

        resetDays(defaultDate.get(Calendar.YEAR) - 2000, defaultDate.get(Calendar.MONTH));

        dayAdapter = new DayArrayWheelAdapter<>(context, days);
        dayAdapter.setItemResource(R.layout.wheel_text_item);
        dayAdapter.setItemTextResource(R.id.text);
        dayView.setViewAdapter(dayAdapter);
        dayView.setCyclic(true);
        dayView.setCurrentItem(defaultDate.get(Calendar.DAY_OF_MONTH) - 1);
        dayView.addChangingListener(new OnWheelChangedListener() {

            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                defaultDate.set(Calendar.DAY_OF_MONTH, newValue + 1);
                if (onChangedListener != null) {
                    onChangedListener.onChanged(defaultDate);
                }
            }
        });
    }

    public interface OnchangedListener {
        void onChanged(Calendar date);
    }
	
	/*public interface OnResultListener{
		public void onCancel();
		public void onConfirm(long date);
	}*/

    class DayArrayWheelAdapter<T> extends ArrayWheelAdapter<T> {

        public DayArrayWheelAdapter(Context context, T[] items) {
            super(context, items);
        }

        public void notifyDataChanged(T items[]) {
            if (items.length == super.items.length) {
                return;
            }
            super.items = items;
            notifyDataChanged(items);
        }

    }

}
