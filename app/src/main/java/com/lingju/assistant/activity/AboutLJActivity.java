package com.lingju.assistant.activity;

import android.os.Bundle;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;

/**
 * Created by Ken on 2016/11/25.
 */
public class AboutLJActivity extends GoBackActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_lingju);
    }
}
