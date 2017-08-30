package com.lingju.model;


import com.lingju.context.entity.Contacts;

import java.util.ArrayList;
import java.util.List;


public class Contact extends Contacts {
	
    private List<RawContact> datas= new ArrayList<>();
    private String formatedName;
    
    
    public void setFormatedName(String formatedName) {
		this.formatedName = formatedName;
	}
    
    public String getFormatedName() {
		return formatedName;
	}
    
    public void setDatas(List<RawContact> datas) {
		this.datas = datas;
	}
    
    public List<RawContact> getDatas() {
		return datas;
	}

/*	private StringBuffer listToString(List<String> list){
		StringBuffer sb=new StringBuffer("]");
		int l=list.size();
		while(--l>0){
			sb.insert(0,list.get(l));
			sb.insert(0, ",");
		}
		if(l==0)
			sb.insert(0, list.get(0));
		sb.insert(0, "[");
		return sb;
	}*/
}
