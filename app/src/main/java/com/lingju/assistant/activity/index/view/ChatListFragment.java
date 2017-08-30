package com.lingju.assistant.activity.index.view;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.MainActivity;
import com.lingju.assistant.activity.event.AlarmStateEvent;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.LongRecognizeEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.activity.event.UpdateTaskCardEvent;
import com.lingju.assistant.activity.index.IChatList;
import com.lingju.assistant.activity.index.model.ChatListAdapter;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.player.event.UpdateWaittingSeekBarEvent;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.RspMsgItemView;
import com.lingju.audio.engine.IflyRecognizer;
import com.lingju.common.log.Log;
import com.lingju.model.AlarmClock;
import com.lingju.model.Memo;
import com.lingju.model.dao.AssistDao;
import com.lingju.model.dao.AssistEntityDao;
import com.lingju.model.temp.speech.SpeechMsg;
import com.lingju.util.SoftKeyboardStateHelper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2016/11/4.
 */
public class ChatListFragment extends Fragment implements IChatList.ChatListView {
    private final static String TAG = "ChatListFragment";

    private View rootView;
    @BindView(R.id.chat_list)
    RecyclerView chatListView;
    public IChatList.Presenter presenter;

    LinearLayoutManager linearLayoutManager;
    private ChatListAdapter chatListApdater;
    public static final String[] SAVE_KEYWORDS = {"保存", "创建", "添加"};
    public static final String[] QUIT_KEYWORDS = {"取消", "退出"};   //退出长时间录音关键词
    private Timer waitActionTimer;      //电话、短信任务倒计时定时器
    private boolean scrollable = true;      //是否需要重置（即保证最新一次输入置顶）聊天视图位置标记
    private int scrollPosition = -1;
    private boolean moveable = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.frag_chat_list, container, false);
        //监听屏幕中的键盘是否打开，如果打开则限制话筒波纹效果的显示
        SoftKeyboardStateHelper softKeyboardStateHelper = new SoftKeyboardStateHelper(rootView);
        softKeyboardStateHelper.addSoftKeyboardStateListener(new SoftKeyboardStateHelper.SoftKeyboardStateListener() {
            @Override
            public void onSoftKeyboardOpened(int keyboardHeightInPx) {
                //话筒波纹效果不展示
                AppConfig.dPreferences.edit().putBoolean("wave_show", false).commit();
            }

            @Override
            public void onSoftKeyboardClosed() {
                //话筒波纹效果展示
                if (getActivity() != null && ((MainActivity) getActivity()).voiceWaveable())
                    AppConfig.dPreferences.edit().putBoolean("wave_show", true).commit();
            }
        });
        ButterKnife.bind(this, rootView);
        chatListView.setHasFixedSize(true);
        chatListView.setLayoutManager((linearLayoutManager = new LinearLayoutManager(getActivity())));
        chatListView.setItemAnimator(new DefaultItemAnimator());
        ((SimpleItemAnimator)chatListView.getItemAnimator()).setSupportsChangeAnimations(false);
        chatListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    scrollable = false;
                    reset();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (moveable) {
                        moveable = false;   //必须放在移动方法的上面
                        moveToPosition(scrollPosition);
                        scrollPosition = -1;
                    }
                }
            }
        });
       /* chatListView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom != oldBottom) {
                    scrollable = true;
                    resetChatLocation();
                }
            }
        });*/
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        chatListApdater = new ChatListAdapter(this);
        chatListView.setAdapter(chatListApdater);
        presenter.subscribe();
        presenter.showOpenTips();
        LingjuAudioPlayer.create(getContext()).requestAudioFocus();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LingjuAudioPlayer.get().abandonAudioFocus();
        presenter.unsubscribe();
    }

    /**
     * 添加聊天信息
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    @Override
    public void addMessage(final ChatMsgEvent msg) {
        scrollable = true;
        if (msg.textMsg != null) {
            chatListApdater.setClickable(false);
            if (!TextUtils.isEmpty(msg.textMsg.text)) {
                presenter.setInputText(msg.textMsg);
                chatListApdater.addData(msg.textMsg);
            }
        } else if (msg.taskCard != null) {
            reset();
            chatListApdater.addData(msg.taskCard);
        } else if (msg.sysTips != null) {
            chatListApdater.addData(msg.sysTips);
        } else if (msg.memo != null) {
            chatListApdater.addData(msg.memo);
        } else if (msg.refresh == ChatMsgEvent.REMOVE_CARD_STATE) {
            scrollable = false;
            int position = chatListApdater.getLastCardPosition(msg.clazz);
            if (position != -1)
                chatListApdater.removeItemView(position);
        } else if (msg.refresh == ChatMsgEvent.REMOVE_LIST_STATE) {
            scrollable = false;
            int cardListPosition = chatListApdater.getLastCardListPosition(msg.clazz);
            if (cardListPosition != -1)
                chatListApdater.removeItemView(cardListPosition);
        } else if (msg.refresh == ChatMsgEvent.UPDATE_CALL_SMS_STATE) {
            scrollable = false;
            int unFinishPos = chatListApdater.getLastUnFinishPos();
            if (unFinishPos != -1) {
                cancelWaitActionTimer();
                chatListApdater.removeItemView(unFinishPos);
            }
        } else if (msg.callAndSmsMsg != null) {
            chatListApdater.setClickable(true);
            chatListApdater.addData(msg.callAndSmsMsg);
        } else if (msg.refresh == ChatMsgEvent.REMOVE_TING_ALBUM_STATE) {
            int lastAlbumViewPos = chatListApdater.getLastAlbumViewPos();
            if (lastAlbumViewPos != -1) {
                cancelWaitActionTimer();
                chatListApdater.removeItemView(lastAlbumViewPos);
            }
        } else if (msg.refresh == ChatMsgEvent.REMOVE_TING_TRACK_STATE) {
            int lastTrackViewPos = chatListApdater.getLastTrackViewPos();
            cancelWaitActionTimer();
            if (lastTrackViewPos != -1)
                chatListApdater.removeItemView(lastTrackViewPos);
        } else if (msg.tingMsg != null) {
            chatListApdater.addData(msg.tingMsg);
        } else {
            /* 移除添加模式视图 */
            scrollable = false;
            chatListApdater.removeItemView(chatListApdater.getItemCount() - 2);
        }
        if (scrollable)
            scrollToLastPosition();
    }

    @Override
    public void scrollToLastPosition() {
        chatListView.postDelayed(new Runnable() {
            @Override
            public void run() {
                chatListView.smoothScrollToPosition(chatListApdater.getItemCount() - 1);
            }
        }, 200);
    }

    private void reset() {
        mRootView = null;
        mEtContent = null;
    }

    @Override
    public void moveToPosition(int position) {
        if (position != -1/* && scrollable*/) {
            moveable = false;
            //先获取第一个和最后一个可见item索引
            int first = linearLayoutManager.findFirstVisibleItemPosition();
            int last = linearLayoutManager.findLastVisibleItemPosition();
            Log.i("LingJu", "滑动索引：" + position + "首个可见索引：" + first + " 末尾可见索引：" + last);
            if (position < first) {   //要移动的item索引在第一个可见索引之前
                chatListView.smoothScrollToPosition(position);
            } else if (position <= last) {      //要移动的item处于可见
                // int top = chatListView.getChildAt(position - first).getTop();
                int top = linearLayoutManager.getHeight() - linearLayoutManager.findViewByPosition(position).getHeight();
                chatListView.smoothScrollBy(0, top);
            } else {     //要移动的item在最后可见item之后
                scrollPosition = position;
                chatListView.smoothScrollToPosition(position);
                moveable = true;
            }
        }
    }

    @Override
    public void showSynthErrorDialog() {
        String tips = "原因一：麦克风被占用，请关闭占用麦克风的程序，试试清空后台程序<br /><br />原因二：我使用麦克风的权限被禁止了，请设置";
        new CommonDialog(getActivity(), "录音失败，连续听不清的原因", Html.fromHtml(tips).toString(), "权限设置", "明白了")
                .setOnCancelListener(new CommonDialog.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.fromParts("package", getActivity().getPackageName(), null));
                        startActivity(intent);
                    }
                })
                .show();
    }

    /**
     * 重新排布对话位置
     **/
 /*   @Override
    public void resetChatLocation() {
        View emptyView = linearLayoutManager.findViewByPosition(chatListApdater.getItemCount() - 1);
        // Log.i("LingJu", "ChatListFragment resetChatLocation()>>>" + emptyView);
        if (emptyView != null) {
            chatListApdater.resetHeight(emptyView);
        } else {
            moveToPosition(chatListApdater.getLastInputPosition());
        }
    }*/
    @Override
    public int getLastInputHeight() {
        /*int lastInputPosition = chatListApdater.getLastInputPosition();
        if (lastInputPosition == -1) {
            return chatListApdater.getItemsHeight(0, chatListApdater.getLastRespViewPosition() - 1);
        }
        View inputView = linearLayoutManager.findViewByPosition(lastInputPosition);
        if (inputView != null) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) inputView.getLayoutParams();
            return inputView.getHeight() + inputView.getPaddingBottom() + inputView.getPaddingTop() + layoutParams.topMargin + layoutParams.bottomMargin;
        }
        return 0;*/
        return chatListApdater.getItemsHeight(0, chatListApdater.getLastRespViewPosition() - 1);
    }

    /**
     * 合成事件响应
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSynthesizeEvent(SynthesizeEvent event) {
        switch (event.getState()) {
            case SynthesizeEvent.SYNTH_START:
                chatListView.post(new Runnable() {
                    @Override
                    public void run() {
                        int respViewPosition = chatListApdater.getLastRespViewPosition();
                        if (respViewPosition != -1) {
                            View itemView = linearLayoutManager.findViewByPosition(respViewPosition);
                            if (itemView != null) {
                                RspMsgItemView resp = (RspMsgItemView) itemView.findViewById(R.id.resp_view);
                                startSpeakerAnimation(resp);
                            }
                        }
                    }
                });
                break;
            case SynthesizeEvent.SYNTH_END:
                stopSpeakerAnimation();
                break;
            default:
                showSynthErrorDialog();
                break;
        }
    }

    /**
     * 刷新闹钟状态视图事件处理
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAlarmSwitchEvent(AlarmStateEvent e) {
        List<AlarmClock> alarms = e.getAlarms();
        List<TaskCard<AlarmClock>> cardList = chatListApdater.getCards(AlarmClock.class);
        List<TaskCard> taskList = chatListApdater.getTaskList(AlarmClock.class);
        for (AlarmClock clock : alarms) {
            for (TaskCard<AlarmClock> card : cardList) {
                if (card.t.getId() == clock.getId()) {
                    card.t.setValid(clock.getValid());
                }
            }
            for (TaskCard card : taskList) {
                if (((AlarmClock) card.t).getId() == clock.getId())
                    ((AlarmClock) card.t).setValid(clock.getValid());
            }
        }
        chatListApdater.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSeekBarUpdateEvent(UpdateWaittingSeekBarEvent e) {
        View itemView = chatListApdater.getLastHolder().itemView;
        if (e.isPhone()) {      //电话、短信倒计时
            if (itemView != null) {
                SeekBar sb = (SeekBar) itemView.findViewById(R.id.sb_confirm);
                if (sb != null) {
                    cancelWaitActionTimer();
                    waitActionTimer = new Timer();
                    waitActionTimer.schedule(new WaitActionTimerTask(sb), 250, 250);
                }
            }
        } else {     //有声内容播放倒计时
            Log.i("LingJu", "onSeekBarUpdateEvent()>>> " + itemView);
            if (itemView != null) {
                RecyclerView albumListView = (RecyclerView) itemView.findViewById(R.id.rv_assistant);
                RecyclerView.LayoutManager layoutManager = albumListView.getLayoutManager();
                View footerView = layoutManager.findViewByPosition(layoutManager.getItemCount() - 1);
                if (footerView != null) {
                    TextView tvCountDown = (TextView) footerView.findViewById(R.id.tv_count_down);
                    TextView tv = (TextView) footerView.findViewById(R.id.tv_play_desc);
                    tvCountDown.setVisibility(View.VISIBLE);
                    tv.setVisibility(View.VISIBLE);
                    cancelWaitActionTimer();
                    waitActionTimer = new Timer();
                    waitActionTimer.schedule(new TingPlayTimerTask(tvCountDown, 5), 0, 1000);
                }
            }
        }

    }

    /**
     * 拨号、发短信确认倒计时任务
     **/
    private class WaitActionTimerTask extends TimerTask {
        private SeekBar seekBar;
        private int percent;

        public WaitActionTimerTask(SeekBar seekBar) {
            this.seekBar = seekBar;
            percent = 0;
        }

        @Override
        public void run() {
            percent += 5;
            if (percent > 100) {
                cancelWaitActionTimer();
            } else {
                seekBar.setProgress(percent);
            }
        }
    }

    class TingPlayTimerTask extends TimerTask {
        private TextView textView;
        private int countdown;

        public TingPlayTimerTask(TextView tv, int countdown) {
            this.textView = tv;
            this.countdown = countdown;
        }

        @Override
        public void run() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(String.valueOf(countdown));
                    // Log.i("LingJu", "TingPlayTimerTask run()>>> " + textView.getText().toString());
                    if (--countdown < 0)
                        cancelWaitActionTimer();
                }
            });
        }
    }

    public void cancelWaitActionTimer() {
        if (waitActionTimer != null) {
            waitActionTimer.cancel();
            waitActionTimer = null;
        }
    }

    private View mRootView;
    private EditText mEtContent;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLongRecordEvent(LongRecognizeEvent e) {
        String content = e.getResult();
        switch (IflyRecognizer.getInstance().getLong_record_mode()) {
            case IflyRecognizer.CREATE_MEMO_MODE:
                if (mRootView == null) {
                    mRootView = linearLayoutManager.findViewByPosition(chatListApdater.getLastCardPosition(Memo.class));
                }
                if (mEtContent == null) {
                    mEtContent = (EditText) mRootView.findViewById(R.id.edit_memo_content);
                }
                if (content.length() < 6 && (content.startsWith(SAVE_KEYWORDS[0]) || content.startsWith(SAVE_KEYWORDS[1]) || content.startsWith(SAVE_KEYWORDS[2]))) {
                    String memo = mEtContent.getText().toString().trim();
                    if (TextUtils.isEmpty(memo)) {  //备忘内容为空不能创建
                        new CommonDialog(getActivity(), "编辑备忘", "您还没输入备忘内容哦", "知道了").show();
                        return;
                    }
                    addMessage(new ChatMsgEvent(new SpeechMsg(content), null, null, null));
                    sendMessageToRobot(memo);
                    return;
                } else if (content.length() < 6 && (content.startsWith(QUIT_KEYWORDS[0]) || content.startsWith(QUIT_KEYWORDS[1]))) {
                    addMessage(new ChatMsgEvent(new SpeechMsg(content), null, null, null));
                    sendMessageToRobot(content);
                    return;
                }
                mEtContent.getText().insert(mEtContent.getSelectionStart(), content);
                mEtContent.setSelection(mEtContent.length());
                break;
            case IflyRecognizer.MODIFY_MEMO_MODE:
                if (mRootView == null)
                    mRootView = linearLayoutManager.findViewByPosition(chatListApdater.getItemCount() - 2);
                if (mEtContent == null)
                    mEtContent = (EditText) mRootView.findViewById(R.id.edit_memo_content);

                if (content.length() < 6 && (content.startsWith(SAVE_KEYWORDS[0]) || content.startsWith(SAVE_KEYWORDS[1]) || content.startsWith(SAVE_KEYWORDS[2]))) {
                    Memo appendMemo = chatListApdater.getAppendMemo();
                    String memo = mEtContent.getText().toString().trim();
                    if (TextUtils.isEmpty(memo)) {  //备忘内容为空不能保存
                        new CommonDialog(getActivity(), "编辑备忘", "您还没输入备忘内容哦", "知道了").show();
                        return;
                    }
                    appendMemo.setContent(memo);
                    appendMemo.setModified(new Date());
                    appendMemo.setSynced(false);
                    AssistDao.getInstance().updateMemo(appendMemo);
                    //移除添加模式编辑视图
                    addMessage(new ChatMsgEvent());
                    //添加对话
                    addMessage(new ChatMsgEvent(new SpeechMsg(content), null, null, null));
                    //刷新聊天列表中任务卡的状态
                    refreshTaskCard(new UpdateTaskCardEvent<>(appendMemo, TaskCard.TaskState.INVALID));
                    //添加修改后的视图
                    addMessage(new ChatMsgEvent(null, new TaskCard<>(appendMemo, TaskCard.TaskState.ACTIVE), null, null));
                    sendMessageToRobot(SAVE_KEYWORDS[0]);
                    //同步备忘记录
                    final AssistEntityDao.MemoEntityDao memoEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.MemoEntityDao.class);
                    AssistEntityDao.create().sync(memoEntityDao);
                    return;
                } else if (content.length() < 6 && (content.startsWith(QUIT_KEYWORDS[0]) || content.startsWith(QUIT_KEYWORDS[1]))) {
                    //移除添加模式编辑视图
                    addMessage(new ChatMsgEvent());
                    //添加对话
                    addMessage(new ChatMsgEvent(new SpeechMsg(content), null, null, null));
                    sendMessageToRobot(QUIT_KEYWORDS[1]);
                }
                mEtContent.getText().insert(mEtContent.getSelectionStart(), content);
                mEtContent.setSelection(mEtContent.length());
                break;
        }

    }

    /**
     * 刷新任务卡状态事件响应
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    @Override
    public void refreshTaskCard(UpdateTaskCardEvent e) {
        List<TaskCard> taskCardList = chatListApdater.getTaskList(e.getUpdateClass());
        switch (e.isState()) {
            case TaskCard.TaskState.ACTIVE:
                TaskCard taskCard = chatListApdater.getCardById(e.getId(), e.getUpdateClass());
                if (taskCard != null && taskCard.taskState != TaskCard.TaskState.INVALID)
                    taskCard.taskState = TaskCard.TaskState.ACTIVE;
                for (TaskCard card : taskCardList) {
                    if (card.getId() == e.getId() && card.taskState != TaskCard.TaskState.INVALID) {
                        card.taskState = TaskCard.TaskState.ACTIVE;
                        break;
                    }
                }
                break;
            /*case TaskCard.TaskState.REVOCABLE:
                for (TaskCard card : taskCardList) {
                    if (card.getId() == e.getId()){
                        card.taskState = TaskCard.TaskState.REVOCABLE;
                        break;
                    }
                }
                break;*/
            case TaskCard.TaskState.DELETED:
                // 刷新任务卡状态
                List<TaskCard> cardList = chatListApdater.getCards(e.getUpdateClass());
                for (TaskCard card : cardList) {
                    if (card.taskState == TaskCard.TaskState.REVOCABLE)
                        card.taskState = TaskCard.TaskState.DELETED;
                    else if (card.getId() == e.getId() && card.taskState != TaskCard.TaskState.INVALID)
                        card.taskState = TaskCard.TaskState.REVOCABLE;
                }
                //刷新任务卡列表状态
                for (TaskCard card : taskCardList) {
                    if (card.taskState == TaskCard.TaskState.REVOCABLE)
                        card.taskState = TaskCard.TaskState.DELETED;
                    else if (card.getId() == e.getId() && card.taskState != TaskCard.TaskState.INVALID)
                        card.taskState = TaskCard.TaskState.REVOCABLE;
                }
                break;
            case TaskCard.TaskState.INVALID:
                if (e.isInvalidItem()) {
                    TaskCard lastCard = chatListApdater.getCardById(e.getId(), e.getUpdateClass());
                    if (lastCard != null)
                        lastCard.taskState = TaskCard.TaskState.INVALID;
                }
                if (e.isInvalidList()) {
                    for (TaskCard card : taskCardList) {
                        if (e.getId() == card.getId()) {
                            card.taskState = TaskCard.TaskState.INVALID;
                            break;
                        }
                    }
                }
                break;
        }
        chatListApdater.notifyDataSetChanged();
    }

    @Override
    public void refreshTingPlayView() {
        int trackViewPos = chatListApdater.getLastTrackViewPos();
        if (trackViewPos != -1)
            chatListApdater.notifyItemChanged(trackViewPos);
    }

    @Override
    public void startSpeakerAnimation(View v) {
        if (v instanceof RspMsgItemView) {
            chatListApdater.setSpeakPosition(((RspMsgItemView) v).getPosition());
            ((RspMsgItemView) v).startAnimation();
        }
    }

    @Override
    public void stopSpeakerAnimation(View v) {
        chatListApdater.setSpeakPosition(-1);
        if (v instanceof RspMsgItemView) {
            ((RspMsgItemView) v).stopAnimation();
        } else
            stopSpeakerAnimation();
    }

    @Override
    public void startSpeakerAnimation(int i) {

    }

    @Override
    public void stopSpeakerAnimation(int i) {

    }

    @Override
    public void stopSpeakerAnimation() {
        Log.i(TAG, "stopSpeakerAnimation");
        chatListApdater.setSpeakPosition(-1);
        List<View> viewList = getAllChatViews();
        for (View item : viewList) {
            if (item instanceof RspMsgItemView) {
                ((RspMsgItemView) item).stopAnimation();
            }
        }

    }

    @Override
    public void handleMemoCard() {
        chatListApdater.handleMemoCard();
    }

    /**
     * 向机器人发送语音信息
     **/
    private void sendMessageToRobot(String text) {
        Intent intent = new Intent(getActivity(), AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT);
        intent.putExtra(AssistantService.TEXT, text);
        intent.putExtra(AssistantService.INPUT_TYPE, AssistantService.INPUT_VOICE);
        getActivity().startService(intent);
    }

    /**
     * 获取所有聊天视图
     **/
    private List<View> getAllChatViews() {
        List<View> temps = new ArrayList<>();
        for (int j = 0; j < chatListApdater.getItemCount() - 1; j++) {
            temps.add(linearLayoutManager.findViewByPosition(j));
        }
        return temps;
    }

    /**
     * 获取列表布局管理器
     **/
    public RecyclerView.LayoutManager getLayoutManager() {
        return linearLayoutManager;
    }

    @Override
    public void setPresenter(IChatList.Presenter presenter) {
        this.presenter = presenter;
    }
}
