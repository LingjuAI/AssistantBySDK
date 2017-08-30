package com.lingju.assistant.download.impl;

import android.content.Context;
import android.util.Log;

import com.lingju.assistant.download.abstracts.IDownloadListener;
import com.lingju.assistant.download.abstracts.IDownloadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件下载器
 */
public class DownloadTask implements IDownloadTask {
	private static final String TAG = "FileDownloader";
	private Context context;
	private FileService fileService;	
	private IDownloadListener mDlListener;
	private boolean pause=false;
	
	/* 已下载文件长度 */
	private int downloadSize = 0;
	/* 原始文件长度 */
	private int fileSize = 0;
	/* 线程池 */
	private ThreadPoolExecutor threadPool;
	private int threadnum;
	/* 本地保存文件 */
	private File saveFile;
	/* 缓存各线程下载的长度*/
	private Map<Integer, Integer> data = new ConcurrentHashMap<Integer, Integer>();
	/* 每条线程下载的长度 */
	private int block;
	/* 下载路径  */
	private String downloadUrl;
	private Map<String,String> downloadUrls;
	private Map<String,HttpURLConnection> mul_conn;
	private boolean singleFile=true;
	private String dir;
	/**
	 * 获取线程数
	 */
	public int getThreadSize() {
		return threadPool.getCorePoolSize();
	}
	/**
	 * 获取文件大小
	 * @return
	 */
	public int getFileSize() {
		return fileSize;
	}
	
	/**
	 * 累计已下载大小
	 * @param size
	 */
	protected synchronized void append(int size) {
		downloadSize += size;
		//this.mDlListener.onProgress();
	}
	/**
	 * 更新指定线程最后下载的位置
	 * @param threadId 线程id
	 * @param pos 最后下载的位置
	 */
	protected void update(int threadId, int pos) {
		this.data.put(threadId, pos);
	}
	/**
	 * 保存记录文件
	 */
	protected synchronized void saveLogFile() {
		this.fileService.update(this.downloadUrl, this.data);
	}
	/**
	 * 构建文件下载器,适用于下载单个大文件
	 * @param downloadUrl 下载路径
	 * @param fileSaveDir 文件保存目录
	 * @param threadNum 下载线程数
	 */
	public DownloadTask(Context context, String downloadUrl, File fileSaveDir, int threadNum) {
		try {
			System.out.println("DownloadTask>>>"+downloadUrl);
			this.context = context;
			this.downloadUrl = downloadUrl;
			fileService = FileService.getInstance();
			URL url = new URL(this.downloadUrl);
			if(!fileSaveDir.exists()) fileSaveDir.mkdirs();
			this.threadnum=threadNum;
			threadPool=new ThreadPoolExecutor(threadnum+1,threadnum+1, 20,TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(),new ThreadPoolExecutor.CallerRunsPolicy());
			HttpURLConnection conn = getConnectionAndConnect(url,3);
			this.fileSize = conn.getContentLength();//根据响应获取文件大小
			if (this.fileSize <= 0) throw new RuntimeException("Unkown file size ");

			String filename = getFileName(conn);
			this.saveFile = new File(fileSaveDir, filename);/* 保存文件 */
			Map<Integer, Integer> logdata = fileService.getData(downloadUrl);
			if(logdata.size()>0){
				for(Map.Entry<Integer, Integer> entry : logdata.entrySet())
					data.put(entry.getKey(), entry.getValue());
			}
			this.block = (this.fileSize % threadnum)==0? this.fileSize /threadnum : this.fileSize / threadnum + 1;
			if(this.data.size()==threadnum){
				for (int i = 0; i < threadnum; i++) {
					this.downloadSize += this.data.get(i);
				}
				Log.i(TAG, "已经下载的长度" + this.downloadSize);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("don't connection this url");
		}
	}
	
	public DownloadTask(Context context, Map<String,String> paths,String fileDir) {
		this.context=context;
		this.downloadUrls=paths;
		this.threadnum=paths.size()+1;
		this.singleFile=false;
		this.dir=fileDir;
		mul_conn=new ConcurrentHashMap<String, HttpURLConnection>();
		this.threadPool=new ThreadPoolExecutor(threadnum,threadnum, 20,TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(),new ThreadPoolExecutor.CallerRunsPolicy());
		File tempDir;
		try {
			for(String key:downloadUrls.keySet()){
					URL url = new URL(downloadUrls.get(key));
					tempDir=new File(dir);
					System.out.println("DownloadTask>>>downloadURL="+downloadUrls.get(key));
					if(!tempDir.exists()) {
						Log.e(TAG, "mkdirs:"+tempDir.getAbsolutePath());
						tempDir.mkdirs();
					}
					HttpURLConnection conn = getConnectionAndConnect(url,3);
					int length= conn.getContentLength();//根据响应获取文件大小
					if (length <= 0) throw new RuntimeException("Unkown file size ");
					this.fileSize +=length;
					mul_conn.put(dir+"/"+key, conn);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("don't connection this url");
		}
	}

	private HttpURLConnection getConnectionAndConnect(URL url,int retry) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(10*1000);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
		conn.setRequestProperty("Accept-Language", "zh-CN");
		conn.setRequestProperty("Charset", "UTF-8");
		conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.connect();
		//printResponseHeader(conn);
		if (conn.getResponseCode()==200) {
			return conn;
		}else{
			conn.disconnect();
			if(--retry>=0){
				return getConnectionAndConnect(url,retry);
			}
			throw new RuntimeException("server no response ");
		}
	}
	
	private String getFileDir(String file){
		int sl=file.lastIndexOf("/");
		if(sl>0){
			return file.substring(0,sl);
		}
		return "";
	}
	/**
	 * 获取文件名
	 */
	private String getFileName(HttpURLConnection conn) {
		String filename = this.downloadUrl.substring(this.downloadUrl.lastIndexOf('/') + 1);
		if(filename==null || "".equals(filename.trim())){//如果获取不到文件名称
			for (int i = 0;; i++) {
				String mine = conn.getHeaderField(i);
				if (mine == null) break;
				if("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase())){
					Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());
					if(m.find()) return m.group(1);
				}
			}
			filename = UUID.randomUUID()+ ".tmp";//默认取一个文件名
		}
		if(filename.indexOf("?")!=-1){
			filename=filename.substring(0,filename.lastIndexOf("?"));
		}
		return filename;
	}
	
