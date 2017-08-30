package com.lingju.assistant.player.audio.model.local;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.model.AudioDataSource;
import com.lingju.model.PlayMusic;
import com.lingju.model.PlayMusicDao;
import com.lingju.model.PlayMusicDao.Properties;
import com.lingju.model.dao.DaoManager;
import com.lingju.common.log.Log;
import com.lingju.util.MusicUtils;
import com.lingju.util.PlayList;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created by Administrator on 2016/11/18.
 */
public class LocalAudioDataSource implements AudioDataSource {

    private PlayMusicDao dao;
    private Context mContext;
    private final static String MIN_AUDIO_SIZE = "120000";

    public LocalAudioDataSource(Context context) {
        if (DaoManager.get() != null) {
            dao = DaoManager.get().getDaoSession().getPlayMusicDao();
            this.mContext = context;
        }
    }

    @Override
    public void delete(PlayMusic music) {
        dao.delete(music);
    }

    @Override
    public void save(PlayMusic music) {
        dao.save(music);
    }

    @Override
    public void insert(PlayMusic music) {
        dao.insert(music);
    }

    @Override
    public void update(PlayMusic music) {
        dao.update(music);
    }

    @Override
    public PlayMusic find(String musicId) {
        return dao.queryBuilder().where(Properties.Musicid.eq(musicId)).unique();
    }

    @Override
    public PlayMusic find(String name, String singer) {
        PlayMusic r = findLocal(name, singer);
        return r == null ? findFavorites(name, singer) : r;
    }

    @Override
    public PlayMusic findByName(String name) {
        return dao.queryBuilder().where(Properties.Title.eq(name)).limit(1).unique();
    }

    /* 根据网络歌曲查找本地记录 */
    public PlayMusic getByCloudMusic(PlayMusic music) {
        WhereCondition condition = dao.queryBuilder().and(Properties.Title.eq(music.getTitle()), Properties.Singer.eq(music.getSinger()));
        return dao.queryBuilder().whereOr(Properties.Musicid.eq(music.getId()), condition).limit(1).unique();
    }

