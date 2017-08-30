package com.lingju.assistant.activity.index.view;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.MainActivity;
import com.lingju.assistant.activity.event.IntroduceShowEvent;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.assistant.activity.event.RobotTipsEvent;
import com.lingju.assistant.activity.event.VolumeChangedEvent;
import com.lingju.assistant.activity.index.IVoiceInput;
import com.lingju.assistant.view.VoiceWaveComponent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VoiceInputFragment extends Fragment implements IVoiceInput.VoiceInputView {

    @BindView(R.id.fbvi_text_input_bt)
    ImageButton mFbviTextInputBt;
    @BindView(R.id.fvbi_manual_bt)
    ImageButton mFvbiManualBt;
    private View rootView;
    @BindView(R.id.fbvi_voice_bt)
    VoiceWaveComponent voiceBt;
    @BindView(R.id.tv_voice_tips)
    TextView tipsText;
    @BindView(R.id.tips_cancel)
    ImageButton mTipsCancel;
    private IVoiceInput.VoicePresenter presenter;
    private int mCx;
    private int mCy;
    private int mFinalRadius;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.frag_bottom_voice_input, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (presenter != null)
            presenter.subscribe();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        if (presenter != null)
            presenter.unsubscribe();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onIntroduceshowed(IntroduceShowEvent showEvent) {
        boolean showIntroduce = AppConfig.dPreferences.getBoolean(AppConfig.SHOW_INTRODUCE, true);
        if (showIntroduce == showEvent.getIntroduceShow()) {
            ((MainActivity) getActivity()).setIntroduceList(showEvent.getIntroduceShow());
            AppConfig.dPreferences.edit().putBoolean(AppConfig.SHOW_INTRODUCE, true).commit();
        }
    }

    @Override
    public void setPresenter(IVoiceInput.VoicePresenter presenter) {
        this.presenter = presenter;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordUpdate(RecordUpdateEvent e) {
        if (e.getType() == 0) {
            setMicButtonState(e.getState());
        }
    }

    @Override
    public void setMicButtonState(int state) {
        // Log.i("LingJu", "VoiceInputFragment setMicButtonState()>>>" + state);
        switch (state) {
            case RecordUpdateEvent.RECORD_IDLE:
            case RecordUpdateEvent.RECORD_IDLE_AFTER_RECOGNIZED:
                voiceBt.setRecordIdleState();
                mFbviTextInputBt.setVisibility(View.VISIBLE);
                mFvbiManualBt.setVisibility(View.VISIBLE);
                break;
            case RecordUpdateEvent.RECORDING:
                ((MainActivity) getActivity()).closeSlidingMenu();
                ((MainActivity) getActivity()).scroll2LastPosition();
                //判断是否展示话筒的波纹效果
                boolean isShow = AppConfig.dPreferences.getBoolean("wave_show", true);
                if (isShow) {
                    mFbviTextInputBt.setVisibility(View.GONE);
                    mFvbiManualBt.setVisibility(View.GONE);
                    voiceBt.setRecordStartState();
                }
                break;
            case RecordUpdateEvent.RECOGNIZING:
                voiceBt.setRecognizeCompletedState();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVolumeChangedEvent(VolumeChangedEvent e) {
        int volume = e.getVolume() > 20 ? 20 : e.getVolume();
        int amplitude = (int) (voiceBt.getBaseAmplitude() + volume * 2.4);
        // Log.i("LingJu", "基本振幅：" + voiceBt.getBaseAmplitude() + " 当前振幅：" + amplitude);
        voiceBt.setWaveAmplitude(amplitude);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRobotTipsEvent(RobotTipsEvent event) {
        String tips = event.getText();
        if (TextUtils.isEmpty(tips)) {
            switchTipsText(false);
        } else {
            tips = tips.replace("/", "\n");
            showMicButtonTips(tips);
            switchTipsText(true);
            if (((MainActivity) getActivity()).voiceWaveable() && !AppConfig.dPreferences.getBoolean(GuideFragment.SHOWED_SPEAK_TIPS_GUIDE, false))
                ((MainActivity) getActivity()).setGuidePager(GuideFragment.SPEAK_TIPS_GUIDE, tips);
        }
    }

    @OnClick(R.id.tips_cancel)
    public void hideTips() {
        switchTipsText(false);
    }

    @Override
    public void showMicButtonTips(String tipsText) {
        this.tipsText.setText(tipsText);
    }

    @OnClick(R.id.fbvi_text_input_bt)
    @Override
    public void switch2TextInput() {
        // XmlyManager.get().authorize(getActivity());
        AppConfig.dPreferences.edit().putBoolean("wave_show", false).commit();
        getActivity().getSupportFragmentManager().beginTransaction().hide(this).commit();
        ((MainActivity) getActivity()).switchKeyboard(true);
        EventBus.getDefault().post(new IntroduceShowEvent(false));
    }

    @Override
    public void switchTipsText(boolean isShow) {
        if (isShow) {    //显示提示语
            /*if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                // 获取揭露/隐藏中点
                mCx = (tipsText.getRight() - tipsText.getLeft()) / 2;
                mCy = tipsText.getBottom();
                // 计算最后半径
                mFinalRadius = Math.max(tipsText.getWidth(), tipsText.getHeight());
                // 开启揭露动画
                switchAnim(0, mFinalRadius, false);
            } else*/
            tipsText.setVisibility(View.VISIBLE);
            mTipsCancel.setVisibility(View.VISIBLE);
            mTipsCancel.getDrawable().setAlpha((int) (255 * 0.54));

        } else if (tipsText.getVisibility() == View.VISIBLE) {       //隐藏提示语
            /*if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
                switchAnim(mFinalRadius, 0, true);
            else*/
            tipsText.setVisibility(View.GONE);
            mTipsCancel.setVisibility(View.GONE);
        }
    }

    /**
     * 设置揭露/隐藏动画
     **/
    private void switchAnim(int startRadius, int endRadius, final boolean isGone) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            // 创建动画
            final Animator anim = ViewAnimationUtils.createCircularReveal(tipsText, mCx, mCy, startRadius, endRadius);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    if (!isGone)
                        tipsText.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (isGone)
                        tipsText.setVisibility(View.GONE);
                }
            });
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.setDuration(800);
            anim.start();
        }
    }

    @OnClick(R.id.fvbi_manual_bt)
    @Override
    public void showIntroduce() {
        if (getActivity() instanceof MainActivity) {
            boolean showIntroduce = AppConfig.dPreferences.getBoolean(AppConfig.SHOW_INTRODUCE, true);
            if (showIntroduce == true) {
                ((MainActivity) getActivity()).setIntroduceList(true);
                AppConfig.dPreferences.edit().putBoolean(AppConfig.SHOW_INTRODUCE, false).commit();
            } else {
                ((MainActivity) getActivity()).setIntroduceList(false);
                AppConfig.dPreferences.edit().putBoolean(AppConfig.SHOW_INTRODUCE, true).commit();
            }
        }

    }

    @Override
    public void switchView(boolean show) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (show)
            transaction.show(this);
        else
            transaction.hide(this);
        transaction.commit();
    }

}
