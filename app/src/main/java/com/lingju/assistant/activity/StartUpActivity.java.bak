package com.lingju.assistant.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.entity.ZipCodeMap;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.common.log.Log;
import com.lingju.model.Item;
import com.lingju.model.SubItem;
import com.lingju.model.Zipcode;
import com.lingju.model.dao.AccountItemDao;
import com.lingju.model.dao.CallAndSmsDao;
import com.lingju.util.ScreenUtil;
import com.tencent.stat.StatConfig;
import com.tencent.stat.StatService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;


@RuntimePermissions
public class StartUpActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private String TAG = "StartUpActivity";
    //Class target=LoginActivity.class;
    /**
     * 收入标记
     **/
    private final static int ITEM_INCOME = 0;
    /**
     * 支出标记
     **/
    private final static int ITEM_EXPENSE = 1;
    private WaitTask mWaitTask;
    private ImageView mImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppConfig.ShowGuide = true;
        ScreenUtil.getInstance().init(this);
        try {
            //if (isStoragePermissionGranted()) {
            // init();
            //}
            StartUpActivityPermissionsDispatcher.initWithCheck(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "onCreate>>" + Boolean.toString(AppConfig.MainProgressInited));
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "StartUpActivity onDestroy()");
        if (mWaitTask != null) {
            mWaitTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "StartUpActivity onResume()>>>");
        mWaitTask = new WaitTask();
        mWaitTask.execute();
        //部分初始化工作
        ((AppConfig) getApplication()).onMainProgressCreate();
        if (!AppConfig.MainServiceStarted) {
            startService(new Intent(this, AssistantService.class));
        }
        if (AppConfig.NewInstallFirstOpen) {
            Log.i("LingJu", "init data >>>>>");
            initZipCode();
            initAccountProject();
            //读取应用列表(只能在主线程中实现)
            getPackageManager().getInstalledPackages(0);
        }
    }



    /**
     * 需要申请权限的方法
     **/
    @NeedsPermission({Manifest.permission.CALL_PHONE,   //拨号
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.RECORD_AUDIO,   //录音
            Manifest.permission.ACCESS_COARSE_LOCATION,     //定位
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,      //读取联系人
            Manifest.permission.READ_CALL_LOG,      //读取通话记录
            Manifest.permission.READ_SMS,       //读取短信
            Manifest.permission.SEND_SMS,       //发送短信
            "android.permission.WRITE_SMS",     //写短信
            Manifest.permission.BLUETOOTH,      //蓝牙
            Manifest.permission.BLUETOOTH_ADMIN})
    public void init() {
        // 因此，MTA的初始化工作需要在本onCreate中进行
        // 在startStatService之前调用StatConfig配置类接口，使得MTA配置及时生效
        initMTAConfig(false);
        String appkey = "Aqc1104472246";
        // 初始化并启动MTA
        // 第三方SDK必须按以下代码初始化MTA，其中appkey为规定的格式或MTA分配的代码。
        // 其它普通的app可自行选择是否调用
        try {
            // 第三个参数必须为：com.tencent.stat.common.StatConstants.VERSION
            StatService.startStatService(getApplicationContext(), appkey,
                    com.tencent.stat.common.StatConstants.VERSION);
        } catch (Exception e) {
            // MTA初始化失败
            e.printStackTrace();
        }
        setContentView(R.layout.activity_startup);
        Log.i(TAG, "StartUpActivity setContentView>>>>>>");
        //设置模拟状态栏的高度
        View statusBar = findViewById(R.id.status_bar);
        ViewGroup.LayoutParams layoutParams = statusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        statusBar.setLayoutParams(layoutParams);
        /*mImg = (ImageView) findViewById(R.id.start_up_img);
        Animation loadAnimation = AnimationUtils.loadAnimation(this, R.anim.start_up_loading);
        String as[] = getResources().getStringArray(R.array.start_up_tips);
        if (as != null && as.length > 0) {
            ((TextView) findViewById(R.id.start_upt_tips)).setText(as[new Random(System.currentTimeMillis()).nextInt(as.length)]);
        }
        mImg.startAnimation(loadAnimation);*/

    }

    /*//挂断电话
    private void endCall() {
        try {
            Log.i("LingJu", "StartUpActivity endCall()");
            //通过反射拿到android.os.ServiceManager里面的getService这个方法的对象
            Method method = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
            //通过反射调用这个getService方法，然后拿到IBinder对象，然后就可以进行aidl啦
            IBinder iBinder = (IBinder) method.invoke(null, TELEPHONY_SERVICE);
            ITelephony telephony = ITelephony.Stub.asInterface(iBinder);
            telephony.endCall();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /**
     * 向用户解释为什么需要这个权限的方法
     **/
    @OnShowRationale({Manifest.permission.CALL_PHONE,   //拨号
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.RECORD_AUDIO,   //录音
            Manifest.permission.ACCESS_COARSE_LOCATION,     //定位
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,      //读取联系人
            Manifest.permission.READ_CALL_LOG,      //读取通话记录
            Manifest.permission.READ_SMS,       //读取短信
            Manifest.permission.SEND_SMS,       //发送短信
            "android.permission.WRITE_SMS",     //写短信
            Manifest.permission.BLUETOOTH,      //蓝牙
            Manifest.permission.BLUETOOTH_ADMIN})
    public void showRationaleForPermission(final PermissionRequest request) {
        Log.i("LingJu", "StartUpActivity showRationaleForPermission()");
        new CommonDialog(this, "申请权限", "应用还需要以下权限才能正常运行", "知道了")
                .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        request.proceed();
                    }
                })
                .show();
    }

    /**
     * 申请权限被拒后调用
     **/
    @OnPermissionDenied({Manifest.permission.CALL_PHONE,   //拨号
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.RECORD_AUDIO,   //录音
            Manifest.permission.ACCESS_COARSE_LOCATION,     //定位
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,      //读取联系人
            Manifest.permission.READ_CALL_LOG,      //读取通话记录
            Manifest.permission.READ_SMS,       //读取短信
            Manifest.permission.SEND_SMS,       //发送短信
            "android.permission.WRITE_SMS",     //写短信
            Manifest.permission.BLUETOOTH,      //蓝牙
            Manifest.permission.BLUETOOTH_ADMIN})
    public void showRecordDenied() {
        Toast.makeText(StartUpActivity.this, "有权限被拒绝将影响程序正常运行, 请自行到应用权限管理页面设置！", Toast.LENGTH_LONG).show();
    }

    private void initMTAConfig(boolean config) {
        //设置最大缓存未发送消息个数（默认1024）
        StatConfig.setMaxStoreEventCount(1024);

        //缓存消息的数量超过阈值时，最早的消息会被丢弃。
        StatConfig.setMaxBatchReportCount(30);

        //（仅在发送策略为PERIOD时有效）设置间隔时间（默认为24*60，即1天）
        StatConfig.setSendPeriodMinutes(1440);

        //开启SDK LogCat开关（默认false）
        StatConfig.setDebugEnable(config);
        StatConfig.initNativeCrashReport(getApplicationContext(), AppConfig.DefaultDir);
    }

    /**
     * 初始化区号
     **/
    private void initZipCode() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                Set<Map.Entry<String, String>> entries = ZipCodeMap.MAP.entrySet();
                List<Zipcode> zipCodeList = new ArrayList<>();
                for (Map.Entry<String, String> entry : entries) {
                    Zipcode zipCode = new Zipcode();
                    zipCode.setCity(entry.getKey());
                    zipCode.setCode(entry.getValue());
                    zipCodeList.add(zipCode);
                }
                CallAndSmsDao.getInstance(StartUpActivity.this).insertZipCodes(zipCodeList);
            }
        })
                .delay(1500, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 首次打开应用时，初始化账单项目数据
     **/
    private void initAccountProject() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                 /* 插入账单子类item */
                String[] subItems = getResources().getStringArray(R.array.sub_items);
                for (int i = 0; i < subItems.length; i++) {
                    SubItem subItem = new SubItem();
                    String[] subArr = subItems[i].split("\\|");
                    subItem.setItemid(Long.valueOf(subArr[0]));
                    subItem.setName(subArr[1]);
                    AccountItemDao.getInstance().insertSubItem(subItem);
                }
            }
        })
                .delay(1500, TimeUnit.MILLISECONDS)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                         /* 插入账单item */
                        String[] expenses = getResources().getStringArray(R.array.expense_items);
                        String[] incomes = getResources().getStringArray(R.array.income_items);
                        /* 支出类 */
                        for (int i = 0; i < expenses.length; i++) {
                            Item item = new Item();
                            item.setItem(expenses[i]);
                            item.setExpense(ITEM_EXPENSE);
                            AccountItemDao.getInstance().inserItem(item);
                        }
                        /* 收入类 */
                        for (int i = 0; i < incomes.length; i++) {
                            Item item = new Item();
                            item.setItem(incomes[i]);
                            item.setExpense(ITEM_INCOME);
                            AccountItemDao.getInstance().inserItem(item);
                        }
                    }
                })
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe();

    }
