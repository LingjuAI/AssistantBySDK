package com.lingju.assistant.entity;

/**
 * Created by Ken on 2017/8/11.<br />
 */
public class SmsBean {

    private String name;        //联系人名字
    private String company;     //联系人公司
    private String job;         //联系人职务
    private String code;        //联系人号码 
    private String content;     //内容

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
