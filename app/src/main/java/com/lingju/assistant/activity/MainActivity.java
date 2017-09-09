package com.lingju.assistant.activity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.BaseActivity;
import com.lingju.assistant.activity.event.BottomBoxStateEvent;
import com.lingju.assistant.activity.event.CallAndSmsEvent;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.DialogEvent;
import com.lingju.assistant.activity.event.InitNaviManagerEvent;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.assistant.activity.event.RobotResponseEvent;
import com.lingju.assistant.activity.event.TrackPlayEvent;
import com.lingju.assistant.activity.index.IAdditionAssist;
import com.lingju.assistant.activity.index.IChatList;
import com.lingju.assistant.activity.index.IVoiceInput;
import com.lingju.assistant.activity.index.presenter.AssistPresenter;
import com.lingju.assistant.activity.index.presenter.CallAndSmsPresenter;
import com.lingju.assistant.activity.index.presenter.ChatListPresenter;
import com.lingju.assistant.activity.index.presenter.VoiceInputPresenter;
import com.lingju.assistant.activity.index.view.ChatListFragment;
import com.lingju.assistant.activity.index.view.GuideFragment;
import com.lingju.assistant.activity.index.view.IntroduceFragment;
import com.lingju.assistant.activity.index.view.PlayerHeaderFragment;
import com.lingju.assistant.activity.index.view.PlayerListPagerFragment;
import com.lingju.assistant.activity.index.view.VoiceInputFragment;
import com.lingju.assistant.baidunavi.adapter.BaiduNaviSuperManager;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.SensorManagerHelper;
import com.lingju.assistant.service.VoiceMediator;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.TingPlayerImageView;
import com.lingju.audio.engine.IflyRecognizer;
import com.lingju.common.log.Log;
import com.lingju.model.Accounting;
import com.lingju.model.AlarmClock;
import com.lingju.model.Memo;
import com.lingju.model.PlayMusic;
import com.lingju.model.dao.AssistDao;
import com.lingju.model.dao.CallAndSmsDao;
import com.lingju.model.dao.TapeEntityDao;
import com.lingju.model.temp.speech.SpeechMsg;
import com.lingju.robot.AndroidChatRobotBuilder;
import com.lingju.util.ScreenUtil;
import com.lingju.util.XmlyManager;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/4.
 */
public class MainActivity extends BaseActivity implements IAdditionAssist.AssistView {

    private final static String TAG = "MainActivity";
    public final static String RESULT_CODE = "resultcode";
    public static final int FOR_INTRODUCE = 80;
    public final static int FOR_RING = 81;

    @BindView(R.id.index_text_editext)
    EditText editText;
    @BindView(R.id.index_bottom_box)
    FrameLayout bottomBox;
    @BindView(R.id.index_body)
    FrameLayout body;
    @BindView(R.id.index_chat_list_box)
    FrameLayout bodyChatList;
    @BindView(R.id.main_content_box)
    FrameLayout mMainContentBox;
    @BindView(R.id.index_text_input_box)
    LinearLayout mIndexTextInputBox;
    @BindView(R.id.fl_guide)
    FrameLayout mFlGuide;

    IVoiceInput.VoiceInputView voiceInputFragment;

    IVoiceInput.VoicePresenter voiceInputPresenter;
    IChatList.ChatListView chatListFragment;
    IntroduceFragment mIntroduceFragment;

    IChatList.Presenter chatListPresenter;
    IAdditionAssist.Presenter assistPresenter;
    PlayerHeaderFragment mPlayerHeaderFragment;
    PlayerListPagerFragment mPlayerListPagerFragment;
    private BaiduNaviSuperManager naviSuperManager;
    private boolean isVisiable = true;      //当前页面是否可见

