package com.lingju.assistant.activity.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.index.model.WrapContentLinearLayoutManager;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.SlidingItem;
import com.lingju.assistant.view.SwitchButton;
import com.lingju.model.dao.AssistDao;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/1/18.
 */
public abstract class BaseAssistActivity extends GoBackActivity {

    @BindView(R.id.tb_assist)
    protected Toolbar mTbAssist;
    @BindView(R.id.tv_time)
    protected TextView mTvTime;
    @BindView(R.id.tv_date)
    protected TextView mTvDate;
    @BindView(R.id.rl_datetime)
    protected RelativeLayout mRlDatetime;
    @BindView(R.id.ll_account)
    protected LinearLayout mLlAccount;
    @BindView(R.id.fl_msg)
    protected FrameLayout mFlMsg;
    @BindView(R.id.ctl_header_bar)
    protected CollapsingToolbarLayout mCtlHeaderBar;
    @BindView(R.id.abl_header)
    protected AppBarLayout mAblHeader;
    @BindView(R.id.assist_recyclerview)
    protected RecyclerView mAssistRecyclerview;
    @BindView(R.id.fab_add)
    protected FloatingActionButton mFabAdd;
    @BindView(R.id.iv_header)
    protected ImageView mIvHeader;
    @BindView(R.id.tv_balance)
    protected TextView mTvBalance;
    @BindView(R.id.et_balance)
    protected EditText mEtBalance;
    @BindView(R.id.cpb_load)
    protected CircleProgressBar mCpbLoad;
    @BindView(R.id.fl_balance)
    protected RelativeLayout mFlBalance;
    /*@BindView(R.id.layout_refresh)
    protected SwipeRefreshLayout mLayoutRefresh;*/

