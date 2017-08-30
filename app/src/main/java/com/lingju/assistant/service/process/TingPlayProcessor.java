package com.lingju.assistant.service.process;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.DialogEvent;
import com.lingju.assistant.activity.event.RobotTipsEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.entity.TingAlbumMsg;
import com.lingju.assistant.entity.action.PlayerEntity;
import com.lingju.assistant.player.event.UpdateWaittingSeekBarEvent;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.VoiceMediator;
import com.lingju.assistant.service.process.base.BaseProcessor;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.common.adapter.ChatRobotBuilder;
import com.lingju.context.entity.AlbumEntity;
import com.lingju.context.entity.Command;
import com.lingju.context.entity.NewAudioEntity;
import com.lingju.context.entity.SyncSegment;
import com.lingju.model.MetaData;
import com.lingju.model.TrackAlbum;
import com.lingju.model.dao.TingAlbumDao;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.robot.AndroidChatRobotBuilder;
import com.lingju.util.XmlyManager;
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.AlbumList;
import com.ximalaya.ting.android.opensdk.model.album.SearchAlbumList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.model.track.TrackList;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2017/6/15.<br/>
 */
public class TingPlayProcessor extends BaseProcessor {

    public final static String KAOLA_FM = "kaolafm";
    private TingAlbumDao mAlbumDao;
    private SpeechMsgBuilder msgBuilder;
    private boolean isResponse;
    private int mEpisode;
    private Timer mPlayTimer;
    private String mType;   //专辑一级分类
    private List<AlbumEntity> mAlbumEntities = new ArrayList<>();
    private boolean isDelay = true;     //是否延迟一段时间后自动选择播放节目标记
    private List<Album> mAlbums;

    public TingPlayProcessor(Context mContext, SystemVoiceMediator mediator) {
        super(mContext, mediator);
        mAlbumDao = TingAlbumDao.getInstance();
    }

    @Override
    public int aimCmd() {
        return CMD_TING;
    }

