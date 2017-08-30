package com.lingju.assistant.activity;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.index.presenter.NaviSetPointPresenter;
import com.lingju.model.BaiduAddress;
import com.lingju.model.dao.BaiduNaviDao;
import com.lingju.common.log.Log;
import com.lingju.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/21.
 */
public class SetFavoriteAddressActivity extends GoBackActivity {

    private final static int HEAD_TYPE = 0;
    private final static int ITEM_TYPE = 1;
    private static final String TAG = "LingJu";
    @BindView(R.id.afam_recycler)
    RecyclerView mAfamRecycler;
    @BindView(R.id.status_bar)
    View mStatusBar;
    private AddressAdapter mAdapter;
    private List<BaiduAddress> favoriteList = new ArrayList<>();
    private BaiduAddress homeAddress;
    private BaiduAddress companyAddress;
    private int resultCode = 0;
    private BaiduNaviDao mNaviDao;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_adress_manager);
        ButterKnife.bind(this);
        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = mStatusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        mStatusBar.setLayoutParams(layoutParams);
        mNaviDao = BaiduNaviDao.getInstance();
        /* 获取常用地址集合 */
        mNaviDao.getFavorList(favoriteList, -1);
        /* 获取家和单位的地址 */
        homeAddress = mNaviDao.getHomeOrCompanyAddress(getResources().getString(R.string.home));
        companyAddress = mNaviDao.getHomeOrCompanyAddress(getResources().getString(R.string.company));
        /* 初始化地址列表 */
        mInflater = LayoutInflater.from(this);
        mAdapter = new AddressAdapter();
        mAfamRecycler.setHasFixedSize(true);
        mAfamRecycler.setLayoutManager(new LinearLayoutManager(this));
        mAfamRecycler.addItemDecoration(new MultiAddressDivider());
        mAfamRecycler.setAdapter(mAdapter);
    }

    @OnClick({R.id.afam_back_bt, R.id.afam_add_bt})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.afam_back_bt:
                goBack();
                break;
            case R.id.afam_add_bt:      //添加收藏地址
                Intent intent = new Intent(SetFavoriteAddressActivity.this, SetFavoriteMapActivity.class);
                startActivityForResult(intent, 1);
                goInto();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode > 16) {
            Log.i(TAG, "resultCode=" + resultCode);
            if ((resultCode & NaviSetPointPresenter.Msg.UPDATE_COMPANY) == NaviSetPointPresenter.Msg.UPDATE_COMPANY) {
                companyAddress = mNaviDao.getHomeOrCompanyAddress(getResources().getString(R.string.company));
            }
            if ((resultCode & NaviSetPointPresenter.Msg.UPDATE_HOME) == NaviSetPointPresenter.Msg.UPDATE_HOME) {
                homeAddress = mNaviDao.getHomeOrCompanyAddress(getResources().getString(R.string.home));
            }
            if ((resultCode & NaviSetPointPresenter.Msg.UPDATE_FAVORITE) == NaviSetPointPresenter.Msg.UPDATE_FAVORITE) {
                mNaviDao.getFavorList(favoriteList, -1);
            }
            mAdapter.notifyDataSetChanged();
            this.resultCode |= resultCode;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void finish() {
        setResult(resultCode);
        super.finish();
    }

    class AddressAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView;
            RecyclerView.ViewHolder viewHolder;
            if (viewType == ITEM_TYPE) {
                itemView = mInflater.inflate(R.layout.favorite_address_item, parent, false);
                viewHolder = new FavoriteAddressHolder(itemView);
            } else {
                itemView = mInflater.inflate(R.layout.home_and_company, parent, false);
                viewHolder = new HomeAndCompanyHolder(itemView);
            }
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HomeAndCompanyHolder) {
                ((HomeAndCompanyHolder) holder).setCompanyAnHome(homeAddress, companyAddress);
            } else {
                BaiduAddress baiduAddress = favoriteList.get(position - 1);
                ((FavoriteAddressHolder) holder).mFaiItem.setTag(position - 1);
                ((FavoriteAddressHolder) holder).mLocItemNameText.setText(((FavoriteAddressHolder) holder).getShowName(baiduAddress));
                ((FavoriteAddressHolder) holder).mLocItemAddressText.setText(baiduAddress.getAddress());
            }
        }

        @Override
        public int getItemCount() {
            return favoriteList.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? HEAD_TYPE : ITEM_TYPE;
        }

        class FavoriteAddressHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.loc_item_name_text)
            TextView mLocItemNameText;
            @BindView(R.id.loc_item_address_text)
            TextView mLocItemAddressText;
            @BindView(R.id.fai_item)
            RelativeLayout mFaiItem;

            public FavoriteAddressHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            private String getShowName(BaiduAddress bd) {
                if (TextUtils.isEmpty(bd.getRemark())) {
                    return bd.getName();
                } else if (bd.getRemark().equals("家") || bd.getRemark().equals("单位")) {
                    return bd.getName();
                } else
                    return bd.getRemark();
            }

            @OnClick(R.id.fai_item)
            public void onItemClick() {
                /* 进入设置收藏地址地图页 */
                int index = (int) mFaiItem.getTag();
                BaiduAddress bd = favoriteList.get(index);
                Intent intent = new Intent(SetFavoriteAddressActivity.this, SetFavoriteMapActivity.class);
                if (bd != null)
                    intent.putExtra("address", bd);

                intent.putExtra(SetFavoriteMapActivity.MODE, SetFavoriteMapActivity.SET_FAVORITE);
                startActivityForResult(intent, NaviSetPointPresenter.Msg.UPDATE_FAVORITE);
                SetFavoriteAddressActivity.this.goInto();
            }
        }

        class HomeAndCompanyHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.hac_home_text)
            TextView mHacHomeText;
            @BindView(R.id.hac_company_text)
            TextView mHacCompanyText;

            public HomeAndCompanyHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);

            }

            public void setCompanyAnHome(BaiduAddress home, BaiduAddress company) {
                mHacHomeText.setText(home == null ? "点击设置" : home.getName());
                mHacCompanyText.setText(company == null ? "点击设置" : company.getName());
            }

            @OnClick({R.id.hac_home_bt, R.id.hac_company_bt})
            public void onClick(View view) {
                Intent intent = new Intent(SetFavoriteAddressActivity.this, SetFavoriteMapActivity.class);
                switch (view.getId()) {
                    case R.id.hac_home_bt:
                        if (homeAddress != null) {
                            intent.putExtra("address", homeAddress);
                        }
                        intent.putExtra(SetFavoriteMapActivity.FAVORITE_TO_SET_MODE, SetFavoriteMapActivity.FAVORITE_SET_HOME);
                        intent.putExtra(SetFavoriteMapActivity.MODE, SetFavoriteMapActivity.SET_HOME);
                        startActivityForResult(intent, NaviSetPointPresenter.Msg.UPDATE_HOME);
                        break;
                    case R.id.hac_company_bt:
                        if (companyAddress != null) {
                            intent.putExtra("address", companyAddress);
                        }
                        intent.putExtra(SetFavoriteMapActivity.FAVORITE_TO_SET_MODE, SetFavoriteMapActivity.FAVORITE_SET_COMPANY);
                        intent.putExtra(SetFavoriteMapActivity.MODE, SetFavoriteMapActivity.SET_COMPANY);
                        startActivityForResult(intent, NaviSetPointPresenter.Msg.UPDATE_COMPANY);
                        break;
                }
                SetFavoriteAddressActivity.this.goInto();
            }
        }
    }

    /**
     * 地址item分割线
     **/
    class MultiAddressDivider extends RecyclerView.ItemDecoration {
        Paint p;

        public MultiAddressDivider() {
            p = new Paint();
            p.setColor(getResources().getColor(R.color.new_line_black_border));
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            View child = parent.getChildAt(0);
            /* 第一个item下划线画满控件宽度 */
            c.drawLine(child.getLeft(), child.getBottom(), child.getRight(), child.getBottom(), p);
            /* 其余从文本部分画 */
            for (int i = 1, size = parent.getChildCount(); i < size; i++) {
                child = parent.getChildAt(i);
                c.drawLine(child.getLeft(), child.getBottom(), child.getRight(), child.getBottom(), p);
            }
        }
    }
}
