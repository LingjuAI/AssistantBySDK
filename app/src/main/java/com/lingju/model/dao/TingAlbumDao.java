package com.lingju.model.dao;

import com.lingju.context.entity.NewAudioEntity;
import com.lingju.model.MetaData;
import com.lingju.model.MetaDataDao;
import com.lingju.model.TrackAlbum;
import com.lingju.model.TrackAlbumDao;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.track.Track;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ken on 2017/6/8.
 */
public class TingAlbumDao {

    private static TingAlbumDao instance;
    private TrackAlbumDao mAlbumDao;
    private MetaDataDao mMetaDataDao;

    private TingAlbumDao() {
        mAlbumDao = DaoManager.get().getDaoSession().getTrackAlbumDao();
        mMetaDataDao = DaoManager.get().getDaoSession().getMetaDataDao();
    }

    public static TingAlbumDao getInstance() {
        if (instance == null) {
            synchronized (TingAlbumDao.class) {
                if (instance == null)
                    instance = new TingAlbumDao();
            }
        }
        return instance;
    }

    public void insertMetaData(MetaData metaData) {
        mMetaDataDao.insertOrReplace(metaData);
    }

    public boolean unSaveMeta() {
        return mMetaDataDao.count() <= 0;
    }

    /**
     * 根据一级分类ID和二级分类名称查询二级分类
     **/
    public List<MetaData> findMetaDataByName(long cateId, String name) {
        String[] subCates = name.split(" ");
        List<MetaData> list = new ArrayList<>();
        //二级分类仍存在分级，需要区分查找。正常：言情    特殊：现代言情（言情-现言）
        if (subCates.length > 1) {
            MetaData metaData = mMetaDataDao.queryBuilder().where(MetaDataDao.Properties.CategoryId.eq(cateId), MetaDataDao.Properties.Name.eq(subCates[1])).limit(1).unique();
            list.add(metaData);
            MetaData metaData2 = mMetaDataDao.queryBuilder().where(MetaDataDao.Properties.CategoryId.eq(cateId)
                    , MetaDataDao.Properties.SuperId.eq(metaData.getKey())
                    , MetaDataDao.Properties.Name.eq(subCates[0])).limit(1).unique();
            if (metaData2 != null)
                list.add(metaData2);
        } else {
            MetaData metaData = mMetaDataDao.queryBuilder().where(MetaDataDao.Properties.CategoryId.eq(cateId), MetaDataDao.Properties.Name.eq(subCates[0])).limit(1).unique();
            if (metaData != null)
                list.add(metaData);
        }
        return list;
    }

    /**
     * 获取所有订阅专辑
     **/
    public List<TrackAlbum> getAllSubscribe() {
        return mAlbumDao.queryBuilder()
                .where(TrackAlbumDao.Properties.SubscribeTime.isNotNull())
                .orderDesc(TrackAlbumDao.Properties.SubscribeTime)
                .list();
    }

    /**
     * 根据指定ID判断该专辑是否订阅
     **/
    public boolean isSubscribe(long albumId) {
        return findSubscribeById(albumId) != null;
    }

    public TrackAlbum findSubscribeById(long albumId) {
        return mAlbumDao.queryBuilder()
                .where(TrackAlbumDao.Properties.SubscribeTime.isNotNull()
                        , TrackAlbumDao.Properties.Id.eq(albumId))
                .unique();
    }

    /**
     * 插入一条订阅记录
     **/
    public void insertSubscribe(Album album) {
        TrackAlbum trackAlbum = mAlbumDao.queryBuilder().where(TrackAlbumDao.Properties.Id.eq(album.getId())).unique();
        if (trackAlbum == null) {
            trackAlbum = new TrackAlbum();
            trackAlbum.setId(album.getId());
        }
        trackAlbum.setAlbumPicUrl(album.getCoverUrlMiddle());
        trackAlbum.setSubscribeTime(new Date());
        mAlbumDao.insertOrReplace(trackAlbum);
    }

