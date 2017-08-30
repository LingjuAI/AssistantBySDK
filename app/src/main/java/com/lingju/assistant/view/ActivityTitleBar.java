package com.lingju.assistant.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.util.ScreenUtil;

/**
 * Created by Ken on 2016/11/24.
 */
public class ActivityTitleBar extends RelativeLayout {

    private TextView mTvTitle;

    public ActivityTitleBar(Context context) {
        super(context);
        initView(context);
    }

    public ActivityTitleBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ActivityTitleBar);
        String title = attributes.getString(R.styleable.ActivityTitleBar_bar_title);
        setTvTitle(title);
        attributes.recycle();
    }

    private void initView(Context context) {
        View.inflate(context, R.layout.bar_activity_title, this);
        mTvTitle = (TextView) findViewById(R.id.tv_title);
        View statusBar = findViewById(R.id.status_bar);
        final GoBackActivity act = (GoBackActivity) context;
        ViewGroup.LayoutParams layoutParams = statusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(act);
        statusBar.setLayoutParams(layoutParams);
        findViewById(R.id.tv_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                act.onBackPressed();
            }
        });
    }

    public void setTvTitle(String title) {
        mTvTitle.setText(title);
    }
}
