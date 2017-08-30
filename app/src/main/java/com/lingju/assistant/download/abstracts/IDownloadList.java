package com.lingju.assistant.download.abstracts;

/**
 * 下载列表接口
 * @author ydshu
 *
 */
public interface IDownloadList {

	/**
	 * 添加下载任务
	 * @param dlTask
	 * 		下载任务
	 * @param tag
	 * 		标签ID，唯?识一个下载任?
	 */
	public void addDlTask(IDownloadTask dlTask, String tag);
	
	/**
	 * 获取?下载任务句柄
	 * @param tag
	 * 		任务ID
	 * @return
	 */
	public IDownloadTask getDlTask(String tag);
	
	/**
	 * 从下载队列移除一个下载任?
	 * @param tag
	 * 		id
	 */
	public void removeDlTask(String tag);
	
	/**
	 * 清空下载队列
	 */
	public void clear();
}
