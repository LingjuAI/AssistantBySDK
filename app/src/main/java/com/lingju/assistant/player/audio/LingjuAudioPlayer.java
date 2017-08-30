package com.lingju.assistant.player.audio;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.entity.Lyric;
import com.lingju.assistant.player.audio.IBatchPlayer.BluetoothChannelController;
import com.lingju.assistant.player.audio.IBatchPlayer.BodyView;
import com.lingju.assistant.player.audio.IBatchPlayer.HeaderView;
import com.lingju.assistant.player.audio.IBatchPlayer.PlayProgressListener;
import com.lingju.assistant.player.audio.IBatchPlayer.PlayState;
import com.lingju.assistant.player.audio.IBatchPlayer.PlayStateListener;
import com.lingju.assistant.player.audio.model.AudioRepository;
import com.lingju.assistant.player.exception.FetchUrlException;
import com.lingju.assistant.player.exception.LingjuPlayException;
import com.lingju.assistant.player.exception.MediaBufferFailedException;
import com.lingju.assistant.player.exception.MediaBufferIOException;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.VoiceMediator;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.config.Setting;
import com.lingju.model.PlayMusic;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.common.log.Log;
import com.lingju.util.MusicUtils;
import com.lingju.util.NetUtil;
import com.lingju.util.PlayList;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;

import org.greenrobot.eventbus.EventBus;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/18.
 */
public class LingjuAudioPlayer implements IBatchPlayer.Presenter {
    private final static String TAG = "LingjuAudioPlayer";
    public static final int NOTIFICATION_ID = 30;
    public final static String PLAY_UNDER3G_HISTORY = "3g下播放歌曲3";

    private IBatchPlayer.HeaderView notiHeader;
    private IBatchPlayer.HeaderView header;
    private IBatchPlayer.BodyView body;

    private final AudioRepository repository;
    private final Context mContext;
    private final AtomicBoolean prepared = new AtomicBoolean(false);
    private final AtomicBoolean initedSate = new AtomicBoolean(false);
    private AtomicBoolean headerInitState = new AtomicBoolean(true);
    private final LocalMusicContentObserver localMusicContentObserver;
    private final Set<PlayProgressListener> progressListeners = new HashSet<>();

    private MediaPlayer mPlayer;

    private PlayStateListener mPsListener;
    private int playlistType;
    private int playMode;
    private int seekOnStartPlay;
    private BluetoothChannelController bluetoothChannelController;
    //移动网络下禁止播放在线音乐
    private boolean noOnlinePlayInMobileNet = true;
    private Disposable playListenerDisposable;

    private static LingjuAudioPlayer instance;
    private AudioManager mAudioManager;
    private NotificationManager notificationManager;

