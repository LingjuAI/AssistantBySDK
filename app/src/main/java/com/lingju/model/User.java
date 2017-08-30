package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.util.Date;

@Entity(nameInDb = "t_user")
public class User {
	@Id
	private int id;
	private String   name="";//                 varchar(20) not null default '' ,--commant '用户名',
	private String   userid;//               varchar(20) not null default '' ,--commant '帐户ID,英文且唯一性',
	private String   email="";//               varchar(40) not null default '' ,--comment '电子信箱',
	private String   sex="n"        ;//          varchar(2) not null default 'n' ,--commant '性别,m:男，f：女，n：未知',
	private String   password=""   ;//          varchar(32) not null default '' ,--commant '密码',
	private String   provinces=""    ;//        varchar(20) default '' ,--commant '居住省区',
	private String   city=""           ;//      varchar(100) default '' ,--commant '居住县市',
	private Date created       ;//       Date not null


	@Generated(hash = 1896361073)
	public User(int id, String name, String userid, String email, String sex, String password, String provinces,
									String city, Date created) {
					this.id = id;
					this.name = name;
					this.userid = userid;
					this.email = email;
					this.sex = sex;
					this.password = password;
					this.provinces = provinces;
					this.city = city;
					this.created = created;
	}
	@Generated(hash = 586692638)
	public User() {
	}
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getSex() {
		return sex;
	}
	public void setSex(String gender) {
		this.sex = gender;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getProvinces() {
		return provinces;
	}
	public void setProvinces(String provinces) {
		this.provinces = provinces;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}



}
