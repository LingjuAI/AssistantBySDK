package com.lingju.assistant.activity.index.presenter;

import android.content.Intent;

import com.lingju.assistant.activity.TingSearchActivity;
import com.lingju.assistant.activity.index.ITingSearch;
import com.lingju.model.dao.TingAlbumDao;
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.LastUpTrack;
import com.ximalaya.ting.android.opensdk.model.album.SearchAlbumList;
import com.ximalaya.ting.android.opensdk.model.word.HotWord;
import com.ximalaya.ting.android.opensdk.model.word.HotWordList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ken on 2017/6/27.
 */
public class TingSearchPresenter implements ITingSearch.IPresenter {

    private ITingSearch.ITingSearchView mTingSearchView;
    private TingAlbumDao mAlbumDao;


    public TingSearchPresenter(ITingSearch.ITingSearchView searchView) {
        this.mTingSearchView = searchView;
        mAlbumDao = TingAlbumDao.getInstance();
    }

    @Override
    public void loadAllDataFromChat(Intent intent) {
        if (intent != null) {
            ArrayList<Album> datas = intent.getParcelableArrayListExtra(TingSearchActivity.ALBUM_LIST);
            if (datas != null) {
                for (Album album : datas) {
                    LastUpTrack lastUptrack = album.getLastUptrack();
                    lastUptrack.setCreatedAt(lastUptrack.getTrackId());
                }
                mTingSearchView.switchSearchListBox(datas);
            }
        }
    }

    @Override
    public void loadHistory() {
        List<String> searchTags = mAlbumDao.getAllSearch();
        mTingSearchView.showSearchTagBox(ITingSearch.HISTORY_TYPE, searchTags);
    }

    @Override
    public void loadHotTag(final boolean isNext) {
        Map<String, String> params = new HashMap<>();
        params.put(DTransferConstants.TOP, "20");
        final List<String> tags = new ArrayList<>();
        CommonRequest.getHotWords(params, new IDataCallBack<HotWordList>() {
            @Override
            public void onSuccess(HotWordList hotWordList) {
                List<HotWord> hotWords = hotWordList.getHotWordList();
                if (hotWords != null) {
                    for (int i = 0; i < hotWords.size(); i++) {
                        if (isNext) {       //显示后10条
                            if (i < 10)
                                continue;
                        } else {            //显示前十条
                            if (i == 10)
                                break;
                        }
                        tags.add(hotWords.get(i).getSearchword());
                    }
                    mTingSearchView.showSearchTagBox(ITingSearch.HOT_TYPE, tags);
                } else {
                    mTingSearchView.showSearchTagBox(ITingSearch.HOT_TYPE, tags);
                }
            }

            @Override
            public void onError(int i, String s) {
                mTingSearchView.showSearchTagBox(ITingSearch.HOT_TYPE, tags);
            }
        });
    }

    @Override
    public void saveHistory(String keyword) {
        mAlbumDao.insertSearch(keyword);
    }

    @Override
    public void clearHistory() {
        mAlbumDao.deleteAllSearch();
        loadHistory();
    }

    @Override
    public void goSearch(final String keyword, int pageNum) {
        if (pageNum == 1)
            mTingSearchView.switchProgressBar(true);
        Map<String, String> params = new HashMap<>();
        params.put(DTransferConstants.SEARCH_KEY, keyword);
        params.put(DTransferConstants.CALC_DIMENSION, "3");     //2-最新，3-最多播放，4-最相关（默认）
        params.put(DTransferConstants.PAGE, String.valueOf(pageNum));
        CommonRequest.getSearchedAlbums(params, new IDataCallBack<SearchAlbumList>() {
            @Override
            public void onSuccess(SearchAlbumList searchAlbumList) {
                List<Album> albums = searchAlbumList.getAlbums();
                if (albums != null) {
                    saveHistory(keyword);
                    mTingSearchView.switchSearchListBox(albums);
                } else {
                    mTingSearchView.switchSearchListBox(null);
                }
            }

            @Override
            public void onError(int i, String s) {
                mTingSearchView.switchSearchListBox(null);
            }
        });
    }
}
