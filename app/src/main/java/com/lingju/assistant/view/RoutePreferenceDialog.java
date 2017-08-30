package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.lingju.assistant.R;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/29.
 */
public class RoutePreferenceDialog extends Dialog {

    @BindView(R.id.ansl_preference_avoid_traffic)
    CheckBox mAnslPreferenceAvoidTraffic;
    @BindView(R.id.ansl_preference_min_distance)
    CheckBox mAnslPreferenceMinDistance;
    @BindView(R.id.ansl_preference_min_time)
    CheckBox mAnslPreferenceMinTime;
    @BindView(R.id.ansl_preference_min_toll)
    CheckBox mAnslPreferenceMinToll;
    private Context mContext;
    private int mRoutePreference;
    private Map<CheckBox, Integer> prefrences = new Hashtable<>();

    public RoutePreferenceDialog(Context context, int preference) {
        super(context, R.style.full_dialog);
        this.mContext = context;
        this.mRoutePreference = preference;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_route_preferences);
        ButterKnife.bind(this);
        init();
    }

    @Override
    public void onBackPressed() {
        if (mDialogListener != null)
            mDialogListener.onClickBack();
        super.onBackPressed();
    }

    private void init() {
       /* contentView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (mDialogListener != null)
                        mDialogListener.onClickBack();
                    return true;
                }
                return false;
            }
        });*/
        prefrences.put(mAnslPreferenceAvoidTraffic, BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM);
        prefrences.put(mAnslPreferenceMinDistance, BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST);
        prefrences.put(mAnslPreferenceMinTime, BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME);
        prefrences.put(mAnslPreferenceMinToll, BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL);

        mAnslPreferenceAvoidTraffic.setOnCheckedChangeListener(checkedChangeListener);
        mAnslPreferenceMinDistance.setOnCheckedChangeListener(checkedChangeListener);
        mAnslPreferenceMinTime.setOnCheckedChangeListener(checkedChangeListener);
        mAnslPreferenceMinToll.setOnCheckedChangeListener(checkedChangeListener);

        setCalculatePrefrenceState();
    }

    @OnClick({R.id.ansl_setting_back_bt, R.id.ansl_setting_save_bt})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ansl_setting_back_bt:
                if (mDialogListener != null)
                    mDialogListener.onClickBack();
                break;
            case R.id.ansl_setting_save_bt:
                if (mDialogListener != null) {
                    mDialogListener.onClickComplete(mRoutePreference);
                }
                break;
        }
    }

    /**
     * 设置算路偏好状态（默认选择时间最短模式）
     **/
    private void setCalculatePrefrenceState() {
        Set<Map.Entry<CheckBox, Integer>> entrySet = prefrences.entrySet();
        for (Map.Entry<CheckBox, Integer> entry : entrySet) {
            entry.getKey().setChecked((mRoutePreference & entry.getValue()) == entry.getValue());
        }
    }

    CheckBox.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                mRoutePreference |= prefrences.get(buttonView);
            } else {
                mRoutePreference &= (prefrences.get(buttonView) ^ 31);
            }
        }
    };

    private OnDialogClickListener mDialogListener;

    public void setOnDialogListener(OnDialogClickListener listener) {
        mDialogListener = listener;
    }

    public interface OnDialogClickListener {
        /**
         * 点击回退图标
         **/
        void onClickBack();

        /**
         * 点击完成按钮
         **/
        void onClickComplete(int preference);
    }
}
