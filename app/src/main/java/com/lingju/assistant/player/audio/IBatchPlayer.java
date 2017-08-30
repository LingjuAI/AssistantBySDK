package com.lingju.assistant.player.audio;

import android.content.Context;
import android.media.AudioManager;

import com.lingju.assistant.IPresenter;
import com.lingju.assistant.IView;
import com.lingju.assistant.entity.Lyric;
import com.lingju.model.PlayMusic;

import java.util.List;

import io.reactivex.Observable;

/**
 * Created by Administrator on 2016/11/18.
 */
public interface IBatchPlayer {

    public interface HeaderView extends IView<Presenter> {
        /**
         * 设置播放状态
         *
         * @param playing
         */
        public void setPlayState(boolean playing);

        /**
         * 更新歌曲进度条
         **/
        public void updateSeekBar(int currentDuration, int duration);

        /**
         * 显示播放中的歌曲信息
         *
         * @param playMusic
         */
        public void showPlayMusic(PlayMusic playMusic);

        /**
         * 显示播放列表
         **/
        void showPlayListPager();

        /**
         * 使播放器UI消失
         **/
        void disappear();

        /**
         * 初始化音乐播放通知栏
         **/
        void initNotification(Context context);

        /**
         * 刷新通知栏
         **/
        void noitfiy(String title, String singer, Boolean isPlay);

    }

    public interface BodyView extends IView<Presenter> {

        /**
         * 使播放器的列表界面消失
         **/
        public void disappear();

        /**
         * 更新歌词显示UI
         *
         * @param currentDuration
         * @param duration
         */
        public void updateLrc(int currentDuration, int duration);

        /**
         * 更新播放列表UI
         *
         * @param listType        列表类型
         * @param playingPosition 播放歌曲所在的位置 base 0
         */
        public void updatePlayerList(int listType, int playingPosition);

        /**
         * 显示当前正在播放的playmuic
         *
         * @param music
         */
        public void showCurrentMusic(PlayMusic music);

        /**
         * 更新当前的播放模式
         *
         * @param playMode
         */
        public void updatePlayMode(int playMode);

        public void setLyric(Lyric lyric);

        void showLyric();

        /**
         * 更新收藏状态
         **/
        void updateFavorte();

    }

    public interface Presenter extends IPresenter {
        /**
         * 播放歌曲
         */
        public void play();

        /**
         * 预加载
         *
         * @param preload
         */
        public Observable<PlayMusic> play(boolean preload);

        /**
         * 播放指定歌曲
         *
         * @param music
         */
        public Observable<PlayMusic> play(PlayMusic music);

        /**
         * 播放指定歌曲
         *
         * @param music
         * @param preload true=预加载，不播放
         */
        public Observable<PlayMusic> play(PlayMusic music, boolean preload);

        /**
         * 播放列表中指定位置的歌曲
         *
         * @param position
         */
        public Observable<PlayMusic> play(int position);

        /**
         * 播放列表中指定位置的歌曲
         *
         * @param position
         * @param preLoad
         * @return
         */
        public Observable<PlayMusic> play(int position, boolean preLoad);

        /**
         * 播放下一首
         */
        public Observable<PlayMusic> playNext();

        /**
         * 播放上一首
         */
        public Observable<PlayMusic> playPre();

        /**
         * 暂停播放
         */
        public void pause();

        /**
         * 从播放记录开始继续播放有声音频
         **/
        void resumeTrack(PlayMusic music);

        /**
         * 当前是否正在播放中
         *
         * @return
         */
        public boolean isPlaying();

        /**
         * 是否播放过歌曲
         **/
        boolean hadPlay();

        /**
         * 是否处于已初始化状态
         *
         * @return
         */
        public boolean isInitedState();

        void setHeaderInitState(boolean isInit);

        /**
         * 当前是否刚好预加载完一个音频文件
         *
         * @return
         */
        public boolean prepared();

        public void addToFavoites(PlayMusic music);

        public void removeFromFavoites(PlayMusic music);

        /**
         * 从收藏列表中移除指定歌曲
         **/
        void removeFavoriteFromList(PlayMusic music);

        /**
         * 停止播放，消除通知栏
         */
        public void stop();

        public void release();

        public void resetPlayProgress(int percent);

        /**
         * 获取当前播放中的PlayMusic
         *
         * @return
         */
        public PlayMusic currentPlayMusic();

        /**
         * 设置播放模式
         *
         * @param playMode
         */
        public void setPlayMode(int playMode);

        public void resetLyric();

        void showLyric();

        public int getPlayMode();

        void closeNotification();

        /**
         * 设置播放列表类型
         *
         * @param playListType
         */
        public void setPlayListType(int playListType);

        public int getPlayListType();

        public List<PlayMusic> getPlayList(int playListType);

        /**
         * 设置播放状态监听器
         *
         * @param stateListener
         */
        public void setPlayStateListener(PlayStateListener stateListener);

        /**
         * 设置蓝牙通道控制器
         *
         * @param controller
         */
        public void setBluetoothChannelController(BluetoothChannelController controller);

        /**
         * 设置是否允许在移动网络播放在线歌曲
         *
         * @param noOnlinePlayInMobileNet
         */
        public void setNoOnlinePlayInMobileNet(boolean noOnlinePlayInMobileNet);

        /**
         * 注册播放进度监听器
         *
         * @param progressListener
         */
        public void registerProgressListener(PlayProgressListener progressListener);

        /**
         * 反注册播放进度监听器
         *
         * @param progressListener
         */
        public void unregisterProgressListener(PlayProgressListener progressListener);

    }

    public interface Error {
        public final static int FETCH_OFFLINE_FAILED = 1;
        public final static int FETCH_ON_2G_NETWORK = 2;
        public final static int FETCH_ON_34G_NETWORK = 3;
        public final static int FETCHED_FAILED = 4;
        public final static int PLAY_ERROR = 5;
        public final static int FORMAT_NOSUPPORT = 6;
    }

    public enum PlayState {
        Idle, Request, Prepared, Start, Playing, Pause;
    }

    public interface PlayListType {
        int PUSH = 0;
        int REQUEST = 1;
        int FAVORITE = 2;
        int LOCAL = 3;
    }

    public interface PlayStateListener {
        void onMusicFetchError(int errorType);

        void onMusicPrepareError(int errorCode);

        void onMusicPlayStart();

        void onMusicPlayCompleted();
    }

    public interface BluetoothChannelController {

        public void execute(AudioManager audioManager);

    }

    public interface PlayProgressListener {
        public void onProgress(int currentDuration, int duration);
    }


}
