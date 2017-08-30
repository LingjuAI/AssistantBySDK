package com.lingju.assistant.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.LingjuSwipeUpLoadRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.index.INaviSetPoint;
import com.lingju.assistant.activity.index.presenter.NaviSetPointPresenter;
import com.lingju.assistant.view.CommonDeleteDialog;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.lbsmodule.location.Address;
import com.lingju.model.BaiduAddress;
import com.lingju.model.NaviRecord;
import com.lingju.model.dao.BaiduNaviDao;
import com.lingju.model.dao.RecordDao;
import com.lingju.util.NetUtil;
import com.lingju.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/20.
 */
public class NaviSetPointActivity extends GoBackActivity implements INaviSetPoint.INaviSetPointView, LingjuSwipeUpLoadRefreshLayout.OnRefreshListener {
    private static final String TAG = "NaviSetPointActivity";
    private static final String LOADMORE = "loadMore";
    @BindView(R.id.navi_start_addr)
    TextView mNaviStartAddr;
    @BindView(R.id.navi_end_addr)
    TextView mNaviEndAddr;
    @BindView(R.id.navi_record_history_list_box)
    LingjuSwipeUpLoadRefreshLayout mNaviRecordRecyclerBox;
    @BindView(R.id.navi_record_history_list)
    RecyclerView mNaviRecordRecycler;
    @BindView(R.id.status_bar)
    View mStatusBar;
    AppConfig mAppConfig;
    private BaiduNaviDao mNaviDao;
    private RecordDao recordDao;
    private INaviSetPoint.IPresenter mPresenter;
    private LayoutInflater mInflater;
    private int preference;


    private RecordAdapter mAdapter;
    private Address address;
    private static int PER_PAGE = 10;
    private BaiduAddress homeAddress;
    private BaiduAddress companyAddress;
    private MyLinearLayoutManager mLinearLayoutManager;

