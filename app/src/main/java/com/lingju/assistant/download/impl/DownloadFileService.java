package com.lingju.assistant.download.impl;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.BaiduResourceEvent;
import com.lingju.assistant.activity.event.VersionUpdateEvent;
import com.lingju.assistant.download.abstracts.IDownloadListener;
import com.lingju.config.Setting;
import com.lingju.model.Version;
import com.lingju.util.ApkInstaller;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件下载的service
 * @author 
 *
 */
public class DownloadFileService extends Service {
        private NotificationManager notificationManager;//状态栏通知管理类
        private Notification notification;//状态栏通知
        private RemoteViews remoteViews;//状态栏通知显示的view
        private int notificationID=30;//通知的id
        private final int updateProgress = 1;//更新状态栏的下载进度
        private final int downloadSuccess = 2;//下载成功
        private final int downloadError = 3;//下载失败
        private final String TAG = "DownloadFileService";
        private Version version;
        DownloadTask downloadTask;
        private String aimlPath;

	    private final Map<String,String>  dFiles=new Hashtable<String, String>();

		public final static String BAIDU_RS_URL="http://bos.nj.bpc.baidu.com/v1/audio/s_2_InputMethod";

		public final static String CMD="cmd";
		public final static int DOWNLOAD_BAIDU_VOICE_RESOURCE=1;

        @Override
        public IBinder onBind(Intent intent) {
                
                return null;
        }

        @Override
        public void onCreate() {
                init();
        }
        
