package com.lingju.audio.engine.base;

import android.content.Context;
import android.util.Log;

import com.lingju.audio.SpeechPlayer;
import com.lingju.audio.SystemVoiceMediator;

import java.util.Observable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class RecognizerBase extends Observable {
	private final static String TAG="RecognizerBase";
	public final static int BAD_NETWORK_THRESHOLD=1500;
	public static boolean isLocalEngine=false;
	
	protected Context mContext;
	protected SystemVoiceMediator mediator;
	protected SpeechPlayer mPlayer;
	protected boolean isRecognizing;
	protected Lock rLock=new ReentrantLock();
	protected boolean cancel=false;
	protected boolean playingBeforRecognize=false;
	protected boolean tingBeforRecognize=false;
	protected long lastRecognized=0l;
	 
	protected int networkStatus=0;
	protected long recognize_end_moments=0;
	protected long recognize_start_moments=0;		 
	protected int errorCount=0;
	
	public boolean isLocalEngine(){
		return isLocalEngine;
	}
	public abstract void startRecognize();
	public abstract void startRecognize(boolean audio);
	public abstract boolean isListening();
	public abstract boolean isRecognizing();
	public abstract long getRecognize_start_moments();
	public abstract long getRecognize_end_moments();
	public abstract boolean isPlayingBeforRecognize();
	public abstract boolean isTingBeforeRecognize();
	public abstract void stopRecognize();
	public abstract void reset();
	public abstract void destory();
	public abstract void resetParam();
	public abstract void setParams(String params,String value);
	public abstract void updateLexicon(String userwords);
	public abstract long getLastRecognized();
	public abstract void setLastRecognized(long lastRecognized);
	public abstract int getNetworkStatus();
	public abstract void setNetworkStatus(int networkStatus);
	public abstract int getErrorCount();
	public void onSpeechCompleted(){
		recognize_end_moments= System.currentTimeMillis();
		mediator.onRecoginzeWait();
	}
	
	protected void onError(int code,String description){
		recognize_end_moments= System.currentTimeMillis();
		errorCount++;
		Log.e(TAG, "onError Code：" + description + ":" + code + ",cancel=" + Boolean.toString(cancel));
		/*TODO 移到mediator
		if(mContext.mobileRing()||cancel*//*||cacelCallback*//*){
			cancel=false;
			return;
		}
		if(!mContext.preToCall()){
			mContext.onRecognizeError(code);
		}*/
		mediator.onRecognizeError(code,description);
	}

	protected void onResult(String result){
		recognize_end_moments= System.currentTimeMillis();
		Log.i(TAG + ".onResult", "result:" + result);
		errorCount=0;
		/*TODO 移到mediator
		if(mContext.mobileRing()||cancel*//*||cacelCallback*//*){
			//cacelCallback=false;
			cancel=false;
			return;
		}
	    networkStatus=(int) (System.currentTimeMillis()-recognize_end_moments);

	    //判断是否在线状态下设置了离线合成引擎，如果网络比较理解，把合成引擎设置为在线
	   *//* if(!LingjuSynthesizer.getInstance().forceLocalEngine&&LingjuSynthesizer.getInstance().getSynthesizer().getParameter(SpeechConstant.ENGINE_TYPE)==SpeechConstant.TYPE_LOCAL&&LingjuGlobal.online){
    		if(networkStatus<BAD_NETWORK_THRESHOLD){
    			LingjuSynthesizer.getInstance().resetParam(NetType.NETWORK_TYPE_WIFI);
    		}
    	}*//*
	    mContext.onRecoginzeResult(result);*/
		mediator.onRecoginzeResult(result);
    	cancel=false;
	}

	/** 长时间录音结果回调 **/
	protected void onLondRecordResult(String result){
		recognize_end_moments= System.currentTimeMillis();
		// .i(TAG + ".onResult", "result:" + result);
		errorCount=0;
		mediator.onLongRecoginzeResult(result);
	}

}
