package com.lingju.assistant.activity.index.model;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.LevelListDrawable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.AccountingActivity;
import com.lingju.assistant.activity.MainActivity;
import com.lingju.assistant.activity.TingAlbumDetailActivity;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.RobotTipsEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.activity.event.UpdateTaskCardEvent;
import com.lingju.assistant.activity.index.view.ChatListFragment;
import com.lingju.assistant.entity.CallAndSmsMsg;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.entity.TingAlbumMsg;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.RemindService;
import com.lingju.assistant.service.process.MobileCommProcessor;
import com.lingju.assistant.service.process.TingPlayProcessor;
import com.lingju.assistant.view.AlarmFrDialog;
import com.lingju.assistant.view.AlarmItemDialog;
import com.lingju.assistant.view.ItemExpenseDialog;
import com.lingju.assistant.view.ItemIncomeDialog;
import com.lingju.assistant.view.MemoEditDialog;
import com.lingju.assistant.view.RemindFrDialog;
import com.lingju.assistant.view.ReqMsgItemView;
import com.lingju.assistant.view.RingListDialog;
import com.lingju.assistant.view.RspMsgItemView;
import com.lingju.assistant.view.SlidingItem;
import com.lingju.assistant.view.SwitchButton;
import com.lingju.audio.engine.IflyRecognizer;
import com.lingju.model.Accounting;
import com.lingju.model.AlarmClock;
import com.lingju.model.Memo;
import com.lingju.model.Remind;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;
import com.lingju.model.dao.AssistEntityDao;
import com.lingju.model.dao.TingAlbumDao;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.model.temp.speech.SpeechMsg;
import com.lingju.util.AssistUtils;
import com.lingju.util.ScreenUtil;
import com.lingju.util.StringUtils;
import com.lingju.util.TimeUtils;
import com.lingju.util.XmlyManager;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.LastUpTrack;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2016/12/8.
 */
