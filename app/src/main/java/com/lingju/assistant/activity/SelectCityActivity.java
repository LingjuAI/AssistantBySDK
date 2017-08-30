package com.lingju.assistant.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.SelectCityEvent;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.view.SelectCityDialog;
import com.lingju.common.log.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by Administrator on 2015/9/10.
 */
public class SelectCityActivity extends Activity {

    private SelectCityDialog selectCityDialog;
    private String cities[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_city);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
        }
        EventBus.getDefault().register(this);
        cities = getIntent().getStringArrayExtra("cities");
        String title = getIntent().getStringExtra("title");
        if (cities != null) {
            selectCityDialog = new SelectCityDialog(this, title, cities, new SelectCityDialog.OnSelectListener() {

                @Override
                public void onSelect(String city) {
                    Intent it = new Intent(SelectCityActivity.this, AssistantService.class);
                    it.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
                    String text = "第";
                    for (int i = 0; i < cities.length; i++) {
                        if (cities[i].equals(city)) {
                            text += (i + 1) + "个";
                            break;
                        }
                    }
                    it.putExtra("text", text);
                    it.putExtra(AssistantService.CALLBACK, true);
                    startService(it);
                    finish();
                    overridePendingTransition(R.anim.activity_start_in, R.anim.activity_start_out);
                }
            });
            selectCityDialog.setCancelable(false);
            selectCityDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface dialog,
                                     int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        onBackPressed();
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            selectCityDialog.setCancelListener(new SelectCityDialog.OnCancelListener() {
                @Override
                public void onCancel() {
                    finish();
                }
            });
        } else {
            finish();
            overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (selectCityDialog != null)
            selectCityDialog.show();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.i("selectCityActivity", "onBackPressed");
        Intent it = new Intent(SelectCityActivity.this, AssistantService.class);
        it.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
        it.putExtra("text", "取消");
        startService(it);
//        if (selectCityDialog != null) {
//            selectCityDialog.cancel();
//        }
        finish();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSelectCityEvent(SelectCityEvent e) {
        Log.i("SelectCityActivity", "onEventMainThread>>SelectCityEvent");
        if (getApplication() instanceof AppConfig && !TextUtils.isEmpty(e.getCity())) {
            ((AppConfig) getApplication()).selectedCityInSearchPoi = e.getCity();
        }
//        if (selectCityDialog != null) {
//            selectCityDialog.cancel();
//        }
        finish();
        overridePendingTransition(R.anim.activity_start_in, R.anim.activity_start_out);
    }

}
