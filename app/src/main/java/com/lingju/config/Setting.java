package com.lingju.config;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Base64;

import com.lingju.assistant.AppConfig;
import com.lingju.lbsmodule.location.Address;
import com.lingju.common.log.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Setting {
	private final static String TAG = "Settings";
	/**百度api的ak**/
	//public final static String map_ak = "7501cd9451c35db814df6106f20c36e1";

	//该值请勿修改
	public final static String NEW_VERSION_DIR="newVersionDir";
	public final static String WECHAT_APPID="wx3ace16534f78f2b8";
	/** 设置地理位置纬度，值为double型 */
	private final static String SET_LOCATION_LATITUDE = "set_location_latitude";
	/** 设置地理位置经度，值为double型 */
	private final static String SET_LOCATION_LONGITUDE = "set_location_longitude";
	/** 地理位置设置key */
	private final static String SET_ADDRESS = "set_address";
	/** 保存设置Key: 本地引擎语法是否已构建 */
	private final static String SET_LOCAL_GRAMMAR_BUILDED = "local_gramar_builded";
	/** 保存设置Key: 本地引擎词典是否已经上传 */
	private final static String SET_LOCAL_LEXICON_UPLOADED = "local_lexicon_uploaded";
	/** 保存设置Key: 在线引擎词典是否已经上传 */
	private final static String SET_NET_LEXICON_UPLOADED = "net_lexicon_uploaded";
	/** 保存设置Key: 离线引擎联系人数据版本号列表 */
	private final static String SET_LOCAL_CONTACTS_VERSION = "local_contacts_version";
	/** 保存设置Key: 在线引擎联系人数据版本号列表 */
	private final static String SET_NET_CONTACTS_VERSION = "net_contacts_version";	
	/**是否禁止识别**/
	private static final String FORBID_RECOGNIZE="forbid_Recognize";
	/**是否禁止合成播放语音**/
	private static final String FORBID_SPEECH="forbid_seech";
	private static final String VODS_BEGIN="vods_b_key";
	private static final String VODS_END="vods_e_key";
	private static final String VOLUME="volume";
	private static final String VOL_SPEED="vol_speed";
	private static final String VOL_NAME="speaker_key";
	private static final String REC_ENGINE_TYPE="rec_engine_key";
	private static final String SYN_ENGINE_TYPE="syn_engine_key";
	public final static String USER_NAME="userName";
	public final static String LAST_STARTED="last_started";
	public final static String LAST_PUSH_MUSICS="last_push";
	public static final String MUSIC_TYPE_PREFIX="【MUSIC_TYPE】:";
	public static final String RECOGNIZE_ERROR_TIPS="刚刚识别出错了";
	public static final String RECOGNIZE_NETWORK_ERROR="我刚才无法连上网络";
	public static final String RECOGNIZE_NODATA_ERROR="我似乎什么都没听到";
	public static final String RECOGNIZE_NOMATCH_ERROR="我不能连接互联网，请检查你的网络。";
	public static final String CONFIRM_PLAY_UNDER_3G="在线播放歌曲需要消耗流量，是否继续播放音乐？";
	

	/** 音量最大值 */
	public final static int MAX_VOL = 30;
	/** 音量最小值 */
	public final static int MIN_VOL = 0;
	
	public final static String DELETE_LOCAL_FAVORITE_MUSICS="deleteLocalMusic";
	public static boolean DB_INITED=false;
	
	public final static int COMMON_GREEN=Color.rgb(69, 201, 170);
	public final static int COMMON_TEXT_COLOR=Color.rgb(109, 109, 109);
	public final static int WHITE=Color.rgb(255, 255, 255);
	public final static int TEXT_COLOR=Color.rgb(57, 57, 57);
	
	public final static int INDICATOR_BLUE=Color.rgb(0, 160, 233);
	public final static int INDICATOR_RED=Color.rgb(232, 117, 90);
	public final static int INDICATOR_GREEN=Color.rgb(173, 200, 90);

	public static final String KEY_GRAMMAR_ABNF_ID = "grammar_abnf_id";
	public static final String GRAMMAR_TYPE_ABNF = "abnf";
	public final static String SDCARD_PATH="/mnt/sdcard";
	public final static String STORAGE_PATH="/LingjuAssistant";
	
	public final static String[] Q_MANUFACTURER=new String[]{"huawei"};
	public final static String[] L_MANUFACTURER=new String[]{"xiaomi","yulong"};
	/**
	 * 必须访问全部通话记录的机型
	 */
	public final static String[] V_CALLS_MANUFACTURER=new String[]{"xiaomi"};
	public static boolean MediaPlayerFast=false; 
	public static boolean must_visit_calls=false; 
	private final static String FAVORITE_MUSICS_STRING="favoriteMusics";

	public final static String UNKNOWN="unknown";
	public static String machineMsg=UNKNOWN;
	public static String mac=UNKNOWN;
	public static String imei=UNKNOWN;
	public static String serial=UNKNOWN;
	public static String DEFAULT_DIR;
	static{
		DEFAULT_DIR=getExtSdcardPath()+STORAGE_PATH;
	}
	

	public static String getExtSdcardPath(){
		File dir=new File("/mnt");
		String name=null;
		String status = Environment.getExternalStorageState();
		// 是否只读
		if (status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			status = Environment.MEDIA_MOUNTED;
		}
		if (status.equals(Environment.MEDIA_MOUNTED)) {
			try {
				File path = Environment.getExternalStorageDirectory();
				//System.out.println("Environment.getExternalStorageDirectory()="+Environment.getExternalStorageDirectory());
				StatFs stat = new StatFs(path.getPath());
				int sdc=calculateSizeInMB(stat);
				//System.out.println("path.getAbsolutePath() capacity:"+sdc);
				if(sdc>20||sdc==0){
					return path.getAbsolutePath();
				}
			} catch (Exception e) {
				e.printStackTrace();
				status = Environment.MEDIA_REMOVED;
			}
		}
		/*File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		Log.i(TAG, "path.getAbsolutePath() capacity:"+calculateSizeInMB(stat));
		if(calculateSizeInMB(stat)>50){
			return "/mnt";
		}*/
		
		if(dir.exists()&&dir.isDirectory()){
			File[] fs=dir.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String filename) {
					if(filename.toLowerCase().contains("sdcard"))return true;
					return false;
				}
			});
			int sdc=0;
			int esdc=0;
			String sdcard=null;
			for(File f:fs){
				if(f.getName().equals("sdcard")){
					sdcard=SDCARD_PATH;
					sdc=calculateSizeInMB(new StatFs(SDCARD_PATH));
					//System.out.println("getExtSdcardPath:/mnt/sdcard,size="+sdc);
					if(sdc>0&&sdc<20)sdcard=null;
				}
				else{
					name="/mnt/"+f.getName();
					int c=calculateSizeInMB(new StatFs(name));
					//System.out.println("getExtSdcardPath:"+name+",size="+esdc);
					if(c>20&&c>=esdc){
						esdc=c;
					}
					else{
						name=null;
					}
				}
			}
			//Log.i(TAG, "sdc:"+sdc+",esdc"+esdc);
			if(sdcard!=null)return sdcard;
			if(name!=null&&(esdc>20||esdc==0)){
				return name;
			}
		}
		return null;
	}
	
	
	 /**
     * 
     * @param path
     *            文件路径
     * @return 文件路径的StatFs对象
     * @throws Exception
     *             路径为空或非法异常抛出
     */
    private static StatFs getStatFs(String path) {
            try {
                    return new StatFs(path);
            } catch (Exception e) {
                    e.printStackTrace();
            }
            return null;
    }
    
    public static void saveFavoriteMusics(String text){
    	text=Base64.encodeToString(text.getBytes(), Base64.DEFAULT);
    	AppConfig.dPreferences.edit().putString(FAVORITE_MUSICS_STRING, text).commit();
    }
    
    public static String getFavoriteMusics(){
    	String text=AppConfig.dPreferences.getString(FAVORITE_MUSICS_STRING, null);
    	if(text!=null){
    		text=new String(Base64.decode(text.getBytes(), Base64.DEFAULT));
    		//System.out.println(FAVORITE_MUSICS_STRING+":"+text);
    	}
    	return text;
    }

    /**
     * 
     * @param stat
     *            文件StatFs对象
     * @return 剩余存储空间的MB数
     * 
     */
    private static int calculateSizeInMB(StatFs stat) {
            if (stat != null) {
            	return (int)( (((long)stat.getAvailableBlocks())* ((long)stat.getBlockSize()))/(long)0x100000);
            }
            return 0;
    }
	
	public static int getVodsBegin(){
		return AppConfig.dPreferences.getInt(VODS_BEGIN, 5000);
	}
	
	public static int getVodsEnd() {
		return AppConfig.dPreferences.getInt(VODS_END, 1000);
	}
	
	public static int getVolume() {
		return AppConfig.dPreferences.getInt(VOLUME, 100);
	}
	
	public static String getVolName() {
		return AppConfig.dPreferences.getString(VOL_NAME, "vixq");
	}
	
	public static void setVolName(String name){
		AppConfig.dPreferences.edit().putString(VOL_NAME, name).commit();
	}
	
	public static int getVolSpeed() {
		return AppConfig.dPreferences.getInt(VOL_SPEED, 60);
	}
	
	public static String getRecEngineType() {
		return AppConfig.dPreferences.getBoolean(REC_ENGINE_TYPE, true)?"auto":"local";
	}
	
	public static String getSynEngineType() {
		return AppConfig.dPreferences.getBoolean(SYN_ENGINE_TYPE, true)?"auto":"local";
	}
	/**
	 * 获取上次保存的经度
	 * @return
	 * 		经度
	 */
	public static float getLastLongitude(){
		Log.d(TAG, "getLastLongitude ");
		float val = -1f;
		val = AppConfig.getSettingFloat(SET_LOCATION_LONGITUDE, -1);
		
		return val;
	}
	
	/**
	 * 获取上次保存的纬度
	 * @return
	 */
	public static float getLastLatitude(){
		Log.d(TAG, "getLastLatitude ");
		float val = -1f;
		val = AppConfig.getSettingFloat(SET_LOCATION_LATITUDE, -1);
		return val;
	}
	
	/**
	 * 保存经度
	 * @param val
	 * 		经度值
	 */
	public static void setLatitude(float val){
		Log.d(TAG, "setLatitude ");
		AppConfig.setSettingFloat(SET_LOCATION_LATITUDE, val);
	}
	
	public static String formatCity(String city){
		if(city==null)return city;
		int l=city.length();
		if(l>1&&city.substring(l-1,l).equals("市")){
			return city.substring(0, l-1);
		}
		return city;
	}
	
	/**
	 * 保存纬度
	 * @param val
	 * 		纬度值
	 */
	public static void setLongitude(float val){
		Log.d(TAG, "setLongitude ");
		AppConfig.setSettingFloat(SET_LOCATION_LONGITUDE, val);
	}
	
	/** 保存地理位置 */
	@SuppressLint("NewApi")
	public static void setAddress(Address addr){
		Log.d(TAG, "saveAddress ");
		
		// 将AddressInfo对象转换成String保存
		String saveAddress = "";
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(addr); 
			saveAddress = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
			Log.d(TAG, "saveAddress | saveAddress: " + saveAddress);
		} catch (Exception e){
			e.printStackTrace();
		}
		
		if(!TextUtils.isEmpty(saveAddress)){
			AppConfig.setSettingString(SET_ADDRESS, saveAddress);
		} else {
			Log.e(TAG, "saveAddress empty");
		}
	}
	
	/**
	 * 读取地理位置对象
	 * @return
	 */
	@SuppressLint("NewApi")
	public static Address getAddress(){
		Log.d(TAG, "getAddress ");
		
		Address getAddress = null;
		String tmpSaveBase64 = AppConfig.getSettingString(SET_ADDRESS);
		if(!TextUtils.isEmpty(tmpSaveBase64)){
			// 对Base64格式的字符串进行解码 
			byte[] base64Bytes = Base64.decode(tmpSaveBase64.getBytes(), Base64.DEFAULT);
			try {
				ByteArrayInputStream bais = new ByteArrayInputStream(base64Bytes);
				ObjectInputStream ois = new ObjectInputStream(bais);
				// 从ObjectInputStream中读取Product对象  
				getAddress = (Address) ois.readObject();
			} catch (Exception e){
				e.printStackTrace();
			}
		} else {
			Log.e(TAG, "read address from cache empty.");
		}

		return getAddress;
	}
	
	/**
	 * 获取一个默认的地理位置，用于获取位置失败时
	 * @return
	 */
	public static Address getDefaultAddress() {
		Log.d(TAG, "getDefaultAddress ");
		
		Address defaultAddress = new Address();
		defaultAddress.setLatitude(23.14);
		defaultAddress.setLongitude(113.33);
		defaultAddress.setCity("广州市");
		defaultAddress.setAddressDetail("广东省广州市");
		defaultAddress.setProvince("广东省");
		defaultAddress.setStreet("体育东路");
		defaultAddress.setStreet("广州市天河体育中心");
		
		return defaultAddress;
	}
	/**
	 * 获取本地语法构建标识位
	 * @return
	 * 		本地语法是否构建完成
	 */
	/*public static boolean getGrammarBuilded(){
		boolean ret = RobotApplication.getSettingBoolean(SET_LOCAL_GRAMMAR_BUILDED, false);
		return ret;
	}*/
	
	/**
	 * 设置本地语法构建标识位
	 * @param val
	 * 			本地语法是否构建完成
	 */
	/*public static void setGrammarBuilded(boolean val){
		RobotApplication.setSettingBoolean(SET_LOCAL_GRAMMAR_BUILDED, val);
	}*/
	
	/**
	 * 设置本地引擎语法词典更新标志位
	 * @param val
	 * 			词典是否已更新
	 */
	/*public static void setLocalLexiconUpdated(boolean val){
		RobotApplication.setSettingBoolean(SET_LOCAL_LEXICON_UPLOADED, val);
	}*/
	
	/**
	 * 获取本地引擎语法词典更新标识
	 * @return
	 * 		true: 已更新词典
	 * 		false：未更新词典
	 */
	/*public static boolean getLocalLexiconUpdated(){
		return RobotApplication.getSettingBoolean(SET_LOCAL_LEXICON_UPLOADED, false);
	}*/
	
	/**
	 * 设置在线引擎语法词典更新标识
	 * @param val
	 */
	public static void setNetLexiconUpdated(boolean val){
//		RobotApplication.setSettingBoolean(SET_NET_LEXICON_UPLOADED, val);
	}
	
	/**
	 * 获取在线引擎语法词典更新标识
	 * @return
	 * 		true: 已更新词典
	 * 		false：未更新词典
	 */
	/*public static boolean getNetLexiconUpdated(){
		return RobotApplication.getSettingBoolean(SET_NET_LEXICON_UPLOADED, false);
	}*/
	
	/**
	 * 保存离线引擎联系人数据库版本号
	 * @param version
	 */
	public static void setLocalContactsVersion(String version){
//		RobotApplication.setSettingString(SET_LOCAL_CONTACTS_VERSION, version);
	}
	
	/**
	 * 保存在线引擎联系人数据库版本号
	 * @param version
	 */
	public static void setNetContactsVersion(String version){
//		RobotApplication.setSettingString(SET_NET_CONTACTS_VERSION, version);
	}
	
	/**
	 * 获取保存的离线联系人数据库版本号
	 * @return
	 */
	/*public static String getLocalContactsVersion(){
		return RobotApplication.getSettingString(SET_LOCAL_CONTACTS_VERSION);
	}*/
	
	/**
	 * 获取保存的在线联系人数据库版本号
	 * @return
	 */
	/*public static String getNetContactsVersion(){
		return RobotApplication.getSettingString(SET_NET_CONTACTS_VERSION);
	}*/

}