    @BindView(R.id.tb_lingju)
    Toolbar mTbLingju;
    @BindView(R.id.nv_left_menu)
    NavigationView mNvLeftMenu;
    @BindView(R.id.dl_slidingmenu)
    DrawerLayout mDlSlidingmenu;
    @BindView(R.id.tiv_track)
    TingPlayerImageView mTivTrack;
    private CallAndSmsPresenter mCallPresenter;
    private ExitTask mExitTask;
    private Fragment guideFragment;
    private CommonDialog mCommonDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("LingJu", "MainActivity onCreate()");
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();
        assistPresenter.subscribe();
        SensorManagerHelper sensorHelper = new SensorManagerHelper(this.getApplicationContext());
        /* 设置摇晃监听 */
        sensorHelper.setOnShakeListener(new SensorManagerHelper.OnShakeListener() {

            @Override
            public boolean onShake() {
                return mCallPresenter.shakeHandle();
            }
        });
        /* 设置根视图布局变化监听 */
        /*mMainContentBox.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = mMainContentBox.getRootView().getHeight() - mMainContentBox.getHeight();
                showKeyboard = heightDiff > ScreenUtil.getInstance().dip2px(200);
            }
        });*/
    }

    private void init() {
        ScreenUtil.getInstance().init(this);
        //每次进入应用时重置该标记的值，保证录音波纹正常显示
        AppConfig.dPreferences.edit().putBoolean("wave_show", true).commit();
        voiceInputFragment = (IVoiceInput.VoiceInputView) getSupportFragmentManager().findFragmentById(R.id.index_voiceinput_box);
        chatListFragment = (IChatList.ChatListView) getSupportFragmentManager().findFragmentById(R.id.index_chat_list_box);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (voiceInputFragment == null) {
            voiceInputFragment = new VoiceInputFragment();
            transaction.add(R.id.index_voiceinput_box, (VoiceInputFragment) voiceInputFragment);
            mIndexTextInputBox.setVisibility(View.GONE);
        }
        if (chatListFragment == null) {
            chatListFragment = new ChatListFragment();
            transaction.add(R.id.index_chat_list_box, (ChatListFragment) chatListFragment);
        }
        transaction.commit();
        voiceInputPresenter = new VoiceInputPresenter(voiceInputFragment);
        chatListPresenter = new ChatListPresenter(chatListFragment, this);
        assistPresenter = new AssistPresenter(this);
        mCallPresenter = new CallAndSmsPresenter(this);

        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    scroll2LastPosition();
                }
                return false;
            }
        });
        mTivTrack.setVisibility(View.GONE);
        /* 设置支持toolbar */
        setSupportActionBar(mTbLingju);
        /*if (getSupportActionBar() != null) {
            //左上角加上一个返回图标
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            //设置左上角图标是否可点击
            getSupportActionBar().setHomeButtonEnabled(true);
        }*/
        /* 抽屉切换监听器 */
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDlSlidingmenu, mTbLingju, R.string.menu_open, R.string.menu_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                //侧边栏滑动，键盘收起
                switchKeyboard(false);
                voiceInputFragment.setMicButtonState(RecordUpdateEvent.RECORD_IDLE);
                super.onDrawerSlide(drawerView, slideOffset);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                mCallPresenter.stopRecognize();
                super.onDrawerOpened(drawerView);
            }
        };
        drawerToggle.syncState();
        /* 设置抽屉开关监听器 */
        mDlSlidingmenu.addDrawerListener(drawerToggle);
        mTbLingju.setNavigationIcon(R.drawable.menu);
        //让item中的图标显示原来的颜色
        mNvLeftMenu.setItemIconTintList(null);
        /* 隐藏侧边栏滑动条 */
        mNvLeftMenu.getChildAt(0).setVerticalScrollBarEnabled(false);
        /* 设置侧边栏item点击监听 */
        mNvLeftMenu.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                /* 关闭侧边栏 */
                mDlSlidingmenu.closeDrawers();
                /* 跳转页面 */
                Intent intent = new Intent();
                switch (item.getItemId()) {
                    case R.id.drawer_navi:
                        intent.setClass(MainActivity.this, NaviSetPointActivity.class);
                        break;
                    case R.id.drawer_account:
                        intent.setClass(MainActivity.this, AccountingActivity.class);
                        break;
                    case R.id.drawer_remind:
                        intent.setClass(MainActivity.this, RemindActivity.class);
                        break;
                    case R.id.drawer_memo:
                        intent.setClass(MainActivity.this, MemoActivity.class);
                        break;
                    case R.id.drawer_alarm:
                        intent.setClass(MainActivity.this, AlarmActivity.class);
                        break;
                    case R.id.drawer_music:
                        /* 显示音乐列表 */
                        setPlayerListPager(true);
                        return false;
                    case R.id.drawer_ting:
                        intent.setClass(MainActivity.this, TingAudioActivity.class);
                        break;
                    case R.id.drawer_setting:
                        intent.setClass(MainActivity.this, SettingActivity.class);
                        break;
                }
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this*//*, item.getActionView(), "tools"*//*);
                    startActivity(intent, options.toBundle());
                } else {*/
                startActivity(intent);
                overridePendingTransition(R.anim.activity_start_in, R.anim.activity_start_out);
                // }
                return false;
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMsg2Robot();
                }
                return true;
            }
        });
        if (!AppConfig.NewInstallFirstOpen && AndroidChatRobotBuilder.get() != null && AndroidChatRobotBuilder.get().robot() != null) {
            Intent intent = new Intent(this, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SYNC_CALL_SMS);
            startService(intent);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateBottomBoxState(BottomBoxStateEvent event) {
        if (event.isShow()) {
            bottomBox.setVisibility(View.VISIBLE);
            // voiceInputFragment.switchView(true);
        } else {
            bottomBox.setVisibility(View.GONE);

        }
    }

    /**
     * 当Activity启动模式不为standard且Activity实例已存在，启动该Activity时调用
     **/
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            if (intent.getAction() != null && intent.getAction().equalsIgnoreCase(Intent.ACTION_VOICE_COMMAND)) {
                // TODO: 2016/12/1 蓝牙耳机操作处理
            } else {
                switch (intent.getIntExtra(RESULT_CODE, 0)) {
                    case FOR_INTRODUCE:
                        setIntroduceList(true);
                        break;
                }
            }
        }
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart()");
        if (!AppConfig.dPreferences.getBoolean(GuideFragment.SHOWED_MENU_GUIDE, false)) {
            mTbLingju.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setGuidePager(GuideFragment.MENU_GUIDE);
                }
            }, 500);
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        isVisiable = true;
        VoiceMediator.create(this).setRemindDialogFlag(0);
        if (VoiceMediator.create(this).isWalkNavi()) {
            VoiceMediator.get().resumeMediaVolume();
        }
        if (LingjuAudioPlayer.create(this).isPlaying()) {
            body.postDelayed(new Runnable() {
                @Override
                public void run() {
                    addPlayerHeader(LingjuAudioPlayer.get().currentPlayMusic());
                }
            }, 200);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //昵称拨号、发短信处理过程
        mCallPresenter.updateData(data, requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        XmlyManager.get().callBack(requestCode, resultCode, data);
    }

    @OnClick(R.id.index_switch_voice_bt)
    void showVoiceInputBox() {
        VoiceMediator.get().setRemindDialogFlag(0);
        //话筒波纹效果展示
        AppConfig.dPreferences.edit().putBoolean("wave_show", true).commit();
        //键盘输入界面切换为话筒界面如果正在识别时打开波纹效果
        if (IflyRecognizer.getInstance().isRecognizing()) {
            EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORDING));
        }
        if (voiceInputFragment != null) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            /* 显示话筒输入视图 */
            getSupportFragmentManager().beginTransaction().show((VoiceInputFragment) voiceInputFragment).commit();
            /* 处理键盘输入视图 */
            switchKeyboard(false);
            mIndexTextInputBox.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.index_text_send_bt)
    void sendMsg2Robot() {
        String input = editText.getText().toString().trim();
        if (!TextUtils.isEmpty(input)) {
            //1.将输入文本添加到聊天视图
            chatListFragment.addMessage(new ChatMsgEvent(new SpeechMsg(input, AssistantService.INPUT_KEYBOARD), null, null, null));
            //2.发送到AssistantService的robot处理
            Intent intent = new Intent(this, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT);
            intent.putExtra(AssistantService.TEXT, input);
            intent.putExtra(AssistantService.INPUT_TYPE, AssistantService.INPUT_KEYBOARD);
            startService(intent);
            //3.清空输入框内容
            editText.setText("");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void addPlayerHeader(PlayMusic m) {
        if (isVisiable) {
            if (mPlayerHeaderFragment == null) {
                //键盘收起
                switchKeyboard(false);
                getSupportFragmentManager().beginTransaction().add(R.id.fl_music_menu, (mPlayerHeaderFragment = new PlayerHeaderFragment())).commit();
                LingjuAudioPlayer.get().addHeader(mPlayerHeaderFragment);
                if (!AppConfig.dPreferences.getBoolean(GuideFragment.SHOWED_MUSIC_GUIDE, false))
                    setGuidePager(GuideFragment.MUSIC_GUIDE);
            } else if (!mPlayerHeaderFragment.isVisible()) {
                getSupportFragmentManager().beginTransaction().show(mPlayerHeaderFragment).commit();
            }
        }
    }

    /**
     * 判断activity是否当前可见
     */

    private boolean isForeground() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            if (MainActivity.class.getName().equals(cpn.getClassName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * 显示/隐藏输入键盘
     **/
    public void switchKeyboard(boolean isShow) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (isShow) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            mIndexTextInputBox.setVisibility(View.VISIBLE);
            editText.requestFocus();
            imm.showSoftInput(editText, 0);
            scroll2LastPosition();
        } else/* if (showKeyboard)*/ {
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }

    public void setIntroduceList(boolean showIntroduce) {
        if (showIntroduce) {
            if (mIntroduceFragment == null) {
                getSupportFragmentManager().beginTransaction().add(R.id.index_body, (mIntroduceFragment = new IntroduceFragment())).commit();
            } else {
                getSupportFragmentManager().beginTransaction().show(mIntroduceFragment).commit();
            }

        } else if (mIntroduceFragment != null) {
            getSupportFragmentManager().beginTransaction().hide(mIntroduceFragment).commit();
        }
    }

    public void setPlayerListPager(boolean show) {
        if (show) {
            /* 隐藏话筒 */
            updateBottomBoxState(new BottomBoxStateEvent(false));
            //话筒波纹效果不展示
            AppConfig.dPreferences.edit().putBoolean("wave_show", false).commit();
            if (mPlayerListPagerFragment == null) {
                getSupportFragmentManager().beginTransaction().add(R.id.index_body, (mPlayerListPagerFragment = new PlayerListPagerFragment())).commit();
                LingjuAudioPlayer.get().addBody(mPlayerListPagerFragment);
            } else {
                getSupportFragmentManager().beginTransaction().show(mPlayerListPagerFragment).commit();
                mPlayerListPagerFragment.updateFavorte();
            }

        } else if (mPlayerHeaderFragment != null) {
            //话筒波纹效果展示
            AppConfig.dPreferences.edit().putBoolean("wave_show", true).commit();
            //键盘输入界面切换为话筒界面如果正在识别时打开波纹效果
            if (IflyRecognizer.getInstance().isRecognizing()) {
                EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORDING));
            }
            getSupportFragmentManager().beginTransaction().hide(mPlayerListPagerFragment).commit();
            /* 显示话筒 */
            updateBottomBoxState(new BottomBoxStateEvent(true));
        }
    }

    public void setGuidePager(int type) {
        setGuidePager(type, null);
    }

    /**
     * 显示引导页
     **/
    public void setGuidePager(int type, String text) {
        mFlGuide.setVisibility(View.VISIBLE);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Bundle arguments;
        if (guideFragment == null) {
            transaction.add(R.id.fl_guide, guideFragment = new GuideFragment());
            arguments = new Bundle();
            arguments.putInt(GuideFragment.SHOW_TYPE, type);
            arguments.putString(GuideFragment.SHOW_TEXT, text);
            guideFragment.setArguments(arguments);
        } else {
            arguments = guideFragment.getArguments();
            arguments.putInt(GuideFragment.SHOW_TYPE, type);
            arguments.putString(GuideFragment.SHOW_TEXT, text);
            transaction.show(guideFragment);
        }
        transaction.commitAllowingStateLoss();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        quitLongRecordMode();
        VoiceMediator.get().setRemindDialogFlag(1);
        super.onStop();
        isVisiable = false;
    }

    /**
     * 退出长录音模式，并处理编辑中的备忘卡片
     **/
    public void quitLongRecordMode() {
        chatListFragment.handleMemoCard();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        if (mExitTask != null) {
            mExitTask.cancel(true);
            mExitTask = null;
        }
        // TODO: 2017/4/1 停止音乐播放并关闭音乐通知栏（不确定）
        if (LingjuAudioPlayer.get().isPlaying())
            LingjuAudioPlayer.get().pause();
        LingjuAudioPlayer.get().closeNotification();
        if (XmPlayerManager.getInstance(this).isPlaying())
            XmPlayerManager.getInstance(this).pause();
        if (assistPresenter != null)
            assistPresenter.unsubscribe();
        if (mCallPresenter != null)
            mCallPresenter.unRegisterReceiver();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mPlayerListPagerFragment != null && mPlayerListPagerFragment.isVisible()) {      //隐藏音乐播放列表
            mPlayerListPagerFragment.disappear();
            return;
        }
        exit++;
        if (exit == 1) {
            mExitTask = new ExitTask();
            mExitTask.execute();
            return;
        } else {
            exit = 0;
            clearRecyleData();
            //关闭唤醒
            if (VoiceMediator.get().isWakeUpMode())
                VoiceMediator.get().setWakeUpMode(false);
            //关闭任务流,并关闭语音合成、识别
            Intent intent = new Intent(this, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT_FOR_END_TASK);
            intent.putExtra(AssistantService.TRY_TO_WAKEUP, false);
            startService(intent);
            finish();
        }
        super.onBackPressed();
    }

    /**
     * 清空数据库中设置回收标记的记录
     **/
    private void clearRecyleData() {
        Single.just(0)
                .observeOn(Schedulers.io())
                .doOnSuccess(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        AssistDao.getInstance().clearRecyleData();
                        TapeEntityDao.getInstance().clearRecyleData();
                        CallAndSmsDao.getInstance(MainActivity.this).clearRecyleData();
                    }
                })
                .subscribe();
    }

    private int exit;

    /**
     * 退出应用任务
     **/
    class ExitTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            mCallPresenter.stopRecognize();
            Snackbar.make(bottomBox, VoiceMediator.get().isWakeUpMode() ? R.string.press_to_exit2 : R.string.press_to_exit, Snackbar.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                //打断睡眠中的线程
                Thread.currentThread().interrupt();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            exit = 0;
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDialogEvent(DialogEvent event) {
        switch (event.getEventType()) {
            case DialogEvent.CANCEL_TOGGLE_TYPE:
                assistPresenter.cancelToggleDialog();
                break;
            case DialogEvent.SHOW_WALK_TYPE:
                mCommonDialog = new CommonDialog(this, "距离较近，是否用步行导航", "选择“是”则跳转到百度地图应用", "否", "是")
                        .setOnCancelListener(new CommonDialog.OnCancelListener() {
                            @Override
                            public void onCancel() {
                                sendMessageToRobot("否");
                            }
                        })
                        .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                            @Override
                            public void onConfirm() {
                                sendMessageToRobot("是");
                            }
                        });
                mCommonDialog.show();
                break;
            case DialogEvent.CANCEL_WALK_TYPE:
                if (mCommonDialog != null) {
                    mCommonDialog.dismiss();
                    mCommonDialog = null;
                }
                break;
        }
    }

    /**
     * 发送文本到后台机器人
     **/
    private void sendMessageToRobot(String text) {
        Intent intent = new Intent(this, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT);
        intent.putExtra(AssistantService.TEXT, text);
        startService(intent);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTrackPlayEvent(TrackPlayEvent e) {
        chatListFragment.refreshTingPlayView();
        mTivTrack.setVisibility(View.VISIBLE);
        if (e.isPlaying()) {
            mTivTrack.setImage(this, ((Track) e.getPlayTrack()).getCoverUrlMiddle());
            mTivTrack.startAnim();
        } else {
            mTivTrack.stopAnim();
        }
    }

    @OnClick(R.id.tiv_track)
    public void toggleTingPlay() {
        if (XmlyManager.get().isPlaying()) {
            XmlyManager.get().getPlayer().pause();
        } else {
            XmlyManager.get().getPlayer().play();
        }
    }

    /**
     * 闹钟、提醒等指令处理
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    @Override
    public void updateDialogState(RobotResponseEvent e) {
        if (e != null)
            assistPresenter.onAdditionResponse(e.getCmd(), e.getText(), e.getType());
    }

    /**
     * 电话、短信指令处理
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallEvent(CallAndSmsEvent event) {
        mCallPresenter.onCallResponse(event.getText(), event.getCmd(), event.getType());
    }

    @Override
    public String getInput() {
        return chatListPresenter.getInputText();
    }

    /**
     * 由于手动结束新建任务，需要重置编辑标记
     **/
    public void resetEditState(Class cla) {
        if (Memo.class.getSimpleName().equals(cla.getSimpleName())) {
            assistPresenter.setMemoEditState(false);
        } else if (AlarmClock.class.getSimpleName().equals(cla.getSimpleName())) {
            assistPresenter.setAlarmEditState(false);
        } else if (Accounting.class.getSimpleName().equals(cla.getSimpleName())) {
            assistPresenter.setAccountEditState(false);
        } else {
            assistPresenter.setRemindEditState(false);
        }
    }

    /**
     * 初始化导航引擎
     **/
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onInitNaviEvent(InitNaviManagerEvent e) {
        Log.i(TAG, "NaviShowPointsEvent");
        if (naviSuperManager == null)
            naviSuperManager = new BaiduNaviSuperManager(this, new BaiduNaviManager.NaviInitListener() {
                @Override
                public void onAuthResult(int var1, String var2) {

                }

                @Override
                public void initStart() {
                    Log.i(TAG, "initStart");
                }

                @Override
                public void initSuccess() {
                    Log.i(TAG, "initSuccess");
                }

                @Override
                public void initFailed() {
                    Log.i(TAG, "initFailed");
                }
            }, null, false);
    }

    /**
     * 计算当前文本距离页面头部高度
     **/
    public int getGuideTextMargin(int type) {
        int margin;
        if (type == GuideFragment.MENU_GUIDE) {
            margin = mTbLingju.getHeight();
        } else {
            margin = ScreenUtil.getInstance().dip2px(72);
        }
        return margin;
    }

    public void closeSlidingMenu() {
        /* 关闭侧边栏 */
        if (mDlSlidingmenu.isDrawerOpen(mNvLeftMenu))
            mDlSlidingmenu.closeDrawers();
    }

    public void openSlidingMenu() {
        /* 滑出侧边栏 */
        if (!mDlSlidingmenu.isDrawerOpen(mNvLeftMenu))
            mDlSlidingmenu.openDrawer(mNvLeftMenu);
    }

    /**
     * 通过键盘输入块是否可见来判断录音波纹是否显示
     **/
    public boolean voiceWaveable() {
        return mIndexTextInputBox.getVisibility() == View.GONE;
    }

    public void scroll2LastPosition() {
        chatListFragment.scrollToLastPosition();
    }

    /**
     * 发送短信
     **/
    public void sendSms(String number, String content, boolean immediately) {
        mCallPresenter.manualSendSms(number, content, immediately);
    }

    /**
     * 取消短信发送
     **/
    public void cancelSms() {
        mCallPresenter.cancelCommunicationTimer();
    }

    @Override
    public void setPresenter(IAdditionAssist.Presenter presenter) {
    }

    /**
     * 当activity失去窗口焦点时调用，用于处理系统通知栏下拉后回调
     * 注意：通知栏下拉不会触发onPause()
     **/
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && VoiceMediator.get() != null && !VoiceMediator.get().isFocus()) {
            Intent intent = new Intent(this, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.GET_HOOK);
            startService(intent);
        }
    }
}
