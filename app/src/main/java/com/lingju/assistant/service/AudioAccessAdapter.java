package com.lingju.assistant.service;

import android.text.TextUtils;

import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.common.adapter.MusicContext;
import com.lingju.context.entity.AudioEntity;
import com.lingju.model.PlayMusic;

import java.util.ArrayList;
import java.util.List;

/**
 * 音乐播放的上下文接口.<br>
 * 实现该接口是为了让聊天机器人能够随时获取当前播放的音频文件的信息
 *
 * @author Leung
 */
public class AudioAccessAdapter implements MusicContext {

    private LingjuAudioPlayer mPlayManager;

    public AudioAccessAdapter(LingjuAudioPlayer audioPlayer) {
        this.mPlayManager = audioPlayer;
    }

    /**
     * PlayMusic类型集合转换为AudioEntity类型集合
     **/
    private List<AudioEntity> convert(List<PlayMusic> list) {
        List<AudioEntity> result = new ArrayList<>();
        if (list != null && list.size() > 0) {
            for (PlayMusic pl : list) {
                String singer = pl.getSinger();
                String[] singers = singer.split("，");
                result.add(new AudioEntity(pl.getTitle(), singers, pl.getMusicid(), pl.getAlbum()));
            }
            return result;
        }
        return result;
    }

    /**
     * 获取当前播放歌曲的名字
     */
    @Override
    public String getName() {
        return mPlayManager.currentPlayMusic() != null ? mPlayManager.currentPlayMusic().getTitle() : null;
    }

    /**
     * 获取当前播放歌曲的演唱歌手
     */
    @Override
    public String getSinger() {
        return mPlayManager.currentPlayMusic() != null ? mPlayManager.currentPlayMusic().getSinger() : null;
    }

    /**
     * 获取当前播放歌曲所属的专辑名称
     */
    @Override
    public String getAlums() {
        return mPlayManager.currentPlayMusic() != null ? mPlayManager.currentPlayMusic().getAlbum() : null;
    }

    /**
     * 获取当前播放那个歌曲的的MusicId
     */
    @Override
    public String getMusicId() {
        return mPlayManager.currentPlayMusic() != null ? mPlayManager.currentPlayMusic().getMusicid() : null;
    }

    /**
     * 根据歌曲名获取对应的歌曲实体集合
     *
     * @param name 歌曲名
     */
    @Override
    public List<AudioEntity> getMusicByName(String name) {
        return convert(getBySongName(name, mPlayManager.getPlayList(IBatchPlayer.PlayListType.LOCAL)));
    }

    /**
     * 根据歌手获取对应歌手的所有歌曲实体集合
     *
     * @param singer 歌手名
     */
    @Override
    public List<AudioEntity> getMusicBySinger(String singer) {
        return convert(getBySinger(singer, mPlayManager.getPlayList(IBatchPlayer.PlayListType.LOCAL)));
    }

    /**
     * 获取对应专辑的所有歌曲实体集合
     *
     * @param album 专辑名
     */
    @Override
    public List<AudioEntity> getMusicByAlbum(String album) {
        return convert(getByAlbums(album, mPlayManager.getPlayList(IBatchPlayer.PlayListType.LOCAL)));
    }

    /**
     * 获取对应歌曲名+歌手的歌曲实体集合
     *
     * @param name   歌名
     * @param singer 歌手
     */
    @Override
    public List<AudioEntity> getMusicByNameAndSinger(String name, String singer) {
        return convert(getBySongNameAndSinger(name, singer, mPlayManager.getPlayList(IBatchPlayer.PlayListType.LOCAL)));
    }

    /**
     * 获取对应歌曲名+专辑名的歌曲实体集合
     *
     * @param name  歌曲名
     * @param album 专辑名
     */
    @Override
    public List<AudioEntity> getMusicByNameAndAlbum(String name, String album) {
        return convert(getBySongNameAndAblums(name, album, mPlayManager.getPlayList(IBatchPlayer.PlayListType.LOCAL)));
    }

    /**
     * 根据歌手或者歌名获取对应歌曲实体集合
     *
     * @param str 歌名or歌手
     */
    @Override
    public List<AudioEntity> getMusicByNameOrSinger(String str) {
        return convert(getBySingerOrSong(str, mPlayManager.getPlayList(IBatchPlayer.PlayListType.LOCAL)));

    }

    /**
     * 当前播放列表歌曲是否是在线歌曲
     *
     * @return true：在线，false：离线
     */
    @Override
    public boolean isOnlineMC() {
        if (mPlayManager.currentPlayMusic()!=null) {
            return mPlayManager.currentPlayMusic().isCloud();
        }
        return false;
    }

    /**
     * 判断手机里是否有歌曲
     *
     * @return true：有，false：没有
     */
    @Override
    public boolean hasMusic() {
        return mPlayManager.getPlayList(IBatchPlayer.PlayListType.LOCAL).size() > 0;
    }

