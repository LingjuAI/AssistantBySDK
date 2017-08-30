package com.lingju.model;

import com.lingju.context.entity.BillEntity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.util.Calendar;
import java.util.Date;

@Entity(nameInDb = "t_accounting")
public class Accounting {
    @Id(autoincrement = true)
    private Long id;
    private int atype;      //收入-->1  支出-->0
    private double amount;        //金额
    private String etype;        //项目
    private String memo;        //备注
    private Date created;  //创建时间
    private Date rdate;        //账单时间
    private Date modified;    //修改时间
    private int month;    //账单月份

    //以下与服务器同步标记
    private String sid;        //服务器记录id,该记录的唯一标示
    private long timestamp;    //时间戳，记录的时效性标记
    private int recyle;     //0：有效， 1：回收
    private boolean synced = true;  //false表示未同步，待更新

    @Generated(hash = 2109571958)
    public Accounting(Long id, int atype, double amount, String etype, String memo,
                      Date created, Date rdate, Date modified, int month, String sid,
                      long timestamp, int recyle, boolean synced) {
        this.id = id;
        this.atype = atype;
        this.amount = amount;
        this.etype = etype;
        this.memo = memo;
        this.created = created;
        this.rdate = rdate;
        this.modified = modified;
        this.month = month;
        this.sid = sid;
        this.timestamp = timestamp;
        this.recyle = recyle;
        this.synced = synced;
    }

    @Generated(hash = 1099190509)
    public Accounting() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getAtype() {
        return this.atype;
    }

    public void setAtype(int atype) {
        this.atype = atype;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getEtype() {
        return this.etype;
    }

    public void setEtype(String etype) {
        this.etype = etype;
    }

    public String getMemo() {
        return this.memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRdate() {
        return this.rdate;
    }

    public void setRdate(Date rdate) {
        this.rdate = rdate;
    }

    public Date getModified() {
        return this.modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public int getMonth() {
        return this.month;
    }

    public void setMonth(int month) {
        this.month = month;
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

    public void fromBill(BillEntity bill) {
        this.atype = bill.getPay();
        this.amount = bill.getMoney();
        this.etype = (bill.getType() == null ? "" : bill.getType()) + (bill.getItem() == null ? "" : "，" + bill.getItem());
        this.memo = bill.getMemo();
        this.created = bill.getCreated();
        this.modified = bill.getModified();
        if (bill.getPayTime() > 0) {
            this.rdate = new Date(bill.getPayTime());
            Calendar cl = Calendar.getInstance();
            cl.setTime(rdate);
            this.month = cl.get(Calendar.MONTH) + 1;
        }
        this.sid = bill.getSid();
        this.timestamp = bill.getTimestamp();
        this.recyle = bill.getRecyle();
    }

    public BillEntity toBill() {
        BillEntity bill = new BillEntity();
        bill.setPay(this.atype);
        bill.setMoney(this.getAmount());
        String[] types = this.etype.split("，");
        bill.setType(types[0]);
        if (types.length > 1)
            bill.setItem(types[1]);
        bill.setCreated(this.created);
        bill.setModified(this.modified);
        bill.setPayTime(this.rdate.getTime());
        bill.setMemo(this.memo);
        bill.setLid(this.id.intValue());
        bill.setRecyle(this.recyle);
        bill.setSid(this.sid);
        bill.setTimestamp(this.timestamp);
        bill.setSynced(this.synced);
        return bill;
    }
}
