package com.lingju.assistant.service.process;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.iflytek.cloud.SpeechConstant;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.RobotTipsEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.entity.action.VoiceEngineEntity;
import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.player.audio.model.AudioRepository;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.process.base.BaseProcessor;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.config.IflySynConfig;
import com.lingju.audio.engine.IflyRecognizer;
import com.lingju.audio.engine.IflySynthesizer;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.config.Setting;
import com.lingju.context.entity.Command;
import com.lingju.context.entity.NewAudioEntity;
import com.lingju.context.entity.base.IChatResult;
import com.lingju.model.Memo;
import com.lingju.model.PlayMusic;
import com.lingju.model.dao.AssistDao;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.util.JsonUtils;
import com.lingju.util.PlayList;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;

import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/5.
 */
public class MusicOptProcessor extends BaseProcessor {

    private LingjuAudioPlayer mPlayer;
    private final AudioRepository mRepository;
    private IChatResult currentChatResult;
    private final AppConfig mAppConfig;

    public MusicOptProcessor(Context mContext, SystemVoiceMediator mediator) {
        super(mContext, mediator);
        mPlayer = LingjuAudioPlayer.create(mContext);
        mRepository = AudioRepository.get();
        mAppConfig = (AppConfig) ((Service) mContext).getApplication();
    }

    @Override
    public int aimCmd() {
        return CMD_OPTIONS;
    }