    /**
     * 根据“歌名&&&专辑”从本地歌曲列表中查询相应的歌曲列表
     */
    public List<PlayMusic> getBySongNameAndAblums(String title, String albums, List<PlayMusic> localMusics) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(albums))
            return null;
        List<PlayMusic> list = new ArrayList<>();
        /*if(keyword==null||keyword.length()==0)return list;
        int i=keyword.indexOf("&&&");
		if(i<0||keyword.endsWith("&&&"))return list;
		String title=keyword.substring(0, i);
		String albums=keyword.substring(i+3);*/
        for (PlayMusic m : localMusics) {
            if (m.getTitle().equals(title) && m.getAlbum().equals(albums)) {
                list.add(m);
            }
        }
        if (list.size() == 0) {
            for (PlayMusic m : localMusics) {
                if ((m.getUri().contains(title) || m.getTitle().contains(title))
                        && m.getUri().contains(albums)) {
                    list.add(m);
                }
            }
        }
        return list;
    }

    /**
     * 根据“歌名&&&歌手”从本地歌曲列表中查询相应的歌曲列表
     */
    public List<PlayMusic> getBySongNameAndSinger(String title, String singer, List<PlayMusic> localMusics) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(singer))
            return null;
        List<PlayMusic> list = new ArrayList<>();
        /*if(keyword==null||keyword.length()==0)return list;
        int i=keyword.indexOf("&&&");
		if(i<0||keyword.endsWith("&&&"))return list;
		String title=keyword.substring(0, i);
		String singer=keyword.substring(i+3);*/
        for (PlayMusic m : localMusics) {
            if (m.getSinger().equals(singer) && m.getTitle().equals(title)) {
                list.add(m);
            }
        }
        if (list.size() == 0) {
            for (PlayMusic m : localMusics) {
                if ((m.getUri().contains(singer) || m.getTitle().contains(singer) || m.getSinger().contains(singer))
                        && (m.getUri().contains(title) || m.getTitle().contains(title) || m.getSinger().contains(title))) {
                    list.add(m);
                }
            }
        }
        return list;
    }

    /**
     * 根据“'歌名'或者'歌手的歌名'”从本地歌曲列表中查询相应的歌曲列表
     *
     * @param keyword     '歌名'或者'歌手的歌名'
     * @param localMusics 本地音乐集合
     */
    private List<PlayMusic> getBySingerOrSong(String keyword, List<PlayMusic> localMusics) {
        List<PlayMusic> list = new ArrayList<>();
        if (TextUtils.isEmpty(keyword))
            return list;
        String singer = "", title;
        int l = keyword.indexOf("的");
        if (l == 0 || l == (keyword.length() - 1))
            return list;
        if (l > -1) {
            singer = keyword.substring(0, l);
            if (keyword.endsWith("的歌")) {
                list = getBySinger(singer, localMusics);
            }
        }
        if (list.size() > 0)
            return list;

        title = keyword.substring(l + 1);
        if (singer.length() > 0)
            for (PlayMusic m : localMusics) {
                if (m.getSinger().equals(singer) && m.getTitle().contains(title)) {
                    list.add(m);
                }
            }
        if (list.size() == 0) {
            for (PlayMusic m : localMusics) {
                if (m.getTitle().contains(keyword) || m.getUri().contains(keyword)) {
                    list.add(m);
                }
            }
        }
        return list;
    }

    /**
     * 根据“歌手”从本地歌曲列表中查询相应的歌曲列表
     *
     * @param singer      歌手
     * @param localMusics 本地音乐集合
     */
    private List<PlayMusic> getBySinger(String singer, List<PlayMusic> localMusics) {
        System.out.println("getBySinger>>>>>>>>>>singer" + singer);
        List<PlayMusic> list = new ArrayList<>();
        if (TextUtils.isEmpty(singer))
            return list;
        for (PlayMusic m : localMusics) {
            if (m.getSinger().equals(singer) || m.getUri().contains(singer) || m.getTitle().contains(singer)) {
                list.add(m);
            }
        }
        System.out.println("getBySinger>>>>>>>>>>list=" + list.size());
        return list;
    }

    /**
     * 根据“专辑”从本地歌曲列表中查询相应的歌曲列表
     *
     * @param albums      专辑
     * @param localMusics 本地音乐集合
     */
    private List<PlayMusic> getByAlbums(String albums, List<PlayMusic> localMusics) {
        List<PlayMusic> list = new ArrayList<>();
        if (TextUtils.isEmpty(albums))
            return list;
        for (PlayMusic m : localMusics) {
            if (m.getAlbum().equals(albums) || m.getUri().contains(albums) || m.getTitle().contains(albums)) {
                list.add(m);
            }
        }
        return list;
    }

    /**
     * 根据“歌名”从本地歌曲列表中查询相应的歌曲列表
     *
     * @param title       歌名
     * @param localMusics 本地音乐集合
     */
    private List<PlayMusic> getBySongName(String title, List<PlayMusic> localMusics) {
        System.out.println("getBySongName>>>>>>>>>>title" + title);
        List<PlayMusic> list = new ArrayList<>();
        if (TextUtils.isEmpty(title))
            return list;
        for (PlayMusic m : localMusics) {
            if (m.getTitle().equals(title) || m.getUri().contains(title) || m.getTitle().contains(title)) {
                list.add(m);
            }
        }
        System.out.println("getBySongName>>>>>>>>>>list=" + list.size());
        return list;
    }

}
