package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity(nameInDb="t_zipcode")
public class Zipcode{
	@Id(autoincrement = true)
	private Long id;
	private String city;
	private String code;
	public Zipcode() {
	}
	@Generated(hash = 944261775)
	public Zipcode(Long id, String city, String code) {
					this.id = id;
					this.city = city;
					this.code = code;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}

	public Long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	public void setId(Long id) {
					this.id = id;
	}
}
