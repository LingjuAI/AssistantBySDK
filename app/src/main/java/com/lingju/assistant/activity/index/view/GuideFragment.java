package com.lingju.assistant.activity.index.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.MainActivity;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.assistant.activity.index.IGuide;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.service.VoiceMediator;
import com.lingju.audio.engine.IflyRecognizer;
import com.lingju.model.PlayMusic;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Ken on 2017/3/21.
 */
public class GuideFragment extends Fragment implements IGuide.IGuideView, View.OnClickListener {

    public final static String SHOWED_RESP_GUIDE = "showed_resp_guide";
    public final static String SHOWED_MUSIC_GUIDE = "showed_music_guide";
    public final static String SHOWED_MENU_GUIDE = "showed_menu_guide";
    public final static String SHOWED_SPEAK_TIPS_GUIDE = "showed_speak_tips_guide";
    public final static String SHOW_TYPE = "show_type";
    public final static String SHOW_TEXT = "show_text";
    public final static int RESP_GUIDE = 0;
    public final static int MUSIC_GUIDE = 1;
    public final static int MENU_GUIDE = 2;
    public final static int SPEAK_TIPS_GUIDE = 3;
    @BindView(R.id.menu_guide_box)
    RelativeLayout mMenuGuideBox;
    @BindView(R.id.resp_guide)
    LinearLayout mRespGuide;
    @BindView(R.id.resp_guide_text)
    TextView mRespGuideText;
    @BindView(R.id.music_name)
    TextView mMusicName;
    @BindView(R.id.music_author)
    TextView mMusicAuthor;
    @BindView(R.id.music_guide_box)
    LinearLayout mMusicGuideBox;
//    @BindView(R.id.resp_text)
//    TextView mRespText;
//    @BindView(R.id.resp_guide_box)
//    LinearLayout mRespGuideBox;
    @BindView(R.id.tips_text)
    TextView mTipsText;
    @BindView(R.id.tips_guide_box)
    LinearLayout mTipsGuideBox;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.frag_guide, container, false);
        ButterKnife.bind(this, contentView);
        contentView.setOnClickListener(this);
        setGuideView();
        return contentView;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {  //fragment重新显示时触发
            setGuideView();
        }
        super.onHiddenChanged(hidden);
    }


    @Override
    public void onClick(View v) {
        disppear();
    }

    @Override
    public void setGuideView() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            String key = null;
            int type = arguments.getInt(SHOW_TYPE, -1);
            mMenuGuideBox.setVisibility(View.GONE);
//            mRespGuideBox.setVisibility(View.GONE);
            mMusicGuideBox.setVisibility(View.GONE);
            mTipsGuideBox.setVisibility(View.GONE);
            switch (type) {
//                case RESP_GUIDE:
//                    mRespGuideBox.setVisibility(View.VISIBLE);
//                    mRespText.setText(arguments.getString(SHOW_TEXT));
//                    int guideTextMargin = ((MainActivity) getActivity()).getGuideTextMargin(RESP_GUIDE);
//                    ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mRespGuideBox.getLayoutParams();
//                    layoutParams.topMargin = guideTextMargin;
//                    key = SHOWED_RESP_GUIDE;
//                    break;
                case MUSIC_GUIDE:
                    //话筒波纹效果不展示
                    AppConfig.dPreferences.edit().putBoolean("wave_show",false).commit();
                    mMusicGuideBox.setVisibility(View.VISIBLE);
                    PlayMusic music = LingjuAudioPlayer.get().currentPlayMusic();
                    mMusicAuthor.setText(music.getSinger());
                    mMusicName.setText(music.getTitle());
                    key = SHOWED_MUSIC_GUIDE;
                    break;
                case MENU_GUIDE:
                    mMenuGuideBox.setVisibility(View.VISIBLE);
                    mRespGuideText.setText(getResources().getString(R.string.first_welcome));
                    int textMargin = ((MainActivity) getActivity()).getGuideTextMargin(MENU_GUIDE);
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mRespGuide.getLayoutParams();
                    params.topMargin = textMargin;
                    key = SHOWED_MENU_GUIDE;
                    break;
                case SPEAK_TIPS_GUIDE:
                    //话筒波纹效果不展示
                    AppConfig.dPreferences.edit().putBoolean("wave_show",false).commit();
                    mTipsGuideBox.setVisibility(View.VISIBLE);
                    mTipsText.setText(arguments.getString(SHOW_TEXT));
                    ViewGroup.MarginLayoutParams tipsLayoutParams = (ViewGroup.MarginLayoutParams) mTipsGuideBox.getLayoutParams();
                    tipsLayoutParams.bottomMargin = ((MainActivity) getActivity()).getGuideTextMargin(SPEAK_TIPS_GUIDE);
                    key = SHOWED_SPEAK_TIPS_GUIDE;
                    break;
            }
            AppConfig.dPreferences.edit().putBoolean(key, true).commit();
        }
    }

    @Override
    public void disppear() {
        getFragmentManager().beginTransaction().hide(this).commit();
        //话筒波纹效果展示
        AppConfig.dPreferences.edit().putBoolean("wave_show",true).commit();
        //键盘输入界面切换为话筒界面如果正在识别时打开波纹效果
        if(IflyRecognizer.getInstance().isRecognizing()){
            EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORDING));
        }
    }
}
