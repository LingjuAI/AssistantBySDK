package com.lingju.assistant.entity;

import java.util.List;

/**
 * Created by Ken on 2017/8/24.<br />
 */
public class KaoLaAudioList {
    private int count;      //声音总集数
    private int currentPage;    //当前页码
    private int sumPage;        //总页数
    private List<KaoLaAudio> dataList;  //当前页码的声音集合

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public List<KaoLaAudio> getDataList() {
        return dataList;
    }

    public void setDataList(List<KaoLaAudio> dataList) {
        this.dataList = dataList;
    }

    public int getSumPage() {
        return sumPage;
    }

    public void setSumPage(int sumPage) {
        this.sumPage = sumPage;
    }

    public class KaoLaAudio {
        private String aacPlayUrl;      //播放url
        private long albumId;
        private String albumName;
        private String albumPic;
        private long audioId;
        private String audioName;
        private String audioPic;
        private int duration;
        private int orderNum;
        private long updateTime;

        public String getAlbumPic() {
            return albumPic;
        }

        public void setAlbumPic(String albumPic) {
            this.albumPic = albumPic;
        }

        public String getAacPlayUrl() {
            return aacPlayUrl;
        }

        public void setAacPlayUrl(String aacPlayUrl) {
            this.aacPlayUrl = aacPlayUrl;
        }

        public long getAlbumId() {
            return albumId;
        }

        public void setAlbumId(long albumId) {
            this.albumId = albumId;
        }

        public String getAlbumName() {
            return albumName;
        }

        public void setAlbumName(String albumName) {
            this.albumName = albumName;
        }

        public long getAudioId() {
            return audioId;
        }

        public void setAudioId(long audioId) {
            this.audioId = audioId;
        }

        public String getAudioName() {
            return audioName;
        }

        public void setAudioName(String audioName) {
            this.audioName = audioName;
        }

        public String getAudioPic() {
            return audioPic;
        }

        public void setAudioPic(String audioPic) {
            this.audioPic = audioPic;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public int getOrderNum() {
            return orderNum;
        }

        public void setOrderNum(int orderNum) {
            this.orderNum = orderNum;
        }

        public long getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }
    }
}