    @Override
    public void handle(Command cmd, String text, int inputType) {
        super.handle(cmd, text, inputType);
        currentChatResult = THREAD_CHATRESULT.get();
        SpeechMsgBuilder msgBuilder = SpeechMsgBuilder.create(null);
        if (cmd.getOutc() == DefaultProcessor.OUTC_ASK)
            msgBuilder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
        try {
            JSONArray actions = new JSONArray(cmd.getActions());
            JSONObject lastAction = actions.getJSONObject(actions.length() - 1);
            JSONObject lastTarget = lastAction.getJSONObject("target");
            switch (lastTarget.getInt("id")) {
                case RobotConstant.ACTION_FAVOR:
                    Integer action = RobotConstant.ActionMap.get(lastAction.getString("action"));
                    switch (action) {
                        case RobotConstant.INSERT:     //收藏
                            PlayList<PlayMusic> currentList = (PlayList<PlayMusic>) mPlayer.getPlayList(mPlayer.getPlayListType());
                            if (currentList.getCurrent() == null) {
                                text = "你当前没有播放歌曲哟！";
                            } else if (!currentList.getCurrent().getFavorite()) {
                                new AudioRepository.FavoriteTask(mAppConfig.user, mRepository, mAppConfig.isLocalMode(), new AudioRepository.FavoriteCallback() {

                                    @Override
                                    public void oncomplete(boolean f, PlayMusic m) {
                                        mRepository.findByListType(IBatchPlayer.PlayListType.FAVORITE).resetIterator();
                                    }

                                    @Override
                                    public void onSynComplete(boolean result) {
                                    }
                                }).execute(mPlayer.currentPlayMusic());
                            } else {
                                text = "该歌曲已收藏！";
                            }
                            break;
                        case RobotConstant.DELETE:
                        case RobotConstant.CANCEL:      //取消收藏
                            JSONObject collectobject = lastTarget.getJSONObject("collectobject");
                            NewAudioEntity audioEntity = JsonUtils.getObj(collectobject.toString(), NewAudioEntity.class);
                            if (audioEntity.getSinger() != null && audioEntity.getName() != null) {     //取消收藏指定歌名+歌手歌曲
                                String[] singers = audioEntity.getSinger();
                                StringBuilder builder = new StringBuilder();
                                for (String singer : singers) {
                                    builder.append(singer).append("，");
                                }
                                builder.setLength(builder.length() - 1);
                                PlayMusic playMusic = mRepository.find(audioEntity.getName(), builder.toString());
                                if (playMusic == null) {
                                    text = "抱歉，您没有收藏过这首歌";
                                } else if (playMusic.getFavorite()) {
                                    mRepository.removeFavorites(playMusic);
                                    mRepository.removeById(mRepository.findByListType(IBatchPlayer.PlayListType.FAVORITE), playMusic.getId());
                                    mRepository.findByListType(IBatchPlayer.PlayListType.FAVORITE).resetIterator();
                                }
                            } else if (audioEntity.getName() != null) {         //取消收藏指定歌名歌曲
                                PlayMusic music = mRepository.findByName(audioEntity.getName());
                                if (music == null) {
                                    text = "抱歉，您没有收藏过这首歌";
                                } else if (music.getFavorite()) {
                                    mRepository.removeFavorites(music);
                                    mRepository.removeById(mRepository.findByListType(IBatchPlayer.PlayListType.FAVORITE), music.getId());
                                    mRepository.findByListType(IBatchPlayer.PlayListType.FAVORITE).resetIterator();
                                }
                            } else {
                                PlayList<PlayMusic> musicList = (PlayList<PlayMusic>) mPlayer.getPlayList(mPlayer.getPlayListType());
                                if (musicList.getCurrent() == null) {
                                    text = "你当前没有播放歌曲哟！";
                                } else if (musicList.getCurrent().getFavorite()) {
                                    new AudioRepository.FavoriteTask(mAppConfig.user, mRepository, mAppConfig.isLocalMode(), new AudioRepository.FavoriteCallback() {
                                        @Override
                                        public void oncomplete(boolean favorite, PlayMusic m) {
                                            if (!favorite) {
                                            /* 将取消收藏的歌曲从收藏列表中移除 */
                                                mRepository.removeById(mRepository.findByListType(IBatchPlayer.PlayListType.FAVORITE), m.getId());
                                                mRepository.findByListType(IBatchPlayer.PlayListType.FAVORITE).resetIterator();
                                            }
                                        }

                                        @Override
                                        public void onSynComplete(boolean result) {

                                        }
                                    }).execute(mPlayer.currentPlayMusic());
                                } else {
                                    text = "该歌曲已取消收藏";
                                }
                            }
                            break;
                        case RobotConstant.QUERY:       //查询收藏列表
                            PlayList<PlayMusic> favoriteList = mRepository.findByListType(IBatchPlayer.PlayListType.FAVORITE);
                            if (favoriteList.size() == 0) {
                                text = "我没有找到任何收藏歌曲";
                            } else {
                                text = "我找到了" + favoriteList.size() + "首收藏的歌曲，需要播放吗？";
                                msgBuilder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
                            }
                            break;
                        case RobotConstant.CLEAR:       //清空收藏列表
                            PlayList<PlayMusic> favorList = mRepository.findByListType(IBatchPlayer.PlayListType.FAVORITE);
                            for (PlayMusic music : favorList) {
                                mRepository.removeFavorites(music);
                            }
                            favorList.clear();
                            text = "已为您清空收藏列表";
                            break;
                    }
                    break;
                case RobotConstant.ACTION_VOICE_ENGINE:     // 声音引擎
                    VoiceEngineEntity voiceEngineEntity = JsonUtils.getObj(lastTarget.toString(), VoiceEngineEntity.class);
                    String recordmode = voiceEngineEntity.getRecordmode();
                    if (recordmode != null) {       //备忘添加模式处理
                        if ("LONG".equals(recordmode)) {        //进入添加模式
                            JSONObject memoAction = actions.getJSONObject(0);
                            Memo appendMemo = AssistDao.getInstance().findMemoBySid(memoAction.getJSONObject("target").getString("sid"));
                            cmd.setTtext("添加模式，所说内容将转化为备忘内容");
                            // 发送回复文本到聊天视图
                            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
                            EventBus.getDefault().post(new ChatMsgEvent(null, null, null, appendMemo));
                            switchRecordMode(true, IflyRecognizer.MODIFY_MEMO_MODE);
                            speakAndShow(cmd, text, inputType, msgBuilder);
                            return;
                        } else if ("DEFAULT".equals(recordmode)) {        //退出添加模式/录音模式
                            if (IflyRecognizer.getInstance().getLong_record_mode() >= IflyRecognizer.DEFAULT_TAPE) {    //退出录音模式
                                switchRecordMode(false, -1);
                            } else {    //退出添加模式
                                switchRecordMode(false, IflyRecognizer.MODIFY_MEMO_MODE);
                                // 1.添加系统提示语
                                EventBus.getDefault().post(new ChatMsgEvent(null, null, "已" + (voiceEngineEntity.getType() == 1 ? "保存" : "取消") + "退出添加模式", null));
                            }
                        } else if ("LONG_TAPE".equals(recordmode)) {      //长录音，并本地保存音频文件
                            switchRecordMode(true, IflyRecognizer.LONG_TAPE);
                        } else if ("DEFAULT_TAPE".equals(recordmode)) {     //正常录音，并本地保存音频文件
                            switchRecordMode(true, IflyRecognizer.DEFAULT_TAPE);
                        }
                    } else {
                        IflyRecognizer.getInstance().setParams(SpeechConstant.LANGUAGE, "ENGLISH".equals(voiceEngineEntity.getLanguage()) ? "en_us" : "zh_cn");
                        IflySynConfig config = IflySynConfig.get(RobotConstant.VoiceMap.get(voiceEngineEntity.getRole()));
                        if (config != null) {
                            if (RobotConstant.VOICE_RANDOM.equals(config.getRole())) {  //随机切换与当前不同的声音
                                boolean differ = true;
                                while (differ) {
                                    int index = new Random().nextInt(mAppConfig.speakers.length);
                                    if (!mAppConfig.speakers[index][3].equals(Setting.getVolName())) {
                                        differ = false;
                                        config.setVolName(mAppConfig.speakers[index][3]);
                                    }
                                }
                            }
                            if (voiceEngineEntity.getType() == 0) {    // 一次切换
                                config.applyTo(IflySynthesizer.getInstance().getSpeechEngine());
                            } else {     //永久切换
                                Setting.setVolName(config.getVolName());
                                for (int i = 0; i < mAppConfig.speakers.length; i++) {
                                    if (mAppConfig.speakers[i][3].equals(config.getVolName())) {
                                        mAppConfig.checkedSpeakerItemPosition = i;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 发送回复文本到聊天视图
        EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
        speakAndShow(cmd, text, inputType, msgBuilder);
    }

    /**
     * 合成语音并显示文本
     **/
    private void speakAndShow(Command cmd, String text, int inputType, SpeechMsgBuilder msgBuilder) {
        if (EventBus.getDefault().hasSubscriberForEvent(RobotTipsEvent.class))
            EventBus.getDefault().post(new RobotTipsEvent(cmd.getTtext()));
        if (inputType == AssistantService.INPUT_VOICE) {
            msgBuilder.setText(text);
            SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
                    /* 合成是在Observable的subscribe()开始的，所以要在这之前通知动画播放。
                     *  doOnSubscribe 执行在离它最近的 subscribeOn() 所指定的线程。*/
                    .doOnNext(new Consumer<SpeechMsg>() {
                        @Override
                        public void accept(SpeechMsg speechMsg) throws Exception {
                            if (speechMsg.state() == SpeechMsg.State.OnBegin)
                                EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_START));
                        }
                    })
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_END));
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe();
        }
    }

    @Override
    public IChatResult getCurrentChatResult() {
        return currentChatResult;
    }

    /**
     * 切换录音识别模式
     **/
    public void switchRecordMode(boolean mode, int long_record_mode) {
        if (IflyRecognizer.isInited()) {
            if (!mode) {
                Intent intent = new Intent(mContext, AssistantService.class);
                intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
                mContext.startService(intent);
            }
            IflyRecognizer.getInstance().setLong_record_mode(long_record_mode);
            IflyRecognizer.getInstance().setRecognizeMode(mode);
        }
    }

    /**
     * 获取要收藏的歌曲
     **/
    private List<PlayMusic> getFavoriteMusics() {
        // List<AudioEntity> list = currentChatResult.getMusicList();
        List<PlayMusic> playList = mPlayer.getPlayList(mPlayer.getPlayListType());
        if (null != playList && playList.size() > 0) {
            /*List<PlayMusic> l = new ArrayList<>();
            for (AudioEntity a : list) {
                l.add(new PlayMusic(a, 0));
            }*/
            mRepository.insertCloudMusics(playList);
            return playList;
        }
        return null;
    }
}
