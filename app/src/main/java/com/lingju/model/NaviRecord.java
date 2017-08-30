package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.util.Date;

/**
 * Created by Dyy on 2017/3/1.
 */
@Entity(nameInDb = "t_record")
public class NaviRecord {
    @Id(autoincrement = true)
    private Long id;
    private double startLatitude;
    private double startLongitude;
    private String startName;
    private double endLatitude;
    private double endLongitude;
    private String endName;
    private Date created = new Date(System.currentTimeMillis());//Date not null
    @Generated(hash = 2053991145)
    public NaviRecord(Long id, double startLatitude, double startLongitude,
            String startName, double endLatitude, double endLongitude,
            String endName, Date created) {
        this.id = id;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.startName = startName;
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
        this.endName = endName;
        this.created = created;
    }
    @Generated(hash = 2139648801)
    public NaviRecord() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public double getStartLatitude() {
        return this.startLatitude;
    }
    public void setStartLatitude(double startLatitude) {
        this.startLatitude = startLatitude;
    }
    public double getStartLongitude() {
        return this.startLongitude;
    }
    public void setStartLongitude(double startLongitude) {
        this.startLongitude = startLongitude;
    }
    public String getStartName() {
        return this.startName;
    }
    public void setStartName(String startName) {
        this.startName = startName;
    }
    public double getEndLatitude() {
        return this.endLatitude;
    }
    public void setEndLatitude(double endLatitude) {
        this.endLatitude = endLatitude;
    }
    public double getEndLongitude() {
        return this.endLongitude;
    }
    public void setEndLongitude(double endLongitude) {
        this.endLongitude = endLongitude;
    }
    public String getEndName() {
        return this.endName;
    }
    public void setEndName(String endName) {
        this.endName = endName;
    }
    public Date getCreated() {
        return this.created;
    }
    public void setCreated(Date created) {
        this.created = created;
    }
}
