package com.lingju.assistant.download.abstracts;

/**
 * 下载回调
 * @author ydshu
 *
 */
public interface IDownloadListener {

	/**
	 * 下载�?��
	 */
	public void onStart();
	/**
	 * 下载进度
	 * @param percent
	 * 		进度（百分比制）
	 */
	public void onProgress(int percent);
	/**
	 * 下载完成
	 */
	public void onCompleted(int length, String filePath[]);
	
	public void onError(int code);
}
