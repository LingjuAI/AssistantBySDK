package com.lingju.assistant;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;

import com.lingju.common.log.Log;
import com.lingju.config.Setting;
import com.lingju.context.entity.ContactNum;
import com.lingju.lbsmodule.location.Address;
import com.lingju.lbsmodule.location.BaiduLocateManager;
import com.lingju.lbsmodule.location.LocateListener;
import com.lingju.model.BaiduAddress;
import com.lingju.model.Contact;
import com.lingju.model.SimpleDate;
import com.lingju.model.SmsInfo;
import com.lingju.model.User;
import com.lingju.model.Version;
import com.lingju.model.dao.DaoManager;
import com.lingju.model.dao.UserManagerDao;
import com.lingju.util.CrashHandler;
import com.lingju.util.NetUtil;
import com.lingju.util.PhoneContactUtils;

import java.util.Calendar;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/4.
 */
public class AppConfig extends MultiDexApplication {
    private final static String TAG = "AppConfig";

    //是否已经做了初步的授权
    public static boolean Granted = false;
    //是否浏览了引导页面
    public static boolean ShowGuide;
    //主进程Application是否进行了初始化工作
    public static boolean MainProgressInited;
    //主服务是否已经启动
    public static boolean MainServiceStarted;
    //是否安装后第一次打开
    public static boolean NewInstallFirstOpen;
    //是否安装新版本后第一次打开
    public static boolean NewVersionFirstOpen;

