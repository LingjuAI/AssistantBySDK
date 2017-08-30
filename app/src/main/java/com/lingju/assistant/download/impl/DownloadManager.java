package com.lingju.assistant.download.impl;

import android.content.Context;
import android.util.Log;

import com.lingju.assistant.download.abstracts.IDownloadList;
import com.lingju.assistant.download.abstracts.IDownloadTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 下载管理
 * @author ydshu
 *
 */
public class DownloadManager implements IDownloadList {
	private final static String TAG = "DownloadManager";
	private static DownloadManager mInstance;
	private Context mContext;
	private Object mLocker = new Object();
	

	/** 下载列表 */
	private HashMap<String, IDownloadTask> mDownloadList = new HashMap<>();

	/**
	 * 私有构�?
	 * @param context
	 */
	private DownloadManager(Context context){
		mContext = context;
	}

	/**
	 * 创建静�?实例
	 * @param context
	 * @return
	 */
	public static DownloadManager createInstance(Context context){
		mInstance = new DownloadManager(context);
		return mInstance;
	}

	/**
	 * 获取静�?实例
	 * @return
	 */
	public static DownloadManager getInstance(){
		if (mInstance == null){
			Log.e(TAG, "DownloadManager getInstance null.");
		}
		return mInstance;
	}

	@Override
	public void addDlTask(IDownloadTask dlTask, String tag) {
		synchronized(mLocker){
			Log.d(TAG, "addDlTask tag=" + tag);
			if (dlTask != null && tag != null && mDownloadList != null){
				mDownloadList.put(tag, dlTask);
			} else {
				Log.e(TAG, "addDlTask unexpected arguments tag=" + tag);
			}
		}		
	}

	@Override
	public IDownloadTask getDlTask(String tag) {
		synchronized(mLocker){
			Log.d(TAG, "getDlTask tag=" + tag);
			IDownloadTask task = null;
			if (tag != null && mDownloadList != null)
				task = mDownloadList.get(tag);
			return task;
		}
	}

	@Override
	public void removeDlTask(String tag) {
		synchronized(mLocker){
			Log.d(TAG, "removeDlTask tag=" + tag);
			if (tag != null && mDownloadList != null){
				IDownloadTask task = mDownloadList.get(tag);
				if (task != null){
					task.stop();
				}
				mDownloadList.remove(tag);
			}
		}
	}

	@Override
	public void clear() {
		synchronized(mLocker){
			Log.d(TAG, "clear");
			if (mDownloadList != null)
			{
				Iterator iter = mDownloadList.entrySet().iterator();  
				while (iter.hasNext()) {  
					Map.Entry entry = (Map.Entry) iter.next();  
					String tag = (String) entry.getKey();  
					IDownloadTask task = (IDownloadTask) entry.getValue(); 
					Log.d(TAG, "clear tag=" + tag);
					if (task != null){
						task.stop();
					}
					mDownloadList.remove(tag);
				}  
			}
		}
	}
}
