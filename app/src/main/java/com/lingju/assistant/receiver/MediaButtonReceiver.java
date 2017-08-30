package com.lingju.assistant.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.view.KeyEvent;

import com.lingju.assistant.service.AssistantService;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.log.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MediaButtonReceiver extends BroadcastReceiver {
	private final static String TAG="MediaButtonReceiver";
	/*
     * It should be safe to use static variables here once registered via the
     * AudioManager
     */
    private static long mHeadsetDownTime = 0;
    private static long mHeadsetUpTime = 0;
	private static Lock eventLock=new ReentrantLock();
    private static Condition eventCondition=eventLock.newCondition();
	private static AtomicInteger clicks=new AtomicInteger(0);
	/**
	 * 每次按键的超时时间（单位毫秒），超过该时间则认定为某种按键，建议300毫秒以上，太短灵敏度降低，太长反应太慢
	 */
    private static int timeout=450;
	
	public MediaButtonReceiver() {
	}
	
	private Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {
		//获取对应Acton，判断是否是需要的ACTION_MEDIA_BUTTON
		/*if(VoiceService.get().getAwakener().isWorking()){
			return;
		}*/
		this.mContext=context;
        String action = intent.getAction();
        Log.e(TAG, "onReceive:"+action+",context="+context);
        if(action.equalsIgnoreCase("android.media.AUDIO_BECOMING_NOISY")){
        	Intent it=new Intent(this.mContext,AssistantService.class);
        	it.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PAUSE_PLAY);
    		this.mContext.startService(it);
        }
        else if (action.equalsIgnoreCase(Intent.ACTION_MEDIA_BUTTON)){
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null)
                return;
            if (event.getKeyCode() != KeyEvent.KEYCODE_HEADSETHOOK
                    && event.getKeyCode() != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    && event.getAction() != KeyEvent.ACTION_DOWN)
                return;
            if(!eventLock.tryLock())return;
            try{
	            switch (event.getKeyCode()){
	            case KeyEvent.KEYCODE_HEADSETHOOK:
	            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
	            	//Log.e(TAG, "KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE");
	                long time = SystemClock.uptimeMillis();
	                switch (event.getAction()){
	                case KeyEvent.ACTION_DOWN:
	                	//Log.e(TAG, "KeyEvent.ACTION_DOWN，count="+event.getRepeatCount());
	                    if (event.getRepeatCount() > 0)
	                        break;
	                    mHeadsetDownTime = time;
	                    break;
	                case KeyEvent.ACTION_UP:
	                	//Log.e(TAG, "KeyEvent.ACTION_UP，count="+event.getRepeatCount());
	                	long t=(time-mHeadsetUpTime);
	                    mHeadsetUpTime = time;
	                	if (time - mHeadsetDownTime >= 1000){
	                    	longClick();
							clicks.set(0);
	                        break;
	                    }
	                	else if(clicks.get()!=0&&t<timeout){
	                		clicks.incrementAndGet();
	                		eventCondition.signal();
	                	}
	                	else if(clicks.get()==0){
	                		clicks.incrementAndGet();
	                		new EventAsynTask().execute();
	                	}
	                    break;
	                }
	                break;
	           case KeyEvent.KEYCODE_MEDIA_PLAY:
	        	   Log.e(TAG, "KeyEvent.KEYCODE_MEDIA_PLAY");
	                click();
	                break;
	            case KeyEvent.KEYCODE_MEDIA_PAUSE:
	            	Log.e(TAG, "KeyEvent.KEYCODE_MEDIA_PAUSE");
	                click();
	                break;
	            case KeyEvent.KEYCODE_MEDIA_STOP:
	            	Log.e(TAG, "KeyEvent.KEYCODE_MEDIA_STOP");
	            	click();
	                break;
	            case KeyEvent.KEYCODE_MEDIA_NEXT:
	            	Log.e(TAG, "KeyEvent.KEYCODE_MEDIA_NEXT");
	            	doubleClick();
	                break;
	            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
	            	Log.e(TAG, "KeyEvent.KEYCODE_MEDIA_PREVIOUS");
	                trebleClick();
	                break;
	            }
	
	            if (isOrderedBroadcast()){
	                Log.e(TAG, "isOrderedBroadcast");
	            	abortBroadcast();
	            } 
            }finally{
            	eventLock.unlock();
            }
        }
        
	}
	/**
	 * android4.0以后有线耳机的长按已被谷歌的声音搜索占用，Media_Button广播无法接收
	 */
	private void longClick(){
		Log.e(TAG, "longClick");
	}

	private void click(){
		Log.e(TAG, "click");
		if(!SynthesizerBase.isInited())return;
		Intent intent=new Intent(this.mContext,AssistantService.class);
		intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.HEADSET_RECOGNIZE);
		this.mContext.startService(intent);
	}
	
	private void doubleClick(){
		Log.e(TAG, "doubleClick");
		if(SynthesizerBase.isInited()){
			SynthesizerBase.get().stopSpeakingAbsolte();
		}
		Intent intent=new Intent(this.mContext,AssistantService.class);
		intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.TOGGLE_PLAY);
		this.mContext.startService(intent);
	}
	
	private void trebleClick(){
		Log.e(TAG, "trebleClick");
		Intent intent=new Intent(this.mContext,AssistantService.class);
		intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.NEXT_MUSIC);
		this.mContext.startService(intent);
	}
	
	class EventAsynTask extends AsyncTask<Void, Void, Integer>{

		@Override
		protected Integer doInBackground(Void... params) {
			int result=0;
			eventLock.lock();
			try{
				eventCondition.await(timeout, TimeUnit.MILLISECONDS);
				if(clicks.get()==1){
					result=1;
				}
				else if(clicks.get()==2){
					eventCondition.await(timeout, TimeUnit.MILLISECONDS);
					if(clicks.get()==2){
						result=2;
					}
					else{
						result=3;
					}
				}
				else if(clicks.get()==3){
					result=3;
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
			finally{
				clicks.set(0);
				eventLock.unlock();
			}
			return result;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			switch(result){
			case 1:click();break;
			case 2:doubleClick();break;
			case 3:trebleClick();break;
			default:break;
			}
		}
	}
}
