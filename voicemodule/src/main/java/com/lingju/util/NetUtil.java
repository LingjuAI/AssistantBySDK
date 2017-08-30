package com.lingju.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;


/**
 * 网络工具
 * @author ydshu
 *
 */
public class NetUtil {
	private final static String TAG = "NetUtil";
	private Context mContext;
	private static NetUtil mInstance;
	
	public static enum NetType {
		NETWORK_TYPE_NONE,  // 断网情况
		NETWORK_TYPE_2G, // 2G模式
		NETWORK_TYPE_3G, // 3/4G模式
		NETWORK_TYPE_WIFI;   // WiFi模式
		public boolean isOnline(){
			return this.ordinal()>NETWORK_TYPE_2G.ordinal();
		}
		public boolean isMobileNetwork(){
			return this==NETWORK_TYPE_3G||this==NETWORK_TYPE_2G;
		}
		public String toString(){
			switch(this){
			case NETWORK_TYPE_WIFI:return "WiFi网络";
			case NETWORK_TYPE_2G:return "2G网络";
			case NETWORK_TYPE_3G:return "3/4G网络";
			case NETWORK_TYPE_NONE:return "没有可用网络";
			default:return "";
			}
		}
	}
	
	private NetUtil(Context context){
		mContext = context.getApplicationContext();
	}
	
	public static NetUtil getInstance(Context context){
		if (mInstance == null)
			mInstance = new NetUtil(context);
		return mInstance;
	}

	
	/**
	 * 获取当前网络状�?的类�?
	 * @return 返回网络类型
	 */
	public NetType getCurrentNetType(){
		ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI); // wifi
		NetworkInfo mobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE); // gprs
		NetworkInfo ethnet=connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
		if(wifi != null && wifi.isConnected()||
				ethnet!=null&&ethnet.isConnected()){
			Log.d(TAG, "Current net type:  WIFI.");
			return NetType.NETWORK_TYPE_WIFI;
		}else if(mobile != null && mobile.isConnected()){
			switch(mobile.getSubtype()){
			case TelephonyManager.NETWORK_TYPE_GPRS:
			case TelephonyManager.NETWORK_TYPE_EDGE:
			case TelephonyManager.NETWORK_TYPE_CDMA:
			case TelephonyManager.NETWORK_TYPE_1xRTT:
			case TelephonyManager.NETWORK_TYPE_IDEN:
				Log.d(TAG, "Current net type:  2G.");
				return NetType.NETWORK_TYPE_2G;
			default:
				return NetType.NETWORK_TYPE_3G;
			}
		} else {
			Log.d(TAG, "Current net type:  NONE.");
			return NetType.NETWORK_TYPE_NONE;
		}
	}
	
	
}
