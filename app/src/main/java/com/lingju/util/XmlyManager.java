package com.lingju.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.TrackPlayEvent;
import com.lingju.assistant.entity.KaoLaAlbum;
import com.lingju.assistant.entity.KaoLaAudioList;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.entity.action.PlayerEntity;
import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.service.VoiceMediator;
import com.lingju.assistant.social.weibo.Constants;
import com.lingju.context.entity.AlbumEntity;
import com.lingju.context.entity.NewAudioEntity;
import com.lingju.model.TrackAlbum;
import com.lingju.model.dao.TingAlbumDao;
import com.lingju.robot.AndroidChatRobotBuilder;
import com.ximalaya.ting.android.opensdk.auth.call.IXmlyAuthListener;
import com.ximalaya.ting.android.opensdk.auth.exception.XmlyException;
import com.ximalaya.ting.android.opensdk.auth.handler.XmlySsoHandler;
import com.ximalaya.ting.android.opensdk.auth.model.XmlyAuth2AccessToken;
import com.ximalaya.ting.android.opensdk.auth.model.XmlyAuthInfo;
import com.ximalaya.ting.android.opensdk.auth.utils.AccessTokenKeeper;
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.LastUpTrack;
import com.ximalaya.ting.android.opensdk.model.album.SubordinatedAlbum;
import com.ximalaya.ting.android.opensdk.model.metadata.Attributes;
import com.ximalaya.ting.android.opensdk.model.metadata.ChildAttributes;
import com.ximalaya.ting.android.opensdk.model.metadata.ChildMetadata;
import com.ximalaya.ting.android.opensdk.model.metadata.MetaData;
import com.ximalaya.ting.android.opensdk.model.metadata.MetaDataList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;
import com.ximalaya.ting.android.opensdk.player.service.IXmPlayerStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayerConfig;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayerException;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2017/6/5.
 */
public class XmlyManager {

    public final static int BASE_COUNT = 30;    //默认一页的记录数
    private static XmlyManager instance;
    private XmlySsoHandler mSsoHandler;
    private Context mContext;
    private XmlyAuth2AccessToken mAccessToken;
    private XmPlayerManager mPlayerManager;
    private boolean isPlaying;      //有声内容播放标记
    private boolean willPlay = true;
    private int i;
    private int totalPage;
    private int totalCount;
    private PlayerEntity<NewAudioEntity> playerEntity;
    private Disposable mUploadDisposable;

    private XmlyManager(Context context) {
        this.mContext = context.getApplicationContext();
        playerEntity = new PlayerEntity<>();
        playerEntity.setType("AUDIO");
        //初始化喜马拉雅SDK
        CommonRequest.getInstanse().init(mContext, Constants.XIMALAYA_APPSECRET);
        CommonRequest.getInstanse().setDefaultPagesize(BASE_COUNT);
        //初始化播放器
        mPlayerManager = XmPlayerManager.getInstance(mContext);
        mPlayerManager.init();
        mPlayerManager.addPlayerStatusListener(mPlayerStatusListener);
        XmPlayerConfig.getInstance(mContext).setSDKHandleAudioFocus(false);
        if (TingAlbumDao.getInstance().unSaveMeta()) {
            final String[] cates = mContext.getResources().getStringArray(R.array.ting_category);
            fillMetaData(cates);
        }
    }

    public static XmlyManager create(Context context) {
        if (instance == null) {
            synchronized (XmlyManager.class) {
                if (instance == null)
                    instance = new XmlyManager(context);
            }
        }
        return instance;
    }

    public static XmlyManager get() {
        return instance;
    }

