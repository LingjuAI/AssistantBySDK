package com.lingju.assistant.download.abstracts;


/**
 * 下载任务接口
 * @author ydshu
 *
 */
public interface IDownloadTask {

	/**
	 * 下载准备
	 * @param url
	 * 		资源URL
	 */
	public void prepare(String url);
	
	/**
	 * ?下载
	 */
	public void start();
	/**
	 * 暂停下载
	 */
	public void pause();
	/**
	 * 取消下载
	 */
	public void stop();
	
	/**
	 * 设置下载回调
	 * @param listener
	 */
	public void setDownloadListener(IDownloadListener listener);
}