    /**
     * 删除一条订阅记录
     **/
    public void delSubscribeById(long albumId) {
        TrackAlbum trackAlbum = findSubscribeById(albumId);
        trackAlbum.setSubscribeTime(null);
        mAlbumDao.update(trackAlbum);
    }

    /**
     * 获取指定ID的播放记录
     **/
    public TrackAlbum getHistoryById(long id) {
        return mAlbumDao.queryBuilder()
                .where(TrackAlbumDao.Properties.Id.eq(id), TrackAlbumDao.Properties.BreakTime.isNotNull())
                .limit(1)
                .unique();
    }

    /**
     * 保存播放记录
     **/
    public void insertHistory(PlayableModel currSound, int breakPos) {
        Track track = (Track) currSound;
        // Log.i("LingJu", "TingAlbumDao insertHistory()>>> 声音记录：" + track.getDuration() + " 记录断点：" + breakPos / 1000);
        TrackAlbum history = mAlbumDao.queryBuilder().where(TrackAlbumDao.Properties.Id.eq(track.getAlbum().getAlbumId())).limit(1).unique();
        if (history == null)
            history = new TrackAlbum();
        history.setId(track.getAlbum().getAlbumId());
        history.setAlbumPicUrl(track.getAlbum().getCoverUrlMiddle());
        history.setAlbumTitle(track.getAlbum().getAlbumTitle());
        history.setTrackId(track.getDataId());
        history.setTrackTitle(track.getTrackTitle());
        history.setTrackPicUrl(track.getCoverUrlMiddle());
        history.setTrackUrl(track.getPlayUrl24M4a());
        history.setDuration(track.getDuration());
        history.setBreakPos(breakPos / 1000);
        history.setBreakTime(new Date());
        history.setOrderNum(track.getOrderNum());
        mAlbumDao.insertOrReplace(history);
    }


    /**
     * 获取所有搜索记录(前8条)
     **/
    public List<String> getAllSearch() {
        List<TrackAlbum> albumList = mAlbumDao.queryBuilder()
                .where(TrackAlbumDao.Properties.Keyword.isNotNull())
                .orderDesc(TrackAlbumDao.Properties.SearchTime)
                .limit(10)
                .list();
        List<String> keywords = new ArrayList<>();
        for (TrackAlbum album : albumList) {
            keywords.add(album.getKeyword());
        }
        return keywords;
    }

    /**
     * 插入一条搜索记录
     **/
    public void insertSearch(String keyowrd) {
        TrackAlbum album = mAlbumDao.queryBuilder().where(TrackAlbumDao.Properties.Keyword.eq(keyowrd)).limit(1).unique();
        if (album == null) {
            album = new TrackAlbum();
            album.setId(System.currentTimeMillis());
            album.setSearchTime(new Date());
            album.setKeyword(keyowrd);
            mAlbumDao.insert(album);
        } else {
            album.setSearchTime(new Date());
            mAlbumDao.update(album);
        }

    }

    /**
     * 清空搜索记录
     **/
    public void deleteAllSearch() {
        List<TrackAlbum> albumList = mAlbumDao.queryBuilder().where(TrackAlbumDao.Properties.Keyword.isNotNull()).list();
        mAlbumDao.deleteInTx(albumList);

    }

    /**
     * 获取上一次播放的声音
     **/
    public TrackAlbum findLastTrack() {
        return mAlbumDao.queryBuilder()
                .where(TrackAlbumDao.Properties.BreakTime.isNotNull())
                .orderDesc(TrackAlbumDao.Properties.BreakTime)
                .limit(1)
                .unique();
    }

    /**
     * 获取指定名称（和集数）的历史记录
     **/
    public TrackAlbum checkHistory(NewAudioEntity audio) {
        QueryBuilder<TrackAlbum> builder = mAlbumDao.queryBuilder().where(TrackAlbumDao.Properties.TrackTitle.like(audio.getName() + "%"));
        if (audio.getEpisode() > 0)
            builder = builder.where(TrackAlbumDao.Properties.OrderNum.eq(audio.getEpisode() - 1));
        return builder.where(TrackAlbumDao.Properties.BreakTime.isNotNull())
                .limit(1).unique();
    }
}
