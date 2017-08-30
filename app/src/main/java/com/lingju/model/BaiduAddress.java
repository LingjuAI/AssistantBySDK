package com.lingju.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.lingju.context.entity.Address;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2015/7/14.
 */
@Entity(nameInDb = "t_address")
public class BaiduAddress implements Parcelable {
    @Id(autoincrement = true)
    private Long id;
    private String address;//varchar(1000) not null default '',
    private String city;//varchar(50) not null default '',
    private int hasCaterDetails;//int  not null default 0,
    private int isPano;//int not null default 0,
    private double latitude;//REAL default null,
    private double longitude;//REAL default null,
    private String name;//varchar(500) not null default '',
    private String searchKeyWord;
    private String phoneNum;//varchar(20) default null,
    private String postCode;//varchar(10) default null,
    private int type;//int not null default 0,
    private String uid;//varchar(500) default null,
    private String remark;//varchar(500) default null,
    private int disable = 0;              //int not null default 0,
    private Date favoritedTime;//DATETIME default null,
    private Date created = new Date(System.currentTimeMillis());//Date not null

    //以下与服务器同步标记
    private String sid;        //服务器记录id,该记录的唯一标示
    private long timestamp;    //时间戳，记录的时效性标记
    private int recyle;     //0：有效， 1：回收
    private boolean synced = true;  //false表示未同步，待更新

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getHasCaterDetails() {
        return hasCaterDetails;
    }

    public void setHasCaterDetails(int hasCaterDetails) {
        this.hasCaterDetails = hasCaterDetails;
    }

    public String getSearchKeyWord() {
        return searchKeyWord;
    }

    public void setSearchKeyWord(String searchKeyWord) {
        this.searchKeyWord = searchKeyWord;
    }

    public int getIsPano() {
        return isPano;
    }

