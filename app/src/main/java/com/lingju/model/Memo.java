package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import java.util.Date;
import org.greenrobot.greendao.annotation.Generated;


@Entity(nameInDb="t_memo")
public class Memo {
	@Id(autoincrement = true)
	private Long id;
	private String content		;		
	private Date created       ;
	private Date modified;
	//以下是服务器同步标记
	private String sid;        //服务器记录id,该记录的唯一标示
	private long timestamp;    //时间戳，记录的时效性标记
	private int recyle;     //0：有效， 1：回收
	private boolean synced = true;  //false表示未同步，待更新
	public Memo() {
	}
	public Memo(String content) {
		this.content=content;
		this.created=new Date(System.currentTimeMillis());
	}
	@Generated(hash = 409704887)
	public Memo(Long id, String content, Date created, Date modified, String sid,
									long timestamp, int recyle, boolean synced) {
					this.id = id;
					this.content = content;
					this.created = created;
					this.modified = modified;
					this.sid = sid;
					this.timestamp = timestamp;
					this.recyle = recyle;
					this.synced = synced;
	}
	public Long getId() {
					return this.id;
	}
	public void setId(Long id) {
					this.id = id;
	}
	public String getContent() {
					return this.content;
	}
	public void setContent(String content) {
					this.content = content;
	}
	public Date getCreated() {
					return this.created;
	}
	public void setCreated(Date created) {
					this.created = created;
	}
	public Date getModified() {
					return this.modified;
	}
	public void setModified(Date modified) {
					this.modified = modified;
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

}