/*
    public  boolean isStoragePermissionGranted() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
					== PackageManager.PERMISSION_GRANTED) {
				Log.i(TAG,"Permission is granted");
				return AssistantApplication.granted=true;
			} else {
				Log.i(TAG,"Permission is revoked");
				ActivityCompat.requestPermissions(this, new String[]{
						Manifest.permission.WRITE_EXTERNAL_STORAGE,
						Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
						Manifest.permission.RECEIVE_BOOT_COMPLETED,
						Manifest.permission.READ_CONTACTS,
						Manifest.permission.WAKE_LOCK,
						Manifest.permission.VIBRATE,
						Manifest.permission.RECORD_AUDIO,
						Manifest.permission.INTERNET,
						Manifest.permission.CHANGE_NETWORK_STATE,
						Manifest.permission.WRITE_SETTINGS,
						Manifest.permission.MODIFY_AUDIO_SETTINGS,
						Manifest.permission.READ_CALL_LOG,
						Manifest.permission.CALL_PHONE,
						Manifest.permission.SEND_SMS,
						Manifest.permission.CALL_PHONE,
						Manifest.permission.RECEIVE_SMS,
						Manifest.permission.READ_SMS,
						Manifest.permission.GET_TASKS,
						Manifest.permission.BLUETOOTH,
						Manifest.permission.BLUETOOTH_ADMIN,
						Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.ACCESS_WIFI_STATE,
						Manifest.permission.ACCESS_NETWORK_STATE,
						Manifest.permission.CHANGE_WIFI_STATE,
						Manifest.permission.INTERNET,
						Manifest.permission.READ_LOGS,
						Manifest.permission.READ_PHONE_STATE}, 1);
				return AssistantApplication.granted=false;
			}
		}
		else { //permission is automatically granted on sdk<23 upon installation
			Log.v(TAG,"Permission is granted");
			return AssistantApplication.granted=true;
		}
	}

	@TargetApi(Build.VERSION_CODES.M)
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		Log.i(TAG,"permissions>>"+permissions.length+",grantResults>>>>>>>>>>>>>>>>"+grantResults);
		if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
			Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
			AssistantApplication.granted=true;
			if(checkSystemWritePermission()){
				init();
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.M)
	protected boolean checkSystemWritePermission() {
		boolean retVal = true;
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				retVal = Settings.System.canWrite(this);
				Log.d(TAG, "Can Write Settings: " + retVal);
				if (!retVal) {
					openAndroidPermissionsMenu();
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return retVal;
	}

	protected void openAndroidPermissionsMenu() {
		Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
		intent.setData(Uri.parse("package:" + getPackageName()));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivityForResult(intent,110);
	}*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        StartUpActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 110) {
            init();
            // StartUpActivityPermissionsDispatcher.initWithCheck(this);
        }
    }

    class WaitTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            startActivity(new Intent(StartUpActivity.this, MainActivity.class));
            finish();
            overridePendingTransition(R.anim.startup_act_in, R.anim.startup_act_out);
        }

    }

}
