package com.lingju.assistant.player.audio.model;

import android.content.Context;
import android.os.AsyncTask;

import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.model.cloud.CloudAudioDataSource;
import com.lingju.assistant.player.audio.model.local.LocalAudioDataSource;
import com.lingju.config.Setting;
import com.lingju.model.PlayMusic;
import com.lingju.model.User;
import com.lingju.util.MusicUtils;
import com.lingju.util.PlayList;

import org.greenrobot.greendao.Property;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created by Administrator on 2016/11/18.
 */
public class AudioRepository implements AudioDataSource {

    private LocalAudioDataSource local;
    private CloudAudioDataSource cloud;
    private PlayList<PlayMusic> localList = new PlayList<>();
    private PlayList<PlayMusic> favoritesList = new PlayList<>();

    private static AudioRepository instance;

    private AudioRepository(Context context) {
        local = new LocalAudioDataSource(context.getApplicationContext());
        cloud = new CloudAudioDataSource();
    }

    public static synchronized AudioRepository create(Context context) {
        if (instance == null)
            instance = new AudioRepository(context);
        return instance;
    }

    public static AudioRepository get() {
        return instance;
    }

    public void syncCloud() {
        //TODO 启动时作云端同步操作

    }

    @Override
    public void delete(PlayMusic music) {
        local.delete(music);
        cloud.delete(music);
    }

    public void resetRquestList(List<PlayMusic> list) {
        this.cloud.resetRquestList(list);
    }

    @Override
    public void save(PlayMusic music) {
        cloud.save(music);
        local.save(music);
    }

    @Override
    public void insert(PlayMusic music) {
        local.insert(music);
        cloud.insert(music);
    }

