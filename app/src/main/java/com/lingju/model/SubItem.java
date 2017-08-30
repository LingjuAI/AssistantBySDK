package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity(nameInDb="t_subitem")
public class SubItem{
	@Id(autoincrement = true)
	private Long id;
	private long itemid;
	private String name;
	public SubItem() {
	}

	public SubItem(Item item, String name) {
		super();
		this.itemid = item.getId();
		this.name = name;
	}

	@Generated(hash = 215584569)
	public SubItem(Long id, long itemid, String name) {
					this.id = id;
					this.itemid = itemid;
					this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getItemid() {
		return itemid;
	}

	public void setItemid(long itemid) {
		this.itemid = itemid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
