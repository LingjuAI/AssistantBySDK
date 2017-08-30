package com.lingju.assistant.view;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.RemindActivity;
import com.lingju.assistant.service.RemindService;
import com.lingju.model.Remind;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;

public class RemindDialog extends Activity {

    private long id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remind_dialog);

        Intent intent = getIntent();
        if (intent != null) {
            String text = intent.getStringExtra("text");
            id = intent.getLongExtra("id", 0);
            if (!TextUtils.isEmpty(text)) {
                ((TextView) findViewById(R.id.ard_text)).setText(text);
            }
            ((TextView) findViewById(R.id.remind_time)).setText(new SimpleDate().toString());
        }
        findViewById(R.id.ard_close).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.cancel(RemindService.NotificationId);
                finish();
            }
        });
        findViewById(R.id.ard_detail).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.cancel(RemindService.NotificationId);
                Intent resultIntent = new Intent(RemindDialog.this, RemindActivity.class);
                resultIntent.putExtra("id", id);
                resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(resultIntent);
                finish();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            String text = intent.getStringExtra("text");
            id = intent.getIntExtra("id", 0);
            if (!TextUtils.isEmpty(text)) {
                if (findViewById(R.id.ard_text) != null)
                    ((TextView) findViewById(R.id.ard_text)).setText(text);
            }
        }
    }

    @Override
    protected void onDestroy() {
        Remind remind = AssistDao.getInstance().findRemindById(id);
        if (remind.getFrequency() == 0) {
            remind.setValid(0);
            AssistDao.getInstance().updateRemind(remind);
            Intent rIntent = new Intent(this, RemindService.class);
            rIntent.putExtra(RemindService.CMD, (RemindService.REMIND << 4) + RemindService.CANCEL);
            rIntent.putExtra(RemindService.ID, remind.getId());
            this.startService(rIntent);
        }
        super.onDestroy();
    }
}
