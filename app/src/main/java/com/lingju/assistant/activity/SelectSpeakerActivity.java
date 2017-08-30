package com.lingju.assistant.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.event.OnItemClickListener;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.config.Setting;
import com.lingju.util.NetUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2017/1/6.
 */
public class SelectSpeakerActivity extends GoBackActivity implements OnItemClickListener {

    @BindView(R.id.speaker_list)
    RecyclerView mSpeakerList;
    private AppConfig mAppConfig;
    private SpeakerAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_speaker);
        ButterKnife.bind(this);
        mAppConfig = (AppConfig) getApplication();
        mAdapter = new SpeakerAdapter(this);
        mSpeakerList.setHasFixedSize(true);
        mSpeakerList.setLayoutManager(new LinearLayoutManager(this));
        mSpeakerList.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View itemView, int position) {
        if (mAppConfig.checkedSpeakerItemPosition != position) {
            /* 保存本地参数设置 */
            mAppConfig.checkedSpeakerItemPosition = position;
            mAdapter.notifyDataSetChanged();
            /* 讯飞合成设置合成声音 */
            if (!mAppConfig.isLocalMode()) {
                if (SynthesizerBase.get().isSpeaking()) {
                    SynthesizerBase.get().stopSpeaking();
                }
                SynthesizerBase.get().resetParam(NetUtil.NetType.NETWORK_TYPE_WIFI);
            }
            Setting.setVolName(mAppConfig.speakers[position][3]);
            String text = String.format(getResources().getString(R.string.speaker_text), mAppConfig.speakers[position][0]);
            SpeechMsgBuilder builder = SpeechMsgBuilder.create(text);
            SynthesizerBase.get().startSpeakAbsolute(builder.build())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe();
        }
    }

    @Override
    public void onLongClick(View intemView, int position) {

    }

    @Override
    protected void onDestroy() {
        SynthesizerBase.get().stopSpeakingAbsolte();
        super.onDestroy();
    }

    class SpeakerAdapter extends RecyclerView.Adapter<SpeakerAdapter.SpeakerHolder> {

        private OnItemClickListener itemListener;

        public SpeakerAdapter(OnItemClickListener listener) {
            this.itemListener = listener;
        }

        @Override
        public SpeakerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.speaker_line, parent, false);
            return new SpeakerHolder(item);
        }

        @Override
        public void onBindViewHolder(SpeakerHolder holder, int position) {
            String[] speaker = mAppConfig.speakers[position];
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < speaker.length - 1; i++) {
                sb.append(speaker[i]).append("-");
            }
            sb.deleteCharAt(sb.length() - 1);
            holder.mListSpeakerText.setText(sb.toString());
            if (mAppConfig.checkedSpeakerItemPosition == position) {
                holder.mListFavoriteBt.setChecked(true);
            } else {
                holder.mListFavoriteBt.setChecked(false);
            }
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mAppConfig.speakers.length;
        }

        class SpeakerHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.list_speaker_text)
            TextView mListSpeakerText;
            @BindView(R.id.list_favorite_bt)
            AppCompatRadioButton mListFavoriteBt;

            public SpeakerHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = (int) v.getTag();
                        if (itemListener != null) {
                            itemListener.onClick(v, position);
                        }
                    }
                });
            }
        }
    }
}