	public void download(){
		download(null);
	}
	
	public void download(IDownloadListener mdownloadListener){
		if(singleFile)
		downloadSingleFile(mdownloadListener);
		else{
			downloadMultipleFile(mdownloadListener);
		}
	}
	
	public void downloadMultipleFile(IDownloadListener mdownloadListener) {
		pause=false;
		this.mDlListener=mdownloadListener;
		for(String key:mul_conn.keySet()){
			HttpURLConnection conn=mul_conn.get(key);
			threadPool.execute(new SingleDownloader(conn, key));
		}
		setListenerThread();
	}
	
	private void setListenerThread(){
		threadPool.execute(new Runnable() {
			
			@Override
			public void run() {
				while(!pause){
					if(threadPool.getActiveCount()==1){
						if(downloadSize==fileSize){
							threadPool.shutdown();
							if(singleFile) {
								fileService.delete(downloadUrl);
								mDlListener.onCompleted(downloadSize,new String[]{saveFile.getAbsolutePath()});
							}
							else{
								mDlListener.onCompleted(downloadSize,mul_conn.keySet().toArray(new String[mul_conn.size()]));
							}
						}
						else{
							mDlListener.onError(0);
						}
						break;
					}
					else{
						mDlListener.onProgress((int)(((long)downloadSize*100)/fileSize));
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if(singleFile)
				saveLogFile();
			}
		});
	}
	/**
	 *  开始下载文件
	 * @throws Exception
	 */
	public void downloadSingleFile(IDownloadListener mdownloadListener) {
		if(mdownloadListener!=null){
			this.mDlListener=mdownloadListener;
		}
		try {
			pause=false;
			RandomAccessFile randOut = new RandomAccessFile(this.saveFile, "rw");
			if(this.fileSize>0) randOut.setLength(this.fileSize);
			randOut.close();
			if(this.data.size() != threadnum){
				this.data.clear();
				for (int i = 0; i < threadnum; i++) {
					this.data.put(i, 0);
				}
			}
			URL url = new URL(this.downloadUrl/*+(this.downloadUrl.indexOf("?")>-1?"&":"?")+"r="+System.currentTimeMillis()*/);
			for (int i = 0; i < threadnum; i++) {
				int downLength = this.data.get(i);
				Log.i(TAG, "线程" + i + "前一次下载：" + downLength + "--block:" + block);
				if(downLength < this.block && this.downloadSize<this.fileSize){
					threadPool.execute(new Downloader(this.block, url, this.saveFile,  downLength, i));
				}
			}
			this.fileService.save(this.downloadUrl, this.data);
			setListenerThread();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 获取Http响应头字段
	 * @param http
	 * @return
	 */
	public static Map<String, String> getHttpResponseHeader(HttpURLConnection http) {
		Map<String, String> header = new LinkedHashMap<String, String>();
		for (int i = 0;; i++) {
			String mine = http.getHeaderField(i);
			if (mine == null) break;
			header.put(http.getHeaderFieldKey(i), mine);
		}
		return header;
	}
	/**
	 * 打印Http头字段
	 * @param http
	 */
	public static void printResponseHeader(HttpURLConnection http){
		Map<String, String> header = getHttpResponseHeader(http);
		for(Map.Entry<String, String> entry : header.entrySet()){
			String key = entry.getKey()!=null ? entry.getKey()+ ":" : "";
			Log.i(TAG, key + entry.getValue());
		}
	}


	class SingleDownloader implements Runnable{
		HttpURLConnection connect;
		String filePath;
		public SingleDownloader(HttpURLConnection conn,String paths){
			this.connect=conn;
			this.filePath=paths;
		}
		
		@Override
		public void run() {
			if(pause)return;
			try{
				FileOutputStream f=new FileOutputStream(new File(filePath));
				InputStream inputStream = connect.getInputStream();  
	            byte[] buffer = new byte[1024];  
	            int len = 0;  
	            while( (len = inputStream.read(buffer)) != -1){
	            	f.write(buffer, 0, len);  
	                append(len);
	                if(pause){
	                	break;
	                }
	            }  
	            f.close();  
	            inputStream.close(); 
			}catch(Exception e){
				e.printStackTrace();
			}
            connect.disconnect();
		}
		
	}
	
	class Downloader implements Runnable{
		private int block;//每条线程下载的数据长度  
        private URL url;//下载路径  
        private File file;//本地文件  
        private int threaid;//线程id  
        private int beginLenght;
          
        public Downloader(int block, URL url, File file, int beginLength,int i) {  
            this.block = block;  
            this.url = url;  
            this.file = file;  
            this.threaid = i;  
            this.beginLenght=beginLength;
            Thread.currentThread().setName("downloader_thread"+i);
        } 
        
		@Override
		public void run() {
			try {  
				download();
            } catch (Exception e) {  
                e.printStackTrace();
            }  
		}
		
		private void download() throws Exception{
			int startpos = threaid * block+beginLenght;//计算该线程从文件的什么位置开始下载  
            int endpos = (threaid+1) * block - 1;//计算该线程下载到文件的什么位置结束
			System.out.println("threaid="+threaid+",startpos="+startpos+",endpos="+endpos);
            endpos=endpos>=fileSize?fileSize-1:endpos;
			System.out.println("threaid="+threaid+",startpos="+startpos+",endpos="+endpos);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(10*1000);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
			conn.setRequestProperty("Accept-Language", "zh-CN");
			conn.setRequestProperty("Charset", "UTF-8");
			conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Range", "bytes="+ startpos+"-"+ endpos);
            InputStream inputStream = conn.getInputStream();

			System.out.println("threaid="+threaid+",startpos="+startpos+",endpos="+endpos+",contentLength="+conn.getContentLength());
            RandomAccessFile rfile = new RandomAccessFile(file, "rwd");  
            rfile.seek(startpos);  
            byte[] buffer = new byte[1024];  
            int len = 0;  
            while( (len = inputStream.read(buffer)) != -1){
                rfile.write(buffer, 0, len);  
                beginLenght+=len;
				update(threaid, beginLenght);
                append(len);
                if(pause){
                	System.out.println("break "+threaid);
                	break;
                }
            }  
            rfile.close();  
            inputStream.close();  
            conn.disconnect();
		}
	}
	
	@Override
	public void prepare(String url) {
		
	}
	
	@Override
	public void start() {
		if(threadPool.getActiveCount()>1&&!pause)return;
		try {
			while(pause&&threadPool.getActiveCount()>0){
				Thread.sleep(300);
			}
			download();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void pause() {
		this.pause=true;
	}
	
	@Override
	public void stop() {
		this.pause=true;
		threadPool.shutdown();
	}
	
	@Override
	public void setDownloadListener(IDownloadListener listener) {
		this.mDlListener=listener;
	}

}