    private LingjuAudioPlayer(Context context) {
        this.mContext = context;
        this.repository = AudioRepository.create(context);
        localMusicContentObserver = new LocalMusicContentObserver(new Handler(Looper.getMainLooper()));
        mContext.getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, false, localMusicContentObserver);
        boolean allow3g = AppConfig.dPreferences.getBoolean(AppConfig.DOWNLOAD_ON_WIFI, false);
        noOnlinePlayInMobileNet = !allow3g;
        /**
         * 使用Observable.subscribe()时，RxJava默认提供一个出错时消费者，然而该消费者默认为空（无语）；
         * 需要开发者自行赋值。
         * **/
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                throwable.printStackTrace();
                if (throwable instanceof FetchUrlException) {
                    Log.i("LingJu", "ErrorHandler>>>" + ((FetchUrlException) throwable).getCode());
                    String text = null;
                    SpeechMsgBuilder builder = SpeechMsgBuilder.create(text);
                    switch (((FetchUrlException) throwable).getCode()) {
                        case IBatchPlayer.Error.FETCH_OFFLINE_FAILED:
                            text = mContext.getResources().getString(R.string.no_play_network_none);
                            break;
                        case IBatchPlayer.Error.FETCH_ON_2G_NETWORK:
                            text = mContext.getResources().getString(R.string.no_play_network_2g);
                            break;
                        case IBatchPlayer.Error.FETCH_ON_34G_NETWORK:
                            Intent intent = new Intent(mContext, AssistantService.class);
                            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
                            intent.putExtra(AssistantService.TEXT, PLAY_UNDER3G_HISTORY);
                            mContext.startService(intent);
                            text = Setting.CONFIRM_PLAY_UNDER_3G;
                            builder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
                            break;
                        case IBatchPlayer.Error.FETCHED_FAILED:
                            break;
                    }
                    if (TextUtils.isEmpty(text))
                        return;
                    //添加回复文本
                    EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
                    //合成声音
                    SynthesizerBase.get().startSpeakAbsolute(builder.setText(text).build())
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
                            })
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.computation())
                            .subscribe();
                }
            }
        });
    }

    public static synchronized LingjuAudioPlayer create(Context context) {
        if (instance == null)
            instance = new LingjuAudioPlayer(context.getApplicationContext());
        return instance;
    }

    public static LingjuAudioPlayer get() {
        return instance;
    }


    public void addHeader(HeaderView header) {
        this.header = header;
        if (header == null)
            return;
        this.header.setPresenter(this);
    }

    public void addBody(BodyView body) {
        this.body = body;
        if (body == null)
            return;
        this.body.setPresenter(this);
    }

    @Override
    public void subscribe() {
        if (header != null) {
            header.showPlayMusic(currentPlayMusic());
            header.updateSeekBar(0, 100);
        }
    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public void setNoOnlinePlayInMobileNet(boolean noOnlinePlayInMobileNet) {
        this.noOnlinePlayInMobileNet = noOnlinePlayInMobileNet;
    }

    public boolean isNoOnlinePlayInMobileNet() {
        return noOnlinePlayInMobileNet;
    }

    @Override
    public void registerProgressListener(PlayProgressListener progressListener) {
        if (progressListener == null || progressListeners.contains(progressListener))
            return;
        progressListeners.add(progressListener);
    }

    @Override
    public void unregisterProgressListener(PlayProgressListener progressListener) {
        if (progressListener == null)
            return;
        progressListeners.remove(progressListener);
    }

    @Override
    public void play() {
        Log.i("LingJu", "LingjuAudioPlayer play()>>>>开始播放 " + Thread.currentThread().getName());
        if (mPlayer != null) {
            VoiceMediator.get().setAudioPlayType(VoiceMediator.AUTO_TYPE);
            if (XmPlayerManager.getInstance(mContext).isPlaying())      //播放歌曲时暂停有声内容播放
                XmPlayerManager.getInstance(mContext).pause();
            if (mAudioManager == null)
                mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if (bluetoothChannelController != null) {
                bluetoothChannelController.execute(mAudioManager);
            }
            /*if (robotService.isBlueToothHeadSet()) {
                if (!robotService.isSuportA2DP()) {
                    if (audioManager.getMode() != AudioManager.MODE_NORMAL) {
                        Log.e(TAG, "playInChannel>>setMode(AudioManager.MODE_NORMAL)");
                        audioManager.setMode(AudioManager.MODE_NORMAL);
                    }
                    if (audioManager.isBluetoothScoOn()) {
                        audioManager.setBluetoothScoOn(false);
                        audioManager.stopBluetoothSco();
                    }
                } else {
                    if (!audioManager.isBluetoothA2dpOn()) {
                        Log.e(TAG, "playInChannel>>setBluetoothA2dpOn(true)");
                        audioManager.setBluetoothA2dpOn(true);
                    }
                }
            }*/
            mPsListener.onMusicPlayStart();
            mPlayer.start();
            updateView(PlayState.Start);
            if (playListenerDisposable == null || playListenerDisposable.isDisposed())
            /** 每隔一秒刷新一次歌词列表 **/
                playListenerDisposable = Observable.interval(500L, 1000L, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                //Log.i(TAG,"playListenerDisposable>>doOnSubscribe");
                                if (mPlayer != null && !mPlayer.isPlaying()) {
                                    disposable.dispose();
                                }
                            }
                        })
                        .doOnNext(new Consumer<Long>() {
                            @Override
                            public void accept(Long l) throws Exception {
                                // Log.i(TAG,"playListenerDisposable>>doOnNext");
                                if (mPlayer == null || !mPlayer.isPlaying())
                                    return;
                                updateView(PlayState.Playing);
                                for (PlayProgressListener listener : progressListeners) {
                                    listener.onProgress(mPlayer.getCurrentPosition(), mPlayer.getDuration());
                                }
                            }
                        })
                        .subscribe();

        }
    }

    public void requestAudioFocus() {
        //获取音频焦点，并监控；当焦点变化时音量进行调整（如导航中播放歌曲，导航播报需大声，音乐需小声）
        if (mAudioManager == null)
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    public void abandonAudioFocus() {
        mAudioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    @Override
    public Observable<PlayMusic> play(boolean preload) {
        return play(currentPlayMusic(), preload);
    }

    @Override
    public Observable<PlayMusic> play(PlayMusic music) {
        return play(music, false);
    }

    @Override
    public Observable<PlayMusic> play(final PlayMusic music, final boolean preload) {
        prepared.set(false);
        return music == null
                ? Observable.<PlayMusic>empty()
                : Observable.just(music)
                .subscribeOn(Schedulers.io())
                .map(new Function<PlayMusic, PlayMusic>() {//获取music的url
                    @Override
                    public PlayMusic apply(PlayMusic music) throws Exception {
                        if (music.getCloud()) {
                            NetUtil.NetType type = NetUtil.getInstance(mContext).getCurrentNetType();
                            //Log.e(TAG, "FetchUrlLrcTask:"+type.toString());
                            if (type == NetUtil.NetType.NETWORK_TYPE_NONE) {
                                // mPsListener.onMusicFetchError(IBatchPlayer.Error.FETCH_OFFLINE_FAILED);
                                throw new FetchUrlException(IBatchPlayer.Error.FETCH_OFFLINE_FAILED);
                                //SynthesizerBase.get().startSpeakAbsolute(robotService.getResources().getString(R.string.no_play_network_none));
                            } else if (type == NetUtil.NetType.NETWORK_TYPE_2G) {
                                // mPsListener.onMusicFetchError(IBatchPlayer.Error.FETCH_ON_2G_NETWORK);
                                throw new FetchUrlException(IBatchPlayer.Error.FETCH_ON_2G_NETWORK);
                                //SynthesizerBase.get().startSpeakAbsolute(robotService.getResources().getString(R.string.no_play_network_2g));
                            } else if (type == NetUtil.NetType.NETWORK_TYPE_3G && noOnlinePlayInMobileNet) {
                                // mPsListener.onMusicFetchError(IBatchPlayer.Error.FETCH_ON_34G_NETWORK);
                                throw new FetchUrlException(IBatchPlayer.Error.FETCH_ON_34G_NETWORK);
                            }
                            String temp = TextUtils.isEmpty(music.getUri()) ? MusicUtils.getMusicOnlineUri(music.getMusicid()) : music.getUri();
                            Log.e(TAG, "FetchUrlTask:uri=" + temp);
                            if (null != temp && temp.length() > 0) {
                                music.setUri(temp);
                                music.setFetched(true);
                                music.setCreated(new Timestamp(System.currentTimeMillis()));
                            } else {
                                Log.e(TAG, "获取路径出错，一般为网络问题");
                                // mPsListener.onMusicFetchError(IBatchPlayer.Error.FETCHED_FAILED);
                                throw new FetchUrlException(IBatchPlayer.Error.FETCHED_FAILED);
                            }
                        }
                        return music;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())  //指定flatMap执行线程
                .flatMap(new Function<PlayMusic, ObservableSource<PlayMusic>>() {
                    @Override
                    public ObservableSource<PlayMusic> apply(final PlayMusic music) throws Exception {
                        updateView(PlayState.Request);
                        return _play(music, preload);
                    }
                });
    }

    @Override
    public Observable<PlayMusic> play(int position) {
        if (repository.findByListType(playlistType).getPlayMode() != this.playMode) {
            repository.findByListType(playlistType).setPlayMode(this.playMode);
        }
        return play(repository.findByListType(playlistType).getAndMark(position));
    }

    @Override
    public Observable<PlayMusic> play(int position, boolean preLoad) {
        if (repository.findByListType(playlistType).getPlayMode() != this.playMode) {
            repository.findByListType(playlistType).setPlayMode(this.playMode);
        }
        return play(repository.findByListType(playlistType).getAndMark(position), preLoad);
    }

    @Override
    public Observable<PlayMusic> playNext() {
        if (repository.findByListType(playlistType).getPlayMode() != this.playMode) {
            repository.findByListType(playlistType).setPlayMode(this.playMode);
        }
        return play(repository.findByListType(playlistType).getNext());
    }

    @Override
    public Observable<PlayMusic> playPre() {
        if (repository.findByListType(playlistType).getPlayMode() != this.playMode) {
            repository.findByListType(playlistType).setPlayMode(this.playMode);
        }
        return play(repository.findByListType(playlistType).getPre());
    }

    private Observable<PlayMusic> _play(final PlayMusic music, final boolean preload) throws Exception {
        Observable<PlayMusic> r = Observable.create(new ObservableOnSubscribe<PlayMusic>() {
            @Override
            public void subscribe(final ObservableEmitter<PlayMusic> e) throws Exception {
                if (null != mPlayer) {
                    if (mPlayer.isPlaying()) {
                        if (playListenerDisposable != null && !playListenerDisposable.isDisposed())
                            playListenerDisposable.dispose();
                        mPlayer.stop();
                        playListenerDisposable = null;
                        // TODO: 2017/6/2 停止并保存正在播放的有声音频记录，重置播放器准备播放下一首
                    }
                }
                Log.e(TAG, "e============" + (e == null));
                //InitSongEvent initSongEvent = new InitSongEvent();
                if (!music.getUri().matches("^(http|\\/).+\\.(aac|ogg|wma|mmf|mp3|amr|mid|m4a|mp4|wav|pcm|tta|flac|au|ape|tak|wv)$")) {
                    Log.w(TAG, "PlayMusicByInternerUrl music link error");
                    //initSongEvent.setError(IBatchPlayer.Error.FORMAT_NOSUPPORT);
                    e.onError(new LingjuPlayException(IBatchPlayer.Error.FORMAT_NOSUPPORT));
                } else {
                    try {
                        Log.w(TAG, "PlayMusicByInternerUrl url=" + music.getUri());
                        Uri myUri = Uri.parse(music.getUri());
                        if (mPlayer == null) {
                            mPlayer = new MediaPlayer();
                            mPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
                            /*mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

                                @Override
                                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                                    Log.i(TAG, "onBufferingUpdate>>" + percent);
                                }
                            });*/
                            mPlayer.setOnCompletionListener(onCompletionListener);
                        }
                        initedSate.set(false);
                        mPlayer.reset();
                        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                            @Override
                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                Log.i("LingJu", "LingjuAudioPlayer what=" + what + " extra=" + extra);
                                if (Build.VERSION.SDK_INT >= 17 && (extra == -1004 || extra == -110)) {
                                    e.onError(new MediaBufferIOException(extra));
                                } else
                                    e.onError(new MediaBufferFailedException(extra));

                                return true;
                            }
                        });
                        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                initedSate.set(true);
                                int duration = mPlayer.getDuration();
                                if (seekOnStartPlay > 0) {
                                    mPlayer.seekTo(seekOnStartPlay * duration / 100);
                                    seekOnStartPlay = 0;
                                }
                                music.setDuration(duration);
                                Log.i(TAG, "onPrepared duration:" + duration + "-e=" + (e == null) + "--music=" + music);
                                prepared.set(true);
                                updateView(PlayState.Prepared);
                                e.onNext(music);
                                e.onComplete();
                            }
                        });
                        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mPlayer.setDataSource(mContext, myUri);
                        music.setDuration(-1);
                        mPlayer.prepareAsync();
                        //initSongEvent.setError(0);
                        //initSongEvent.setDuration(-1);
                        //EventBus.getDefault().post(initSongEvent);
                        //fetchLrc(playTask);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        mPlayer.reset();
                        //initSongEvent.setError(2);
                        throw ex;
                    }
                }
            }
        })
                .subscribeOn(Schedulers.io())
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        if (throwable instanceof LingjuPlayException)
                            mPsListener.onMusicPrepareError(((LingjuPlayException) throwable).getCode());
                    }
                })
                .retry(5, new Predicate<Throwable>() {
                    @Override
                    public boolean test(Throwable throwable) throws Exception {
                        return throwable instanceof MediaBufferIOException;
                    }
                });
        if (!preload) {     //没有预加载（手动点击）
            r = r.observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(new Consumer<PlayMusic>() {
                        @Override
                        public void accept(PlayMusic music) throws Exception {
                            play();
                        }
                    });
        }
        return r;
    }

    private void updateView(PlayState state) {
        switch (state) {
            case Request:
                PlayMusic music = currentPlayMusic();
                if (music == null)
                    return;
                if (header != null) {
                    header.showPlayMusic(music);
                    header.updateSeekBar(0, 100);
                }
                //加载歌词
                Observable.just(music)
                        .subscribeOn(Schedulers.newThread())
                        .doOnNext(new Consumer<PlayMusic>() {
                            @Override
                            public void accept(PlayMusic music) throws Exception {
                                if (TextUtils.isEmpty(music.getLyirc()))
                                    music.setLyirc(repository.getLyric(music.getMusicid()));
                            }
                        }).map(new Function<PlayMusic, Lyric>() {
                    @Override
                    public Lyric apply(PlayMusic music) throws Exception {
                        return TextUtils.isEmpty(music.getLyirc()) ? new Lyric() : new Lyric(music.getLyirc());
                    }
                })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(new Consumer<Lyric>() {
                            @Override
                            public void accept(Lyric lyric) throws Exception {
                                if (body != null) {
                                    body.setLyric(lyric);
                                }
                            }
                        })
                        .subscribe();
                break;
            case Prepared:
                if (body != null) {
                    body.updatePlayerList(playlistType, repository.findByListType(playlistType).getCurrentOrderIndex());
                }
                break;
            case Start:
                PlayMusic m = currentPlayMusic();
                if (header != null) {
                    header.setPlayState(true);
                    if (m != null) {
                        header.showPlayMusic(m);
                        header.noitfiy(m.getTitle(), m.getSinger(), true);
                    }
                }
                if (body != null) {
                    body.updatePlayerList(playlistType, repository.findByListType(playlistType).getCurrentOrderIndex());
                    if (m != null) {
                        body.showCurrentMusic(m);
                    }
                }
                break;
            case Playing:
                if (header != null) {
                    header.updateSeekBar(mPlayer.getCurrentPosition(), mPlayer.getDuration());
                    header.noitfiy(null, null, null);
                    if (headerInitState.get()) {     //在其他页面播放音乐回到主界面时，显示头部播放栏需要刷新歌曲信息
                        PlayMusic pm = currentPlayMusic();
                        if (pm != null) {
                            header.showPlayMusic(pm);
                            header.setPlayState(true);
                            header.noitfiy(pm.getTitle(), pm.getSinger(), true);
                        }
                        headerInitState.set(false);
                    }
                }
                if (body != null) {
                    body.updateLrc(mPlayer.getCurrentPosition(), mPlayer.getDuration());
                }
                break;
            case Pause:
            case Idle:
                if (header != null) {
                    header.setPlayState(false);
                    header.noitfiy(null, null, false);
                }
                break;
        }
    }

    @Override
    public void closeNotification() {
        if (notificationManager == null)
            notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public void pause() {
        if (mPlayer != null)
            mPlayer.pause();
        updateView(PlayState.Pause);
        if (playListenerDisposable != null && !playListenerDisposable.isDisposed()) {
            playListenerDisposable.dispose();
            playListenerDisposable = null;
        }
        // TODO: 2017/6/2 保存当前有声音频播放记录
    }

    @Override
    public void resumeTrack(PlayMusic music) {
        // TODO: 2017/6/7 设置播放进度 seekOnStartPlay
    }

    @Override
    public boolean isPlaying() {
        return mPlayer != null && isInitedState() && mPlayer.isPlaying();
    }

    @Override
    public boolean hadPlay() {
        return mPlayer != null && prepared();
    }

    @Override
    public boolean isInitedState() {
        return initedSate.get();
    }

    @Override
    public void setHeaderInitState(boolean isInit) {
        headerInitState.set(isInit);
    }

    @Override
    public boolean prepared() {
        return prepared.get();
    }

    @Override
    public void addToFavoites(PlayMusic music) {
        repository.addFavorites(music);
    }

    @Override
    public void removeFromFavoites(PlayMusic music) {
        repository.removeFavorites(music);
    }

    @Override
    public void removeFavoriteFromList(PlayMusic music) {
        repository.removeFromFavorteList(music);
    }

    @Override
    public void stop() {
        if (mPlayer != null) {
            if (playListenerDisposable != null && !playListenerDisposable.isDisposed())
                playListenerDisposable.dispose();
            mPlayer.stop();
            playListenerDisposable = null;
        }
    }


    @Override
    public void release() {
        stop();
        initedSate.set(false);
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        progressListeners.clear();
        mContext.getContentResolver().unregisterContentObserver(localMusicContentObserver);
    }

    @Override
    public void resetPlayProgress(int percent) {
        if (mPlayer != null) {
            try {
                if (isInitedState()) {
                    mPlayer.seekTo(mPlayer.getDuration() * percent / 100);
                    if (!mPlayer.isPlaying()) {
                        play();
                    }
                } else {
                    seekOnStartPlay = percent;
                }
            } catch (Exception e) {
                seekOnStartPlay = percent;
            }
        }
    }

    @Override
    public PlayMusic currentPlayMusic() {
        //Log.i(TAG,"playlistType>>"+playlistType+">>"+repository.findByListType(playlistType).getCurrent().getMusicid());
        return repository.findByListType(playlistType).getCurrent();
    }

    @Override
    public void setPlayMode(int playMode) {
        if (playMode >= PlayList.PlayMode.ORDER && playMode <= PlayList.PlayMode.SINGLE) {
            this.playMode = playMode;
            if (repository.findByListType(playlistType).getPlayMode() != this.playMode) {
                repository.findByListType(playlistType).setPlayMode(this.playMode);
            }
            Observable.just(this.playMode)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) throws Exception {
                            if (body != null) {
                                body.updatePlayMode(integer);
                            }
                        }
                    })
                    .subscribe();
        }
    }

    @Override
    public void resetLyric() {
        PlayMusic m = currentPlayMusic();
        if (m != null && !TextUtils.isEmpty(m.getLyirc()) && this.body != null) {
            this.body.setLyric(new Lyric(m.getLyirc()));
        }
    }

    @Override
    public void showLyric() {
        //显示播放列表 (有可能存在歌词列表还未显示过（body==null）的情况，通过头部播放栏先保证播放列表显示)
        if (header != null)
            header.showPlayListPager();
        //展开歌词列表(若音乐列表是第一次显示时，视图初始化需要一段时间，不能马上展开歌词列表)
        if (body != null) {
            Single.just(0)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) throws Exception {
                            body.showLyric();
                        }
                    })
                    .subscribe();
        }
    }


    @Override
    public int getPlayMode() {
        return playMode;
    }

    @Override
    public void setPlayListType(int playListType) {
        if (playListType >= IBatchPlayer.PlayListType.PUSH && playListType <= IBatchPlayer.PlayListType.LOCAL) {
            this.playlistType = playListType;
        }
    }

    @Override
    public int getPlayListType() {
        return playlistType;
    }

    @Override
    public List<PlayMusic> getPlayList(int playListType) {
        return this.repository.findByListType(playListType);
    }

    @Override
    public void setPlayStateListener(IBatchPlayer.PlayStateListener stateListener) {
        this.mPsListener = stateListener;
    }

    @Override
    public void setBluetoothChannelController(BluetoothChannelController controller) {
        this.bluetoothChannelController = controller;
    }


    class LocalMusicContentObserver extends ContentObserver {


        public LocalMusicContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        }

        //监听系统的音频数据库是否有改变，有则更新本地音频数据的缓存
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            // Log.i("LocalMusicContentObserver", selfChange + "--uri>>" + uri.toString());
            if (selfChange && MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.equals(uri)) {
                repository.updateLocalMusicCache();
            }
        }

    }

    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        private float volScalar = 0f;

        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.e(TAG, "audioFocusChangeListener.onAudioFocusChange>>>>>>>>>>>>>>>>>>" + focusChange);
            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                    if (mPlayer != null) {
                        volScalar = ((float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        // Log.i("LingJu", "获取媒体音量：" + volScalar);
                        mPlayer.setVolume(0.2f, 0.2f);

                    }
                    break;
                }
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mPlayer != null) {
                        // Log.i("LingJu", "获取焦点后设置播放音量：" + volScalar);
                        volScalar = volScalar == 0 ? 0.5f : volScalar;
                        mPlayer.setVolume(volScalar, volScalar);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            // abandonAudioFocus();
            mPsListener.onMusicPlayCompleted();
            // TODO: 2017/6/2 若保存过当前播放的有声音频记录，播放结束清除播放记录
            playNext().subscribe();
        }
    };

}
