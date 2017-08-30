package com.lingju.assistant.entity;

public class Sentence {
	    private long fromTime;//这句的起始时间,时间是以毫秒为单位
	    private String content;//这一句的内容
	    public Sentence(String content, long fromTime) {
	        this.content = content;
	        this.fromTime = fromTime;
	    }

	    public Sentence(String content) {
	        this(content, 0);
	    }

	    public long getFromTime() {
	        return fromTime;
	    }

	    public void setFromTime(long fromTime) {
	        this.fromTime = fromTime;
	    }
	    
	    public void setContent(String content) {
			this.content = content;
		}

	    /**
	     * 得到这一句的内容
	     * @return 内容
	     */
	    public String getContent() {
	        return content;
	    }
}