    public XmPlayerManager getPlayer() {
        return mPlayerManager;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public void setWillPlay(boolean willPlay) {
        this.willPlay = willPlay;
    }

    /**
     * 保存喜马拉雅二级分类
     *
     * @param cates
     */
    private void fillMetaData(final String[] cates) {
        if (i == cates.length)
            return;
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                String cate = cates[i++];
                final String cateId = cate.substring(0, cate.indexOf("-"));
                Log.i("LingJu", "XmlyManager subscribe()>>>分类：" + cate + "id：" + cateId + " " + i);
                Map<String, String> params = new HashMap<>();
                params.put(DTransferConstants.CATEGORY_ID, cateId);
                CommonRequest.getMetadataList(params, new IDataCallBack<MetaDataList>() {
                    @Override
                    public void onSuccess(MetaDataList metaDataList) {
                        List<MetaData> metaDatas = metaDataList.getMetaDatas();
                        if (metaDatas != null && metaDatas.size() > 0) {
                            for (MetaData meta : metaDatas) {
                                List<Attributes> attributes = meta.getAttributes();
                                if (attributes != null && attributes.size() > 0) {
                                    for (Attributes attr : attributes) {
                                        com.lingju.model.MetaData metadata = new com.lingju.model.MetaData();
                                        metadata.setCategoryId(Long.valueOf(cateId));
                                        String superKey = attr.getAttrKey();
                                        metadata.setKey(superKey);
                                        metadata.setValue(attr.getAttrValue());
                                        metadata.setName(attr.getDisplayName());
                                        TingAlbumDao.getInstance().insertMetaData(metadata);
                                        List<ChildMetadata> childMetadatas = attr.getChildMetadatas();
                                        if (childMetadatas != null && childMetadatas.size() > 0) {
                                            for (ChildMetadata childMeta : childMetadatas) {
                                                List<ChildAttributes> childAttrs = childMeta.getAttributes();
                                                for (ChildAttributes childAttr : childAttrs) {
                                                    com.lingju.model.MetaData child = new com.lingju.model.MetaData();
                                                    child.setCategoryId(Long.valueOf(cateId));
                                                    child.setName(childAttr.getDisplayName());
                                                    child.setKey(childAttr.getAttrKey());
                                                    child.setValue(childAttr.getAttrValue());
                                                    child.setSuperId(superKey);
                                                    TingAlbumDao.getInstance().insertMetaData(child);
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }
                        fillMetaData(mContext.getResources().getStringArray(R.array.ting_category));
                    }

                    @Override
                    public void onError(int i, String s) {
                        Log.i("LingJu", "XmlyManager onError()>>>" + i + " " + s);
                    }
                });
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 将喜马拉雅Album对象转换成SDKAlbumEntity对象
     **/
    public AlbumEntity convertEntity(Album album, String type) {
        AlbumEntity entity = new AlbumEntity();
        entity.setAlbumId(album.getId());
        entity.setName(album.getAlbumTitle());
        entity.setSinger(album.getAnnouncer().getNickname());
        entity.setHot(album.getPlayCount());
        entity.setEpisode((int) album.getIncludeTrackCount());
        entity.setType(type);
        entity.setCoverUrl(album.getCoverUrlMiddle());
        NewAudioEntity audioEntity = new NewAudioEntity();
        LastUpTrack lastUptrack = album.getLastUptrack();
        audioEntity.setMusicId(String.valueOf(lastUptrack.getTrackId()));
        audioEntity.setName(lastUptrack.getTrackTitle());
        audioEntity.setRelease(lastUptrack.getCreatedAt());
        entity.setLastAudio(audioEntity);
        return entity;
    }

    /**
     * 将灵聚SDK AlbumEntity对象转换成喜马拉雅Album对象
     **/
    public Album fromEntity(AlbumEntity entity) {
        Album album = new Album();
        album.setId(entity.getAlbumId());
        album.setAlbumTitle(entity.getName());
        album.setPlayCount(entity.getHot());
        album.setCoverUrlMiddle(entity.getCoverUrl());
        album.setIncludeTrackCount(entity.getEpisode());
        NewAudioEntity lastAudio = entity.getLastAudio();
        if (lastAudio != null) {
            LastUpTrack lastTrack = new LastUpTrack();
            lastTrack.setTrackId(Long.valueOf(lastAudio.getMusicId()));
            lastTrack.setTrackTitle(lastAudio.getName());
            lastTrack.setCreatedAt(lastAudio.getCreated());
            album.setLastUptrack(lastTrack);
        }
        return album;
    }

    /**
     * 将本地保存专辑信息填充到喜马拉雅Track对象
     **/
    public Track transformTrack(TrackAlbum localTrack) {
        Track track = new Track();
        track.setKind(PlayableModel.KIND_TRACK);
        track.setDownloadedSaveFilePath(localTrack.getTrackUrl());
        track.setTrackTitle(localTrack.getTrackTitle());
        track.setDataId(localTrack.getTrackId());
        track.setCoverUrlMiddle(localTrack.getTrackPicUrl());
        track.setPlayUrl24M4a(localTrack.getTrackUrl());
        track.setDuration(localTrack.getDuration());
        track.setOrderNum(localTrack.getOrderNum());
        SubordinatedAlbum album = new SubordinatedAlbum();
        album.setAlbumId(localTrack.getId());
        album.setAlbumTitle(localTrack.getAlbumTitle());
        album.setCoverUrlMiddle(localTrack.getAlbumPicUrl());
        track.setAlbum(album);
        return track;
    }

    /**
     * 将灵聚SDK音频对象转换为喜马拉雅声音对象
     **/
    public Track convert2Track(NewAudioEntity audio, Album playAlbum) {
        Track track = new Track();
        track.setKind(PlayableModel.KIND_TRACK);
        track.setDownloadedSaveFilePath(audio.getUrl());
        track.setTrackTitle(audio.getName());
        track.setDataId(Long.valueOf(audio.getMusicId()));
        track.setPlayUrl24M4a(audio.getUrl());
        track.setDuration(audio.getDuration() / 1000);
        track.setCreatedAt(audio.getCreated());
        track.setOrderNum(audio.getEpisode() == 0 ? 0 : audio.getEpisode() - 1);
        track.setCoverUrlMiddle(audio.getAudioPic());
        SubordinatedAlbum album = new SubordinatedAlbum();
        album.setAlbumId(playAlbum.getId());
        album.setAlbumTitle(playAlbum.getAlbumTitle());
        album.setCoverUrlMiddle(playAlbum.getCoverUrlMiddle());
        track.setAlbum(album);
        return track;
    }

    /**
     * 将考拉音频信息对象转换为喜马拉雅声音对象
     **/
    public Track convert2Track(KaoLaAudioList.KaoLaAudio audio) {
        Track track = new Track();
        track.setKind(PlayableModel.KIND_TRACK);
        track.setDownloadedSaveFilePath(audio.getAacPlayUrl());
        track.setTrackTitle(audio.getAudioName());
        track.setDataId(audio.getAudioId());
        track.setPlayUrl24M4a(audio.getAacPlayUrl());
        track.setDuration(audio.getDuration() / 1000);
        track.setCreatedAt(audio.getUpdateTime());
        track.setOrderNum(audio.getOrderNum() - 1);
        track.setCoverUrlMiddle(audio.getAudioPic());
        SubordinatedAlbum album = new SubordinatedAlbum();
        album.setAlbumId(audio.getAlbumId());
        album.setCoverUrlMiddle(audio.getAlbumPic());
        album.setAlbumTitle(audio.getAlbumName());
        track.setAlbum(album);
        return track;
    }

    /**
     * 获取考拉FM的声音集合
     *
     * @param id      专辑id
     * @param pageNum 页码
     * @param sort    排序  0（默认，降序） 1（升序）
     * @return trackList：声音集合   size>0(正常)   size=0(没有集数)   =null(异常状态)
     **/
    public List<Track> getKaoLaTrackByAlbumId(long id, int pageNum, int sort) {
        List<Track> trackList = null;
        try {
            if (AndroidChatRobotBuilder.get() != null) {
                //参数 1：专辑ID  2：一页的数量  3：页码  4：排序（0：默认，降序  1：升序）
                String tracks = AndroidChatRobotBuilder.get().robot().ThirdPartyApiAccessor().searchAudioAid(id, XmlyManager.BASE_COUNT, pageNum, sort);
                JSONObject trackJson = new JSONObject(tracks);
                if (trackJson.getInt("status") == 0) {
                    trackList = new ArrayList<>();
                    KaoLaAudioList kaoLaAudioList = JsonUtils.getObj(trackJson.optJSONObject("content").toString(), KaoLaAudioList.class);
                    totalCount = kaoLaAudioList.getCount();
                    totalPage = kaoLaAudioList.getSumPage();
                    List<KaoLaAudioList.KaoLaAudio> dataList = kaoLaAudioList.getDataList();
                    for (KaoLaAudioList.KaoLaAudio audio : dataList) {
                        trackList.add(convert2Track(audio));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trackList;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getTotalPage() {
        return totalPage;
    }

    /**
     * 获取指定id的考拉专辑信息
     **/
    public Album getKaoLaAlbumById(long albumId) {
        Album album = null;
        try {
            if (AndroidChatRobotBuilder.get() != null) {
                String albumMsg = AndroidChatRobotBuilder.get().robot().ThirdPartyApiAccessor().searchSingleAlbumById(albumId);
                JSONObject albumJson = new JSONObject(albumMsg);
                if (albumJson.getInt("status") == 0) {
                    KaoLaAlbum kaoLaAlbum = JsonUtils.getObj(albumJson.optJSONObject("content").toString(), KaoLaAlbum.class);
                    album = fromKaoLaAlbum(kaoLaAlbum);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return album;
    }

    private Album fromKaoLaAlbum(KaoLaAlbum kaoLaAlbum) {
        Album album = new Album();
        album.setId(kaoLaAlbum.getId());
        album.setAlbumTitle(kaoLaAlbum.getName());
        album.setPlayCount(kaoLaAlbum.getListenNum());
        album.setCoverUrlMiddle(kaoLaAlbum.getImg());
        album.setIncludeTrackCount(kaoLaAlbum.getCountNum());
        List<Track> tracks = getKaoLaTrackByAlbumId(kaoLaAlbum.getId(), 1, 0);
        if (tracks != null && tracks.size() > 0) {
            Track lastTrack = tracks.get(0);
            //部分专辑倒序查找第一条记录并非最新记录，需要按正序查找第一条
            if (lastTrack.getOrderNum() != totalCount)
                tracks = getKaoLaTrackByAlbumId(kaoLaAlbum.getId(), 1, 1);
            if (tracks != null && tracks.size() > 0) {
                lastTrack = tracks.get(0);
                LastUpTrack lastUpTrack = new LastUpTrack();
                lastUpTrack.setTrackId(lastTrack.getDataId());
                lastUpTrack.setTrackTitle(lastTrack.getTrackTitle());
                lastUpTrack.setCreatedAt(lastTrack.getCreatedAt());
                album.setLastUptrack(lastUpTrack);
            }
        }
        return album;
    }

    /**
     * 获取一组指定id的专辑
     **/
    public List<Album> getKaoLaAlbumByIds(List<Long> ids) {
        List<Album> albums = new ArrayList<>();
        try {
            if (AndroidChatRobotBuilder.get() != null) {
                String albumMsg = AndroidChatRobotBuilder.get().robot().ThirdPartyApiAccessor().searchMultipleByAlbumByIds(ids);
                List<KaoLaAlbum> list = JsonUtils.getList(albumMsg, KaoLaAlbum.class);
                for (KaoLaAlbum kaola : list) {
                    albums.add(fromKaoLaAlbum(kaola));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return albums;
    }

    public XmlyManager createSsoHandler(Activity activity) {
        XmlyAuthInfo authInfo = new XmlyAuthInfo(mContext, Constants.XIMALAYA_APPKEY, Constants.XIMALAYA_REDIRECT_URL, "");
        mSsoHandler = new XmlySsoHandler(activity, authInfo);
        return instance;
    }

    /**
     * 授权登录
     **/
    public void authorize(Activity activity) {
        AccessTokenKeeper.clear(mContext);
        XmlyAuthInfo authInfo = new XmlyAuthInfo(mContext, Constants.XIMALAYA_APPKEY, Constants.XIMALAYA_REDIRECT_URL, "");
        mSsoHandler = new XmlySsoHandler(activity, authInfo);
        mSsoHandler.authorize(new AuthListener());
    }

    /**
     * 授权口令是否有效
     **/
    public boolean isSessionValid() {
        mAccessToken = AccessTokenKeeper.readAccessToken(mContext);
        return mAccessToken.isSessionValid();
    }

    public XmlyAuth2AccessToken getAccessToken() {
        return mAccessToken;
    }

    /**
     * 授权回调，用于调起授权页面的Activity的onActivityResult()方法中
     **/
    public void callBack(int requestCode, int resultCode, Intent data) {
        if (mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }

    /**
     * 授权监听器
     **/
    class AuthListener implements IXmlyAuthListener {

        @Override
        public void onComplete(Bundle bundle) {
            mAccessToken = AccessTokenKeeper.readAccessToken(mContext);
            // AccessTokenManager.getInstanse().setAccessTokenAndUid(mAccessToken.getToken(), mAccessToken.getExpiresAt(), mAccessToken.getUid());
            Log.i("LingJu", "AuthListener onComplete()>>> 有效期：" + TimeUtils.time2String(new Date(mAccessToken.getExpiresAt())) + " " + mAccessToken.toString());
        }

        @Override
        public void onXmlyException(XmlyException e) {
            String msg = e.getMessage();
            Log.i("LingJu", "AuthListener onXmlyException()>>" + msg);
            /*if ("206".equals(msg))      // access_token失效
                authorize();*/
        }

        @Override
        public void onCancel() {
            Log.i("LingJu", "AuthListener onCancel()>>>>");
        }
    }

    /**
     * 播放器回调监听器
     **/
    private IXmPlayerStatusListener mPlayerStatusListener = new IXmPlayerStatusListener() {

        //播放
        @Override
        public void onPlayStart() {
            if (LingjuAudioPlayer.get().isPlaying())
                LingjuAudioPlayer.get().pause();
            isPlaying = true;
            VoiceMediator.get().setAudioPlayType(VoiceMediator.XIMALAYA_TYPE);
            // Toast.makeText(mContext, "onPlayStart", Toast.LENGTH_SHORT).show();
            PlayableModel currSound = mPlayerManager.getCurrSound();
            EventBus.getDefault().post(new TrackPlayEvent(true, currSound));
            updatePlayerState(IBatchPlayer.PlayState.Start, (Track) currSound);
        }

        //暂停
        @Override
        public void onPlayPause() {
            isPlaying = false;
            // Toast.makeText(mContext, "onPlayPause", Toast.LENGTH_SHORT).show();
            PlayableModel currSound = mPlayerManager.getCurrSound();
            EventBus.getDefault().post(new TrackPlayEvent(false, currSound));
            //保存播放记录
            TingAlbumDao.getInstance().insertHistory(currSound, mPlayerManager.getHistoryPos(currSound.getDataId()));
            updatePlayerState(IBatchPlayer.PlayState.Pause, (Track) currSound);
        }

        //停止
        @Override
        public void onPlayStop() {
            isPlaying = false;
            EventBus.getDefault().post(new TrackPlayEvent(false, mPlayerManager.getCurrSound()));
        }

        //结束
        @Override
        public void onSoundPlayComplete() {

        }

        //准备
        @Override
        public void onSoundPrepared() {
        }

        //开始播放（播放流程最先执行方法，在这已经开始播放声音）
        @Override
        public void onSoundSwitch(PlayableModel laModel, PlayableModel curModel) {
            Log.i("LingJu", "XmlyManager onSoundSwitch()>>> " + willPlay);
            if (willPlay) {
                isPlaying = true;
                // Toast.makeText(mContext, "onSoundSwitch", Toast.LENGTH_SHORT).show();
                if (curModel != null)
                    EventBus.getDefault().post(new TrackPlayEvent(true, curModel));
                if (laModel != null)
                    TingAlbumDao.getInstance().insertHistory(laModel, mPlayerManager.getHistoryPos(laModel.getDataId()));
            }
            willPlay = true;
        }

        //开始缓冲
        @Override
        public void onBufferingStart() {
        }

        //停止缓冲
        @Override
        public void onBufferingStop() {

        }

        //缓冲进度回调
        @Override
        public void onBufferProgress(int i) {

        }

        //播放进度回调
        @Override
        public void onPlayProgress(int i, int i1) {

        }

        //播放出错
        @Override
        public boolean onError(XmPlayerException e) {
            isPlaying = false;
            EventBus.getDefault().post(new TrackPlayEvent(false, mPlayerManager.getCurrSound()));
            return false;
        }
    };

    public void setPlayerEntity(PlayerEntity<NewAudioEntity> entity) {
        this.playerEntity = entity;
    }

    /**
     * 更新播放器状态并上传服务器
     **/
    private void updatePlayerState(IBatchPlayer.PlayState state, Track track) {
        switch (state) {
            case Start:
                playerEntity.setControl("PLAY");
                if (mUploadDisposable != null && !mUploadDisposable.isDisposed()) {
                    mUploadDisposable.dispose();
                    mUploadDisposable = null;
                }
                keepPlayer();
                break;
            case Pause:
                playerEntity.setControl("PAUSE");
                break;
        }
        setAudioEntity(track);
        uploadPlayer();
    }

    private void uploadPlayer() {
        Log.i("LingJu", "LingjuAudioPlayer uploadPlayer()>> " + playerEntity);
        List<PlayerEntity<NewAudioEntity>> list = new ArrayList<>();
        list.add(playerEntity);
        AndroidChatRobotBuilder.get().robot().actionTargetAccessor().uploadContextObject(RobotConstant.ACTION_PLAYER, list);
    }

    private void setAudioEntity(Track track) {
        List<NewAudioEntity> result = new ArrayList<>();
        if (track != null) {
            NewAudioEntity audioEntity = new NewAudioEntity();
            audioEntity.setMusicId(String.valueOf(track.getDataId()));
            audioEntity.setUrl(track.getPlayUrl24M4a());
            audioEntity.setName(track.getTrackTitle());
            audioEntity.setAlbum(String.valueOf(track.getAlbum().getAlbumId()));
            audioEntity.setEpisode(track.getOrderNum() == 0 ? 0 : track.getOrderNum() + 1);
            audioEntity.setDuration(track.getDuration() * 1000);
            audioEntity.setAudioPic(track.getCoverUrlMiddle());
            audioEntity.setCreated(track.getCreatedAt());
            result.add(audioEntity);
        }
        playerEntity.setObject(result);
    }

    private void keepPlayer() {
        mUploadDisposable = Observable.interval(2, 2, TimeUnit.MINUTES)
                .takeUntil(new Predicate<Long>() {
                    @Override
                    public boolean test(Long aLong) throws Exception {
                        return !isPlaying();
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        uploadPlayer();
                    }
                });
    }
}
