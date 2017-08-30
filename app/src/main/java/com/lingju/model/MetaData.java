package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by Ken on 2017/6/20.
 */
@Entity
public class MetaData {
    @Id(autoincrement = true)
    private Long id;
    private String key;
    private String value;
    private String name;
    private long categoryId;
    private String superId;

    @Generated(hash = 971768360)
    public MetaData(Long id, String key, String value, String name, long categoryId,
            String superId) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.name = name;
        this.categoryId = categoryId;
        this.superId = superId;
    }
    @Generated(hash = 92376568)
    public MetaData() {
    }
   
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getKey() {
        return this.key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getValue() {
        return this.value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public long getCategoryId() {
        return this.categoryId;
    }
    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public String toString() {
        return "MetaData{" +
                "categoryId=" + categoryId +
                ", id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
    public String getSuperId() {
        return this.superId;
    }
    public void setSuperId(String superId) {
        this.superId = superId;
    }
}

