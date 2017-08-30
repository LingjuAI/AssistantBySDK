package com.lingju.assistant.activity.base;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.service.AssistantService;
import com.lingju.common.log.Log;
import com.tencent.stat.StatService;

import java.util.List;

public class StopListennerActivity extends AppCompatActivity {
	
	@Override
	protected void onStop() {
		if(!isAppOnForeground()&& AppConfig.Granted){
			Intent intent=new Intent(this, AssistantService.class);
			intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PLAY_IN_BACKGROUND);
			intent.putExtra(AssistantService.FLAG,true);
			startService(intent);
		}
		super.onStop();
	}
	
	 private boolean isAppOnForeground() { 
		    ActivityManager aManager=((ActivityManager)getSystemService(Context.ACTIVITY_SERVICE));
	        List<RunningAppProcessInfo> appProcesses = aManager.getRunningAppProcesses(); 
	        if (appProcesses == null) return false; 
	        for (RunningAppProcessInfo appProcess : appProcesses) { 
	            if (appProcess.processName.equals(getPackageName())){
	            	    Log.e("StopListennerActivity", "appProcess.importance="+appProcess.importance);
	            	   // inRunningTasks(aManager);
	            	    if(appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) { 
	            			return true; 
	            		}else{
	            			return false;
	            		}
	            } 
	        } 
	        return false; 
	 }


	@Override
	protected void onResume() {
		try {
			StatService.onResume(this);
		}catch (Exception e){
			e.printStackTrace();
		}
		if(AppConfig.Granted) {
			Intent intent = new Intent(this, AssistantService.class);
			intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PLAY_IN_BACKGROUND);
			startService(intent);
		}
		super.onResume();
	}
		
	@Override
	protected void onPause() {
		try {
			StatService.onPause(this);
		}catch (Exception e){
			e.printStackTrace();
		}
		super.onPause();
	}
	 
	  // 运行中的任务
	/* private boolean inRunningTasks(ActivityManager mAm) {
	        List<RunningTaskInfo> taskList = mAm.getRunningTasks(1);
	        for (RunningTaskInfo rti : taskList) {
	            Log.e("showRunningTasks", "Running task, numActivities="
	                    + rti.numActivities);
	            Log.e("showRunningTasks", ", description=" + rti.description);
	            Log.e("showRunningTasks",
	                    ", baseActivity=" + rti.baseActivity.getClassName());
	            Log.e("showRunningTasks",
	                    ", topActivity=" + rti.topActivity.getClassName());
	           
	        }
	        return false;
	    }*/
}
