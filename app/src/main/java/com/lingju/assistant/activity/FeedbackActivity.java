package com.lingju.assistant.activity;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.util.MusicUtils;

import java.lang.reflect.Method;

public class FeedbackActivity extends GoBackActivity implements OnClickListener {
    private EditText mFeedBack;
    private EditText mContactWay;
    private TextView mTvLimit;
    private StringBuilder mBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        findViewById(R.id.afb_submit).setOnClickListener(this);
        mFeedBack = (EditText) findViewById(R.id.afb_edit);
        mContactWay = (EditText) findViewById(R.id.et_contact_way);
        mTvLimit = (TextView) findViewById(R.id.tv_limit);

        mFeedBack.addTextChangedListener(mTextWatcher);
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            mTvLimit.setText(s.length() + "/200");
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.afb_submit:
                if (TextUtils.isEmpty(mFeedBack.getText().toString().trim())) {
                    new CommonDialog(FeedbackActivity.this, "错误提示", "提交失败，反馈内容不能为空", "确定").show();
                    return;
                } else {
                    mBuilder = new StringBuilder();
                    mBuilder.append(mFeedBack.getText().toString().trim())
                            .append("\\n联系方式：")
                            .append(mContactWay.getText().toString().trim());
                    new SubmitFeedbackTask().execute();
                }
                break;
        }
    }

    private String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method get = clz.getMethod("get", String.class, String.class);
            return (String) get.invoke(clz, key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    class SubmitFeedbackTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            if (mBuilder.length() > 6) {
                /*User u= ((AssistantApplication)getApplication()).user;
                if(u==null||u.getUserid()==null){
					return 2;
				}*/

                mBuilder.append("\\n版本号：V" + AppConfig.versionName)
                        .append("\\n手机型号：").append(Build.MODEL)
                        .append("\\n系统版本：").append(getSystemProperty("ro.build.display.id", "未知"));
//                Log.i("LingJu", "反馈信息：" + mBuilder.toString());
                return MusicUtils.submitFeedback(mBuilder.toString(), /*((AssistantApplication)getApplication()).user*/ null) ? 0 : 1;
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case 0:
                    // Toast.makeText(FeedbackActivity.this, "提交成功，感谢您的参与！！", Toast.LENGTH_SHORT).show();
                    Snackbar.make(mFeedBack,"提交成功，感谢您的反馈！", Snackbar.LENGTH_SHORT)
                            .setCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar snackbar, int event) {
                                    finish();
                                    super.onDismissed(snackbar, event);
                                }
                            })
                            .show();
                    break;
                case 1:
                    new CommonDialog(FeedbackActivity.this, "错误提示", "提交失败，请稍后再试", "确定").show();
                    break;
                case 2:
                    new CommonDialog(FeedbackActivity.this, "错误提示", "提交失败，您还未登录", "确定").show();
                    break;
            }
        }

    }
}
