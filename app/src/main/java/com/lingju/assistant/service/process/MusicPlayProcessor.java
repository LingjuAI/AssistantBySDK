package com.lingju.assistant.service.process;

import android.content.Context;
import android.media.AudioManager;
import android.text.TextUtils;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.DialogEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.entity.action.PlayerEntity;
import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.player.audio.model.AudioRepository;
import com.lingju.assistant.service.VoiceMediator;
import com.lingju.assistant.service.process.base.BaseProcessor;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.log.Log;
import com.lingju.context.entity.Command;
import com.lingju.context.entity.Get;
import com.lingju.context.entity.NewAudioEntity;
import com.lingju.context.entity.SyncSegment;
import com.lingju.context.entity.TapeEntity;
import com.lingju.context.entity.base.IChatResult;
import com.lingju.model.PlayMusic;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.util.JsonUtils;
import com.lingju.util.NetUtil;
import com.lingju.util.PlayList;
import com.lingju.util.TimeUtils;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

/**
 * Created by Administrator on 2016/11/5.
 */
public class MusicPlayProcessor extends BaseProcessor {
    private String TAG = "Lingju";
    private LingjuAudioPlayer aPlayer;
    private XmPlayerManager mXmlyPlayer;
    private IChatResult chatResult;
    private String[] playTips = {"马上", "即将", "开始"};

    public MusicPlayProcessor(Context mContext, SystemVoiceMediator mediator) {
        super(mContext, mediator);
        aPlayer = LingjuAudioPlayer.create(mContext);
        mXmlyPlayer = XmPlayerManager.getInstance(mContext);
    }

    @Override
    public int aimCmd() {
        return CMD_PLAY;
    }