    public static NetUtil.NetType Network;
    public final static String ORIGINAL_VERSION = "o_version";
    public final static String VERSION = "version";
    public final static String WAKEUP_MODE = "is_wakeup_mode";
    public final static String NAVI_TIPS_COUNT = "nav_tips_count";
    public final static String INCOMING_TIPS_TIMES = "incoming_tips_times";
    public final static String INCOMING_TIPS = "incoming_tips";
    public final static String INCOMING_SPEAKER_ON = "incoming_speaker_on";
    public final static String IN_MSG_TIPS = "in_msg_tips";
    public final static String DND_EX_MODE = "dnd_ex_mode";
    public final static String DND_START_MOMENTS = "dnd_start";
    public final static String DND_END_MOMENTS = "dnd_end";
    public final static String SHAKE_WAKE = "SHAKE_WAKE";
    public final static String DAYS_OPEN = "daysopen";
    public final static String DOWNLOAD_ON_WIFI = "DOWIFI";
    public final static String PLAY_CHANNEL = "play_channel";
    public final static String ALLOW_WIRE = "allow_wire";
    public final static String DND_MODE = "dnd_mode";
    public static String DefaultDir = "";
    public final static String SHOW_INTRODUCE = "show_introduce";
    public final static String IS_DND_START = "is_dnd_start";
    public final static String ACCOUNT_AMOUNT = "account_amount";
    public final static String HAS_AMOUNT = "has_amount";
    public final static String PLAY_NO_WIFI = "play_no_wifi";
    /**
     * 当前定位位置对象
     **/
    public Address address;
    /**
     * 当前定位位置具体位置名称
     **/
    public String myAddress;
    /**
     * 出发地位置
     */
    public BaiduAddress startAddress;
    /**
     * 目的地位置
     */
    public BaiduAddress endAddress;
    /**
     * 回家时间text
     */
    public String goHomeTime;
    /**
     * 去公司时间
     */
    public String goCompanyTime;
    /**
     * 搜索地点时所在城市
     **/
    public String selectedCityInSearchPoi;
    public static SharedPreferences dPreferences;
    public static PhoneContactUtils mContactUtils;
    public SmsInfo lastSms = new SmsInfo();     //最新一条短信信息对象
    public boolean speaker_on_one;      //打开免提标记
    public boolean incoming_tips;      //来电提醒标记
    public boolean incoming_speaker_on;    //是否免提标记
    public boolean dndExMode = true;   //重复来电不受限制
    public boolean dndMode = true;      //夜间勿扰
    public boolean inmsg_tips = false;  //接收短信播报标记
    /**
     * 通话中的未接来电联系人
     */
    public Queue<Contact> missedCallContacts = new ConcurrentLinkedQueue<>();
    /**
     * 通话中或者正在提示来信中的未提示短信
     */
    public Queue<SmsInfo> missedMsgs = new ConcurrentLinkedQueue<>();
    public SimpleDate dndStart;
    public SimpleDate dndEnd;
    public long lastContactTime;
    public Contact lastContact;
    public User user;   //当前用户对象
    public static String versionName = "";    //应用版本号
    public Version newVersion;
    public String[][] speakers = null;
    public int checkedSpeakerItemPosition;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        //设置是否首次打开的标识
        dPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        checkNewVersionFirstOpen();
        // 注册全局异常处理
        CrashHandler.getInstance().init(this);
        // LeakCanary.install(this);
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        BaiduLocateManager.get().deleteObserver(locateListener);
    }

    /**
     * 主进程onCreate后必须进行的先始化工作
     */
    public void onMainProgressCreate() {
        if (!MainProgressInited) {
            Log.i("LingJu", "init start>>>>>>");
            Network = NetUtil.getInstance(this).getCurrentNetType();
            init();
            //初次定位
            BaiduLocateManager.createInstance(this).addObserver(locateListener);
            BaiduLocateManager.get().start(Network != NetUtil.NetType.NETWORK_TYPE_NONE);
            Observable.create(new ObservableOnSubscribe<Object>() {
                @Override
                public void subscribe(ObservableEmitter<Object> e) throws Exception {
                    initExtra();
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe();
        }
    }

    private void init() {
        //创建数据库相关的表及dao
        DaoManager.create(this);
        this.address = Setting.getAddress();
        if (this.address == null)
            this.address = Setting.getDefaultAddress();

        //TODO 初始化其他的一些SharedPreferences的变量
        // VoiceMediator.create(this);
        MainProgressInited = true;
    }

    /**
     * 初始化额外参数
     **/
    private void initExtra() {
        mContactUtils = PhoneContactUtils.getInstance(this);
        inmsg_tips = dPreferences.getBoolean(IN_MSG_TIPS, false);
        incoming_tips = dPreferences.getBoolean(INCOMING_TIPS, false);
        incoming_speaker_on = dPreferences.getBoolean(INCOMING_SPEAKER_ON, false);
        dndExMode = dPreferences.getBoolean(DND_EX_MODE, true);
        dndMode = dPreferences.getBoolean(DND_MODE, true);
        dndStart = new SimpleDate(dPreferences.getString(DND_START_MOMENTS, "23:30"));
        dndEnd = new SimpleDate(dPreferences.getString(DND_END_MOMENTS, "08:30"));
        //获取手机联系人
        mContactUtils.setContactsNameList();
        user = UserManagerDao.getInstance().findNewCreated();
        initSpeakers();
        Log.i("LingJu", "init end>>>>>>");
    }


    /**
     * 初始化合成声音数组
     **/
    private void initSpeakers() {
        String temp[] = getResources().getStringArray(R.array.speakers);
        speakers = new String[temp.length][4];
        int i = 0;
        String s = Setting.getVolName();
        for (String t : temp) {
            speakers[i] = t.split("\\|");
            if (speakers[i][3].equals(s)) {
                checkedSpeakerItemPosition = i;
            }
            i++;
        }
    }

    /**
     * 网络状态是否为本地模式（离线，无网络）
     **/
    public boolean isLocalMode() {
        return Network == NetUtil.NetType.NETWORK_TYPE_NONE || Network == NetUtil.NetType.NETWORK_TYPE_2G;
    }

    /**
     * 判断是否在勿扰模式的时间段中
     *
     * @param c
     * @return
     */
    public boolean notInDND(Contact c) {
        if (dndStart.equals(dndEnd))
            return true;
        Calendar cl = Calendar.getInstance();
        long now = cl.getTimeInMillis();
        if (dndExMode && (now - lastContactTime) < 180000 && lastContact != null) {
            lastContactTime = now;
            for (ContactNum n1 : c.getCodes()) {
                for (ContactNum n2 : lastContact.getCodes()) {
                    if (n1.getNumber().equals(n2.getNumber())) {
                        lastContact = c;
                        Log.w(TAG, "三分钟内拨了第二次");
                        return true;
                    }
                }
            }
        }
        lastContactTime = now;
        lastContact = c;
        SimpleDate n = new SimpleDate();
        if (dndStart.gt(dndEnd)) {
            //Log.e(TAG, "notInDND111>>"+Boolean.toString(n.gt(dndEnd)&&n.lt(dndStart)));
            return n.gt(dndEnd) && n.lt(dndStart);
        } else {
            //Log.e(TAG, "notInDND222>>"+Boolean.toString(n.lt(dndStart)||n.gt(dndEnd)));
            return n.lt(dndStart) || n.gt(dndEnd);
        }
    }

    /**
     * 定位更新监听器
     **/
    private LocateListener locateListener = new LocateListener(LocateListener.CoorType.BD09LL) {
        @Override
        public void update(Address address) {
            AppConfig.this.address = address;
            /* 刷新定位 */
            Setting.setAddress(address);
            /*if (AndroidChatRobotBuilder.get() != null) {
                SimpleAddress sa = AndroidChatRobotBuilder.get().address();
                Address tmp = address.clone().setBD09LL();
                sa.city = address.getCity();
                sa.lat = Double.toString(tmp.getLatitude());
                sa.lng = Double.toString(tmp.getLongitude());
                sa.position = address.getAddressDetail();
                AndroidChatRobotBuilder.get().updateAddress();
            }*/
        }

    };

    protected void checkNewVersionFirstOpen() {
        versionName = getAppVersionName();
        NewInstallFirstOpen = checkNewInstallFirstOpen();
        String sv = dPreferences.getString(VERSION, "d");
        if (!sv.equals(versionName)) {
            NewVersionFirstOpen = true;
            dPreferences.edit().putString(VERSION, versionName).commit();
        }
    }

    public boolean checkNewInstallFirstOpen() {
        boolean result = dPreferences.getBoolean("NewInstallFirstOpen", true);
        if (result) {
            dPreferences.edit().putBoolean("NewInstallFirstOpen", false).commit();
        }
        return result;
    }

    //获取当前版本号
    private String getAppVersionName() {
        String versionName = "";
        try {
            versionName = dPreferences.getString(ORIGINAL_VERSION, null);
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            if (versionName == null || compare(packageInfo.versionName, versionName) > 0) {
                versionName = packageInfo.versionName;
                dPreferences.edit().putString(ORIGINAL_VERSION, versionName).commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }

    /**
     * 比较两个版本的新旧
     *
     * @param v1
     * @param v2
     * @return >0:v1新，=0:同样的版本号，<0:v2版本新
     */
    public static int compare(String v1, String v2) {
        try {
            String vs1[] = v1.split("\\.");
            String vs2[] = v2.split("\\.");
            int l = Math.min(vs1.length, vs2.length);
            int t = 0;
            for (int i = 0; i < l; i++) {
                t = Integer.parseInt(vs1[i]) - Integer.parseInt(vs2[i]);
                if (t != 0)
                    break;
            }
            if (t == 0) {
                return vs1.length - vs2.length;
            } else {
                return t;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getSettingInt(String key, int defaultVal) {
        int ret = dPreferences.getInt(key, defaultVal);
        return ret;
    }

    public static void setSettingInt(String key, int val) {
        SharedPreferences.Editor editor = dPreferences.edit();
        editor.putInt(key, val);
        editor.commit();
    }

    public static float getSettingFloat(String key, float defaultVal) {
        float ret = defaultVal;
        ret = dPreferences.getFloat(key, defaultVal);
        return ret;
    }

    public static void setSettingFloat(String key, float val) {
        SharedPreferences.Editor editor = dPreferences.edit();
        editor.putFloat(key, val);
        editor.commit();
    }

    public static String getSettingString(String key) {
        //System.out.println("key="+key);
        String ret = dPreferences.getString(key, null);
        return ret;
    }

    public static void setSettingString(String key, String val) {
        SharedPreferences.Editor editor = dPreferences.edit();
        editor.putString(key, val);
        editor.commit();
    }
}
