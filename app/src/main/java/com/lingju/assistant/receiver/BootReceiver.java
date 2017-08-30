package com.lingju.assistant.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** 开机广播接收者 **/
public class BootReceiver extends BroadcastReceiver {
	public final static String ACTION="com.lingju.assistant.START_COMPLETED";
	
	public BootReceiver() {
	}
	
	@Override
	public void onReceive(Context context, Intent mintent) {
		//if (Intent.ACTION_BOOT_COMPLETED.equals(mintent.getAction())) {  
            // 启动完成  
		Log.i("BootReceiver", "onReceive");
            Intent intent = new Intent(context, AlarmReceiver.class);  
            intent.setAction(AlarmReceiver.ACTION);  
            PendingIntent sender = PendingIntent.getBroadcast(context, 0,  
                    intent, 0);  
            AlarmManager am = (AlarmManager) context  
                    .getSystemService(Context.ALARM_SERVICE);  
            if(android.os.Build.VERSION.SDK_INT<android.os.Build.VERSION_CODES.KITKAT){
				/** 开机激活闹钟 **/
            	am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+60000, sender);
			}
			else{
				am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+60000, sender);
			}
       // }  
	}
}
