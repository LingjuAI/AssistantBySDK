package com.lingju.assistant.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.lingju.assistant.service.AssistantService;

/**
 * 闹钟激活广播接收者
 **/
public class AlarmReceiver extends BroadcastReceiver {
    public final static String ACTION = "com.lingju.assistant.AlarmReceiver";

    public AlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent();
        i.setClass(context, AssistantService.class);
        /*if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            context.sendBroadcast(new Intent(BootReceiver.ACTION));

			//  if(cl.get(Calendar.HOUR_OF_DAY)==0&&cl.get(Calendar.MINUTE)==0){
			//  	i.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.INIT_ALARM);
			//  }
        }*/
        Log.i("AlarmReceiver", "onReceive,action=" + intent.getAction());
       /* Calendar cl = Calendar.getInstance();
        int hour = cl.get(Calendar.HOUR_OF_DAY);
        if ((6 < hour && hour < 10) || (12 <= hour && hour <= 14) || (18 <= hour && hour <= 20)) {
            i.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.INIT_ALARM);
        }*/
        context.startService(i);

    }

}