    @Override
    public void handle(Command cmd, String text, int inputType) {
        super.handle(cmd, text, inputType);
        chatResult = THREAD_CHATRESULT.get();
        Observable<PlayMusic> playObserable = null;
        SpeechMsgBuilder msgBuilder = SpeechMsgBuilder.create(text);
        try {
            JSONArray actions = new JSONArray(cmd.getActions());
            JSONObject action = actions.getJSONObject(actions.length() - 1);
            JSONObject target = action.getJSONObject("target");
            PlayerEntity playerEntity = JsonUtils.getObj(target.toString(), PlayerEntity.class);
            if (!TextUtils.isEmpty(playerEntity.getControl())) {
                Integer control = RobotConstant.PlayerMap.get(playerEntity.getControl());
                switch (control) {
                    case RobotConstant.CONTROL_PLAY:    //播放列表歌曲
                        List<PlayMusic> musicList = convert(playerEntity.getObject());
                        voiceMediator.setAudioPlayType(VoiceMediator.AUTO_TYPE);
                        if (musicList.size() > 0) {
                            EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_TOGGLE_TYPE));
                            AudioRepository.get().resetRquestList(musicList);
                            aPlayer.setPlayListType(IBatchPlayer.PlayListType.REQUEST);
                            boolean allowPlay = NetUtil.getInstance(mContext).getCurrentNetType() == NetUtil.NetType.NETWORK_TYPE_WIFI ||
                                    (NetUtil.getInstance(mContext).getCurrentNetType() == NetUtil.NetType.NETWORK_TYPE_3G && !aPlayer.isNoOnlinePlayInMobileNet());
                            if (allowPlay) {
                                playObserable = aPlayer.play(true);
                                if (aPlayer.getPlayList(IBatchPlayer.PlayListType.REQUEST).size() > 1) {
                                    PlayMusic music = aPlayer.currentPlayMusic();
                                    text = new StringBuilder("好的，").append(playTips[new Random().nextInt(playTips.length)])
                                            .append("播放").append(music.getSinger()).append("的").append(music.getTitle()).toString();
                                }

                                //在允许3/4g网络播放歌曲的情况下，每天提醒一次流量播歌
                                if ((NetUtil.getInstance(mContext).getCurrentNetType() == NetUtil.NetType.NETWORK_TYPE_3G && !aPlayer.isNoOnlinePlayInMobileNet())) {
                                    long dayTime = AppConfig.dPreferences.getLong(AppConfig.PLAY_NO_WIFI, 0);
                                    long todayTime = TimeUtils.getTodayDate().getTime();
                                    if (todayTime != dayTime) {
                                        text += "。当前正在使用3G/4G网络播放歌曲，请注意流量哦！";
                                        AppConfig.dPreferences.edit().putLong(AppConfig.PLAY_NO_WIFI, todayTime).commit();
                                    }
                                }
                            } else {
                                aPlayer.play(true).observeOn(AndroidSchedulers.mainThread()).subscribe();
                                return;
                            }
                        } else {
                            String origin = playerEntity.getOrigin();
                            if ("LOCAL".equals(origin)) {
                                if (playerEntity.getObject() == null) {     //播放本地歌曲
                                    int localSize = AudioRepository.get().findByListType(IBatchPlayer.PlayListType.LOCAL).size();
                                    if (localSize > 0) {
                                        int local_play_index = new Random().nextInt(localSize);
                                        aPlayer.setPlayListType(IBatchPlayer.PlayListType.LOCAL);
                                        playObserable = aPlayer.play(local_play_index, true/*prePlay = true*/);
                                    } else {
                                        text = "当前手机里没有歌曲";
                                    }
                                } else {     //播放录音
                                    JSONArray object = target.getJSONArray("object");
                                    List<PlayMusic> playMusics = tape2Music(object);
                                    if (playMusics.size() > 0) {
                                        AudioRepository.get().resetRquestList(playMusics);
                                        aPlayer.setPlayListType(IBatchPlayer.PlayListType.REQUEST);
                                        // TODO: 2017/6/3 注意设置播放模式时，录音播放顺序
                                        playObserable = aPlayer.play(true);
                                    }
                                }
                            } else if ("COLLECT".equals(origin)) {      //播放收藏歌曲
                                if (AudioRepository.get().findByListType(IBatchPlayer.PlayListType.FAVORITE).size() > 0) {
                                    aPlayer.setPlayListType(IBatchPlayer.PlayListType.FAVORITE);
                                    playObserable = aPlayer.play(true);
                                } else {
                                    text = "当前没有收藏的歌曲";
                                }
                            } else if ("QUERY".equals(origin) ||
                                    (actions.length() > 1 && "QUERY".equals(actions.getJSONObject(0).getString("action")))) {
                                JSONObject targetJson = actions.getJSONObject(0).getJSONObject("target");
                                NewAudioEntity audioEntity = SyncSegment.fromJson(targetJson.toString(), NewAudioEntity.class);
                                text = "抱歉，我暂时未接入第三方" + audioEntity.getType() + "库，还不能为您执行该功能。";
                            } else {
                                text = "很抱歉，我的曲库里没有这首歌，请试试别的歌吧。";
                            }
                        }
                        break;
                    case RobotConstant.CONTROL_PAUSE:       //暂停
                        if (voiceMediator.getAudioPlayType() == VoiceMediator.AUTO_TYPE) {
                            if (aPlayer.isPlaying()) {
                                aPlayer.pause();
                                if (voiceMediator.isWakeUpMode()) {
                                    voiceMediator.setWakeUpMode(true);
                                }
                            }
                        } else {
                            mXmlyPlayer.pause();
                        }
                        break;
                    case RobotConstant.CONTROL_RESUME:      //继续
                        if (voiceMediator.getAudioPlayType() == VoiceMediator.AUTO_TYPE) {
                            if (aPlayer.currentPlayMusic() != null) {
                                if (aPlayer.prepared()) {
                                    voiceMediator.stopWakenup();
                                    //发送当前播放歌曲事件，通知音乐播放栏显示
                                    EventBus.getDefault().post(aPlayer.currentPlayMusic());
                                    Single.just(0)
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .doOnSuccess(new Consumer<Integer>() {
                                                @Override
                                                public void accept(Integer integer) throws Exception {
                                                    aPlayer.play();
                                                }
                                            })
                                            .subscribe();
                                } else
                                    playObserable = aPlayer.play(aPlayer.currentPlayMusic(), true);
                            } else {
                                text = "您还没有播放任何歌曲";
                            }
                        } else {
                            mXmlyPlayer.play();
                        }
                        break;
                    case RobotConstant.CONTROL_REPLAY:      //重播
                        if (voiceMediator.getAudioPlayType() == VoiceMediator.AUTO_TYPE) {
                            if (aPlayer.currentPlayMusic() != null) {
                                playObserable = aPlayer.play(aPlayer.currentPlayMusic(), true);
                            } else {
                                text = "您还没有播放任何歌曲";
                            }
                        } else {
                            mXmlyPlayer.seekTo(0);
                            mXmlyPlayer.play();
                        }
                        break;
                    case RobotConstant.CONTROL_PRE:         //上一首
                        if (voiceMediator.getAudioPlayType() == VoiceMediator.AUTO_TYPE) {
                            if (aPlayer.currentPlayMusic() != null) {
                                playObserable = aPlayer.playPre();
                            } else {
                                text = "您还没有播放任何歌曲";
                            }
                        } else {
                            if (mXmlyPlayer.hasPreSound()) {
                                mXmlyPlayer.playPre();
                            } else {
                                text = "已经是播放列表中的第一集了";
                            }
                        }
                        break;
                    case RobotConstant.CONTROL_NEXT:        //下一首
                        if (voiceMediator.getAudioPlayType() == VoiceMediator.AUTO_TYPE) {
                            if (aPlayer.currentPlayMusic() != null) {
                                playObserable = aPlayer.playNext();
                            } else {
                                text = "您还没有播放任何歌曲";
                            }
                        } else {
                            if (mXmlyPlayer.hasNextSound()) {
                                mXmlyPlayer.playNext();
                            } else {
                                text = "已经是播放列表中的最后一集了";
                            }
                        }
                        break;
                }
            } else if (!TextUtils.isEmpty(playerEntity.getMode())) {
                Integer playMode = RobotConstant.PlayerMap.get(playerEntity.getMode());
                switch (playMode) {
                    case RobotConstant.MODE_ORDER:
                    case RobotConstant.MODE_ORDER_CYCLE:        //顺序
                        aPlayer.setPlayMode(PlayList.PlayMode.ORDER);
                        break;
                    case RobotConstant.MODE_RANDOM:             //随机播放
                        aPlayer.setPlayMode(PlayList.PlayMode.RANDOM);
                        break;
                    case RobotConstant.MODE_SINGLE_CYCLE:       //单曲循环
                        aPlayer.setPlayMode(PlayList.PlayMode.SINGLE);
                        break;
                }
            } else if (playerEntity.getVolume() != null && !TextUtils.isEmpty(playerEntity.getVolume().getProgress())) {
                switch (playerEntity.getVolume().getType()) {
                    case 0:     //按百分比调整音量
                        String volume = playerEntity.getVolume().getProgress();
                        voiceMediator.changeMediaVolume(Integer.valueOf(volume));
                        text = "音量已调整到" + volume + "%";
                        break;
                    case 1:     //增大音量
                        text = "音量已增大至" + ajustVol(true, Integer.valueOf(playerEntity.getVolume().getProgress()));
                        break;
                    case 3:     //减小音量
                        text = "音量已减小至" + ajustVol(false, Integer.valueOf(playerEntity.getVolume().getProgress()));
                        break;
                    case 8:     //最大音量
                        voiceMediator.changeMediaVolume(100);
                        text = "已调整到最大音量";
                        break;
                    case 9:     //静音
                        voiceMediator.changeMediaVolume(0);
                        text = "已静音";
                        break;
                }
            } else if (playerEntity.getPlaynettype() != null) {
                if ("3G".equals(playerEntity.getPlaynettype())) {   //允许3G网络下播放歌曲
                    aPlayer.setNoOnlinePlayInMobileNet(false);
                    AppConfig.dPreferences.edit().putBoolean(AppConfig.DOWNLOAD_ON_WIFI, true).commit();
                    if (aPlayer.currentPlayMusic() != null) {
                        try {
                            Log.i(TAG, "g.current_music.getMusicid()=" + aPlayer.currentPlayMusic().getMusicid());
                            playObserable = aPlayer.play(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {     //不允许，播放本地歌曲
                    aPlayer.setNoOnlinePlayInMobileNet(true);
                    AppConfig.dPreferences.edit().putBoolean(AppConfig.DOWNLOAD_ON_WIFI, false).commit();
                    int localSize = AudioRepository.get().findByListType(IBatchPlayer.PlayListType.LOCAL).size();
                    if (localSize > 0) {
                        int local_play_index = new Random().nextInt(localSize);
                        aPlayer.setPlayListType(IBatchPlayer.PlayListType.LOCAL);
                        playObserable = aPlayer.play(local_play_index, true/*prePlay = true*/);
                    } else {
                        text = "好的";
                    }
                }

            } else if (playerEntity.getTextshowtype() != null) {
                if ("LYRIC".equals(playerEntity.getTextshowtype())) {     //显示歌词
                    if (aPlayer.currentPlayMusic() != null) {
                        Single.just(0)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnSuccess(new Consumer<Integer>() {
                                    @Override
                                    public void accept(Integer integer) throws Exception {
                                        aPlayer.showLyric();
                                    }
                                })
                                .subscribe();
                    } else {
                        text = "抱歉，您还没有播放任何歌曲";
                    }
                }
            } else if (!action.isNull("get")) {     //获取当前播放歌曲信息
                Get obj = JsonUtils.getObj(action.getJSONObject("get").toString(), Get.class);
                if ("VALUE".equals(obj.getType())) {
                    PlayList<PlayMusic> playList = (PlayList<PlayMusic>) aPlayer.getPlayList(aPlayer.getPlayListType());
                    if (playList.getCurrent() == null) {
                        text = "你当前没有播放歌曲，想听什么就对我说吧";
                    } else if (playList.getCurrent().getFavorite()) {
                        text = "现在播放的是" + playList.getCurrent().getSinger() + "的" + playList.getCurrent().getTitle();
                    } else {
                        text = "现在播放的是" + playList.getCurrent().getSinger() + "的" + playList.getCurrent().getTitle() + ",需要我帮你收藏吗？";
                        msgBuilder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(text)) {
            msgBuilder.setText(text);
            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
            final Observable<SpeechMsg> s = SynthesizerBase.get()
                    .startSpeakAbsolute(msgBuilder.build())
                    .doOnNext(new Consumer<SpeechMsg>() {
                        @Override
                        public void accept(SpeechMsg speechMsg) throws Exception {
                            if (speechMsg.state() == SpeechMsg.State.OnBegin) {
                                EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_START));
                            }
                        }
                    })
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_END));
                        }
                    });
            s.subscribe();
                    /*.filter(new Predicate<SpeechMsg>() {
                        @Override
                        public boolean test(SpeechMsg speechMsg) throws Exception {
                            return speechMsg.state().ordinal() >= SpeechMsg.State.OnInterrupted.ordinal();
                        }
                    });*/
            if (playObserable != null) {
                /*s.zipWith(playObserable, new BiFunction<SpeechMsg, PlayMusic, Pair<SpeechMsg, PlayMusic>>() {
                    @Override
                    public Pair<SpeechMsg, PlayMusic> apply(SpeechMsg speechMsg, PlayMusic music) throws Exception {
                        Log.i(TAG, "startSpeakAbsolute.zipwith(play) BiFunction.........." + Thread.currentThread());
                        return new Pair<SpeechMsg, PlayMusic>(speechMsg, music);
                    }
                })*/
                /* 发送当前播放歌曲事件，通知音乐播放栏显示 */
                EventBus.getDefault().post(aPlayer.currentPlayMusic());
                playObserable.observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(new Consumer<PlayMusic>() {
                            @Override
                            public void accept(PlayMusic music) throws Exception {
                                voiceMediator.stopWakenup();
                                aPlayer.play();
                            }
                        })
                        /*.doOnComplete(new Action() {
                            @Override
                            public void run() throws Exception {
                                Log.i(TAG, "startSpeakAbsolute.zipwith(play) completed>>" + Thread.currentThread());
                                EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_END));

                            }
                        })*/.subscribe();
            }
        } else if (playObserable != null) {     //不用预加载，直接播放(当前策略不会走该方法)。适用于play(PlayMusic music)、playPre()、playNext()
            voiceMediator.stopWakenup();
            /* 发送当前播放歌曲事件，通知音乐播放栏显示 */
            EventBus.getDefault().post(aPlayer.currentPlayMusic());
            playObserable.observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        }
    }

    private List<PlayMusic> tape2Music(JSONArray tapes) {
        List<PlayMusic> playMusics = new ArrayList<>();
        try {
            for (int i = 0; i < tapes.length(); i++) {
                JSONObject tapeJson = tapes.getJSONObject(i);
                TapeEntity tapeEntity = SyncSegment.fromJson(tapeJson.toString(), TapeEntity.class);
                String uri = tapeEntity.getUrl().replace("\\", "");
                PlayMusic music = new PlayMusic();
                music.setMusicid(tapeEntity.getSid());
                music.setSinger("未知");
                music.setAlbum("未知");
                music.setTitle(uri.substring(uri.lastIndexOf("/") + 1, uri.length()));
                music.setUri(uri);
                music.setCloud(false);
                music.setRequestGroupId(0);
                music.setCreated(tapeEntity.getCreated());
                playMusics.add(music);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return playMusics;
    }

    /**
     * 调整音量
     *
     * @param increase true=增加百分之20 false=减少百分之20
     */

    public String ajustVol(boolean increase, int progress) {
        AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = (int) (currentVolume + max * (progress / 100.0f) * (increase ? 1 : -1));
        if (currentVolume < 0)
            currentVolume = 0;
        else if (currentVolume > max)
            currentVolume = (int) max;
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
        //弹出系统媒体音量调节框
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_SAME, AudioManager.FLAG_PLAY_SOUND
                        | AudioManager.FLAG_SHOW_UI);

        NumberFormat nf = NumberFormat.getPercentInstance();
        //返回数的整数部分所允许的最大位数
        nf.setMaximumIntegerDigits(3);
        //返回数的小数部分所允许的最大位数
        // nf.setMaximumFractionDigits(2);
        return nf.format(currentVolume / max);
    }

    @Override
    public IChatResult getCurrentChatResult() {
        return chatResult;
    }

    private List<PlayMusic> convert(List<NewAudioEntity> list) {
        Log.i(TAG, "convert>>");
        List<PlayMusic> rs = new ArrayList<>();
        if (null != list && list.size() > 0) {
            try {
                for (NewAudioEntity a : list) {
                    if (a.getId() == RobotConstant.ACTION_TAPE)
                        continue;
                    Log.i(TAG, a.toJsonString());
                    rs.add(new PlayMusic(a, 0));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rs;
    }
}