        private void init(){
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                remoteViews = new RemoteViews(getPackageName(), R.layout.down_notification);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
                mBuilder.setAutoCancel(false);
                mBuilder.setSmallIcon(R.drawable.ic_launcher);
        		mBuilder.setContent(remoteViews);
        	    mBuilder.setLights(Color.BLUE, 500, 500);  
        	    mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);               
        		notification = mBuilder.build();
        }

		private void setDfiles(String urls[]){
			Pattern pt=Pattern.compile("\\/(AiBase\\.jar|py\\.dat|dls\\.dat)\\?{0,1}");
			Matcher mt;
			for(String url:urls){
				mt=pt.matcher(url);
				System.out.println("url>>>>>>>"+url);
				if(mt.find()) {
					System.out.println(mt.group(1));
					dFiles.put(mt.group(1),url);
				}
			}
		}


        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
			Log.i(TAG,"intent>>"+intent);
			int cmd=intent!=null?intent.getIntExtra(CMD,0):-1;
			if(cmd==0) {
				version = ((AppConfig) getApplication()).newVersion;
				Log.i(TAG, "version:" + version.toString());
				String fileDir = Setting.getExtSdcardPath();
				if (fileDir != null) {
					notificationManager.notify(notificationID, notification);
					final String path = fileDir + Setting.STORAGE_PATH + "/temp";
					new Thread(new Runnable() {
						@Override
						public void run() {
							if (!version.isUpdate()) return;
							try {
								//version.setApk_path("http://dd.myapp.com/16891/E6EBA1C45C01AFF862764F2BBE35DD2C.apk?fsname=com.lingju.assistant_3.0.0_3.apk");
								if (version.isUpdateApk()) {
									downloadTask = new DownloadTask(getApplicationContext(), version.getApk_path(), new File(path), 3);
									downloadTask.setDownloadListener(idListener);
									downloadTask.start();
								} else {
									Log.e(TAG, "getFilesDir>>>>>>" + path);
									setDfiles(version.getDbs());
									downloadTask = new DownloadTask(getApplicationContext(), dFiles, path);
									downloadTask.downloadMultipleFile(idListener);
								}
							}catch (Exception e){
								e.printStackTrace();
								idListener.onError(1);
							}
						}
					}).start();
				}
			}
			else if(cmd==DOWNLOAD_BAIDU_VOICE_RESOURCE){
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							//version.setApk_path("http://dd.myapp.com/16891/E6EBA1C45C01AFF862764F2BBE35DD2C.apk?fsname=com.lingju.assistant_3.0.0_3.apk");
							downloadTask = new DownloadTask(getApplicationContext(), BAIDU_RS_URL, new File(Setting.DEFAULT_DIR), 3);
							downloadTask.setDownloadListener(idListener);
							downloadTask.start();
						}catch (Exception e){
							e.printStackTrace();
							idListener.onError(1);
						}
					}
				}).start();
			}
            return super.onStartCommand(intent, Service.START_REDELIVER_INTENT, startId);
        }
        
        
        
        @Override
        public void onDestroy() {
                Log.i(TAG, TAG+" is onDestory...");
                super.onDestroy();
        }
        
        private IDownloadListener idListener=new IDownloadListener() {
			
			@Override
			public void onStart() {
				
			}
			
			@Override
			public void onProgress(int percent) {
				Message msg=new Message();
				msg.what=updateProgress;
				msg.obj=percent;
				handler.sendMessage(msg);
			}
			
			@Override
			public void onCompleted(int length,String filePaths[]) {
				Message msg=new Message();
				msg.what=downloadSuccess;
				msg.obj=filePaths;
				handler.sendMessage(msg);
			}

			@Override
			public void onError(int code) {
				Message msg=new Message();
				msg.what=downloadError;
				msg.obj=code;
				handler.sendMessage(msg);
			}
		};

		private boolean resetAiBaseResource(String paths[]){
			String fn;
			try {
				for (String p : paths) {
					copyFile(new File(p));
				}
				return true;
			}catch (IOException e){
				e.printStackTrace();
				Log.e(TAG,"复制文件出错:"+e.getMessage());
				idListener.onError(11);
			}
			return false;
		}


		private void copyFile(File file) throws IOException {
			String fileName=file.getName();
			FileOutputStream out=new FileOutputStream(new File(getDir(Setting.NEW_VERSION_DIR,Context.MODE_PRIVATE),fileName));
			FileInputStream in=new FileInputStream(file);
			byte buffer[]=new byte[1024];
			int l=0;
			while((l=in.read(buffer))!=-1){
				out.write(buffer,0,l);
			}
			out.close();
			in.close();
		}
		

        Handler handler = new Handler(){

                @Override
                public void handleMessage(Message msg) {
                        if (msg.what == updateProgress) {//更新下载进度
                                int percent=(Integer)msg.obj;
                                if(percent > 0){
                                        remoteViews.setTextViewText(R.id.download_text,  percent+ "%");
                            			remoteViews.setTextViewText(R.id.download_msg, "下载中...");
                                        remoteViews.setProgressBar(R.id.download_progressBar, 100, percent, false);
                                        notification.contentView = remoteViews;
                                        notificationManager.notify(notificationID, notification);
									    EventBus.getDefault().post(new VersionUpdateEvent(percent,version!=null&&version.isUpdateApk()));
                                }
                        } else if (msg.what == downloadSuccess) {//下载完成
								String paths[]= (String[])msg.obj;
								String path=paths[0];
							    Log.i(TAG, "PATH=" + path);
                        		remoteViews.setTextViewText(R.id.download_text,  "100%");
                                remoteViews.setProgressBar(R.id.download_progressBar, 100, 100, false);
                                notification.contentView = remoteViews;
								if(version!=null) {
									remoteViews.setTextViewText(R.id.download_msg, "下载完成");
									if (version.isUpdateApk()) {
										ApkInstaller.installApk(DownloadFileService.this, path);
									} else {
										if (version.getNew_version().trim().length() > 0 && resetAiBaseResource(paths)) {
											AppConfig.dPreferences.edit().putString(AppConfig.ORIGINAL_VERSION, version.getNew_version())
													.putString(AppConfig.VERSION, version.getNew_version())
													.commit();
											AppConfig.versionName = version.getNew_version();

											/*Intent intent = new Intent(DownloadFileService.this, AssistantService.class);
											intent.putExtra(AssistantService.PLAY_TYPE, AssistantService.CHATROBOT_REINITED);
											startService(intent);*/
											EventBus.getDefault().post(new VersionUpdateEvent(101, false));
										}
									}
								}
								else{
									remoteViews.setTextViewText(R.id.download_msg, "下载完成");
									EventBus.getDefault().post(new BaiduResourceEvent(1));
								}
							notificationManager.notify(notificationID, notification);
							notificationManager.cancel(notificationID);
							stopService(new Intent(getApplicationContext(), DownloadFileService.class));//stop service
                        } else if (msg.what == downloadError) {//下载失败
                    			remoteViews.setTextViewText(R.id.download_msg, "下载出错，请在设置中重新下载");
							    EventBus.getDefault().post(new BaiduResourceEvent(2));
                    			notification.contentView = remoteViews;
                    			notificationManager.notify(notificationID, notification);
								if(downloadTask!=null)
                    			downloadTask.stop();
                                //stopService(new Intent(getApplicationContext(),DownloadFileService.class));//stop service
                        }
                }
                
     };
        
}