public class ChatListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    /**
     * 用户输入文本视图
     **/
    private static final int REQ_VIEW = 0;
    /**
     * 机器人回复文本视图
     **/
    private static final int RESP_VIEW = 1;
    /**
     * 提醒卡片视图
     **/
    private static final int REMIND_CARD_VIEW = 2;
    /**
     * 备忘卡片视图
     **/
    private static final int MEMO_CARD_VIEW = 7;
    /**
     * 闹钟卡片
     **/
    private static final int ALARM_CARD_VIEW = 8;
    /**
     * 账单卡片
     **/
    private static final int ACCOUNTING_CARD_VIEW = 9;
    /**
     * 电话短信视图
     **/
    private static final int CALL_AND_SMS_VIEW = 10;
    /**
     * 系统提示视图
     **/
    private static final int SYS_TIPS_VIEW = 3;
    /**
     * 备忘添加模式视图
     **/
    private static final int MEMO_APPEND_VIEW = 4;
    /**
     * 任务记录列表视图
     **/
    private static final int CARD_LIST_VIEW = 5;
    /**
     * 空视图，用于填充列表可视区域，保证列表显示一次对话内容
     **/
    private static final int EMPTY_VIEW = 6;
    /**
     * 专辑列表视图
     **/
    private static final int TING_ALBUM_VIEW = 11;
    /**
     * 播放专辑视图
     **/
    private static final int TING_PLAY_VIEW = 12;
    /**
     * 聊天列表最大容量
     **/
    private static final int MAX_COUNT = 100;
    private List<Object> datas = new ArrayList<>(MAX_COUNT);
    private RecyclerView.ViewHolder mLastHolder;
    private Context mContext;
    private final LayoutInflater mInflater;
    private ChatListFragment mListFragment;
    private AssistDao mAssistDao;
    private int remindFr;     //提醒周期
    private int alarmFr;     //闹钟周期
    private String path;     //闹钟铃声路径
    private boolean repeat;     //是否循环闹钟标记
    private SlidingItem expandedItem;
    private int speakPosition = -1;      //正在说话的回复文本索引
    private boolean clickable = true;    //拨号、发短信任务卡中按钮是否可点击


    public ChatListAdapter(ChatListFragment fragment) {
        this.mContext = fragment.getActivity();
        this.mListFragment = fragment;
        this.mInflater = LayoutInflater.from(mContext);
        mAssistDao = AssistDao.getInstance();
    }

    /**
     * 添加数据并刷新视图
     **/
    public void addData(Object data) {
        addData(data, true);
    }

    /**
     * 添加数据并选择是否刷新视图
     **/
    public void addData(Object data, boolean isRefresh) {
        if (datas.size() >= MAX_COUNT)
            datas.remove(0);
        datas.add(data);
        if (isRefresh)
            notifyDataSetChanged();
    }

    public void resetHeight(View emptyView) {
        int chatHeight = 0;
        int inputPosition = getLastInputPosition();
        if (inputPosition >= 0) {
            chatHeight = getItemsHeight(inputPosition, getItemCount() - 2);
        }
        int visibleHeight = mListFragment.getLayoutManager().getHeight();
        int emptyHeight = visibleHeight - chatHeight;
        // Log.i("LingJu", "最新输入视图：" + inputPosition + " 空视图高度：( " + visibleHeight + " - " + chatHeight + " )= " + emptyHeight);
        ViewGroup.LayoutParams layoutParams = emptyView.getLayoutParams();
        layoutParams.height = emptyHeight <= 0 ? ScreenUtil.getInstance().dip2px(12) : emptyHeight;
        emptyView.setLayoutParams(layoutParams);
        mListFragment.moveToPosition(inputPosition);
    }

    /**
     * 获取指定区域item的高度
     **/
    public int getItemsHeight(int start, int end) {
        int height = 0;
        if (start > end || end > datas.size() - 1) {
            return height;
        }
        start = start < 0 ? 0 : start;
        RecyclerView.LayoutManager layoutManager = mListFragment.getLayoutManager();
        for (int i = start; i <= end; i++) {
            View item = layoutManager.findViewByPosition(i);
            if (item != null) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) item.getLayoutParams();
                height += item.getHeight() + item.getPaddingBottom() + item.getPaddingTop() + layoutParams.topMargin + layoutParams.bottomMargin /*item.getBottom() - item.getTop()*/;
                    /*int desiredWidth = View.MeasureSpec.makeMeasureSpec(item.getWidth(), View.MeasureSpec.AT_MOST);
                    item.measure(desiredWidth, 0); // 计算子项View 的宽高
                    height += item.getMeasuredHeight();*/
            }
        }

        return height;
    }

    public RecyclerView.ViewHolder getLastHolder() {
        return mLastHolder;
    }

    /**
     * 针对于添加模式，获取需要修改的备忘
     **/
    public Memo getAppendMemo() {
        return (Memo) datas.get(datas.size() - 1);
    }

    /**
     * 获取最近一次回复视图索引
     **/

    public int getLastRespViewPosition() {
        int index = datas.size() - 1;
        while (index >= 0) {
            if (datas.get(index) instanceof ResponseMsg) {
                return index;
            }
            index--;
        }
        return -1;
    }

    /**
     * 获取最近一次输入文本
     **/
    public String getLastInputText() {
        int index = datas.size() - 1;
        while (index >= 0) {
            if (datas.get(index) instanceof SpeechMsg && !(datas.get(index) instanceof ResponseMsg)) {
                return ((SpeechMsg) datas.get(index)).text;
            }
            index--;
        }
        return "";
    }

    /**
     * 获取最近一次输入视图索引
     **/
    public int getLastInputPosition() {
        int index = datas.size() - 1;
        while (index >= 0) {
            Object obj = datas.get(index);
            if (obj instanceof SpeechMsg && !(obj instanceof ResponseMsg)) {
                return index;
            }
            index--;
        }
        return index;
    }

    /**
     * 获取指定类型的任务卡数据集合
     **/
    public <T> List<TaskCard<T>> getCards(Class<T> clazz) {
        List<TaskCard<T>> list = new ArrayList<>();
        for (Object obj : datas) {
            if (obj instanceof TaskCard) {
                if (((TaskCard) obj).t != null) {
                    if (clazz.getSimpleName().equals(((TaskCard) obj).t.getClass().getSimpleName())) {
                        list.add((TaskCard) obj);
                    }
                }

            }
        }
        return list;
    }

    /**
     * 获取指定类型的任务卡列表中的数据集合
     **/
    public List<TaskCard> getTaskList(Class clazz) {
        int index = datas.size() - 1;
        while (index >= 0) {
            Object obj = datas.get(index);
            if (obj instanceof TaskCard && ((TaskCard) obj).taskDatas != null
                    && ((TaskCard) ((TaskCard) obj).taskDatas.get(0)).t.getClass().getSimpleName().equals(clazz.getSimpleName())) {
                return ((TaskCard) obj).taskDatas;
            }
            index--;
        }
        return new ArrayList<>();
    }

    public int getLastCardListPosition(Class clazz) {
        int index = datas.size() - 1;
        while (index >= 0) {
            Object obj = datas.get(index);
            if (obj instanceof TaskCard
                    && ((TaskCard) obj).taskDatas != null
                    && ((TaskCard) ((TaskCard) obj).taskDatas.get(0)).t.getClass().getSimpleName().equals(clazz.getSimpleName())) {
                return index;
            }
            index--;
        }
        return -1;
    }

    /**
     * 获取指定类型的任务卡的最后一个索引
     *
     * @return 指定类型任务卡最后索引  -1：没有该类任务卡
     **/
    public int getLastCardPosition(Class clazz) {
        int index = datas.size() - 1;
        while (index >= 0) {
            Object obj = datas.get(index);
            if (obj instanceof TaskCard && ((TaskCard) obj).t != null
                    && clazz.getSimpleName().equals(((TaskCard) obj).t.getClass().getSimpleName())) {
                return index;
            }
            index--;
        }
        return index;
    }

    /**
     * 获取指定类型和ID的任务卡数据
     **/
    public TaskCard getCardById(long id, Class clazz) {
        int index = datas.size() - 1;
        while (index >= 0) {
            Object obj = datas.get(index);
            if (obj instanceof TaskCard) {
                TaskCard taskCard = (TaskCard) obj;
                if (taskCard.t != null
                        && clazz.getSimpleName().equals(taskCard.t.getClass().getSimpleName())
                        && taskCard.getId() == id)
                    return taskCard;
            }
            index--;
        }
        return null;
    }

    /**
     * 获取最近一个处于未完成态（需要移除）的电话短信卡片索引
     **/
    public int getLastUnFinishPos() {
        int index = datas.size() - 1;
        while (index >= 0) {
            Object obj = datas.get(index);
            if (obj instanceof CallAndSmsMsg && !((CallAndSmsMsg) obj).isCompleted()) {
                return index;
            }
            index--;
        }
        return index;
    }

    /**
     * 获取上一个专辑列表视图索引
     **/
    public int getLastAlbumViewPos() {
        int index = datas.size() - 1;
        while (index >= 0) {
            Object obj = datas.get(index);
            if (obj instanceof TingAlbumMsg && ((TingAlbumMsg) obj).getPlayTrack() == null)
                return index;
            index--;
        }
        return index;
    }

    /**
     * 获取上一个播放专辑视图索引
     **/
    public int getLastTrackViewPos() {
        int index = datas.size() - 1;
        while (index >= 0) {
            Object obj = datas.get(index);
            if (obj instanceof TingAlbumMsg && ((TingAlbumMsg) obj).getPlayTrack() != null)
                return index;
            index--;
        }
        return index;
    }

    public void setSpeakPosition(int position) {
        this.speakPosition = position;
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    /**
     * 移除指定位置非空视图
     **/
    public void removeItemView(int position) {
        datas.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case REQ_VIEW:
                View req = mInflater.inflate(R.layout.item_req_view, parent, false);
                viewHolder = new ReqMsgHolder(req);
                break;
            case RESP_VIEW:
                View resp = mInflater.inflate(R.layout.item_resp_view, parent, false);
                viewHolder = new RespMsgHolder(resp);
                break;
            case REMIND_CARD_VIEW:
                View remindView = mInflater.inflate(R.layout.card_remind, parent, false);
                viewHolder = new RemindHolder(remindView);
                break;
            case ALARM_CARD_VIEW:
                View alarmView = mInflater.inflate(R.layout.card_alarm, parent, false);
                viewHolder = new AlarmHolder(alarmView);
                break;
            case MEMO_CARD_VIEW:
                View memoView = mInflater.inflate(R.layout.card_memo, parent, false);
                viewHolder = new MemoHolder(memoView);
                break;
            case ACCOUNTING_CARD_VIEW:
                View cardView = mInflater.inflate(R.layout.card_account, parent, false);
                viewHolder = new AccountHolder(cardView);
                break;
            case CARD_LIST_VIEW:
                View listView = mInflater.inflate(R.layout.item_tasklist_view, parent, false);
                viewHolder = new TaskListHolder(listView);
                break;
            case SYS_TIPS_VIEW:
                View tipsView = mInflater.inflate(R.layout.item_tips_view, parent, false);
                viewHolder = new TipsHolder(tipsView);
                break;
            case MEMO_APPEND_VIEW:
                View appendView = mInflater.inflate(R.layout.card_memo, parent, false);
                viewHolder = new MemoAppendHolder(appendView);
                break;
            case EMPTY_VIEW:
                View emptyView = new View(mContext);
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-2, -2);
                layoutParams.height = 24;
                parent.addView(emptyView, layoutParams);
                viewHolder = new EmptyHolder(emptyView);
                break;
            case CALL_AND_SMS_VIEW:
                View callView = mInflater.inflate(R.layout.item_call_and_sms_view, parent, false);
                viewHolder = new CallAndSmsHolder(callView);
                break;
            case TING_ALBUM_VIEW:
                View albumView = mInflater.inflate(R.layout.item_tasklist_view, parent, false);
                viewHolder = new TingAlbumHolder(albumView);
                break;
            case TING_PLAY_VIEW:
                View playView = mInflater.inflate(R.layout.item_ting_play, parent, false);
                viewHolder = new TingPlayHolder(playView);
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position < datas.size()) {
            if (position == datas.size() - 1) {
                mLastHolder = holder;
            }
            Object data = datas.get(position);
            if (holder instanceof ReqMsgHolder) {       //用户输入文本
                ((ReqMsgHolder) holder).mReqItem.bind(position, (SpeechMsg) data);
            } else if (holder instanceof RespMsgHolder) {       //机器人回复文本
                ((RespMsgHolder) holder).mRspItem.bind(position, (SpeechMsg) data);
                if (position == speakPosition)
                    ((RspMsgItemView) holder.itemView).startAnimation();
                else
                    ((RspMsgItemView) holder.itemView).stopAnimation();
            } else if (holder instanceof TipsHolder) {      //系统提示语
                ((TipsHolder) holder).tips.setText((String) data);
            } else if (holder instanceof TaskListHolder) {      //任务卡列表
                List<TaskCard> taskDatas = ((TaskCard) data).taskDatas;
                RecyclerView listView = ((TaskListHolder) holder).taskList;
                TaskListAdapter listAdapter = new TaskListAdapter(mContext, taskDatas);
                listView.setLayoutManager(new FullyLinearLayoutManager(mContext) {
                    @Override
                    public boolean canScrollVertically() {
                        return false;
                    }
                });
                listView.setAdapter(listAdapter);
            } else if (holder instanceof MemoAppendHolder) {        //备忘添加模式
                MemoAppendHolder appendHolder = (MemoAppendHolder) holder;
                Memo memo = (Memo) data;
                appendHolder.mDelItem.setVisibility(View.GONE);
                appendHolder.mEditMemoItem.setVisibility(View.VISIBLE);
                appendHolder.mEditMemoContent.setText(memo.getContent());
                appendHolder.mEditMemoContent.setSelection(appendHolder.mEditMemoContent.length());
                String time;
                if (memo.getModified() == null) {
                    time = new SimpleDate().toString();
                } else {
                    time = TimeUtils.getTime(memo.getModified());
                }
                appendHolder.mTvCancel.setText("退出");
                appendHolder.mEditMemoTime.setText(time);
                appendHolder.mEditMemoCount.setText("字数" + appendHolder.mEditMemoContent.getText().length());
                /*((MemoAppendHolder) holder).etAppend.setText(((Memo) data).getContent());
                ((MemoAppendHolder) holder).etAppend.setSelection(((Memo) data).getContent().length());*/
            } else if (holder instanceof RemindHolder) {        //提醒卡片
                Remind t = (Remind) ((TaskCard) data).t;
                ((RemindHolder) holder).mDelItem.setVisibility(View.GONE);
                ((RemindHolder) holder).mTvState.setVisibility(View.GONE);
                ((RemindHolder) holder).mEditRemindItem.setVisibility(View.VISIBLE);
                ((RemindHolder) holder).mLlRemindFr.setVisibility(View.GONE);
                ((RemindHolder) holder).mIvShowMore.setVisibility(View.VISIBLE);
                ((RemindHolder) holder).mIvShowMore.setImageLevel(0);
                ((RemindHolder) holder).mEtRemind.setText("");
                if (!TextUtils.isEmpty(t.getContent()) && t.getRdate() != null) {   //完成状态
                    ((RemindHolder) holder).mDelItem.setVisibility(View.VISIBLE);
                    ((RemindHolder) holder).mEditRemindItem.setVisibility(View.GONE);
                    ((RemindHolder) holder).mRemindContent.setText(t.getContent());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(t.getRdate());
                    ((RemindHolder) holder).mRemindDatetime.setText(new StringBuilder().append(t.getFrequency() == 0 ? TimeUtils.format(t.getRdate()) : AssistUtils.translateRemindFrequency(t.getFrequency(), calendar))
                            .append("    ").append(new SimpleDate(t.getRtime()).toString()).toString());
                    refreshCard(((TaskCard) data).taskState, ((RemindHolder) holder).mTvState, ((RemindHolder) holder).mDelItem, null);
                } else {     //展开状态
                    ((RemindHolder) holder).mEditRemindContent.getEditText().setText(t.getContent());
                    if (t.getRdate() == null) {
                        ((RemindHolder) holder).mEditRemindDate.setText(TimeUtils.format(new Date()));
                        ((RemindHolder) holder).mEditRemindTime.setText(new SimpleDate().toString());
                    } else {
                        ((RemindHolder) holder).mEditRemindDate.setText(TimeUtils.format(t.getRdate()));
                        ((RemindHolder) holder).mEditRemindTime.setText(new SimpleDate(t.getRtime()).toString());
                    }
                }
                remindFr = t.getFrequency();
                Calendar cl = Calendar.getInstance();
                if (t.getRdate() != null)
                    cl.setTime(t.getRdate());
                ((RemindHolder) holder).mEditRemindFr.setText(AssistUtils.translateRemindFrequency(t.getFrequency(), cl));
                ((RemindHolder) holder).mTvSave.setText("创建");
                ((RemindHolder) holder).mEditRemindContent.setErrorEnabled(false);
                // mListFragment.scrollToPosition(position);
            } else if (holder instanceof AlarmHolder) {     //闹钟卡片
                TaskCard<AlarmClock> alarmCard = (TaskCard<AlarmClock>) data;
                AlarmHolder alarmHolder = (AlarmHolder) holder;
                alarmHolder.mDelItem.setVisibility(View.GONE);
                alarmHolder.mTvState.setVisibility(View.GONE);
                alarmHolder.mEditAlarmItem.setVisibility(View.VISIBLE);
                alarmHolder.mLlEditFr.setVisibility(View.GONE);
                alarmHolder.mLlEditRing.setVisibility(View.GONE);
                alarmHolder.mIvShowMore.setVisibility(View.VISIBLE);
                alarmHolder.mIvShowMore.setImageLevel(0);
                if (alarmCard.t.getId() == null) {
                    //编辑状态
                    alarmHolder.mEditAlarmTime.setText(new SimpleDate().toString());
                    alarmHolder.mEditAlarmDesc.setText("闹钟");
                    alarmHolder.mEditAlarmRing.setText("默认");
                    alarmHolder.mEditAlarmFr.setText(AssistUtils.transalteWeekDayString(alarmCard.t.getFrequency()));
                    alarmHolder.mTvSave.setText("创建");
                    int week = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                    alarmFr = week - 1 > 0 ? week - 1 : 7;
                } else {
                    //完成状态
                    alarmFr = alarmCard.t.getFrequency(true);
                    repeat = alarmCard.t.getRepeat();
                    alarmHolder.mDelItem.setVisibility(View.VISIBLE);
                    alarmHolder.mEditAlarmItem.setVisibility(View.GONE);
                    alarmHolder.mAlarmTime.setText(new SimpleDate(alarmCard.t.getRtime()).toString());
                    String desc = TextUtils.isEmpty(alarmCard.t.getItem()) ? "闹钟" : alarmCard.t.getItem();
                    alarmHolder.mAlarmDesc.setText(desc);
                    alarmHolder.mEditAlarmDesc.setText(desc);
                    alarmHolder.mAlarmFr.setText(AssistUtils.transalteWeekDayString(alarmCard.t.getFrequency()));
                    alarmHolder.mAlarmSwitchBtn.setChecked(alarmCard.t.getValid() == 1);
                    refreshCard(alarmCard.taskState, alarmHolder.mTvState, alarmHolder.mDelItem, alarmHolder.mAlarmSwitchBtn);
                }
            } else if (holder instanceof MemoHolder) {      //备忘卡片
                TaskCard<Memo> memoCard = (TaskCard<Memo>) data;
                MemoHolder memoHolder = (MemoHolder) holder;
                memoHolder.mDelItem.setVisibility(View.GONE);
                memoHolder.mTvState.setVisibility(View.GONE);
                memoHolder.mEditMemoItem.setVisibility(View.VISIBLE);
                String content = memoCard.t.getContent();
                if (TextUtils.isEmpty(content)) {
                    memoHolder.mEditMemoTime.setText("今天" + new SimpleDate().toString());
                    memoHolder.mEditMemoCount.setText("字数" + 0);
                    memoHolder.mTvSave.setText("创建");
                    memoHolder.mEditMemoContent.setText("");
                } else {
                    memoHolder.mDelItem.setVisibility(View.VISIBLE);
                    memoHolder.mEditMemoItem.setVisibility(View.GONE);
                    memoHolder.mMemoContent.setText(content);
                    refreshCard(memoCard.taskState, memoHolder.mTvState, memoHolder.mDelItem, null);
                }
            } else if (holder instanceof AccountHolder) {       //记账卡片
                TaskCard<Accounting> accountCard = (TaskCard<Accounting>) data;
                AccountHolder accountHolder = (AccountHolder) holder;
                if (/*!TextUtils.isEmpty(accountCard.t.getMemo()) &&*/ !TextUtils.isEmpty(accountCard.t.getEtype()) && accountCard.t.getAmount() != 0) {
                    //信息齐全，完成状态
                    accountHolder.mDelItem.setVisibility(View.VISIBLE);
                    accountHolder.mEditAccountItem.setVisibility(View.GONE);
                    accountHolder.mAccountEtype.setText(accountCard.t.getEtype());
                    accountHolder.mAccountDate.setText(TimeUtils.formatDate(accountCard.t.getRdate()));
                    if (accountCard.t.getAtype() == 0) {    //支出
                        accountHolder.mAccountAmount.setTextColor(mContext.getResources().getColor(R.color.green));
                        accountHolder.mAccountAmount.setText("-" + AssistUtils.formatAmount(accountCard.t.getAmount()));
                    } else {     //收入
                        accountHolder.mAccountAmount.setTextColor(mContext.getResources().getColor(R.color.red_style));
                        accountHolder.mAccountAmount.setText("+" + AssistUtils.formatAmount(accountCard.t.getAmount()));
                    }
                    refreshCard(accountCard.taskState, accountHolder.mTvState, accountHolder.mDelItem, null);
                } else {
                    //编辑状态
                    accountHolder.mDelItem.setVisibility(View.GONE);
                    accountHolder.mTvState.setVisibility(View.GONE);
                    accountHolder.mEditAccountItem.setVisibility(View.VISIBLE);
                    accountHolder.mLlAccountRtime.setVisibility(View.GONE);
                    accountHolder.mLlAccountMemo.setVisibility(View.GONE);
                    accountHolder.mIvShowMore.setVisibility(View.VISIBLE);
                    accountHolder.mIvShowMore.setImageLevel(0);
                    accountHolder.mEditAccountAmount.getEditText().setText(accountCard.t.getAmount() + "");
                    accountHolder.mEditAccountAtype.setText(accountCard.t.getAtype() == 0 ? "支出" : "收入");
                    accountHolder.mEditAccountEtype.setText(TextUtils.isEmpty(accountCard.t.getEtype()) ? "其他" : accountCard.t.getEtype());
                    if (accountCard.t.getRdate() == null) {
                        accountHolder.mEditAccountDate.setText(TimeUtils.format(new Date()));
                        accountHolder.mEditAccountTime.setText(new SimpleDate().toString());
                    } else {
                        accountHolder.mEditAccountDate.setText(TimeUtils.format(accountCard.t.getRdate()));
                        accountHolder.mEditAccountTime.setText(TimeUtils.getTime(accountCard.t.getRdate()));
                    }
                    accountHolder.mEditAccountMemo.setText(accountCard.t.getMemo());
                    accountHolder.mTvSave.setText("创建");
                }
            } else if (holder instanceof CallAndSmsHolder) {        //电话短信卡片
                CallAndSmsHolder csHolder = (CallAndSmsHolder) holder;
                CallAndSmsMsg callAndSmsMsg = (CallAndSmsMsg) data;
                int type = callAndSmsMsg.getType();
                String[] contents = callAndSmsMsg.getContents();
                csHolder.mRgSelector.setVisibility(View.GONE);
                csHolder.mLlCallSmsContainer.setVisibility(View.VISIBLE);
                csHolder.mDashDivider.setVisibility(View.VISIBLE);
                csHolder.mTvConfirm.setVisibility(View.VISIBLE);
                csHolder.mTvCancel.setVisibility(View.VISIBLE);
                csHolder.mTvAppend.setVisibility(View.GONE);
                csHolder.mLlTipsItem.setVisibility(View.GONE);
                csHolder.mSbConfirm.setVisibility(View.GONE);
                csHolder.mSbConfirm.setProgress(0);
                csHolder.mIvUser.setImageLevel(0);
                csHolder.mTvName.setTextColor(mContext.getResources().getColor(R.color.white));
                csHolder.mFlCallSms.setBackgroundResource(R.color.card_edit_bg);
                if (TextUtils.isEmpty(contents[0]) || contents[0].matches("^[0-9]*$")) {
                    csHolder.mTvName.setText("未知号码");
                } else
                    csHolder.mTvName.setText(contents[0]);
                if (type >> 3 == MobileCommProcessor.CallDialogType) {
                    csHolder.mTvTitle.setText("电话");
                    csHolder.mLlCallNumber.setVisibility(View.VISIBLE);
                    csHolder.mLlSmsContent.setVisibility(View.GONE);
                    csHolder.mTvCallNumber.setText(contents[1]);
                    csHolder.mTvCancel.setText("取消呼叫");
                    csHolder.mTvConfirm.setText("确定呼叫");
                    csHolder.mIvPhone.setImageLevel(0);
                    csHolder.mTvCallNumber.setTextColor(mContext.getResources().getColor(R.color.white));
                    switch (type) {
                        case MobileCommProcessor.WaittingForCall:
                            csHolder.mSbConfirm.setVisibility(View.VISIBLE);
                            break;
                        case MobileCommProcessor.ConfirmNameCall:
                        case MobileCommProcessor.ConfirmNameSms:
                            csHolder.mTvCancel.setText("错误");
                            csHolder.mTvConfirm.setText("正确");
                            break;
                        case MobileCommProcessor.ConfirmLastCall:
                            csHolder.mTvCancel.setText("取消");
                            csHolder.mTvConfirm.setText("呼叫");
                            break;
                        case MobileCommProcessor.CompletedCall:
                            csHolder.mFlCallSms.setBackgroundResource(R.color.white);
                            csHolder.mTvName.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                            csHolder.mTvCallNumber.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                            callAndSmsMsg.setCompleted(true);
                            csHolder.mLlTipsItem.setVisibility(View.VISIBLE);
                            csHolder.mTvTips.setText("通话已结束");
                            csHolder.mCallSmsState.setImageLevel(0);
                            csHolder.mTvCancel.setVisibility(View.GONE);
                            csHolder.mTvConfirm.setVisibility(View.GONE);
                            csHolder.mIvUser.setImageLevel(1);
                            csHolder.mIvPhone.setImageLevel(1);
                            csHolder.mDashDivider.setVisibility(View.GONE);
                            break;
                        case MobileCommProcessor.FailedCall:
                            csHolder.mFlCallSms.setBackgroundResource(R.color.white);
                            csHolder.mTvName.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                            csHolder.mTvCallNumber.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                            callAndSmsMsg.setCompleted(true);
                            csHolder.mLlTipsItem.setVisibility(View.VISIBLE);
                            csHolder.mTvTips.setText("拨号失败");
                            csHolder.mCallSmsState.setImageLevel(1);
                            csHolder.mTvCancel.setVisibility(View.GONE);
                            csHolder.mTvConfirm.setVisibility(View.GONE);
                            csHolder.mIvUser.setImageLevel(1);
                            csHolder.mIvPhone.setImageLevel(1);
                            csHolder.mDashDivider.setVisibility(View.GONE);
                            break;
                    }
                } else if (type >> 3 == MobileCommProcessor.SmsDialogType) {
                    csHolder.mTvTitle.setText("短信");
                    csHolder.mLlCallNumber.setVisibility(View.GONE);
                    csHolder.mLlSmsContent.setVisibility(View.VISIBLE);
                    csHolder.mRlSmsInput.setVisibility(View.GONE);
                    csHolder.mTvContent.setVisibility(View.VISIBLE);
                    csHolder.mTvContent.setTextColor(mContext.getResources().getColor(R.color.white));
                    String content = TextUtils.isEmpty(contents[contents.length - 1]) || contents[contents.length - 1].equals("null") ? "" : contents[contents.length - 1];
                    csHolder.mTvContent.setText(content);
                    csHolder.mIvSms.setImageLevel(0);
                    csHolder.mTilSmsContent.setError("");
                    csHolder.mTilSmsContent.setErrorEnabled(false);
                    switch (type) {
                        case MobileCommProcessor.WaittingForSend:
                            csHolder.mTvCancel.setText("取消发送");
                            csHolder.mTvConfirm.setText("立即发送");
                            csHolder.mSbConfirm.setVisibility(View.VISIBLE);
                            break;
                        case MobileCommProcessor.ConfirmForSend:
                            csHolder.mTvCancel.setText("取消发送");
                            csHolder.mTvConfirm.setText("确定发送");
                            csHolder.mTvAppend.setVisibility(View.VISIBLE);
                            csHolder.mTvContent.setVisibility(View.GONE);
                            csHolder.mRlSmsInput.setVisibility(View.VISIBLE);
                            csHolder.mEtContent.setText(content);
                            break;
                        case MobileCommProcessor.ConfirmLastMsg:
                            csHolder.mTvCancel.setText("回复");
                            csHolder.mTvConfirm.setText("朗读");
                            break;
                        case MobileCommProcessor.CompletedSend:
                            csHolder.mFlCallSms.setBackgroundResource(R.color.white);
                            csHolder.mTvName.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                            csHolder.mTvCallNumber.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                            csHolder.mTvContent.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                            callAndSmsMsg.setCompleted(true);
                            csHolder.mLlTipsItem.setVisibility(View.VISIBLE);
                            csHolder.mTvTips.setText("短信已发送");
                            csHolder.mCallSmsState.setImageLevel(0);
                            csHolder.mIvSms.setImageLevel(1);
                            csHolder.mIvUser.setImageLevel(1);
                            csHolder.mTvCancel.setVisibility(View.GONE);
                            csHolder.mTvConfirm.setVisibility(View.GONE);
                            csHolder.mDashDivider.setVisibility(View.GONE);
                            break;
                        case MobileCommProcessor.FailedSend:
                            csHolder.mFlCallSms.setBackgroundResource(R.color.white);
                            csHolder.mTvName.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                            csHolder.mTvCallNumber.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                            csHolder.mTvContent.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                            callAndSmsMsg.setCompleted(true);
                            csHolder.mLlTipsItem.setVisibility(View.VISIBLE);
                            csHolder.mTvTips.setText("发送失败");
                            csHolder.mCallSmsState.setImageLevel(1);
                            csHolder.mIvSms.setImageLevel(1);
                            csHolder.mIvUser.setImageLevel(1);
                            csHolder.mTvCancel.setVisibility(View.GONE);
                            csHolder.mTvConfirm.setVisibility(View.GONE);
                            csHolder.mDashDivider.setVisibility(View.GONE);
                            break;
                    }
                } else if (type >> 3 == MobileCommProcessor.CheckListDialogType) {
                    switch (type) {
                        case MobileCommProcessor.CheckForNameCall:
                        case MobileCommProcessor.CheckForNumCall:
                            csHolder.mTvTitle.setText("电话");
                            csHolder.mTvCancel.setText("取消拨打");
                            break;
                        case MobileCommProcessor.CheckForNameSms:
                        case MobileCommProcessor.CheckForNumSms:
                            csHolder.mTvTitle.setText("短信");
                            csHolder.mTvCancel.setText("取消发送");
                            break;
                    }
                    csHolder.mRgSelector.setVisibility(View.VISIBLE);
                    csHolder.mLlCallSmsContainer.setVisibility(View.GONE);
                    csHolder.mTvConfirm.setVisibility(View.GONE);
                    csHolder.mRgSelector.removeAllViews();
                    for (int i = 0; i < contents.length; i++) {
                        AppCompatRadioButton radioButton = new AppCompatRadioButton(mContext);
                        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-1, -2);
                        layoutParams.height = 96;
                        radioButton.setLayoutParams(layoutParams);
                        radioButton.setText(contents[i]);
                        radioButton.setTextSize(15);
                        radioButton.setPadding(32, 0, 0, 0);
                        radioButton.setTextColor(mContext.getResources().getColor(R.color.white));
                        radioButton.setId(i);
                        csHolder.mRgSelector.addView(radioButton);
                    }
                }
            } else if (holder instanceof TingAlbumHolder) {       //有声内容专辑列表视图
                TingAlbumMsg albumMsg = (TingAlbumMsg) datas.get(position);
                TingAlbumHolder albumHolder = (TingAlbumHolder) holder;
                albumHolder.albumListView.setLayoutManager(new FullyLinearLayoutManager(mContext));
                ChatAlbumListAdapter albumListAdapter = new ChatAlbumListAdapter(mContext, albumMsg.getAlbums(), albumMsg.getEpisode());
                albumHolder.albumListView.setAdapter(albumListAdapter);
                albumListAdapter.setOnCountDownCancelListener(new ChatAlbumListAdapter.OnCountDownCancelListener() {
                    @Override
                    public void onCancel() {
                        mListFragment.cancelWaitActionTimer();
                    }
                });
            } else if (holder instanceof TingPlayHolder) {        //播放专辑视图
                TingPlayHolder playHolder = (TingPlayHolder) holder;
                TingAlbumMsg albumMsg = (TingAlbumMsg) datas.get(position);
                Album album = albumMsg.getAlbums().get(0);
                Glide.with(mContext).load(album.getCoverUrlMiddle()).into(playHolder.mIvAlbum);
                playHolder.mTvAlbumTitle.setText(album.getAlbumTitle());
                LastUpTrack lastUptrack = album.getLastUptrack();
                playHolder.mTvLastTrack.setText(new StringBuilder().append("更新至").append(TimeUtils.formatDate(new Date(lastUptrack.getCreatedAt()))).append("  ").append(lastUptrack.getTrackTitle()).toString());
                playHolder.mTvPlayCount.setText(StringUtils.formPlayCount(album.getPlayCount()) + "次播放");
                playHolder.mTvTrackCount.setText(album.getIncludeTrackCount() + "集");
                //订阅状态
                boolean isSubscribe = TingAlbumDao.getInstance().isSubscribe(album.getId());
                playHolder.mTvSubscribe.setText(isSubscribe ? "已订阅" : "订阅");
                LevelListDrawable ld = (LevelListDrawable) playHolder.mTvSubscribe.getBackground();
                ld.setLevel(isSubscribe ? 1 : 0);
                Track track = albumMsg.getPlayTrack();
                playHolder.mTvTrackTitle.setText(track.getTrackTitle());
                // long playTrackId = currSound == null ? 0 : currSound.getDataId();
                playHolder.mTvTrackTitle.setTextColor(/*track.getDataId() == playTrackId*/XmlyManager.get().isPlaying()
                        ? mContext.getResources().getColor(R.color.second_base_color)
                        : mContext.getResources().getColor(R.color.new_text_color_first));
                /*playHolder.mIvTingSwitch.setImageLevel(0);
                if (track.getDataId() == playTrackId)*/
                playHolder.mIvTingSwitch.setImageLevel(XmlyManager.get().isPlaying() ? 1 : 0);
                playHolder.mTvCreated.setText(TimeUtils.getInstance().getDateString(new Date(track.getCreatedAt())));
                playHolder.mTvDuration.setText(new SimpleDate().formDuration(track.getDuration()));
            }
        }
    }

    /**
     * 刷新任务卡视图
     **/

    private void refreshCard(int taskState, TextView tvState, SlidingItem item, SwitchButton sb) {
        if (sb != null)
            sb.setFocusable(false);
        setFocus(item, false);
        tvState.setVisibility(View.VISIBLE);
        switch (taskState) {
            case TaskCard.TaskState.ACTIVE:
                if (sb != null)
                    sb.setFocusable(true);
                setFocus(item, true);
                tvState.setVisibility(View.GONE);
                break;
            case TaskCard.TaskState.REVOCABLE:
                tvState.setText("撤销");
                tvState.setTextColor(mContext.getResources().getColor(R.color.base_blue));
                break;
            case TaskCard.TaskState.DELETED:
                tvState.setText("已删除");
                tvState.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                break;
            case TaskCard.TaskState.INVALID:
                tvState.setText("已作废");
                tvState.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                break;
        }
    }

    /**
     * 设置可滑动item焦点
     **/
    private void setFocus(SlidingItem item, boolean focus) {
        item.setSlidable(focus);
        item.setClickable(focus);
    }

    @Override
    public int getItemCount() {
        return datas.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return EMPTY_VIEW;
        } else {
            Object obj = datas.get(position);
            if (obj instanceof SpeechMsg) {
                return obj instanceof ResponseMsg ? RESP_VIEW : REQ_VIEW;
            } else if (obj instanceof TaskCard) {
                if (((TaskCard) obj).t instanceof Remind) {
                    return REMIND_CARD_VIEW;
                } else if (((TaskCard) obj).t instanceof Memo) {
                    return MEMO_CARD_VIEW;
                } else if (((TaskCard) obj).t instanceof AlarmClock) {
                    return ALARM_CARD_VIEW;
                } else if (((TaskCard) obj).t instanceof Accounting) {
                    return ACCOUNTING_CARD_VIEW;
                } else {
                    return CARD_LIST_VIEW;
                }
            } else if (obj instanceof String) {
                return SYS_TIPS_VIEW;
            } else if (obj instanceof Memo) {
                return MEMO_APPEND_VIEW;
            } else if (obj instanceof CallAndSmsMsg) {
                return CALL_AND_SMS_VIEW;
            } else if (obj instanceof TingAlbumMsg) {
                return ((TingAlbumMsg) obj).getPlayTrack() == null ? TING_ALBUM_VIEW : TING_PLAY_VIEW;
            }
        }
        return super.getItemViewType(position);
    }

    /**
     * 停止识别
     **/
    private void stopRecognize() {
        Intent intent = new Intent(mContext, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
        mContext.startService(intent);
    }

    /**
     * 中断任务流
     **/
    private void interruptTask(TaskCard taskCard, int positon) {
        // 停止识别
        stopRecognize();
        //结束任务流
        sendForEndTask();
        if (taskCard.firstTouch && taskCard.getId() == null) {
            datas.add(positon, mContext.getResources().getString(R.string.manual_operation_tips));
            notifyItemInserted(positon);
            taskCard.firstTouch = false;
        }
    }

    /**
     * 主页面跳转时，保存或取消备忘
     **/
    public void handleMemoCard() {
        if (IflyRecognizer.isInited() && IflyRecognizer.getInstance().isLong_time_record()) {
            Memo memo;
            int position = datas.size() - 1;
            EditText etContent = (EditText) mLastHolder.itemView.findViewById(R.id.edit_memo_content);
            if (etContent == null)
                return;
            String content = etContent.getText().toString().trim();
            if (IflyRecognizer.getInstance().getLong_record_mode() == IflyRecognizer.CREATE_MEMO_MODE) {    //新建备忘
                memo = (Memo) ((TaskCard) datas.get(position)).t;
                if (TextUtils.isEmpty(content)) {
                    ((MemoHolder) mLastHolder).cancelHandle(memo);
                } else {
                    ((MemoHolder) mLastHolder).saveHandle(memo);
                }
            } else {   //添加模式
                memo = (Memo) datas.get(position);
                if (TextUtils.isEmpty(content))
                    ((MemoAppendHolder) mLastHolder).cancel();
                else
                    ((MemoAppendHolder) mLastHolder).quitAppendMode(memo);
            }
            EventBus.getDefault().post(new RobotTipsEvent(""));
        }
    }

    /**
     * 用户输入视图
     **/
    class ReqMsgHolder extends RecyclerView.ViewHolder {
        ReqMsgItemView mReqItem;

        public ReqMsgHolder(View itemView) {
            super(itemView);
            mReqItem = (ReqMsgItemView) itemView.findViewById(R.id.req_view);
        }
    }

    /**
     * 机器人回复视图
     **/
    class RespMsgHolder extends RecyclerView.ViewHolder {

        RspMsgItemView mRspItem;

        public RespMsgHolder(View itemView) {
            super(itemView);
            mRspItem = (RspMsgItemView) itemView.findViewById(R.id.resp_view);
            mRspItem.setListener(rspItemClickListener);
        }
    }

    class CallAndSmsHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_title)
        TextView mTvTitle;
        @BindView(R.id.rg_selector)
        RadioGroup mRgSelector;
        @BindView(R.id.tv_name)
        TextView mTvName;
        @BindView(R.id.tv_call_number)
        TextView mTvCallNumber;
        @BindView(R.id.et_content)
        EditText mEtContent;
        @BindView(R.id.til_sms_content)
        TextInputLayout mTilSmsContent;
        @BindView(R.id.tv_content)
        TextView mTvContent;
        @BindView(R.id.tv_tips)
        TextView mTvTips;
        @BindView(R.id.tv_cancel)
        TextView mTvCancel;
        @BindView(R.id.tv_confirm)
        TextView mTvConfirm;
        @BindView(R.id.sb_confirm)
        SeekBar mSbConfirm;
        @BindView(R.id.ll_call_sms_container)
        LinearLayout mLlCallSmsContainer;
        @BindView(R.id.ll_sms_content)
        LinearLayout mLlSmsContent;
        @BindView(R.id.tv_append)
        TextView mTvAppend;
        /*@BindView(R.id.iv_clear)
        ImageView mIvClear;*/
        @BindView(R.id.rl_sms_input)
        RelativeLayout mRlSmsInput;
        @BindView(R.id.ll_tips_item)
        LinearLayout mLlTipsItem;
        @BindView(R.id.ll_call_number)
        LinearLayout mLlCallNumber;
        @BindView(R.id.call_sms_state)
        ImageView mCallSmsState;
        @BindView(R.id.dash_divider)
        View mDashDivider;
        @BindView(R.id.iv_user)
        ImageView mIvUser;
        @BindView(R.id.iv_phone)
        ImageView mIvPhone;
        @BindView(R.id.iv_sms)
        ImageView mIvSms;
        @BindView(R.id.fl_call_sms)
        FrameLayout mFlCallSms;

        public CallAndSmsHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            /*mIvClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!TextUtils.isEmpty(mEtContent.getText().toString())) {
                        sendMessageToRobot("重新输入");
                    }
                }
            });*/
            mEtContent.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        CallAndSmsMsg callAndSmsMsg = (CallAndSmsMsg) datas.get(getAdapterPosition());
                        manualSmsEdit(callAndSmsMsg);
                    }
                    return false;
                }
            });
            mRgSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    sendMessageToRobot("第" + (checkedId + 1) + "个");
                }
            });
        }

        /**
         * 中断任务流，提示用户手动编辑短信
         **/
        private void manualSmsEdit(CallAndSmsMsg callAndSmsMsg) {
            if (callAndSmsMsg.getType() == MobileCommProcessor.ConfirmForSend && callAndSmsMsg.isFirstTouchOnSmsEdit()) {
                callAndSmsMsg.setFirstTouchOnSmsEdit(false);
                sendForEndTask();
                datas.add(getAdapterPosition() + 1, mContext.getResources().getString(R.string.manual_sms_edit_tips));
                notifyItemInserted(getAdapterPosition() + 1);
            }
        }

        @OnClick({R.id.tv_content, R.id.tv_cancel, R.id.tv_confirm, R.id.tv_append, R.id.et_content})
        public void onClick(View v) {
            stopRecognize();
            int position = getAdapterPosition();
            CallAndSmsMsg callAndSmsMsg = (CallAndSmsMsg) datas.get(position);
            manualSmsEdit(callAndSmsMsg);
            switch (v.getId()) {
                case R.id.tv_content:
                    TextUtils.TruncateAt ellipsize = mTvContent.getEllipsize();
                    if (ellipsize == TextUtils.TruncateAt.END) {    //展开文本
                        mTvContent.setMaxLines(Integer.MAX_VALUE);
                        mTvContent.setEllipsize(TextUtils.TruncateAt.START);
                    } else {
                        mTvContent.setMaxLines(1);
                        mTvContent.setEllipsize(TextUtils.TruncateAt.END);
                    }
                    break;
                case R.id.tv_cancel:
                    if (callAndSmsMsg.getType() == MobileCommProcessor.ConfirmForSend || !callAndSmsMsg.isFirstTouchOnSmsEdit()) {       //短信编辑（确认发送）
                        datas.set(position, "已取消短信发送");
                        notifyItemChanged(position);
                        ((MainActivity) mListFragment.getActivity()).cancelSms();
                    } else {
                        sendMessageToRobot(mTvCancel.getText().toString());
                    }
                    mListFragment.cancelWaitActionTimer();
                    break;
                case R.id.tv_confirm:
                    int type = callAndSmsMsg.getType();
                    if (type == MobileCommProcessor.ConfirmForSend) {
                        String smsContent = mEtContent.getText().toString().trim();
                        if (TextUtils.isEmpty(smsContent)) {
                            mTilSmsContent.setErrorEnabled(true);
                            mTilSmsContent.setError("请输入短信内容");
                            if (mTilSmsContent.getEditText().getBackground() != null)
                                mTilSmsContent.getEditText().getBackground().clearColorFilter();
                            return;
                        } else {
                            mTilSmsContent.setError("");
                            mTilSmsContent.setErrorEnabled(false);
                            final String[] contents = callAndSmsMsg.getContents();
                            //设置短信内容
                            contents[contents.length - 1] = smsContent;
                            //设置任务卡类型
                            callAndSmsMsg.setType(MobileCommProcessor.WaittingForSend);
                            //移除旧卡片，添加提示回复
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.UPDATE_CALL_SMS_STATE));
                            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg("短信发送中"), null, null, null));
                            //添加新卡片
                            EventBus.getDefault().post(new ChatMsgEvent(callAndSmsMsg));
                            Single.just(0)
                                    .delay(100, TimeUnit.MILLISECONDS)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnSuccess(new Consumer<Integer>() {
                                        @Override
                                        public void accept(Integer integer) throws Exception {
                                            //真正发送短信
                                            ((MainActivity) mListFragment.getActivity()).sendSms(contents[1], contents[2], false);
                                        }
                                    })
                                    .subscribeOn(Schedulers.io())
                                    .subscribe();
                        }
                    } else {
                        if (type == MobileCommProcessor.WaittingForCall || type == MobileCommProcessor.WaittingForSend) {
                            mListFragment.cancelWaitActionTimer();
                            if (type == MobileCommProcessor.WaittingForSend && !callAndSmsMsg.isFirstTouchOnSmsEdit()) {
                                String[] contents = callAndSmsMsg.getContents();
                                ((MainActivity) mListFragment.getActivity()).sendSms(contents[1], contents[2], true);
                                return;
                            }
                        }
                        sendMessageToRobot(mTvConfirm.getText().toString());
                    }
                    break;
                case R.id.tv_append:
                    mEtContent.setText("");
                    break;
            }
        }
    }

    class AccountHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

        @BindView(R.id.edit_account_amount)
        TextInputLayout mEditAccountAmount;
        @BindView(R.id.edit_account_atype)
        TextView mEditAccountAtype;
        @BindView(R.id.edit_account_etype)
        TextView mEditAccountEtype;
        @BindView(R.id.edit_account_date)
        TextView mEditAccountDate;
        @BindView(R.id.edit_account_time)
        TextView mEditAccountTime;
        @BindView(R.id.edit_account_memo)
        EditText mEditAccountMemo;
        @BindView(R.id.tv_save)
        TextView mTvSave;
        @BindView(R.id.iv_show_more)
        ImageView mIvShowMore;
        @BindView(R.id.edit_account_item)
        LinearLayout mEditAccountItem;
        @BindView(R.id.account_amount)
        TextView mAccountAmount;
        @BindView(R.id.account_etype)
        TextView mAccountEtype;
        @BindView(R.id.del_item)
        SlidingItem mDelItem;
        @BindView(R.id.tv_state)
        TextView mTvState;
        @BindView(R.id.ll_account_rtime)
        LinearLayout mLlAccountRtime;
        @BindView(R.id.ll_account_memo)
        LinearLayout mLlAccountMemo;
        @BindView(R.id.account_date)
        TextView mAccountDate;
        private AssistEntityDao.BillEntityDao mBillEntityDao;

        public AccountHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mDelItem.setOnSlidingItemListener(this);
            mBillEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.BillEntityDao.class);
            final EditText etContent = mEditAccountAmount.getEditText();
            etContent.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        TaskCard taskCard = (TaskCard) datas.get(getAdapterPosition());
                        interruptTask(taskCard, getAdapterPosition() + 1);
                    }
                    return false;
                }
            });
            mEditAccountMemo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    return (event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
                }
            });
            etContent.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    String amount = etContent.getText().toString();
                    if (TextUtils.isEmpty(amount) || !AssistUtils.isNumber(amount) || Double.valueOf(amount) == 0)
                        mTvSave.setTextColor(mContext.getResources().getColor(R.color.forbid_click_color));
                    else
                        mTvSave.setTextColor(mContext.getResources().getColor(R.color.white));
                }
            });
        }

        @OnClick({R.id.ll_account_atype, R.id.ll_account_etype, R.id.et_account_amount, R.id.edit_account_date_box,
                R.id.edit_account_time, R.id.tv_cancel, R.id.tv_save, R.id.iv_show_more, R.id.tv_state})
        public void onClick(View view) {
            TaskCard taskCard = (TaskCard) datas.get(getAdapterPosition());
            Accounting account = (Accounting) taskCard.t;
            interruptTask(taskCard, getAdapterPosition() + 1);
            switch (view.getId()) {
                case R.id.ll_account_atype:
                    if (account.getAtype() == 0) {
                        mEditAccountAtype.setText("收入");
                        account.setAtype(1);
                    } else {
                        mEditAccountAtype.setText("支出");
                        account.setAtype(0);
                    }
                    break;
                case R.id.ll_account_etype:
                    String cEtype = mEditAccountEtype.getText().toString();
                    if (account.getAtype() == 0) {
                        ItemExpenseDialog expenseDialog = new ItemExpenseDialog(mContext, cEtype, new ItemExpenseDialog.OnItemExpenseListener() {
                            @Override
                            public void onItemSelected(String item) {
                                mEditAccountEtype.setText(item);
                            }
                        });
                        expenseDialog.show();
                    } else {
                        ItemIncomeDialog incomeDialog = new ItemIncomeDialog(mContext, cEtype, new ItemIncomeDialog.OnItemIncomeListener() {
                            @Override
                            public void onIncomeSelected(String item) {
                                mEditAccountEtype.setText(item);
                            }
                        });
                        incomeDialog.show();
                    }
                    break;
                case R.id.edit_account_date_box:
                    Date rDate = TimeUtils.parse(mEditAccountDate.getText().toString());
                    Calendar now = Calendar.getInstance();
                    now.setTime(rDate);
                    //将当前日期传入日期选择器，并显示
                    DatePickerDialog datePicker = DatePickerDialog.newInstance(
                            this,
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                    );
                    datePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    datePicker.show(mListFragment.getActivity().getFragmentManager(), "DatePicker");
                    break;
                case R.id.edit_account_time:
                    SimpleDate rTime = new SimpleDate(mEditAccountTime.getText().toString());
                    TimePickerDialog timePicker = TimePickerDialog.newInstance(
                            this,
                            rTime.getHour(),
                            rTime.getMinute(),
                            true);
                    timePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    timePicker.show(mListFragment.getActivity().getFragmentManager(), "TimePicker");
                    break;
                case R.id.tv_cancel:
                    if (account.getId() == null) {      //新建中
                        datas.set(getAdapterPosition(), "记账已取消创建");
                        notifyItemChanged(getAdapterPosition());
                        ((MainActivity) mListFragment.getActivity()).resetEditState(Accounting.class);
                    } else {
                        mDelItem.setVisibility(View.VISIBLE);
                        mEditAccountItem.setVisibility(View.GONE);
                    }
                    break;
                case R.id.tv_save:
                    String amount = mEditAccountAmount.getEditText().getText().toString();
                    Double money;
                    if (TextUtils.isEmpty(amount) || !AssistUtils.isNumber(amount) || (money = Double.valueOf(amount)) == 0) {
                        mEditAccountAmount.setErrorEnabled(true);
                        mEditAccountAmount.setError("请输入金额");
                        if (mEditAccountAmount.getEditText().getBackground() != null)
                            mEditAccountAmount.getEditText().getBackground().clearColorFilter();
                        return;
                    }
                    mEditAccountAmount.setError("");
                    mEditAccountAmount.setErrorEnabled(false);
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditAccountItem.setVisibility(View.GONE);
                    //保存记录，设置完成视图数据
                    //记录旧金额(用于计算余额)
                    double oldAmount = account.getAmount();
                    account.setAmount(money);
                    if (account.getAtype() == 0) {
                        mAccountAmount.setTextColor(mContext.getResources().getColor(R.color.green));
                        mAccountAmount.setText("-" + AssistUtils.formatAmount(money));
                    } else {
                        mAccountAmount.setTextColor(mContext.getResources().getColor(R.color.red_style));
                        mAccountAmount.setText("+" + AssistUtils.formatAmount(money));
                    }

                    String etype = mEditAccountEtype.getText().toString();
                    mAccountEtype.setText(etype);
                    account.setEtype(etype);
                    account.setMemo(mEditAccountMemo.getText().toString());
                    String date = mEditAccountDate.getText().toString();
                    String time = mEditAccountTime.getText().toString();
                    account.setRdate(TimeUtils.parseDateTime(date + " " + time));
                    mAccountDate.setText(TimeUtils.formatDate(account.getRdate()));
                    Calendar cl = Calendar.getInstance();
                    cl.setTime(account.getRdate());
                    account.setMonth(cl.get(Calendar.MONTH) + 1);
                    account.setSynced(false);
                    int type;
                    if (account.getId() == null) {
                        account.setCreated(new Date());
                        mAssistDao.insertAccount(account);
                        datas.add(getAdapterPosition() + 1, "记账已创建");
                        notifyItemInserted(getAdapterPosition() + 1);
                        ((MainActivity) mListFragment.getActivity()).resetEditState(Accounting.class);
                        type = AccountingActivity.TYPE_ADD;
                    } else {
                        account.setModified(new Date());
                        mAssistDao.updateAccount(account);
                        type = AccountingActivity.TYPE_UPDATE;
                        if (taskCard.atype == 0)
                            oldAmount = -oldAmount;
                        taskCard.atype = account.getAtype();
                        float balance = AppConfig.dPreferences.getFloat(AppConfig.ACCOUNT_AMOUNT, 0);
                        AppConfig.dPreferences.edit().putFloat(AppConfig.ACCOUNT_AMOUNT, (float) (balance - oldAmount)).commit();
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(account, false));
                    }
                    List<TaskCard<Accounting>> list = new ArrayList<>();
                    list.add(taskCard);
                    countBalance(type, list);
                    //同步账单记录
                    AssistEntityDao.create().sync(mBillEntityDao);
                    break;
                case R.id.iv_show_more:
                    if (mLlAccountRtime.getVisibility() == View.GONE) {      //点击展开
                        mIvShowMore.setImageLevel(1);
                        mLlAccountRtime.setVisibility(View.VISIBLE);
                        mLlAccountMemo.setVisibility(View.VISIBLE);
                    } else {
                        mIvShowMore.setImageLevel(0);
                        mLlAccountRtime.setVisibility(View.GONE);
                        mLlAccountMemo.setVisibility(View.GONE);
                    }
                    break;
                case R.id.tv_state:
                    if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                        taskCard.taskState = TaskCard.TaskState.ACTIVE;
                        account.setSynced(false);
                        mAssistDao.insertAccount(account);
                        List<TaskCard<Accounting>> lists = new ArrayList<>();
                        lists.add(taskCard);
                        countBalance(AccountingActivity.TYPE_ADD, lists);
                        sendForEndTask();
                        datas.add(getAdapterPosition() + 1, "已手动撤销");
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(account, TaskCard.TaskState.ACTIVE));
                        //同步账单记录
                        AssistEntityDao.create().sync(mBillEntityDao);
                    }
                    break;
            }
        }

        @Override
        public void onSliding(SlidingItem item) {
            if (expandedItem != null && expandedItem != item) {
                expandedItem.hide();
            }
        }

        @Override
        public void onBtnClick(View v) {
            TaskCard taskCard = (TaskCard) datas.get(getAdapterPosition());
            interruptTask(taskCard, getAdapterPosition() + 1);
            ((Accounting) taskCard.t).setSynced(false);
            mAssistDao.deleteAccount((Accounting) taskCard.t);
            EventBus.getDefault().post(new UpdateTaskCardEvent<>(taskCard.t, TaskCard.TaskState.DELETED));
            List<TaskCard<Accounting>> list = new ArrayList<>();
            list.add(taskCard);
            countBalance(AccountingActivity.TYPE_DELETE, list);
            //同步账单记录
            AssistEntityDao.create().sync(mBillEntityDao);
        }

        @Override
        public void onContentClick(View v) {
            Accounting accounting = ((TaskCard<Accounting>) datas.get(getAdapterPosition())).t;
            mDelItem.setVisibility(View.GONE);
            mEditAccountItem.setVisibility(View.VISIBLE);
            mIvShowMore.setVisibility(View.GONE);
            mLlAccountMemo.setVisibility(View.VISIBLE);
            mLlAccountRtime.setVisibility(View.VISIBLE);
            mEditAccountAmount.getEditText().setText(accounting.getAmount() + "");
            if (accounting.getAtype() == 0) {
                mEditAccountAtype.setText("支出");
            } else {
                mEditAccountAtype.setText("收入");
            }
            mEditAccountEtype.setText(accounting.getEtype());
            String dateTime = TimeUtils.formatDateTime(accounting.getRdate());
            String[] datetimes = dateTime.split(" ");
            mEditAccountDate.setText(datetimes[0]);
            mEditAccountTime.setText(datetimes[1]);
            mEditAccountMemo.setText(accounting.getMemo());
            mTvSave.setText("保存");
        }

        @Override
        public void onExpanded(SlidingItem item) {
            expandedItem = item;
        }

        @Override
        public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            String month = monthOfYear + 1 < 10 ? "0" + (monthOfYear + 1) : "" + (monthOfYear + 1);
            String day = dayOfMonth < 10 ? "0" + dayOfMonth : "" + dayOfMonth;
            mEditAccountDate.setText(new StringBuilder().append(year).append("年").append(month).append("月").append(day).append("日").toString());
        }

        @Override
        public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
            String hour = hourOfDay >= 10 ? "" + hourOfDay : "0" + hourOfDay;
            String min = minute >= 10 ? "" + minute : "0" + minute;
            mEditAccountTime.setText(hour + ":" + min);
        }

        /**
         * 计算余额
         **/
        private void countBalance(final int type, final List<TaskCard<Accounting>> taskcards) {
            Single.just(0)
                    .doOnSuccess(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) throws Exception {
                            float balance = AppConfig.dPreferences.getFloat(AppConfig.ACCOUNT_AMOUNT, 0);
                            switch (type) {
                                case AccountingActivity.TYPE_ADD:
                                case AccountingActivity.TYPE_UPDATE:
                                    for (TaskCard<Accounting> taskcard : taskcards) {
                                        if (taskcard.t.getAtype() == 0) {    //支出
                                            balance -= taskcard.t.getAmount();
                                        } else {
                                            balance += taskcard.t.getAmount();
                                        }
                                    }
                                    break;
                                case AccountingActivity.TYPE_DELETE:
                                    for (TaskCard<Accounting> taskcard : taskcards) {
                                        if (taskcard.t.getAtype() == 0) {    //支出
                                            balance += taskcard.t.getAmount();
                                        } else {
                                            balance -= taskcard.t.getAmount();
                                        }
                                    }
                                    break;
                            }
                            AppConfig.dPreferences.edit().putFloat(AppConfig.ACCOUNT_AMOUNT, balance).commit();
                        }
                    })
                    .observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .subscribe();
        }
    }

    class MemoHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, MemoEditDialog.OnMemoEditListener {

        @BindView(R.id.edit_memo_time)
        TextView mEditMemoTime;
        @BindView(R.id.edit_memo_count)
        TextView mEditMemoCount;
        @BindView(R.id.edit_memo_content)
        EditText mEditMemoContent;
        @BindView(R.id.tv_save)
        TextView mTvSave;
        @BindView(R.id.edit_memo_item)
        LinearLayout mEditMemoItem;
        @BindView(R.id.memo_content)
        TextView mMemoContent;
        @BindView(R.id.del_item)
        SlidingItem mDelItem;
        @BindView(R.id.tv_state)
        TextView mTvState;
        private AssistEntityDao.MemoEntityDao mMemoEntityDao;

        public MemoHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mMemoEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.MemoEntityDao.class);
            mDelItem.setOnSlidingItemListener(this);
            mEditMemoContent.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    mEditMemoCount.setText("字数" + s.length());
                    if (TextUtils.isEmpty(mEditMemoContent.getText().toString().trim()))
                        mTvSave.setTextColor(mContext.getResources().getColor(R.color.forbid_click_color));
                    else
                        mTvSave.setTextColor(mContext.getResources().getColor(R.color.base_blue));
                }
            });
            mEditMemoContent.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        /*TaskCard taskCard = (TaskCard) datas.get(getAdapterPosition());
                        interruptTask(taskCard, getAdapterPosition() + 1);*/
                        stopRecognize();
                    }
                    return false;
                }
            });
        }

        @OnClick({R.id.edit_memo_full_screen, R.id.tv_cancel, R.id.tv_save, R.id.tv_state})
        public void onClick(View view) {
            TaskCard taskCard = (TaskCard) datas.get(getAdapterPosition());
            Memo memo = (Memo) taskCard.t;
            // interruptTask(taskCard, getAdapterPosition() + 1);
            stopRecognize();
            switch (view.getId()) {
                case R.id.edit_memo_full_screen:
                    MemoEditDialog memoEditDialog = new MemoEditDialog(mContext, mEditMemoContent.getText().toString(),
                            mEditMemoTime.getText().toString(), memo.getId() == null, this);
                    memoEditDialog.show();
                    break;
                case R.id.tv_cancel:
                    cancelHandle(memo);
                    break;
                case R.id.tv_save:
                    saveHandle(memo);
                    break;
                case R.id.tv_state:
                    if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                        taskCard.taskState = TaskCard.TaskState.ACTIVE;
                        memo.setSynced(false);
                        mAssistDao.insertMemo(memo);
                        sendForEndTask();
                        datas.add(getAdapterPosition() + 1, "已手动撤销");
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(memo, TaskCard.TaskState.ACTIVE));
                        //与服务器同步数据
                        AssistEntityDao.create().sync(mMemoEntityDao);
                    }
                    break;
            }
        }

        private void saveHandle(Memo memo) {
            String content = mEditMemoContent.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                return;
            }
            //切换视图
            mDelItem.setVisibility(View.VISIBLE);
            mEditMemoItem.setVisibility(View.GONE);
            mMemoContent.setText(content);
            //保存记录
            memo.setContent(content);
            memo.setSynced(false);
            if (memo.getId() == null) {
                memo.setCreated(new Date());
                mAssistDao.insertMemo(memo);
                datas.add(getAdapterPosition() + 1, "备忘已创建");
                notifyItemInserted(getAdapterPosition() + 1);
                ((MainActivity) mListFragment.getActivity()).resetEditState(Memo.class);
                sendForEndTask();
                EventBus.getDefault().post(new RobotTipsEvent(""));
                IflyRecognizer.getInstance().setRecognizeMode(false);
            } else {
                memo.setModified(new Date());
                mAssistDao.updateMemo(memo);
                EventBus.getDefault().post(new UpdateTaskCardEvent<>(memo, false));
            }
            //与服务器同步数据
            AssistEntityDao.create().sync(mMemoEntityDao);
        }

        private void cancelHandle(Memo memo) {
            if (memo.getId() == null) {
                datas.set(getAdapterPosition(), "备忘已取消创建");
                notifyItemChanged(getAdapterPosition());
                ((MainActivity) mListFragment.getActivity()).resetEditState(Memo.class);
                sendForEndTask();
                EventBus.getDefault().post(new RobotTipsEvent(""));
                IflyRecognizer.getInstance().setRecognizeMode(false);
            } else {
                mDelItem.setVisibility(View.VISIBLE);
                mEditMemoItem.setVisibility(View.GONE);
            }
        }

        @Override
        public void onSliding(SlidingItem item) {
            if (expandedItem != null && expandedItem != item)
                expandedItem.hide();
        }

        @Override
        public void onBtnClick(View v) {
            TaskCard taskCard = (TaskCard) datas.get(getAdapterPosition());
            interruptTask(taskCard, getAdapterPosition() + 1);
            ((Memo) taskCard.t).setSynced(false);
            mAssistDao.deleteMemo((Memo) taskCard.t);
            EventBus.getDefault().post(new UpdateTaskCardEvent<>(taskCard.t, TaskCard.TaskState.DELETED));
            //与服务器同步数据
            AssistEntityDao.create().sync(mMemoEntityDao);
        }

        @Override
        public void onContentClick(View v) {
            Memo memo = ((TaskCard<Memo>) datas.get(getAdapterPosition())).t;
            mDelItem.setVisibility(View.GONE);
            mEditMemoItem.setVisibility(View.VISIBLE);
            mEditMemoContent.setText(memo.getContent());
            String time;
            if (memo.getModified() == null) {
                time = new SimpleDate().toString();
            } else {
                time = TimeUtils.getTime(memo.getModified());
            }
            mEditMemoTime.setText("今天" + time);
            mEditMemoCount.setText("字数" + mEditMemoContent.getText().length());
            mTvSave.setText("保存");
        }

        @Override
        public void onExpanded(SlidingItem item) {
            expandedItem = item;
        }

        @Override
        public void onCancel() {
            Memo memo = ((TaskCard<Memo>) datas.get(getAdapterPosition())).t;
            cancelHandle(memo);
        }

        @Override
        public void onBack(String content) {
            mEditMemoContent.setText(content);
            mEditMemoContent.setSelection(content.length());
        }

        @Override
        public void onSave(String content) {
            Memo memo = ((TaskCard<Memo>) datas.get(getAdapterPosition())).t;
            mEditMemoContent.setText(content);
            saveHandle(memo);
        }
    }

    class AlarmHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, TimePickerDialog.OnTimeSetListener {
        @BindView(R.id.edit_alarm_time)
        TextView mEditAlarmTime;
        @BindView(R.id.edit_alarm_desc)
        TextView mEditAlarmDesc;
        @BindView(R.id.edit_alarm_ring)
        public TextView mEditAlarmRing;
        @BindView(R.id.edit_alarm_fr)
        TextView mEditAlarmFr;
        @BindView(R.id.iv_show_more)
        ImageView mIvShowMore;
        @BindView(R.id.tv_save)
        TextView mTvSave;
        @BindView(R.id.edit_alarm_item)
        LinearLayout mEditAlarmItem;
        @BindView(R.id.alarm_time)
        TextView mAlarmTime;
        @BindView(R.id.alarm_switch_btn)
        SwitchButton mAlarmSwitchBtn;
        @BindView(R.id.alarm_desc)
        TextView mAlarmDesc;
        @BindView(R.id.alarm_fr)
        TextView mAlarmFr;
        @BindView(R.id.del_item)
        SlidingItem mDelItem;
        @BindView(R.id.tv_state)
        TextView mTvState;
        @BindView(R.id.ll_edit_ring)
        LinearLayout mLlEditRing;
        @BindView(R.id.ll_edit_fr)
        LinearLayout mLlEditFr;
        private AssistEntityDao.AlarmEntityDao mAlarmEntityDao;

        public AlarmHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mAlarmEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.AlarmEntityDao.class);
            mDelItem.setOnSlidingItemListener(this);
        }

        @OnClick({R.id.ll_edit_time, R.id.ll_edit_desc, R.id.ll_edit_ring, R.id.ll_edit_fr,
                R.id.iv_show_more, R.id.tv_cancel, R.id.tv_save, R.id.alarm_switch_btn, R.id.tv_state})
        public void onClick(View view) {
            TaskCard taskCard = (TaskCard) datas.get(getAdapterPosition());
            AlarmClock alarm = (AlarmClock) taskCard.t;
            interruptTask(taskCard, getAdapterPosition() + 1);
            switch (view.getId()) {
                case R.id.ll_edit_time:
                    SimpleDate rTime = new SimpleDate(mEditAlarmTime.getText().toString());
                    TimePickerDialog timePicker = TimePickerDialog.newInstance(
                            AlarmHolder.this,
                            rTime.getHour(),
                            rTime.getMinute(),
                            true);
                    timePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    timePicker.show(mListFragment.getActivity().getFragmentManager(), "TimePicker");
                    break;
                case R.id.ll_edit_desc:
                    AlarmItemDialog itemDialog = new AlarmItemDialog(mContext, mEditAlarmDesc.getText().toString(), new AlarmItemDialog.OnItemSelectedListener() {
                        @Override
                        public void onSelected(String item) {
                            mEditAlarmDesc.setText(item);
                        }
                    });
                    itemDialog.show();
                    break;
                case R.id.ll_edit_ring:
                    new RingListDialog(mContext, mEditAlarmRing.getText().toString(), path)
                            .setOnRingSelectedListener(new RingListDialog.OnRingSelectedListener() {
                                @Override
                                public void onSelected(String ring, String path) {
                                    mEditAlarmRing.setText(ring);
                                    ChatListAdapter.this.path = path;
                                }
                            }).show();
                    break;
                case R.id.ll_edit_fr:
                    AlarmFrDialog alarmFrDialog = new AlarmFrDialog((Activity) mContext, alarmFr, repeat, new AlarmFrDialog.OnResultListener() {
                        @Override
                        public void onResult(int fr, boolean repeat) {
                            mEditAlarmFr.setText(AssistUtils.transalteWeekDayString(repeat ? fr : 0));
                            alarmFr = fr;
                            ChatListAdapter.this.repeat = repeat;
                        }
                    });
                    alarmFrDialog.show();
                    break;
                case R.id.iv_show_more:
                    if (mLlEditFr.getVisibility() == View.GONE) {    //展开
                        mIvShowMore.setImageLevel(1);
                        mLlEditRing.setVisibility(View.VISIBLE);
                        mLlEditFr.setVisibility(View.VISIBLE);
                    } else {     //收起
                        mIvShowMore.setImageLevel(0);
                        mLlEditFr.setVisibility(View.GONE);
                        mLlEditRing.setVisibility(View.GONE);
                    }
                    break;
                case R.id.tv_cancel:
                    if (alarm.getId() == null) {
                        //新建，点击则取消创建
                        datas.set(getAdapterPosition(), "闹钟已取消创建");
                        notifyItemChanged(getAdapterPosition());
                        ((MainActivity) mListFragment.getActivity()).resetEditState(AlarmClock.class);
                    } else {
                        //编辑，点击则收起编辑视图
                        mDelItem.setVisibility(View.VISIBLE);
                        mEditAlarmItem.setVisibility(View.GONE);
                    }
                    break;
                case R.id.tv_save:
                    /* 显示完成态视图 */
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditAlarmItem.setVisibility(View.GONE);
                    String time = mEditAlarmTime.getText().toString();
                    String desc = mEditAlarmDesc.getText().toString();
                    mAlarmTime.setText(time);
                    mAlarmDesc.setText(desc);
                    mAlarmFr.setText(AssistUtils.transalteWeekDayString(repeat ? alarmFr : 0));
                    mAlarmSwitchBtn.setChecked(true);
                    //保存闹钟记录
                    alarm.setFrequency(alarmFr);
                    alarm.setValid(1);
                    alarm.setRtime(new SimpleDate(time).toValue());
                    alarm.setRing(mEditAlarmRing.getText().toString());
                    alarm.setItem(desc);
                    alarm.setPath(path);
                    alarm.setRepeat(ChatListAdapter.this.repeat);
                    AssistUtils.setAlarmRdate(alarm);
                    alarm.setSynced(false);
                    if (alarm.getId() == null) {
                        alarm.setCreated(new Date());
                        mAssistDao.insertAlarm(alarm);
                        datas.add(getAdapterPosition() + 1, "闹钟已创建");
                        notifyItemInserted(getAdapterPosition() + 1);
                        ((MainActivity) mListFragment.getActivity()).resetEditState(AlarmClock.class);
                    } else {
                        switchAlarm(alarm, RemindService.CANCEL);
                        mAssistDao.updateAlarm(alarm);
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(alarm, false));
                    }
                    //与服务器同步
                    AssistEntityDao.create().sync(mAlarmEntityDao);
                    //开启闹钟服务
                    switchAlarm(alarm, RemindService.ADD);
                    break;
                case R.id.alarm_switch_btn:
                    mDelItem.hide();
                    mAlarmSwitchBtn.setChecked(mAlarmSwitchBtn.isChecked());
                    AlarmClock switchAlarm = mAssistDao.findAlarmById(alarm.getId());
                    //防止在数据库中已删除的记录被操作
                    if (switchAlarm != null) {
                        switchAlarm.setValid(mAlarmSwitchBtn.isChecked() ? 1 : 0);
                        if (switchAlarm.getValid() == 1) {
                            AssistUtils.setAlarmRdate(switchAlarm);
                            switchAlarm.setSynced(false);
                        }
                        AssistDao.getInstance().updateAlarm(switchAlarm);
                        Intent aIntent = new Intent(mContext, RemindService.class);
                        aIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + (mAlarmSwitchBtn.isChecked() ? RemindService.ADD : RemindService.CANCEL));
                        aIntent.putExtra(RemindService.ID, switchAlarm.getId());
                        mContext.startService(aIntent);
                        if (switchAlarm.getValid() == 1) {      //同步修改响铃日期
                            AssistEntityDao.create().sync(mAlarmEntityDao);
                        }
                    }
                    break;
                case R.id.tv_state:
                    if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                        taskCard.taskState = TaskCard.TaskState.ACTIVE;
                        alarm.setSynced(false);
                        mAssistDao.insertAlarm(alarm);
                        switchAlarm(alarm, RemindService.ADD);
                        sendForEndTask();
                        datas.add(getAdapterPosition() + 1, "已手动撤销");
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(alarm, TaskCard.TaskState.ACTIVE));
                        //与服务器同步
                        AssistEntityDao.create().sync(mAlarmEntityDao);
                    }
                    break;
            }
        }

        @Override
        public void onSliding(SlidingItem item) {
            if (expandedItem != null && expandedItem != item)
                item.hide();
        }

        @Override
        public void onBtnClick(View v) {
            TaskCard taskCard = (TaskCard) datas.get(getAdapterPosition());
            AlarmClock alarmClock = (AlarmClock) taskCard.t;
            interruptTask(taskCard, getAdapterPosition() + 1);
            alarmClock.setSynced(false);
            mAssistDao.deleteAlarm(alarmClock);
            switchAlarm(alarmClock, RemindService.CANCEL);
            EventBus.getDefault().post(new UpdateTaskCardEvent<>(alarmClock, TaskCard.TaskState.DELETED));
            //与服务器同步
            AssistEntityDao.create().sync(mAlarmEntityDao);
        }

        @Override
        public void onContentClick(View v) {
            /* 显示编辑态视图 */
            AlarmClock alarm = ((TaskCard<AlarmClock>) datas.get(getAdapterPosition())).t;
            mDelItem.setVisibility(View.GONE);
            mIvShowMore.setVisibility(View.GONE);
            mEditAlarmItem.setVisibility(View.VISIBLE);
            mLlEditRing.setVisibility(View.VISIBLE);
            mLlEditFr.setVisibility(View.VISIBLE);
            mEditAlarmTime.setText(new SimpleDate(alarm.getRtime()).toString());
            mEditAlarmDesc.setText(alarm.getItem());
            mEditAlarmRing.setText(TextUtils.isEmpty(alarm.getRing()) ? "默认" : alarm.getRing());
            mEditAlarmFr.setText(AssistUtils.transalteWeekDayString(alarm.getFrequency()));
            mTvSave.setText("保存");
        }

        @Override
        public void onExpanded(SlidingItem item) {
            expandedItem = item;
        }

        @Override
        public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
            String hour = hourOfDay >= 10 ? "" + hourOfDay : "0" + hourOfDay;
            String min = minute >= 10 ? "" + minute : "0" + minute;
            mEditAlarmTime.setText(hour + ":" + min);
        }
    }

    class RemindHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

        @BindView(R.id.edit_remind_content)
        TextInputLayout mEditRemindContent;
        @BindView(R.id.edit_remind_date)
        TextView mEditRemindDate;
        @BindView(R.id.edit_remind_time)
        TextView mEditRemindTime;
        @BindView(R.id.edit_remind_fr)
        TextView mEditRemindFr;
        @BindView(R.id.ll_remind_fr)
        LinearLayout mLlRemindFr;
        @BindView(R.id.iv_show_more)
        ImageView mIvShowMore;
        @BindView(R.id.tv_save)
        TextView mTvSave;
        @BindView(R.id.edit_remind_item)
        LinearLayout mEditRemindItem;
        @BindView(R.id.remind_datetime)
        TextView mRemindDatetime;
        @BindView(R.id.remind_content)
        TextView mRemindContent;
        @BindView(R.id.del_item)
        SlidingItem mDelItem;
        @BindView(R.id.tv_state)
        TextView mTvState;
        @BindView(R.id.et_remind)
        EditText mEtRemind;
        private AssistEntityDao.RemindEntityDao mRemindEntityDao;

        public RemindHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mRemindEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.RemindEntityDao.class);
            mDelItem.setOnSlidingItemListener(this);
            mEtRemind.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        TaskCard taskCard = (TaskCard) datas.get(getAdapterPosition());
                        interruptTask(taskCard, getAdapterPosition() + 1);
                    }
                    return false;
                }
            });
            mEtRemind.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (TextUtils.isEmpty(mEtRemind.getText().toString().trim())) {
                        mTvSave.setTextColor(mContext.getResources().getColor(R.color.forbid_click_color));
                    } else {
                        mTvSave.setTextColor(mContext.getResources().getColor(R.color.white));
                    }
                }
            });
        }

        @OnClick({R.id.edit_remind_date_box, R.id.edit_remind_time, R.id.ll_remind_fr,
                R.id.iv_show_more, R.id.tv_cancel, R.id.tv_save, R.id.tv_state})
        public void onClick(View view) {
            //手动操作
            TaskCard taskCard = (TaskCard) datas.get(getAdapterPosition());
            Remind remind = (Remind) taskCard.t;
            interruptTask(taskCard, getAdapterPosition() + 1);
            switch (view.getId()) {
                case R.id.edit_remind_date_box:
                    Date rDate = TimeUtils.parse(mEditRemindDate.getText().toString());
                    Calendar now = Calendar.getInstance();
                    now.setTime(rDate);
                    //将当前日期传入日期选择器，并显示
                    DatePickerDialog datePicker = DatePickerDialog.newInstance(
                            RemindHolder.this,
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                    );
                    datePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    datePicker.show(mListFragment.getActivity().getFragmentManager(), "DatePicker");
                    break;
                case R.id.edit_remind_time:
                    SimpleDate rTime = new SimpleDate(mEditRemindTime.getText().toString());
                    TimePickerDialog timePicker = TimePickerDialog.newInstance(
                            RemindHolder.this,
                            rTime.getHour(),
                            rTime.getMinute(),
                            true);
                    timePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    timePicker.show(mListFragment.getActivity().getFragmentManager(), "TimePicker");
                    break;
                case R.id.ll_remind_fr:
                    final Calendar cl = Calendar.getInstance();
                    if (remind.getRdate() != null)
                        cl.setTime(remind.getRdate());
                    RemindFrDialog frDialog = new RemindFrDialog(mListFragment.getActivity(), remindFr, cl, new RemindFrDialog.OnResultListener() {
                        @Override
                        public void onResult(int fr) {
                            remindFr = fr;
                            mEditRemindFr.setText(AssistUtils.translateRemindFrequency(fr, cl));
                        }
                    });
                    frDialog.show();
                    break;
                case R.id.iv_show_more:
                    if (mLlRemindFr.getVisibility() == View.VISIBLE) {
                        mIvShowMore.setImageLevel(0);
                        mLlRemindFr.setVisibility(View.GONE);
                    } else {
                        mIvShowMore.setImageLevel(1);
                        mLlRemindFr.setVisibility(View.VISIBLE);
                    }
                    break;
                case R.id.tv_cancel:
                    if (remind.getId() == null) {
                        datas.set(getAdapterPosition(), "提醒已取消创建");
                        notifyItemChanged(getAdapterPosition());
                        ((MainActivity) mListFragment.getActivity()).resetEditState(Remind.class);
                    } else {
                        mDelItem.setVisibility(View.VISIBLE);
                        mEditRemindItem.setVisibility(View.GONE);
                    }
                    break;
                case R.id.tv_save:
                    String content = mEtRemind.getText().toString().trim();
                    if (TextUtils.isEmpty(content)) {
                        mEditRemindContent.setErrorEnabled(true);
                        mEditRemindContent.setError("请输入内容");
                        if (mEditRemindContent.getEditText().getBackground() != null) {
                            mEditRemindContent.getEditText().getBackground().clearColorFilter();
                        }
                        return;
                    }
                    mEditRemindContent.setError("");
                    mEditRemindContent.setErrorEnabled(false);
                    /* 显示完成态视图 */
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditRemindItem.setVisibility(View.GONE);
                    /* 设置文本信息 */
                    mRemindContent.setText(content);
                    remind.setFrequency(remindFr);
                    String rdate = mEditRemindDate.getText().toString();
                    String rtime = mEditRemindTime.getText().toString();
                    /* 保存记录 */
                    if (remind.getId() != null) {
                        switchRemind(remind, RemindService.CANCEL);
                    }
                    remind.setContent(content);

                    remind.setRdate(TimeUtils.parseDateTime(rdate + " " + rtime));
                    remind.setRtime(rtime);
                    remind.setSynced(false);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(remind.getRdate());
                    mRemindDatetime.setText(new StringBuilder().append(remind.getFrequency() == 0 ? rdate : AssistUtils.translateRemindFrequency(remind.getFrequency(), calendar)).append("    ").append(rtime).toString());
                    if (remind.getId() == null) {
                        remind.setCreated(new Date());
                        mAssistDao.insertRemind(remind);
                        datas.add(getAdapterPosition() + 1, "提醒已创建");
                        notifyItemInserted(getAdapterPosition() + 1);
                        ((MainActivity) mListFragment.getActivity()).resetEditState(Remind.class);
                    } else {
                        /* 先取消原来的提醒，在保存新的 */
                        mAssistDao.updateRemind(remind);
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(remind, false));
                    }
                    //与服务器同步修改
                    AssistEntityDao.create().sync(mRemindEntityDao);
                    /* 通知提醒服务 */
                    switchRemind(remind, RemindService.ADD);
                    break;
                case R.id.tv_state:
                    if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                        taskCard.taskState = TaskCard.TaskState.ACTIVE;
                        remind.setSynced(false);
                        mAssistDao.insertRemind(remind);
                        switchRemind(remind, RemindService.ADD);
                        // notifyItemChanged(getAdapterPosition());
                        sendForEndTask();
                        datas.add(getAdapterPosition() + 1, "已手动撤销");
                        // notifyItemInserted(getAdapterPosition() + 1);
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(remind, TaskCard.TaskState.ACTIVE));
                        //与服务器同步修改
                        AssistEntityDao.create().sync(mRemindEntityDao);
                    }
                    break;
            }
        }

        @Override
        public void onSliding(SlidingItem item) {
            if (expandedItem != null && expandedItem != item)
                expandedItem.hide();
        }

        @Override
        public void onBtnClick(View v) {
            /*List<TaskCard<Remind>> taskList = getCards(Remind.class);
            for (TaskCard card : taskList) {
                if (card.taskState == TaskCard.TaskState.REVOCABLE)
                    card.taskState = TaskCard.TaskState.DELETED;
            }*/
            TaskCard card = (TaskCard) datas.get(getAdapterPosition());
            Remind delRemind = (Remind) card.t;
            interruptTask(card, getAdapterPosition() + 1);
            if (delRemind != null) {
                // 取消提醒服务，并删除数据库记录
                switchRemind(delRemind, RemindService.CANCEL);
                delRemind.setSynced(false);
                mAssistDao.deleteRemind(delRemind);
                //与服务器同步删除
                AssistEntityDao.create().sync(mRemindEntityDao);
                EventBus.getDefault().post(new UpdateTaskCardEvent<>(delRemind, TaskCard.TaskState.DELETED));
            }
            //sendMessageToRobot("删除提醒");
        }

        @Override
        public void onContentClick(View v) {
            /* 显示编辑态视图 */
            Remind remind = (Remind) ((TaskCard) datas.get(getAdapterPosition())).t;
            mDelItem.setVisibility(View.GONE);
            mEditRemindItem.setVisibility(View.VISIBLE);
            mLlRemindFr.setVisibility(View.VISIBLE);
            mIvShowMore.setVisibility(View.GONE);
            mEtRemind.setText(remind.getContent());
            mEditRemindDate.setText(TimeUtils.format(remind.getRdate()));
            mEditRemindTime.setText(new SimpleDate(remind.getRtime()).toString());
            Calendar cl = Calendar.getInstance();
            cl.setTime(remind.getRdate());
            mEditRemindFr.setText(AssistUtils.translateRemindFrequency(remind.getFrequency(), cl));
            mTvSave.setText("保存");
        }

        @Override
        public void onExpanded(SlidingItem item) {
            expandedItem = item;
        }

        @Override
        public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            String month = monthOfYear + 1 < 10 ? "0" + (monthOfYear + 1) : "" + (monthOfYear + 1);
            String day = dayOfMonth < 10 ? "0" + dayOfMonth : "" + dayOfMonth;
            mEditRemindDate.setText(new StringBuilder().append(year).append("年").append(month).append("月").append(day).append("日").toString());
        }

        @Override
        public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
            String hour = hourOfDay >= 10 ? "" + hourOfDay : "0" + hourOfDay;
            String min = minute >= 10 ? "" + minute : "0" + minute;
            mEditRemindTime.setText(hour + ":" + min);
        }
    }

    /**
     * 任务记录列表视图
     **/
    class TaskListHolder extends RecyclerView.ViewHolder {
        RecyclerView taskList;

        public TaskListHolder(View itemView) {
            super(itemView);
            taskList = (RecyclerView) itemView.findViewById(R.id.rv_assistant);
        }
    }

    /**
     * 有声内容专辑列表视图
     **/
    class TingAlbumHolder extends RecyclerView.ViewHolder {
        RecyclerView albumListView;

        public TingAlbumHolder(View itemView) {
            super(itemView);
            albumListView = (RecyclerView) itemView.findViewById(R.id.rv_assistant);
        }
    }

    /**
     * 播放专辑视图
     **/
    class TingPlayHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_album)
        ImageView mIvAlbum;
        @BindView(R.id.tv_album_title)
        TextView mTvAlbumTitle;
        @BindView(R.id.tv_last_track)
        TextView mTvLastTrack;
        @BindView(R.id.tv_play_count)
        TextView mTvPlayCount;
        @BindView(R.id.tv_track_count)
        TextView mTvTrackCount;
        @BindView(R.id.tv_subscribe)
        TextView mTvSubscribe;
        @BindView(R.id.iv_ting_switch)
        ImageView mIvTingSwitch;
        @BindView(R.id.tv_track_title)
        TextView mTvTrackTitle;
        @BindView(R.id.iv_clock)
        ImageView mIvClock;
        @BindView(R.id.tv_duration)
        TextView mTvDuration;
        @BindView(R.id.tv_created)
        TextView mTvCreated;
        @BindView(R.id.ll_play_track_box)
        LinearLayout mLlPlayTrackBox;

        public TingPlayHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick({R.id.tv_subscribe, R.id.tv_choose_set, R.id.ll_play_track_box, R.id.iv_ting_switch})
        public void onClick(View view) {
            TingAlbumMsg albumMsg = (TingAlbumMsg) datas.get(getAdapterPosition());
            Album album = albumMsg.getAlbums().get(0);
            switch (view.getId()) {
                case R.id.tv_subscribe:
                    LevelListDrawable ld = (LevelListDrawable) mTvSubscribe.getBackground();
                    if (ld.getLevel() == 0) {      //订阅
                        ld.setLevel(1);
                        mTvSubscribe.setText("已订阅");
                        TingAlbumDao.getInstance().insertSubscribe(album);
                    } else {
                        ld.setLevel(0);
                        mTvSubscribe.setText("订阅");
                        TingAlbumDao.getInstance().delSubscribeById(album.getId());
                    }
                    break;
                case R.id.tv_choose_set:
                    Intent intent = new Intent(mContext, TingAlbumDetailActivity.class);
                    intent.putExtra(TingAlbumDetailActivity.ALBUM_ID, album.getId());
                    int type = album.getCoverUrlMiddle().contains(TingPlayProcessor.KAOLA_FM) ? TingAlbumDetailActivity.KAOLA : TingAlbumDetailActivity.XIMALAYA;
                    intent.putExtra(TingAlbumDetailActivity.ALBUM_TYPE, type);
                    mContext.startActivity(intent);
                    ((Activity) mContext).overridePendingTransition(R.anim.activity_start_in, R.anim.activity_start_out);
                    break;
                case R.id.ll_play_track_box:
                case R.id.iv_ting_switch:
                    boolean isPlaying = XmlyManager.get().isPlaying();
                    if (isPlaying) {
                        XmPlayerManager.getInstance(mContext).pause();
                    } else {
                        XmPlayerManager.getInstance(mContext).play();
                    }
                    break;
            }
        }
    }

    /**
     * 提示语视图
     **/
    class TipsHolder extends RecyclerView.ViewHolder {
        public TextView tips;

        public TipsHolder(View itemView) {
            super(itemView);
            tips = (TextView) itemView.findViewById(R.id.tv_tips);
        }
    }

    /**
     * 备忘添加模式视图
     **/
    class MemoAppendHolder extends RecyclerView.ViewHolder implements MemoEditDialog.OnMemoEditListener {
        @BindView(R.id.edit_memo_time)
        TextView mEditMemoTime;
        @BindView(R.id.edit_memo_count)
        TextView mEditMemoCount;
        @BindView(R.id.edit_memo_content)
        EditText mEditMemoContent;
        @BindView(R.id.tv_save)
        TextView mTvSave;
        @BindView(R.id.tv_cancel)
        TextView mTvCancel;
        @BindView(R.id.edit_memo_item)
        LinearLayout mEditMemoItem;
        @BindView(R.id.del_item)
        SlidingItem mDelItem;
        /*public EditText etAppend;
        public Button btnQuit;*/

        public MemoAppendHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mEditMemoContent.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    mEditMemoCount.setText("字数" + s.length());
                    if (TextUtils.isEmpty(mEditMemoContent.getText().toString().trim()))
                        mTvSave.setTextColor(mContext.getResources().getColor(R.color.forbid_click_color));
                    else
                        mTvSave.setTextColor(mContext.getResources().getColor(R.color.base_blue));
                }
            });
            mEditMemoContent.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        stopRecognize();
                    }
                    return false;
                }
            });
            /*etAppend = (EditText) itemView.findViewById(R.id.et_append);
            btnQuit = (Button) itemView.findViewById(R.id.btn_quit);
            btnQuit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Memo appendMemo = getAppendMemo();
                    appendMemo.setContent(etAppend.getText().toString().trim());
                    appendMemo.setModified(new Date());
                    AssistDao.getInstance().updateMemo(appendMemo);
                    //移除添加模式编辑视图
                    mListFragment.addMessage(new ChatMsgEvent());
                    //添加对话
                    mListFragment.addMessage(new ChatMsgEvent(new SpeechMsg(mContext.getResources().getString(R.string.quit_append_mode)), null, null, null));
                    sendMessageToRobot(mContext.getResources().getString(R.string.quit_append_mode));
                }
            });*/
        }

        @OnClick({R.id.edit_memo_full_screen, R.id.tv_save, R.id.tv_cancel})
        public void onClick(View view) {
            stopRecognize();
            switch (view.getId()) {
                case R.id.edit_memo_full_screen:
                    MemoEditDialog memoEditDialog = new MemoEditDialog(mContext, mEditMemoContent.getText().toString(),
                            mEditMemoTime.getText().toString(), false, this);
                    memoEditDialog.show();
                    break;
                case R.id.tv_cancel:
                    cancel();
                    break;
                case R.id.tv_save:
                    quitAppendMode(getAppendMemo());
                    break;
            }
        }

        /**
         * 取消添加模式
         **/
        private void cancel() {
            //移除添加模式编辑视图
            datas.remove(getAdapterPosition());
            // 添加系统提示语
            EventBus.getDefault().post(new ChatMsgEvent(null, null, "已取消退出添加模式", null));
            sendForQuitAppendMode();
            EventBus.getDefault().post(new RobotTipsEvent(""));
            //退出长录音模式
            IflyRecognizer.getInstance().setRecognizeMode(false);
        }

        /**
         * 保存并退出添加模式
         **/
        private void quitAppendMode(Memo memo) {
            String content = mEditMemoContent.getText().toString().trim();
            if (TextUtils.isEmpty(content))
                return;
            memo.setModified(new Date());
            memo.setContent(content);
            memo.setSynced(false);
            mAssistDao.updateMemo(memo);
            // 通知聊天视图刷新
            EventBus.getDefault().post(new UpdateTaskCardEvent<>(memo, TaskCard.TaskState.INVALID));
            //移除添加模式编辑视图
            datas.set(datas.size() - 1, new TaskCard<>(memo, TaskCard.TaskState.ACTIVE));
            notifyItemChanged(datas.size() - 1);
            // 添加系统提示语
            EventBus.getDefault().post(new ChatMsgEvent(null, null, "已保存退出添加模式", null));
            sendForQuitAppendMode();
            EventBus.getDefault().post(new RobotTipsEvent(""));
            //退出长录音模式
            IflyRecognizer.getInstance().setRecognizeMode(false);
            //同步备忘记录
            final AssistEntityDao.MemoEntityDao memoEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.MemoEntityDao.class);
            AssistEntityDao.create().sync(memoEntityDao);
        }

        @Override
        public void onCancel() {
            cancel();
        }

        @Override
        public void onBack(String content) {
            mEditMemoContent.setText(content);
            mEditMemoContent.setSelection(content.length());
        }

        @Override
        public void onSave(String content) {
            mEditMemoContent.setText(content);
            quitAppendMode(getAppendMemo());
        }

        private void sendForQuitAppendMode() {
            Intent intent = new Intent(mContext, AssistantService.class);
            intent.putExtra(AssistantService.TEXT, "退出");
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
            mContext.startService(intent);
        }
    }

    /**
     * 空视图
     **/
    class EmptyHolder extends RecyclerView.ViewHolder {

        public EmptyHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * 向机器人发送信息
     **/
    private void sendMessageToRobot(String text) {
        if (clickable) {
            Intent intent = new Intent(mContext, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT);
            intent.putExtra(AssistantService.TEXT, text);
            mContext.startService(intent);
        }
    }

    /**
     * 取消任务流
     **/
    private void sendForEndTask() {
        Intent intent = new Intent(mContext, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT_FOR_END_TASK);
        intent.putExtra(AssistantService.END_TASK, true);
        mContext.startService(intent);
    }

    /**
     * 通知提醒服务开/关提醒
     **/
    private void switchRemind(Remind remind, int cmd) {
        Intent rIntent = new Intent(mContext, RemindService.class);
        rIntent.putExtra(RemindService.CMD, (RemindService.REMIND << 4) + cmd);
        rIntent.putExtra(RemindService.ID, remind.getId());
        mContext.startService(rIntent);
    }

    /**
     * 通知提醒服务开/关闹钟
     **/
    private void switchAlarm(AlarmClock alarm, int cmd) {
        Intent rIntent = new Intent(mContext, RemindService.class);
        rIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + cmd);
        rIntent.putExtra(RemindService.ID, alarm.getId());
        mContext.startService(rIntent);
    }

    /**
     * 回复文本视图点击事件
     **/
    private RspMsgItemView.OnItemClickListener rspItemClickListener = new RspMsgItemView.OnItemClickListener() {

        @Override
        public void onSpeakerClick(int p, RspMsgItemView v) {
            /* 合成之前先清除合成动画 */
            EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_END));
            mListFragment.presenter.synthesize((SpeechMsg) datas.get(p), v);
        }

        @Override
        public void onTextClick(int p, RspMsgItemView v) {
            mListFragment.presenter.stopSpeaker();
            mListFragment.stopSpeakerAnimation(v);
        }
    };
}
