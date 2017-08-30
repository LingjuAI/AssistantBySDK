package com.lingju.assistant.view;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.audio.engine.base.SynthesizerBase;

public class SelectCityDialog extends Dialog implements View.OnClickListener {
    private Activity context;
    private String cities[];
    private LinearLayout listBox;
    private OnSelectListener listener;
    private OnCancelListener onCancelListener;
    private String title;

    public SelectCityDialog(Activity context, String cities[], OnSelectListener listener) {
        this(context, null, cities, listener);
    }

    public SelectCityDialog(Activity context, String title, String cities[], OnSelectListener listener) {
        super(context, R.style.lingju_dialog1);
        // setCancelable(false);
        if (cities == null || cities.length == 0)
            throw new NullPointerException("cities can not be null or empty");
        this.title = title;
        this.cities = cities;
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_select_city);
        listBox = (LinearLayout) findViewById(R.id.dsc_list_box);
        init();
    }

    private int dp2px(int dp) {
        return (int) (0.5f + context.getResources().getDisplayMetrics().density * (float) dp);
    }

    private void init() {
        int l = cities.length;
        this.setCanceledOnTouchOutside(true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        //  LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ScreenUtil.getInstance().dip2px(1));
        //layoutParams.leftMargin=dp2px(12);
        //layoutParams.rightMargin=dp2px(12);
        for (int i = 0; i < l; i++) {
            TextView textView = new TextView(context);
            textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            textView.setBackgroundResource(R.drawable.selector_view_bg);
            textView.setTextSize(17);
            textView.setTextColor(context.getResources().getColor(R.color.new_text_color_second));
            textView.setText(cities[i]);
            textView.setTag(i);
            textView.setOnClickListener(this);
            textView.setPadding(dp2px(24), dp2px(8), dp2px(24), dp2px(8));
            listBox.addView(textView, layoutParams);

            //            View view = new View(context);
            //            view.setBackgroundColor(context.getResources().getColor(R.color.new_line_border));
            //            listBox.addView(view, params);
        }
        //取消按钮点击监听
        TextView cancle = (TextView) findViewById(R.id.tv_cancel);
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectCityDialog.this.cancel();
            }
        });
        if (!TextUtils.isEmpty(title))
            ((TextView) findViewById(R.id.dsc_title)).setText(title);
    }

    @Override
    public void onClick(View v) {
        cancel();
        int i = (Integer) v.getTag();
        if (listener != null) {
            listener.onSelect(cities[i]);
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        if (onCancelListener != null) {
            onCancelListener.onCancel();
        }
        if (SynthesizerBase.isInited() && SynthesizerBase.get().isSpeaking()) {
            SynthesizerBase.get().stopSpeakingAbsolte();
        }
    }

    @Override
    public void show() {

		/*Window win = getWindow();
        win.getDecorView().setPadding(0, 0, 0, 0);
		LayoutParams lp = win.getAttributes();
		lp.gravity=Gravity.CENTER_VERTICAL;
		lp.width = LayoutParams.MATCH_PARENT;
		lp.height = LayoutParams.WRAP_CONTENT;
		win.setAttributes(lp);*/

	/*
        Window dialogWindow =getWindow();
        LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.CENTER_VERTICAL);
        WindowManager m = context.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
		lp.width = context.getResources().getDisplayMetrics().widthPixels;
		Log.e("SelectCityDialog","width>>>>>>>>>>>>>>>>>"+lp.width);
        //lp.x = 0;
        //lp.y = 0;
        dialogWindow.setAttributes(lp);*/
        super.show();
    }

    public interface OnSelectListener {
        void onSelect(String city);
    }

/*	@Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
		if(context.getWindow().getAttributes().screenBrightness==0.01f){
			LayoutParams params=context.getWindow().getAttributes();
			params.screenBrightness= LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
			context.getWindow().setAttributes(params);
		}
		return super.dispatchTouchEvent(ev);
	}*/

    public void setCancelListener(OnCancelListener cancelListener) {
        this.onCancelListener = cancelListener;
    }

    public interface OnCancelListener {
        void onCancel();
    }
}
