package com.lingju.assistant.entity;

/**
 * Created by Ken on 2017/8/24.<br />
 */
public class KaoLaAlbum {

    private long id;
    private int countNum;
    private String img;
    private long listenNum;
    private String name;

    public int getCountNum() {
        return countNum;
    }

    public void setCountNum(int countNum) {
        this.countNum = countNum;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public long getListenNum() {
        return listenNum;
    }

    public void setListenNum(long listenNum) {
        this.listenNum = listenNum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
