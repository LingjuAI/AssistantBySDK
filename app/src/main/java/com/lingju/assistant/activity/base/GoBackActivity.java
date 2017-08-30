package com.lingju.assistant.activity.base;

import android.view.KeyEvent;

import com.lingju.assistant.R;

/**
 * Created by Ken on 2016/11/25.
 */
public class GoBackActivity extends StopListennerActivity {

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void goBack() {
        finish();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }

    public void goInto() {
        overridePendingTransition(R.anim.activity_start_in, R.anim.activity_start_out);
    }

    @Override
    public void onBackPressed() {

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
            super.onBackPressed();
        }else {*/
        goBack();
        // }
    }
}