    public void setIsPano(int isPano) {
        this.isPano = isPano;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getDisable() {
        return disable;
    }

    public void setDisable(int disable) {
        this.disable = disable;
    }

    public Date getFavoritedTime() {
        return favoritedTime;
    }

    public void setFavoritedTime(Date favoritedTime) {
        this.favoritedTime = favoritedTime;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }


    public void print() {
        System.out.println("[name=" + this.name + ",latitude=" + this.latitude + ",longitude=" + longitude +
                ",address=" + address + ",city=" + city + ",created=" + created + ",disable=" + disable +
                ",favoriteTime=" + favoritedTime + ",hasCaterDetails=" + hasCaterDetails +
                ",isPano=" + isPano + ",searchKeyWord=" + searchKeyWord + ",phoneNum=" + phoneNum +
                ",postCode=" + postCode + ",type=" + type + ",uid=" + uid + ",remark=" + remark + ",id=" + id + "]");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public BaiduAddress() {
    }

    public static List<BaiduAddress> createFromAddress(List<Address> passPoints) {
        List<BaiduAddress> list = new ArrayList<>();
        if (passPoints != null) {
            for (Address address : passPoints) {
                list.add(createFromAddress(address, true));
            }
        }
        return list;
    }

    public static BaiduAddress createFromAddress(Address address, boolean isCGJ02) {
        BaiduAddress ba = new BaiduAddress();
        ba.setName(address.getName());
        ba.setAddress(address.getDetailedaddress());
        ba.setUid(address.getUid());
        ba.setPhoneNum(address.getTelephone());
        ba.setRemark(address.getAlias());
        ba.setLatitude(address.getLatitude());
        ba.setLongitude(address.getLongitude());
        ba.setCity(ba.getCity());
        ba.setSid(address.getSid());
        ba.setSynced(true);
        ba.setTimestamp(address.getTimestamp());
        if (isCGJ02)
            ba.setCGJ02();
        return ba;
    }

    /**
     * 将百度坐标转换成国测局坐标
     **/
    public void setCGJ02() {
        BDLocation bl = new BDLocation();
        bl.setLatitude(this.latitude);
        bl.setLongitude(this.longitude);
        bl = LocationClient.getBDLocationInCoorType(bl, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
        this.latitude = bl.getLatitude();
        this.longitude = bl.getLongitude();
    }

    public void setBD09LL() {
        BDLocation bl = new BDLocation();
        bl.setLatitude(this.latitude);
        bl.setLongitude(this.longitude);
        bl = LocationClient.getBDLocationInCoorType(bl, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
        this.latitude = bl.getLatitude();
        this.longitude = bl.getLongitude();
    }

    public void reset(BaiduAddress address) {
        this.hasCaterDetails = address.hasCaterDetails;
        this.favoritedTime = address.favoritedTime;
        this.disable = address.disable;
        this.address = address.address;
        this.city = address.city;
        this.created = address.created;
        this.isPano = address.isPano;
        this.latitude = address.latitude;
        this.longitude = address.longitude;
        this.name = address.name;
        this.phoneNum = address.phoneNum;
        this.postCode = address.postCode;
        this.remark = address.remark;
        this.searchKeyWord = address.searchKeyWord;
        this.type = address.type;
        this.uid = address.uid;
        this.id = address.id;
    }

    public BaiduAddress(Parcel s) {
        this.id = s.readLong();
        this.address = s.readString();
        this.city = s.readString();
        this.created = new Date(s.readLong());
        this.disable = s.readInt();
        long t = s.readLong();
        this.favoritedTime = t == 0 ? null : new Date(t);
        this.hasCaterDetails = s.readInt();
        this.isPano = s.readInt();
        this.latitude = s.readDouble();
        this.longitude = s.readDouble();
        this.name = s.readString();
        this.searchKeyWord = s.readString();
        this.phoneNum = s.readString();
        this.postCode = s.readString();
        this.type = s.readInt();
        this.uid = s.readString();
        this.remark = s.readString();

        this.sid = s.readString();
        this.timestamp = s.readLong();
        this.recyle = s.readInt();
        this.synced = s.readInt() == 1;
    }

    @Generated(hash = 542674326)
    public BaiduAddress(Long id, String address, String city, int hasCaterDetails, int isPano, double latitude, double longitude,
                        String name, String searchKeyWord, String phoneNum, String postCode, int type, String uid, String remark, int disable,
                        Date favoritedTime, Date created, String sid, long timestamp, int recyle, boolean synced) {
        this.id = id;
        this.address = address;
        this.city = city;
        this.hasCaterDetails = hasCaterDetails;
        this.isPano = isPano;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.searchKeyWord = searchKeyWord;
        this.phoneNum = phoneNum;
        this.postCode = postCode;
        this.type = type;
        this.uid = uid;
        this.remark = remark;
        this.disable = disable;
        this.favoritedTime = favoritedTime;
        this.created = created;
        this.sid = sid;
        this.timestamp = timestamp;
        this.recyle = recyle;
        this.synced = synced;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id == null ? 0L : this.id);
        dest.writeString(this.address);
        dest.writeString(this.city);
        dest.writeLong(this.created.getTime());
        dest.writeInt(this.disable);
        dest.writeLong(this.favoritedTime == null ? 0L : this.favoritedTime.getTime());
        dest.writeInt(this.hasCaterDetails);
        dest.writeInt(this.isPano);
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
        dest.writeString(this.name);
        dest.writeString(this.searchKeyWord);
        dest.writeString(this.phoneNum);
        dest.writeString(this.postCode);
        dest.writeInt(this.type);
        dest.writeString(this.uid);
        dest.writeString(this.remark);

        dest.writeString(this.sid);
        dest.writeLong(this.timestamp);
        dest.writeInt(this.recyle);
        dest.writeInt(this.synced ? 1 : 0);
    }

    public Long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSid() {
        return this.sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getRecyle() {
        return this.recyle;
    }

    public void setRecyle(int recyle) {
        this.recyle = recyle;
    }

    public boolean getSynced() {
        return this.synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public static final Creator<BaiduAddress> CREATOR = new Creator<BaiduAddress>() {
        public BaiduAddress createFromParcel(Parcel in) {
            return new BaiduAddress(in);
        }

        public BaiduAddress[] newArray(int size) {
            return new BaiduAddress[size];
        }
    };


    public void setUpdate(BaiduAddress ba) {
        this.id = ba.getId();
        this.searchKeyWord = ba.getSearchKeyWord();
        this.sid = ba.getSid();
        this.timestamp = ba.getTimestamp();
        this.recyle = ba.getRecyle();
    }
}