    public PlayMusic findLocal(String name, String singer) {
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = null;
        if (cr != null) {
            try {
                cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                                MediaStore.Audio.Media._ID,
                                MediaStore.Audio.Media.TITLE,
                                MediaStore.Audio.Media.DATA,
                                MediaStore.Audio.Media.DURATION,
                                MediaStore.Audio.Media.SIZE,
                                MediaStore.Audio.Media.ALBUM,
                                MediaStore.Audio.Media.ARTIST
                        },
                        MediaStore.Audio.Media.DURATION + ">? AND " + MediaStore.Audio.Media.TITLE + "=? AND " + MediaStore.Audio.Media.ARTIST + "=?",
                        new String[]{MIN_AUDIO_SIZE, name, singer},
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                int count = cursor.getCount();
                PlayMusic m = null;
                if (count > 0) {
                    cursor.moveToFirst();
                    m = new PlayMusic();
                    m.setMusicid(Integer.toString(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID))));
                    m.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                    m.setUri(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                    m.setDuration(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                    m.setSize(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)));
                    m.setSinger(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                    m.setAlbum(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                    m.setCreated(new Timestamp(System.currentTimeMillis()));
                }
                return m;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return null;
    }

    public PlayMusic findFavorites(String name, String singer) {
        QueryBuilder<PlayMusic> q = dao.queryBuilder();
        return q.where(q.and(Properties.Title.eq(name), Properties.Singer.eq(singer), Properties.Favorite.eq(true)))
                .unique();
    }

    @Override
    public PlayMusic find(long id) {
        return dao.load(id);
    }

    @Override
    public int getCount(int playListType) {
        switch (playListType) {
            case IBatchPlayer.PlayListType.LOCAL:
                return getLocalCount();
            case IBatchPlayer.PlayListType.FAVORITE:
                return getFavoritesCount();
        }
        return 0;
    }

    public int getLocalCount() {
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = null;
        if (cr != null) {
            try {
                cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Audio.Media._ID},
                        MediaStore.Audio.Media.DURATION + ">?",
                        new String[]{MIN_AUDIO_SIZE},
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                return cursor.getCount();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return 0;
    }

    public int getFavoritesCount() {
        return (int) dao.queryBuilder().where(Properties.Favorite.eq(true)).buildCount().count();
    }

    @Deprecated
    @Override
    public PlayList<PlayMusic> findByListType(int playListType) {
        return null;
    }

    public List<PlayMusic> getFavorites() {
        return dao.queryBuilder().where(Properties.Favorite.eq(true)).list();
    }

    /**
     * 查询系统音频数据库，请慎用，
     * 建议使用{@linkplain com.lingju.assistant.player.audio.model.AudioRepository#findByListType(int)}  findByListType(local)}查询
     *
     * @return
     */
    public List<PlayMusic> getLocal() {
        Log.i(LocalAudioDataSource.class.getName(), "getLocal>>>");
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = null;
        List<PlayMusic> list = new PlayList<PlayMusic>();
        if (cr != null) {
            try {
                cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                                MediaStore.Audio.Media._ID,
                                MediaStore.Audio.Media.TITLE,
                                MediaStore.Audio.Media.DATA,
                                MediaStore.Audio.Media.DURATION,
                                MediaStore.Audio.Media.SIZE,
                                MediaStore.Audio.Media.ALBUM,
                                MediaStore.Audio.Media.ARTIST
                        },
                        MediaStore.Audio.Media.DURATION + ">?",
                        new String[]{MIN_AUDIO_SIZE},
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                int count = cursor.getCount();
                PlayMusic m = null;
                if (count > 0) {
                    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                        m = new PlayMusic();
                        m.setMusicid(Integer.toString(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID))));
                        m.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                        m.setUri(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                        m.setDuration(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                        m.setSize(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)));
                        m.setSinger(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                        m.setAlbum(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                        m.setCreated(new Timestamp(System.currentTimeMillis()));
                        list.add(m);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return list;
    }

  /*  public Observable<List<PlayMusic>> findLocal(){
        return Observable.create(new ObservableOnSubscribe<List<PlayMusic>>() {
            @Override
            public void subscribe(ObservableEmitter<List<PlayMusic>> emitter) throws Exception {
                emitter.onNext(getLocal());
            }
        })
        .subscribeOn(Schedulers.io());
    }*/

    @Override
    public void addFavorites(PlayMusic music) {
        music.setFavorite(true);
        if (music.isCloud())
            music.setUri("");
        dao.insertOrReplace(music);
    }

    @Override
    public void removeFavorites(PlayMusic music) {
        music.setFavorite(false);
        dao.delete(music);
    }

    @Override
    public int getMaxIntField(Property property) {
        Cursor cursor = dao.getDatabase().rawQuery(
                new StringBuilder("select max(").append(property.columnName).append(") from ").append(PlayMusicDao.TABLENAME).toString(),
                new String[]{});
        if (cursor != null) {
            try {
                return cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }
        return 0;
    }

    @Override
    public String getUrl(String musicId) {
        return musicId;
    }

    @Override
    public String getLyric(String musicId) {
        return null;
    }

    @Override
    public String getLyric(String title, String singer) {
        return MusicUtils.searchLyric(title, singer);
    }

    public void resetFavoriteMusics() {
        List<PlayMusic> list = dao.queryBuilder().where(Properties.Favorite.eq(true)).list();
        for (PlayMusic music : list) {
            music.setSynchronize(false);
        }
        dao.updateInTx(list);
    }

    /**
     * 获取本地已收藏但未同步到云端的歌曲
     *
     * @return
     */
    public List<PlayMusic> getFavoriteUnSyn() {
        return dao.queryBuilder().where(Properties.Favorite.eq(true), Properties.Synchronize.eq(false)).orderDesc(Properties.FavoritedTime).list();
    }
}
