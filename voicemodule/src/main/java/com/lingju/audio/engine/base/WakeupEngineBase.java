package com.lingju.audio.engine.base;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.lingju.audio.SystemVoiceMediator;
import com.lingju.common.log.Log;

import java.util.Observable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;


public abstract class WakeupEngineBase extends Observable {
	private final static String TAG="WakeupEngineBase";
	public interface State{
		public final static int IDLE=0;
		public final static int START_RECOGNIZE=1;
		public final static int WORKING=2;
		public final static int G_ERROR=3;
		public final static int G_RESULT=4;
	}

	public static final int sampleRate=16000;
	public static final int wBufferSize=2* AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
	
	protected final static ThreadPoolExecutor threadPool=new ThreadPoolExecutor(5, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	protected Context mContext;
	protected SystemVoiceMediator mediator;
	protected AtomicBoolean stopInterrupt=new AtomicBoolean(false);
	protected AudioTrack at;
	protected AtomicInteger state=new AtomicInteger(State.IDLE);
	protected AtomicBoolean recognizing=new AtomicBoolean(true);

	public static interface WakeuperListener{
		public void onResult(String result);
		public void onError(int error);
		public void onBeginOfSpeech();
	}

	public abstract boolean isListening();
	public abstract void destory();
	protected abstract void start();
	public abstract void stopCompletely();
	
	public void stopRecord(){
		this.state.set(State.IDLE);
	}
	
	public boolean isRecording(){
		//return ar.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING;
		return false;
	}
	
	/**
	 * 打断（停止）唤醒，调用该方法后马上停止录音，但不保证完全打断唤醒，完全打断可能会有延迟
	 */
	public void stopListening(){
		Log.i(TAG, "stopListening");
		if(isListening())
		try{
			stopRecord();
			if(at.getPlayState()!= AudioTrack.PLAYSTATE_STOPPED){
				stopInterrupt.set(true);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void startListening(){
		Log.i(TAG, "startListening>>>");
		if(!mediator.isWakeUpMode()||(mediator.isPlaying()&&!mediator.isHeadset())){
			Log.e(TAG, "startListening>>>is not in wakeup mode!!!!");
			stopRecord();
			return;
		}
		//mediator.stopSynthesize();
		mediator.stopRecognize();
		mediator.stopBluetoothSco();
		/*if(mediator.isBlueToothHeadSet()){
			AudioManager mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
			if(mAudioManager.isBluetoothScoOn()){
		    	mAudioManager.setBluetoothScoOn(false);
		    	mAudioManager.stopBluetoothSco();
			}
		}*/
		if(!isListening()){
			Log.e(TAG, "start record....................");
			start();
		}
	}
	
	protected void onResult(){
		Log.e(TAG, "WakeuperListener.onResult");
		if(stopInterrupt.get()){
			stopInterrupt.set(false);
			return;
		}
		mediator.stopSynthesize();
		mediator.openBlueHeadSet4Recognition();
		//AudioManager mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		//int v=mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		/*switch(mAudioManager.getMode()){
		case AudioManager.MODE_NORMAL:
			Log.e(TAG, "mode:MODE_NORMAL");
			break;
		case AudioManager.MODE_RINGTONE:
			Log.e(TAG, "mode:MODE_RINGTONE");
			break;
		case AudioManager.MODE_IN_CALL:
			Log.e(TAG, "mode:MODE_IN_CALL");
			break;
		}
		if (!mediator.isHeadset())
			mediator.changeMediaVolume(90);*/
		//mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0.9), 0);

		SynthesizerBase.get().startSpeakAbsolute("在呢")
				.doOnComplete(new Action() {
					@Override
					public void run() throws Exception {
						Log.e(TAG, "WakeuperListener.onResult.speaked......");
						//mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
						// mediator.resumeMediaVolume();

						if(stopInterrupt.get()){
							stopInterrupt.set(false);
							return;
						}
						mediator.onWakenup(null);

					}
				})
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation())
				.subscribe();
//		if(mediator.isBlueToothHeadSet()){
//			if(at==null||at.getStreamType()!= AudioManager.STREAM_VOICE_CALL){
//				if(at!=null)at.release();
//				at=new AudioTrack(AudioManager.STREAM_VOICE_CALL,sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, wBufferSize, AudioTrack.MODE_STREAM);
//			}
//		}
//		else{
//			if(at==null||at.getStreamType()!= AudioManager.STREAM_MUSIC){
//				if(at!=null)at.release();
//				at=new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, wBufferSize, AudioTrack.MODE_STREAM);
//			}
//		}
//		at.setStereoVolume(1.0f, 1.0f);
//		at.play();
//		stopInterrupt.set(false);
//		InputStream in=null;
//		try{
//			in=mContext.getAssets().open("audio/awaken.pcm");
//			byte[] buffer=new byte[wBufferSize];
//			int l=0;
//			while(!stopInterrupt.get()&&(l=in.read(buffer))!=-1){
//				at.write(buffer, 0, l);
//			}
//		}
//		catch(Exception e){
//			e.printStackTrace();
//		}
//		finally{
//			at.stop();
//			if(in!=null){
//				try {
//					in.close();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}
//		Log.e(TAG, "WakeuperListener.onResult.speaked......");
//		//mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
//		mediator.resumeMediaVolume();
//
//		if(stopInterrupt.get()){
//			stopInterrupt.set(false);
//			return;
//		}
//		mediator.onWakenup(null);

	}
}