    /**
     * 插入网络歌曲记录
     **/
    public void insertCloudMusics(List<PlayMusic> list) {
        PlayMusic t;
        PlayMusic p;
        for (int i = 0; i < list.size(); i++) {
            p = list.get(i);
            t = local.getByCloudMusic(p);
            if (t == null) {
                insert(p);
                list.set(i, local.find(p.getMusicid()));
            } else {
                if (p.getLyirc() != null)
                    t.setLyirc(p.getLyirc());
                if (p.getDuration() != 0 && p.getDuration() > 0)
                    t.setDuration(p.getDuration());
                if (p.getRequestGroupId() != 0 && p.getRequestGroupId() > 0)
                    t.setRequestGroupId(p.getRequestGroupId());
                if (p.getPushGroupId() != 0 && p.getPushGroupId() > 0)
                    t.setPushGroupId(p.getPushGroupId());
                list.set(i, t);
                update(t);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).getCloud()) {
                list.add(0, list.remove(i));
            }
        }
    }


    /**
     * 从收藏列表集合中移除指定歌曲
     **/
    public void removeFromFavorteList(PlayMusic music) {
        favoritesList.remove(music);
        favoritesList.resetIterator();
    }

    /**
     * 从指定类型歌曲集合中移除指定ID的歌曲
     **/
    public void removeById(List<PlayMusic> list, long id) {
        int l = list.size();
        while (--l >= 0) {
            if (id == list.get(l).getId()) {
                list.remove(l);
                break;
            }
        }
    }

    /**
     * 将歌曲集合中的元素填充到歌曲数组中
     **/
    public PlayMusic[] convert(List<PlayMusic> list) {
        if (list == null || list.size() == 0)
            return null;
        PlayMusic[] ps = new PlayMusic[list.size()];
        int i = list.size() - 1;
        for (; i >= 0; i--) {
            ps[i] = list.get(i);
        }
        return ps;
    }

    @Override
    public void update(PlayMusic music) {
        local.update(music);
    }

    @Override
    public PlayMusic find(String musicId) {
        PlayMusic r = local.find(musicId);
        return r == null ? cloud.find(musicId) : r;
    }

    @Override
    public PlayMusic find(String name, String singer) {
        PlayMusic r = local.find(name, singer);
        return r == null ? cloud.find(name, singer) : r;
    }

    @Override
    public PlayMusic findByName(String name) {
        PlayMusic r = local.findByName(name);
        return r == null ? cloud.findByName(name) : r;
    }

    @Override
    public PlayMusic find(long id) {
        PlayMusic r = local.find(id);
        return r == null ? cloud.find(id) : r;
    }

    @Override
    public int getCount(int playListType) {
        switch (playListType) {
            case IBatchPlayer.PlayListType.LOCAL:
                return local.getLocalCount();
            case IBatchPlayer.PlayListType.FAVORITE:
                return local.getFavoritesCount();
            case IBatchPlayer.PlayListType.REQUEST:
                return cloud.getRequestList().size();
            case IBatchPlayer.PlayListType.PUSH:
                return cloud.getPushList().size();
        }
        return 0;
    }

    @Override
    public PlayList<PlayMusic> findByListType(int playListType) {
        switch (playListType) {
            case IBatchPlayer.PlayListType.LOCAL:
                if (localList.size() == 0)
                    localList.addAll(local.getLocal());
                return localList;
            case IBatchPlayer.PlayListType.FAVORITE:
                if (favoritesList.size() == 0)
                    favoritesList.addAll(local.getFavorites());
                return favoritesList;
            case IBatchPlayer.PlayListType.REQUEST:
                return cloud.getRequestList();
            case IBatchPlayer.PlayListType.PUSH:
                return cloud.getPushList();
            default:
                return null;
        }
    }

    public void updateLocalMusicCache() {
        localList.addAll(local.getLocal());
    }

    @Override
    public void addFavorites(PlayMusic music) {
        favoritesList.add(music);
        local.addFavorites(music);
        cloud.addFavorites(music);
        favoritesList.resetIterator();
    }

    @Override
    public void removeFavorites(PlayMusic music) {
        local.removeFavorites(music);
        cloud.removeFavorites(music);
    }

    @Override
    public int getMaxIntField(Property property) {
        return local.getMaxIntField(property);
    }

    @Override
    public String getUrl(String musicId) {
        return musicId.startsWith("/") ? local.getUrl(musicId) : cloud.getUrl(musicId);
    }

    @Override
    public String getLyric(String musicId) {
        return musicId.startsWith("/") ? local.getLyric(musicId) : cloud.getLyric(musicId);
    }

    @Override
    public String getLyric(String title, String singer) {
        return cloud.getLyric(title, singer);
    }

    public void resetFavoriteMusics() {
        local.resetFavoriteMusics();
    }

    /**
     * 获取本地已收藏但未同步到云端的歌曲
     *
     * @return
     */
    public List<PlayMusic> getFavoriteUnSyn() {
        return local.getFavoriteUnSyn();
    }

    /**
     * 获取所有本地收藏的歌曲
     **/
    public List<PlayMusic> getFavorite() {
        return local.getFavorites();
    }

    /**
     * 将云端歌曲收藏到本地
     **/
    public int pullFavoriteMusicsFromCloud(User user) {
        return pullFavoriteMusicsFromCloud(MusicUtils.getFavoriteMusicFromCloud(user));
    }

    public int pullFavoriteMusicsFromCloud(String jsonstring) {
        int result = 0;
        try {
            if (jsonstring == null)
                return result;
            JSONObject json = new JSONObject(jsonstring);
            result = json.getInt("counts");
            if (result == 0)
                return result;
            Setting.saveFavoriteMusics(jsonstring);
            JSONArray ja = json.getJSONArray("data");
            int l = ja.length();
            PlayMusic m, temp;
            for (int i = 0; i < l; i++) {
                json = ja.getJSONObject(i);
                m = new PlayMusic();
                m.setCreated(new Timestamp(System.currentTimeMillis()));
                m.setAlbum(json.getString("album"));
                m.setTitle(json.getString("musicname"));
                m.setDuration(Integer.parseInt(json.getString("times")));
                m.setSinger(json.getString("singer"));
                m.setType(json.getString("type"));
                m.setMusicid(json.getString("musicid"));
                m.setCloud(json.getString("islocal").equals("1"));
                if (json.has("requestGroupId")) {
                    m.setRequestGroupId(json.getInt("RequestGroupId"));
                }
                if (json.has("pushGroupId")) {
                    m.setPushGroupId(json.getInt("pushGroupId"));
                }
                m.setFavorite(true);
                m.setSynchronize(true);
                if ((temp = local.getByCloudMusic(m)) != null) {//针对本地有对应文件的云端收藏歌曲，更新该文件记录的收藏字段及收藏同步字段
                    m = temp;
                    if (!m.getFavorite() || !m.getSynchronize()) {
                        m.setFavorite(true);
                        m.setSynchronize(true);
                        update(m);
                    }
                } else if (m.getCloud()) {//针对本地没有对应文件的云端收藏的歌曲：1.在线歌曲直接插入本地数据库，2.本地歌曲忽略
                    insert(m);
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 收藏歌曲任务
     **/
    public static class FavoriteTask extends AsyncTask<PlayMusic, Void, Boolean> {
        private User user;
        private AudioRepository repository;
        private FavoriteCallback callback;
        private boolean isLocalMode;

        public FavoriteTask(User user, AudioRepository repository, boolean isLocalMode, FavoriteCallback callback) {
            this.user = user;
            this.repository = repository;
            this.callback = callback;
            this.isLocalMode = isLocalMode;
        }

        @Override
        protected Boolean doInBackground(PlayMusic... params) {
            boolean r = false;
            for (PlayMusic m : params) {
                if (m.getFavorite()) {
                    /* 取消收藏 */
                    repository.removeFavorites(m);
                    if (callback != null)
                        callback.oncomplete(false, m);
                    if (!isLocalMode && user != null && user.getUserid() != null && m.getSynchronize())
                        r = MusicUtils.unFavoriteMusic(m, user);
                } else {
                    /* 添加收藏 */
                    repository.addFavorites(m);
                    if (callback != null)
                        callback.oncomplete(true, m);
                    if (!isLocalMode && user != null && user.getUserid() != null)
                        r = MusicUtils.favoriteMusic(m, user);
                }
                if (r) {
                    m.setSynchronize(m.getFavorite());
                    repository.update(m);
                }
            }
            if (params.length == 1)
                return r;
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (callback != null)
                callback.onSynComplete(result);
        }
    }

    /**
     * 歌曲收藏回调器
     **/
    public interface FavoriteCallback {
        /**
         * 每次对一个记录收藏或者去收藏完成触发
         *
         * @param favorite true=收藏 false=去收藏
         * @param m        歌曲记录
         */
        void oncomplete(boolean favorite, PlayMusic m);

        /**
         * 收藏任务完成后触发
         *
         * @param result
         */
        void onSynComplete(boolean result);
    }
}