    @Override
    public void handle(Command cmd, String text, int inputType) {
        super.handle(cmd, text, inputType);
        Observable<List<Album>> searchObservable = null;
        msgBuilder = SpeechMsgBuilder.create(text);
        isResponse = true;
        mEpisode = 0;
        mAlbumEntities.clear();
        try {
            if (cmd.getOutc() == DefaultProcessor.OUTC_ASK)
                msgBuilder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
            if (EventBus.getDefault().hasSubscriberForEvent(RobotTipsEvent.class))
                EventBus.getDefault().post(new RobotTipsEvent(cmd.getTtext()));
            JSONArray actions = new JSONArray(cmd.getActions());
            JSONObject lastAction = actions.getJSONObject(actions.length() - 1);
            JSONObject lastTarget = lastAction.getJSONObject("target");
            if (lastTarget.getInt("id") == RobotConstant.ACTION_ALBUM) {    //查找专辑（考拉FM）
                isResponse = false;
                mAlbums = new ArrayList<>();
                for (int i = 0; i < actions.length(); i++) {
                    JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                    AlbumEntity album = SyncSegment.fromJson(target.toString(), AlbumEntity.class);
                    mAlbums.add(XmlyManager.get().fromEntity(album));
                }
                EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(msgBuilder.getText()), null, null, null));
                EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_TING_ALBUM_STATE));
                // 展示专辑列表
                EventBus.getDefault().post(new ChatMsgEvent(new TingAlbumMsg(mAlbums, mEpisode)));

            } else if (!lastTarget.isNull("control")) {
                Integer control = RobotConstant.PlayerMap.get(lastTarget.getString("control"));
                switch (control) {
                    case RobotConstant.CONTROL_PLAY:    //播放有声音频（需要先查找）
                        if ("KAOLA".equals(lastTarget.getString("origin"))) {       // 来源：考拉
                            msgBuilder.setContextMode(SpeechMsg.CONTEXT_AUTO);
                            PlayerEntity player = SyncSegment.fromJson(lastTarget.toString(), PlayerEntity.class);
                            List<NewAudioEntity> audios = player.getObject();
                            NewAudioEntity playAudio = audios.get(0);
                            //先判断是否有播放记录，优先播放历史记录
                            TrackAlbum trackAlbum = mAlbumDao.checkHistory(playAudio);
                            if (trackAlbum != null) {
                                playHistory(trackAlbum);
                                return;
                            }
                            Album playAlbum = null;
                            for (Album album : mAlbums) {
                                if (album.getId() == Long.valueOf(playAudio.getAlbum())) {
                                    playAlbum = album;
                                    break;
                                }
                            }
                            final Track playTrack = XmlyManager.get().convert2Track(playAudio, playAlbum);
                            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
                            //移除上一个播放专辑
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_TING_TRACK_STATE));
                            //显示新的播放专辑
                            EventBus.getDefault().post(new ChatMsgEvent(new TingAlbumMsg(playAlbum, playTrack)));
                            mSynthesizer.startSpeakAbsolute(msgBuilder.build())
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
                                            if (!TextUtils.isEmpty(msgBuilder.getText()))
                                                EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_END));
                                            List<Track> list = new ArrayList<>();
                                            list.add(playTrack);
                                            XmlyManager.get().setPlaying(true);
                                            XmPlayerManager.getInstance(mContext).playList(list, 0);
                                        }
                                    })
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.computation())
                                    .subscribe();
                            return;
                        } else {        //来源：喜马拉雅
                            cancelPlayTimer();
                            isResponse = false;
                            voiceMediator.setAudioPlayType(VoiceMediator.XIMALAYA_TYPE);
                            if (actions.length() > 1) {     //根据查询条件使用喜马拉雅SDK查找专辑
                                isDelay = true;
                                // msgBuilder.setText("");
                                EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_TOGGLE_TYPE));
                                JSONObject action = actions.getJSONObject(0);
                                JSONObject target = action.getJSONObject("target");
                                NewAudioEntity audio = SyncSegment.fromJson(target.toString(), NewAudioEntity.class);
                                //先判断是否有播放记录，优先播放历史记录
                                TrackAlbum trackAlbum = mAlbumDao.checkHistory(audio);
                                if (trackAlbum != null) {
                                    playHistory(trackAlbum);
                                    return;
                                }
                                mType = audio.getType();
                                String cateId = "0";
                                String[] categorys = mContext.getResources().getStringArray(R.array.ting_category);
                                //获取音频分类id
                                for (String cate : categorys) {
                                    if (cate.contains(mType)) {
                                        cateId = cate.substring(0, cate.indexOf("-"));
                                        break;
                                    }
                                }
                                //相声、评书要单独分类
                                if ("12".equals(cateId) && TextUtils.isEmpty(audio.getTag())) {
                                    audio.setTag(audio.getSinger() == null ? mType : audio.getSinger()[0] + " " + mType);
                                }
                                //获取搜索维度
                                int calc_dimension = 3;
                                if (!action.isNull("sort")) {
                                    String sort = action.getJSONObject("sort").getString("orderby");
                                    if ("release".equals(sort))      //最新
                                        calc_dimension = 2;
                                    else if ("tag".equals(sort))    //经典或播放最多
                                        calc_dimension = 3;
                                    else                            //热门、评分、排行
                                        calc_dimension = 1;
                                }

                                //获取播放集数
                                mEpisode = audio.getEpisode();
                                if (!TextUtils.isEmpty(audio.getTag())) {
                                    searchObservable = getAlbumBySubCate(audio.getTag(), cateId, calc_dimension);
                                } else if (!TextUtils.isEmpty(audio.getName()) || audio.getAnchor() != null || audio.getSinger() != null) {
                                    StringBuilder keyword = new StringBuilder();
                                    keyword.append(audio.getAnchor() == null ? "" : audio.getAnchor().get(0));
                                    keyword.append(audio.getSinger() == null ? "" : audio.getSinger()[0]);
                                    keyword.append(TextUtils.isEmpty(audio.getName()) ? "" : audio.getName());
                                    searchObservable = getAlbumByKeyWord(keyword.toString(), cateId, calc_dimension);
                                } else {
                                    searchObservable = getAlbumByCate(cateId, calc_dimension);
                                }
                            } else {        //播放选中的专辑
                                isDelay = false;
                                msgBuilder.setContextMode(SpeechMsg.CONTEXT_AUTO);
                                JSONArray object = lastTarget.getJSONArray("object");
                                for (int i = 0; i < object.length(); i++) {
                                    AlbumEntity entity = SyncSegment.fromJson(object.getJSONObject(i).toString(), AlbumEntity.class);
                                    //先判断是否有播放记录，优先播放历史记录
                                    NewAudioEntity audio = new NewAudioEntity();
                                    audio.setEpisode(mEpisode);
                                    audio.setName(audio.getName());
                                    TrackAlbum trackAlbum = mAlbumDao.checkHistory(audio);
                                    if (trackAlbum != null) {
                                        playHistory(trackAlbum);
                                        return;
                                    }
                                    mAlbumEntities.add(entity);
                                }
                                // EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_TING_ALBUM_STATE));
                                EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
                            }
                        }
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 发送回复文本到聊天视图
        if (isResponse)
            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(msgBuilder.getText()), null, null, null));
        final Observable<SpeechMsg> msgObservable = mSynthesizer.startSpeakAbsolute(msgBuilder.build())
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
                        if (!TextUtils.isEmpty(msgBuilder.getText()))
                            EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_END));
                        if (!isResponse && mAlbumEntities.size() > 0) {      //播放控制，说完话后进行播放倒计时
                            Log.i("LingJu", "TingPlayProcessor doOnComplete()>>> " + mAlbumEntities.get(0).getName());
                            startPlayTask(new PlayTrackTask(mAlbumEntities.get(0)));
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation());
        if (searchObservable != null) {
            searchObservable.observeOn(AndroidSchedulers.mainThread())
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            Log.i("LingJu", "searchObservable doOnComplete()>>> " + msgBuilder.getText());
                            msgObservable.subscribe();
                            //上传专辑列表
                            if (mAlbumEntities != null && mAlbumEntities.size() > 0)
                                AndroidChatRobotBuilder.get().robot().actionTargetAccessor().uploadContextObject(mAlbumEntities.get(0).getId(), mAlbumEntities);
                        }
                    })
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            msgObservable.subscribe();
                        }
                    })
                    .subscribe(defaultObserver);
        } else {
            msgObservable.subscribe();
        }

    }

    /**
     * 播放历史记录
     **/
    private void playHistory(TrackAlbum trackAlbum) {
        msgBuilder.setText("找到播放历史，");
        if (trackAlbum.getTrackPicUrl().contains(KAOLA_FM)) {
            List<Track> tracks = new ArrayList<>();
            tracks.add(XmlyManager.get().transformTrack(trackAlbum));
            String content = msgBuilder.getText() + "即将为您播放" + trackAlbum.getAlbumTitle() + "第" + (trackAlbum.getOrderNum() + 1) + "集";
            synthesizeAndShowResp(tracks, content, 0);
        } else {
            int episode = trackAlbum.getOrderNum() + 1;
            AlbumEntity entity = new AlbumEntity();
            entity.setAlbumId(trackAlbum.getId());
            getTrackByAlbumId(entity, episode);
        }
    }

    @Override
    public void cancelTingTask() {
        cancelPlayTimer();
    }

    /**
     * 按一级分类查找专辑
     **/
    private Observable<List<Album>> getAlbumByCate(String cateId, int calc_dimension) {
        final Map<String, String> params = new HashMap<>();
        params.put(DTransferConstants.CATEGORY_ID, cateId);
        params.put(DTransferConstants.CALC_DIMENSION, String.valueOf(calc_dimension));
        return Observable.create(new ObservableOnSubscribe<List<Album>>() {
            @Override
            public void subscribe(final ObservableEmitter<List<Album>> e) throws Exception {
                CommonRequest.getAlbumList(params, new IDataCallBack<AlbumList>() {
                    @Override
                    public void onSuccess(AlbumList albumList) {
                        //onNext的参数不允许为null
                        e.onNext(albumList.getAlbums());
                        e.onComplete();
                    }

                    @Override
                    public void onError(int i, String s) {
                        e.onError(new Throwable(i + " " + s));
                    }
                });
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 按关键词查找专辑
     **/
    private Observable<List<Album>> getAlbumByKeyWord(String keyword, String cateId, int calc_dimension) {
        final Map<String, String> params = new HashMap<>();
        params.put(DTransferConstants.SEARCH_KEY, keyword);
        params.put(DTransferConstants.CATEGORY_ID, cateId);
        params.put(DTransferConstants.CALC_DIMENSION, String.valueOf(calc_dimension));
        return Observable.create(new ObservableOnSubscribe<List<Album>>() {
            @Override
            public void subscribe(final ObservableEmitter<List<Album>> e) throws Exception {
                CommonRequest.getSearchedAlbums(params, new IDataCallBack<SearchAlbumList>() {
                    @Override
                    public void onSuccess(SearchAlbumList searchAlbumList) {
                        e.onNext(searchAlbumList.getAlbums());
                        e.onComplete();
                    }

                    @Override
                    public void onError(int i, String s) {
                        e.onError(new Throwable(i + " " + s));
                    }
                });
            }
        })
                .subscribeOn(Schedulers.io());
    }

    /**
     * 根据二级分类获取专辑
     **/
    private Observable<List<Album>> getAlbumBySubCate(String tag, String cateId, int calc_dimension) {
        final Map<String, String> params = new HashMap<>();
        params.put(DTransferConstants.CATEGORY_ID, cateId);
        List<MetaData> metaDatas = mAlbumDao.findMetaDataByName(Long.valueOf(cateId), tag);
        if (metaDatas.size() > 0) {
            tag = "";
            for (MetaData meta : metaDatas) {
                tag += meta.getKey() + ":" + meta.getValue() + ";";
            }
            tag = tag.substring(0, tag.length() - 1);
            params.put(DTransferConstants.METADATA_ATTRIBUTES, tag);
        }
        params.put(DTransferConstants.CALC_DIMENSION, String.valueOf(calc_dimension));
        return Observable.create(new ObservableOnSubscribe<List<Album>>() {
            @Override
            public void subscribe(final ObservableEmitter<List<Album>> e) throws Exception {
                CommonRequest.getMetadataAlbumList(params, new IDataCallBack<AlbumList>() {
                    @Override
                    public void onSuccess(AlbumList albumList) {
                        e.onNext(albumList.getAlbums());
                        e.onComplete();
                    }

                    @Override
                    public void onError(int i, String s) {
                        e.onError(new Throwable(i + " " + s));
                    }
                });
            }
        })
                .subscribeOn(Schedulers.io());
    }

    private Observer<List<Album>> defaultObserver = new Observer<List<Album>>() {

        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onNext(List<Album> albums) {
            if (albums != null && albums.size() > 0) {
                // String content = "";
                List<Album> list = new ArrayList<>();
                for (int i = 0; i < albums.size(); i++) {
                    Album album = albums.get(i);
                    AlbumEntity entity = XmlyManager.get().convertEntity(album, mType);
                    mAlbumEntities.add(entity);
                    // content += "第" + (i + 1) + "条：" + album.getPlayCount() + " " + album.getAlbumTitle() + "\n";
                }
                list.addAll(albums);
                // 发送回复文本到聊天视图
                msgBuilder.setText("我找到了以下相关专辑，你要听哪一个呢？");
                EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(msgBuilder.getText()), null, null, null));
                EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_TING_ALBUM_STATE));
                // 展示专辑列表
                EventBus.getDefault().post(new ChatMsgEvent(new TingAlbumMsg(list, mEpisode)));
                // msgBuilder.setText("");
            } else {
                msgBuilder.setText("抱歉，没有找到你说的节目");
                EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(msgBuilder.getText()), null, null, null));
            }
        }

        @Override
        public void onError(Throwable e) {
            msgBuilder.setText("抱歉，您的请求失败了，请再试一次吧");
            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(msgBuilder.getText()), null, null, null));
            // Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onComplete() {

        }
    };

    /**
     * 根据专辑ID查询声音并播放
     **/
    private void getTrackByAlbumId(final AlbumEntity album, final int episode) {
        //计算播放集数在专辑中的页数和播放索引
        int pageNum;
        int playIndex = 0;
        if (episode == 0)
            pageNum = 1;
        else {
            pageNum = episode / XmlyManager.BASE_COUNT;
            playIndex = episode % XmlyManager.BASE_COUNT;
            pageNum = playIndex == 0 ? pageNum : pageNum + 1;
            playIndex = playIndex == 0 ? XmlyManager.BASE_COUNT - 1 : playIndex - 1;
        }
        Log.i("LingJu", "TingPlayProcessor>>>页数：" + pageNum + " 索引：" + playIndex);
        Map<String, String> params = new HashMap<>();
        params.put(DTransferConstants.ALBUM_ID, String.valueOf(album.getAlbumId()));
        params.put(DTransferConstants.PAGE, String.valueOf(pageNum));
        final int finalPlayIndex = playIndex;
        CommonRequest.getTracks(params, new IDataCallBack<TrackList>() {
            @Override
            public void onSuccess(TrackList trackList) {
                final List<Track> tracks = trackList.getTracks();
                if (tracks != null && tracks.size() > 0) {
                    String content = "开始为您播放" + trackList.getAlbumTitle();
                    if (episode == 0) {
                        content += "第1集";
                    } else if (episode == trackList.getTotalCount()) {
                        content += "最新的第" + episode + "集";
                    } else {
                        content += "第" + episode + "集";
                    }
                    synthesizeAndShowResp(tracks, msgBuilder.getText() + content, finalPlayIndex);
                    if (TextUtils.isEmpty(msgBuilder.getText())) {  //播放搜索记录
                        //移除上一个播放专辑
                        EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_TING_TRACK_STATE));
                        //显示新的播放专辑
                        EventBus.getDefault().post(new ChatMsgEvent(new TingAlbumMsg(XmlyManager.get().fromEntity(album), tracks.get(finalPlayIndex))));
                    }
                } else {
                    synthesizeAndShowResp(null, "没有更多集数了", 0);
                }
            }

            @Override
            public void onError(int i, String s) {
                synthesizeAndShowResp(null, "播放异常，请重试", 0);
            }
        });
    }

    /**
     * 合成并显示回复文本
     **/
    private void synthesizeAndShowResp(final List<Track> tracks, String content, final int finalPlayIndex) {
        EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(content), null, null, null));
        mSynthesizer.startSpeakAbsolute(content)
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
                        if (tracks != null)
                            XmlyManager.get().getPlayer().playList(tracks, finalPlayIndex);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe();

    }

    /**
     * 执行播放任务
     **/
    private void startPlayTask(final PlayTrackTask trackTask) {
        cancelPlayTimer();
        msgBuilder.setText("");
        mPlayTimer = new Timer();
        if (isDelay) {
            Single.just(0)
                    .delay(100, TimeUnit.MILLISECONDS)
                    .doOnSuccess(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) throws Exception {
                            EventBus.getDefault().post(new UpdateWaittingSeekBarEvent(false));
                            mPlayTimer.schedule(trackTask, 5000);
                        }
                    })
                    .observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .subscribe();
        } else {
            mPlayTimer.schedule(trackTask, 0);
        }
    }

    /**
     * 关闭定时器
     **/
    private void cancelPlayTimer() {
        if (mPlayTimer != null) {
            mPlayTimer.cancel();
            mPlayTimer = null;
        }
    }

    /**
     * 声音播放任务
     **/
    class PlayTrackTask extends TimerTask {
        private AlbumEntity album;

        public PlayTrackTask(AlbumEntity entity) {
            this.album = entity;
            // episode = mEpisode == -1 ? entity.getEpisode() : mEpisode;
        }

        @Override
        public void run() {
            if (isDelay) {
                //停止识别，并推送NOANSWER告知Robot本地已自动选择播放节目
                Intent rIntent = new Intent(mContext, AssistantService.class);
                rIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
                mContext.startService(rIntent);
                Intent pushIntent = new Intent(mContext, AssistantService.class);
                pushIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
                pushIntent.putExtra(AssistantService.TEXT, ChatRobotBuilder.NOANSWER);
                mContext.startService(pushIntent);
            }
            getTrackByAlbumId(album, mEpisode);
            cancelPlayTimer();
        }
    }
}