    /**
     * 导航记录集合
     */
    private final List<NaviRecord> recordList = new ArrayList<>();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("NaviSetPointActivity开始初始化");
        setContentView(R.layout.activity_navi_set_point);
        /** 必须写在setContentView()之后 **/
        ButterKnife.bind(this);
        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = mStatusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        mStatusBar.setLayoutParams(layoutParams);
        mAppConfig = (AppConfig) getApplication();
        mNaviDao = BaiduNaviDao.getInstance();
        recordDao = RecordDao.getInstance();
        mInflater = LayoutInflater.from(this);
        mLinearLayoutManager = new MyLinearLayoutManager(this);
        mNaviRecordRecycler.setLayoutManager(mLinearLayoutManager);
        mPresenter = new NaviSetPointPresenter(this, mAppConfig);
        mPresenter.setLocation();
        homeAddress = mNaviDao.getHomeOrCompanyAddress("家");
        companyAddress = mNaviDao.getHomeOrCompanyAddress("单位");
        mPresenter.initData();
        mPresenter.initBaiduNaiv(this, new Handler());
        // mPresenter.setGoCompanyAndGoHomeCalculate();
        preference = AppConfig.dPreferences.getInt(NaviSetLineActivity.CALCULATE_MODE, BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND);
        mAdapter = new RecordAdapter();
        mNaviRecordRecyclerBox.setColorSchemeResources(R.color.base_blue);
        mNaviRecordRecyclerBox.setOnRefreshListener(this);
        mNaviRecordRecycler.setHasFixedSize(true);
        //recylerView滑动至第一条
        mLinearLayoutManager.scrollToPosition(0);
        mNaviRecordRecycler.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        //recylerView滑动至第一条
        mLinearLayoutManager.scrollToPosition(0);
        //界面重新获取焦点，刷新recycleView的数据
        homeAddress = mNaviDao.getHomeOrCompanyAddress("家");
        companyAddress = mNaviDao.getHomeOrCompanyAddress("单位");
        mPresenter.initStartAddress();
        mAdapter.notifyDataSetChanged();
        mPresenter.setGoCompanyAndGoHomeCalculate();
        super.onResume();
    }

    @Override
    protected void onStop() {
        mPresenter.destoryNaviManager();
        if (mPresenter.isUpdateHistory()) {
            mPresenter.updateHistoryList();
            mPresenter.setUpdateHistory(false);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //退出导航界面清空出发地和目的地
        mAppConfig.startAddress = null;
        mAppConfig.endAddress = null;
        mNaviDao.removeHomeOrCompany("出发地");
        mNaviDao.removeHomeOrCompany("目的地");
        mPresenter.destoryListener();
        mPresenter.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPresenter.updateData(resultCode);
        mAdapter.notifyDataSetChanged();

        super.onActivityResult(requestCode, resultCode, data);
    }

    @OnClick({R.id.navi_back_bt, R.id.navi_map_traffic_bt, R.id.navi_manager_favorite_bt, R.id.navi_start_addr, R.id.navi_end_addr,
            R.id.navi_exchange_bt})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.navi_back_bt:
                goBack();
                break;
            case R.id.navi_exchange_bt:
                mPresenter.exchangeAddress();
                break;
            case R.id.navi_map_traffic_bt:
                startActivity(new Intent(NaviSetPointActivity.this, TrafficShowActivity.class));
                goInto();
                break;
            case R.id.navi_manager_favorite_bt:
                startActivityForResult(new Intent(this, SetFavoriteAddressActivity.class), 1);
                goInto();
                break;
            case R.id.navi_start_addr:
                mPresenter.toSetStartAddr();
                mPresenter.setLocation();
                break;
            case R.id.navi_end_addr:
                mPresenter.toSetEndAddr();
                mPresenter.setLocation();
                break;

        }
    }

    @Override
    public void setStartAddrText(String startAddr) {
        mNaviStartAddr.setText(startAddr);
    }

    @Override
    public String getStartAddrText() {
        return mNaviStartAddr.getText().toString().trim();
    }

    @Override
    public void setEndAddrText(String endAddr) {
        mNaviEndAddr.setText(endAddr);
    }

    @Override
    public String getEndAddrText() {
        return mNaviEndAddr.getText().toString().trim();
    }

    @Override
    public TextView getGoHomeTextView() {
        if (mLinearLayoutManager.findViewByPosition(0) != null) {
            return (TextView) mLinearLayoutManager.findViewByPosition(0).findViewById(R.id.navi_to_home_time);
        } else {
            return null;
        }
    }

    @Override
    public TextView getGoCompanyTextView() {
        if (mLinearLayoutManager.findViewByPosition(0) != null) {
            return (TextView) mLinearLayoutManager.findViewByPosition(0).findViewById(R.id.navi_to_company_time);
        } else {
            return null;
        }
    }

    @Override
    public void showSnackBar(String s) {
        Snackbar.make(mNaviRecordRecycler,s, Snackbar.LENGTH_SHORT).show();
    }


    /**
     * 上拉加载更多
     **/
    @Override
    public void onUpPullRefresh() {
        mNaviRecordRecyclerBox.setRefreshing(false);
        PER_PAGE += 10;
        Boolean moreRecord = recordDao.getMoreRecordList(recordList, PER_PAGE, -1) > 0;
        mAdapter.notifyDataSetChanged();
        if (!moreRecord) {
            showSnackBar("没有更多记录");
           // Toast.makeText(this,"没有更多记录",Toast.LENGTH_SHORT).show();
        }
    }


    class RecordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final static int HEAD_TYPE = 0;
        private final static int ITEM_TYPE = 1;
        private final static int BOTTOM_TYPE = 2;


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView;
            RecyclerView.ViewHolder viewHolder = null;
            if (viewType == HEAD_TYPE) {
                itemView = mInflater.inflate(R.layout.navi_go_home_company, parent, false);
                viewHolder = new NaviGoHomeCompanyHolder(itemView);
            } else if (viewType == ITEM_TYPE) {
                itemView = mInflater.inflate(R.layout.navi_record_item, parent, false);
                viewHolder = new RecordListHolder(itemView);
            } else if (viewType == BOTTOM_TYPE) {
                itemView = mInflater.inflate(R.layout.navi_clear_record, parent, false);
                viewHolder = new RecordListAddAndClearHolder(itemView);
            }
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof NaviGoHomeCompanyHolder) {
                if (companyAddress != null) {
                    ((NaviGoHomeCompanyHolder) holder).setStartAddrToCompanyText("从我的位置出发");
                    mPresenter.updateCompany();
                    ((NaviGoHomeCompanyHolder) holder).setToCompanySetBtnInvisible(true);
                    ((NaviGoHomeCompanyHolder) holder).setGoCompanyTimeTextInvisible(true);
                } else {
                    ((NaviGoHomeCompanyHolder) holder).setStartAddrToCompanyText("点此设置");
                    ((NaviGoHomeCompanyHolder) holder).setToCompanySetBtnInvisible(false);
                    ((NaviGoHomeCompanyHolder) holder).setGoCompanyTimeTextInvisible(false);
                }
                if (homeAddress != null) {
                    ((NaviGoHomeCompanyHolder) holder).setStartAddrToHomeText("从我的位置出发");
                    mPresenter.updateHome();
                    ((NaviGoHomeCompanyHolder) holder).setToHomeSetBtnInvisible(true);
                    ((NaviGoHomeCompanyHolder) holder).setGoHomeTimeTextInvisible(true);
                } else {
                    ((NaviGoHomeCompanyHolder) holder).setStartAddrToHomeText("点此设置");
                    ((NaviGoHomeCompanyHolder) holder).setToHomeSetBtnInvisible(false);
                    ((NaviGoHomeCompanyHolder) holder).setGoHomeTimeTextInvisible(false);
                }

            } else if (holder instanceof RecordListHolder) {
                System.out.println("position is" + (position - 1));
                System.out.println("recordList size is" + recordList.size());
                NaviRecord record = recordList.get(position - 1);
                ((RecordListHolder) holder).mRecordItem.setTag(position - 1);
                if (record != null) {
                    if (record.getStartName() != null && record.getStartName().startsWith("我的位置")) {
                        record.setStartName(record.getStartName().substring(5, record.getStartName().length() - 3));
                    }
                    ((RecordListHolder) holder).setRecordStart(record.getStartName());
                    ((RecordListHolder) holder).setRecordend(record.getEndName());
                }

            } else if (holder instanceof RecordListAddAndClearHolder) {
                ((RecordListAddAndClearHolder) holder).updateRecordList(-1);

            }
        }

        @Override
        public int getItemCount() {
            return recordList.size() + 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return HEAD_TYPE;
            } else if (position == getItemCount() - 1) {
                return BOTTOM_TYPE;
            } else {
                return ITEM_TYPE;
            }
        }

        class NaviGoHomeCompanyHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.navi_to_company_start_addr)
            TextView mNaviToCompanyStartAddr;
            @BindView(R.id.navi_to_home_start_addr)
            TextView mNaviToHomeStartAddr;
            @BindView(R.id.navi_to_company_set_btn)
            ImageButton mNaviToCompanySetBtn;
            @BindView(R.id.navi_to_home_set_btn)
            ImageButton mNaviToHomeSetBtn;
            @BindView(R.id.navi_to_company_time)
            TextView mNaviToCompanyTime;
            @BindView(R.id.navi_to_home_time)
            TextView mNaviToHomeTime;

            public NaviGoHomeCompanyHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            public void setToCompanySetBtnInvisible(Boolean visible) {
                if (visible) {
                    mNaviToCompanySetBtn.setVisibility(View.VISIBLE);
                } else {
                    mNaviToCompanySetBtn.setVisibility(View.INVISIBLE);
                }

            }

            public void setToHomeSetBtnInvisible(Boolean visible) {
                if (visible) {
                    mNaviToHomeSetBtn.setVisibility(View.VISIBLE);
                } else {
                    mNaviToHomeSetBtn.setVisibility(View.INVISIBLE);
                }

            }

            public void setGoHomeTimeTextInvisible(Boolean visible) {
                if (visible) {
                    mNaviToHomeTime.setVisibility(View.VISIBLE);
                } else {
                    mNaviToHomeTime.setVisibility(View.INVISIBLE);
                }

            }

            public void setGoCompanyTimeTextInvisible(Boolean visible) {
                if (visible) {
                    mNaviToCompanyTime.setVisibility(View.VISIBLE);
                } else {
                    mNaviToCompanyTime.setVisibility(View.INVISIBLE);
                }

            }

            public void setStartAddrToCompanyText(String startAddr) {
                mNaviToCompanyStartAddr.setText(startAddr);
            }

            public void setStartAddrToHomeText(String endAddr) {
                mNaviToHomeStartAddr.setText(endAddr);
            }


            @OnClick({R.id.navi_company_bt, R.id.navi_home_bt, R.id.navi_to_company_set_btn, R.id.navi_to_home_set_btn})
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.navi_company_bt:
                        if (mNaviToCompanyStartAddr.getText().equals("点此设置")) {
                            mPresenter.toSetCompanyAddr();
                        } else {
                            if(NetUtil.getInstance(NaviSetPointActivity.this).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)){
                                showSnackBar(mAppConfig.getResources().getString(R.string.no_network));
                               // Toast.makeText(NaviSetPointActivity.this, mAppConfig.getResources().getString(R.string.no_network), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            mPresenter.setLocation();
                            mPresenter.toCompanyNavi();
                        }
                        break;
                    case R.id.navi_home_bt:
                        if (mNaviToHomeStartAddr.getText().equals("点此设置")) {
                            mPresenter.toSetHomeAddr();
                        } else {
                            if(NetUtil.getInstance(NaviSetPointActivity.this).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)){
                                Snackbar.make(mNaviRecordRecycler, mAppConfig.getResources().getString(R.string.no_network), Snackbar.LENGTH_SHORT).show();
                                return;
                            }
                            mPresenter.setLocation();
                            mPresenter.toHomeNavi();
                        }
                        break;
                    case R.id.navi_to_company_set_btn:
                        mPresenter.toSetCompanyAddr();
                        break;
                    case R.id.navi_to_home_set_btn:
                        mPresenter.toSetHomeAddr();
                        break;
                }
            }

        }

        class RecordListHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.record_item)
            RelativeLayout mRecordItem;
            @BindView(R.id.record_start)
            TextView mRecordStart;
            @BindView(R.id.record_end)
            TextView mRecordEnd;

            public RecordListHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            public void setRecordStart(String text) {
                mRecordStart.setText(text);
            }

            public void setRecordend(String text) {
                mRecordEnd.setText(text);
            }

            @OnClick(R.id.record_item)
            public void onItemClick() {
                if(NetUtil.getInstance(NaviSetPointActivity.this).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)){
                   // Toast.makeText(NaviSetPointActivity.this, mAppConfig.getResources().getString(R.string.no_network), Toast.LENGTH_SHORT).show();
                   // showSnackBar(mAppConfig.getResources().getString(R.string.no_network));
                    final CommonDialog commonDialog =  new CommonDialog(NaviSetPointActivity.this,"网络错误","网络状态不佳，请检查网络设置","确定");
                    commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            commonDialog.cancel();
                        }
                    }).show();
                    return;
                }
                int index = (int) mRecordItem.getTag();
                NaviRecord record = recordList.get(index);
                //                final CommonAlertDialog progressDialog = new CommonAlertDialog(NaviSetPointActivity.this, "导航引擎", "线路规划中...");
                //                progressDialog.show();
                final Intent intent = new Intent(NaviSetPointActivity.this, NaviSetLineActivity.class); //你要转向的Activity
                intent.putExtra("start_latitude", record.getStartLatitude());
                intent.putExtra("start_longitude", record.getStartLongitude());
                intent.putExtra("start_address", record.getStartName());
                intent.putExtra("end_latitude", record.getEndLatitude());
                intent.putExtra("end_longitude", record.getEndLongitude());
                intent.putExtra("end_address", record.getEndName());
                startActivity(intent); //执行
                goInto();
                //                Timer timer = new Timer();
                //                TimerTask task = new TimerTask() {
                //                    @Override
                //                    public void run() {
                //                        startActivity(intent); //执行
                //                        goInto();
                //                        progressDialog.cancel();
                //                    }
                //                };
                //                timer.schedule(task, 2000); //2秒后
            }
        }

        class RecordListAddAndClearHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.navi_clear_record_list_bt)
            TextView mNaviClrarRecordListBt;
            @BindView(R.id.navi_record_list_box)
            RelativeLayout mNaviRecordListBox;

            public RecordListAddAndClearHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);

            }

            public void updateRecordList(int type) {
                System.out.println("recordDao.getMoreRecordList(recordList,PER_PAGE)" + recordDao.getMoreRecordList(recordList, PER_PAGE, type));
                boolean moreRecord = false;
                //展示第一页数据
                recordDao.getMoreRecordList(recordList, PER_PAGE, type);

                if (recordList.size() > 0) {
                    mNaviRecordListBox.setVisibility(View.VISIBLE);
                } else {
                    mNaviRecordListBox.setVisibility(View.GONE);
                }
            }

            public void cleanHistory() {
                recordDao.removeHistoryRecord();
                recordDao.getRecordList(recordList, -1);
                updateRecordList(-1);
            }

            @OnClick({R.id.navi_record_list_box})
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.navi_record_list_box:
                        final CommonDeleteDialog commonDeleteDialog = new CommonDeleteDialog(NaviSetPointActivity.this, "清空", "确定要清空历史记录吗", "取消", "清空");
                        commonDeleteDialog.setOnConfirmListener(new CommonDeleteDialog.OnConfirmListener() {
                            @Override
                            public void onConfirm(String text) {
                                cleanHistory();
                                commonDeleteDialog.cancel();
                            }
                        }).show();
                        break;
                }

            }
        }
    }


    /**
     * 去除动画防止，防止出现recyclerView的bug
     */

    public class MyLinearLayoutManager extends LinearLayoutManager {
        public MyLinearLayoutManager(Context context) {
            super(context);
        }

        @Override
        public boolean supportsPredictiveItemAnimations() {
            return false;
        }

        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();

            }
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                return super.scrollVerticallyBy(dy, recycler, state);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }
    }


}