    public final static int HEADER_VIEW = 0;
    public final static int CONTENT_VIEW = 1;
    public final static int ACCOUNT_CREATE_VIEW = 2;
    public final static int STATE_NORMAL = 3;
    public final static int STATE_DELETE = 4;
    protected boolean cancel_or_back = true;  //true:返回  false:取消
    protected int isEditPosition = 0;  //!=0:item正在展出编辑  0:已完成编辑
    protected String[] mDateTimes;
    public LayoutInflater mInflater;
    protected SlidingItem lastItem;
    protected int pageNum;      //记录加载页数
    public Map<Integer, TaskCard> checkMap = new HashMap<>();
    public int mCurrentState = STATE_NORMAL;
    public int initWidth;
    public boolean init = true;
    public int scrollPosition;
    public AssistDao mAssistDao;
    public LinearLayoutManager mLayoutManager;
    public boolean moveable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_assist_list);
        ButterKnife.bind(this);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        mDateTimes = sdf.format(new Date()).split(" ");
        setSupportActionBar(mTbAssist);
        /* Navigation Icon 要設定在 setSupoortActionBar 才有作用 */
        mTbAssist.setNavigationIcon(R.drawable.back_arrow);
        mTbAssist.inflateMenu(R.menu.edit_menu);
        mTbAssist.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cancel_or_back) {  //退出页面
                    setEditTextFocusable(false);
                    onBackPressed();
                } else {      //取消操作
                    cancel_or_back = true;
                    mTbAssist.setNavigationIcon(R.drawable.selector_back);
                    mTbAssist.getMenu().findItem(R.id.delete).setVisible(false);
                    mTbAssist.getMenu().findItem(R.id.edit).setVisible(true);
                    mCurrentState = STATE_NORMAL;
                    mFabAdd.show();
                    checkMap.clear();
                    onCancel();
                }
            }
        });
        mTbAssist.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.edit:
                        if (isEditPosition != 0) {
                            showEditTips();
                            return true;
                        }
                        cancel_or_back = false;
                        mTbAssist.setNavigationIcon(R.drawable.card_close_btn2);
                        mTbAssist.getMenu().findItem(R.id.delete).setVisible(true);
                        mTbAssist.getMenu().findItem(R.id.edit).setVisible(false);
                        mCurrentState = STATE_DELETE;
                        mFabAdd.hide();
                        onEdit();
                        break;
                    case R.id.delete:
                        if (checkMap.size() == 0) {
                            new CommonDialog(BaseAssistActivity.this, "温馨提示", "请选择要删除的记录", "确定").show();
                            return true;
                        }
                        cancel_or_back = true;
                        mTbAssist.setNavigationIcon(R.drawable.selector_back);
                        mTbAssist.getMenu().findItem(R.id.delete).setVisible(false);
                        mTbAssist.getMenu().findItem(R.id.edit).setVisible(true);
                        mCurrentState = STATE_NORMAL;
                        mFabAdd.show();
                        onDeleteChecked();
                        break;
                    case R.id.select_year:
                        setEditTextFocusable(false);
                        selectedYear();
                        break;
                }
                return true;
            }
        });

        /*mLayoutRefresh.setOnRefreshListener(this);
        mLayoutRefresh.setColorSchemeResources(R.color.base_blue, R.color.red, R.color.light_green, R.color.red_style);*/
        mInflater = LayoutInflater.from(this);
        mAssistDao = AssistDao.getInstance();

        //        mCpbLoad.setColorSchemeResources(R.color.red_style, R.color.second_base_color, R.color.base_blue, R.color.colorPrimary);
        mAssistRecyclerview.setHasFixedSize(true);
        mLayoutManager = new WrapContentLinearLayoutManager(this);
        mAssistRecyclerview.setItemAnimator(new DefaultItemAnimator());
        mAssistRecyclerview.setLayoutManager(mLayoutManager);
        mAssistRecyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && moveable) {    //停止滚动
                    moveable = false;
                    moveToPosition(scrollPosition);
                    scrollPosition = -1;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @OnClick(R.id.fab_add)
    public void onClick() {
        if (isEditPosition != 0 && hadEdit()) {
            showEditTips();
        } else {
        /* 折叠AppBarLayout */
            mAblHeader.setExpanded(false);
            mFabAdd.hide();
            collapseCard(isEditPosition);
            onAssistCreate();
        }
    }

    public void moveToPosition(int position) {
        // Log.i("LingJu", "滑动索引：" + position);
        if (position != -1) {
            moveable = false;
            //先获取第一个和最后一个可见item索引
            int first = mLayoutManager.findFirstVisibleItemPosition();
            int last = mLayoutManager.findLastVisibleItemPosition();
            // Log.i("LingJu", "首个可见索引：" + first + " 末尾可见索引：" + last);
            if (position <= first) {   //要移动的item索引在第一个可见索引之前
                mAssistRecyclerview.smoothScrollToPosition(position);
            } else if (position <= last) {      //要移动的item处于可见
                int top = mAssistRecyclerview.getChildAt(position - first).getTop();
                mAssistRecyclerview.smoothScrollBy(0, top);
            } else {     //要移动的item在最后可见item之后
                mAssistRecyclerview.smoothScrollToPosition(position);
                scrollPosition = position;
                moveable = true;
            }
        }
    }

    /**
     * 设置可滑动item焦点
     **/
    public void setFocus(SlidingItem item, boolean focus) {
        item.setSlidable(focus);
        item.setClickable(focus);
    }

    /**
     * 刷新任务卡视图
     **/

    public void refreshCard(int taskState, TextView tvState, TextView content, SlidingItem item, SwitchButton sb) {
        if (sb != null)
            sb.setFocusable(true);
        setFocus(item, true);
        tvState.setVisibility(View.GONE);
        switch (taskState) {
            case TaskCard.TaskState.ACTIVE:
                if (content != null)
                    content.setTextColor(getResources().getColor(R.color.new_text_color_first));
                break;
            case TaskCard.TaskState.REVOCABLE:
                tvState.setText("撤销");
                item.setSlidable(false);
                tvState.setVisibility(View.VISIBLE);
                tvState.setTextColor(getResources().getColor(R.color.base_blue));
                if (sb != null)
                    sb.setFocusable(false);
                break;
            case TaskCard.TaskState.INVALID:
                if (content != null)
                    content.setTextColor(getResources().getColor(R.color.new_text_color_second));
                break;
        }
    }

    /**
     * 显示正在编辑提示框
     **/
    public void showEditTips() {
        new CommonDialog(this, "温馨提示", "请先保存已打开的卡片！", "我知道了")
                .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        moveToPosition(isEditPosition);
                    }
                })
                .show();
    }

   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        menu.findItem(R.id.delete).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }*/

    /**
     * 无编辑态item时点击悬浮加号按钮触发
     **/
    protected abstract void onAssistCreate();

    /**
     * 判断卡片是否修改过
     **/
    public abstract boolean hadEdit();

    /**
     * 折叠起已展开的卡片（用于自动收起展开但未编辑的卡片）
     **/
    public abstract void collapseCard(int position);

    /**
     * 在选择删除记录状态中点击左上方取消按钮时触发
     **/
    protected void onCancel() {
    }

    /**
     * 在选择删除记录状态中点击右上方删除按钮时触发
     **/
    protected void onDeleteChecked() {
    }

    /**
     * 正常显示状态中点击右上方编辑按钮时触发
     **/
    protected void onEdit() {
    }

    /**
     * 下拉刷新
     **/
    protected void onDownRefresh() {
    }

    /**
     * 子类可选择复写
     **/
    public void selectedYear() {
    }

    /**
     * 设置editText的焦点
     **/
    public void setEditTextFocusable(boolean focusable) {

    }

   /* @Override
    public void onRefresh() {
        if (isEditPosition != 0) {
            showEditTips();
            mLayoutRefresh.setRefreshing(false);
            return;
        }
        onDownRefresh();
    }*/
}
