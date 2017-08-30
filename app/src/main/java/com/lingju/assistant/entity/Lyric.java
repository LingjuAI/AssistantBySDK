package com.lingju.assistant.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 * @author
 */
public class Lyric {
 
	private List<Sentence> list = new ArrayList<Sentence>();//里面装的是所有的句子
	private int offset=0;//整首歌的偏移量
	private int recent_position=0;
	
	public Lyric(){
    	init("[00:00.00]当前没有歌词");
	}

    public Lyric(String content) {
    	if(content==null||content.length()==0){
    		content="[00:00.00]当前没有歌词";
    	}
    	init(content);
    }
    
    public int size(){
    	return list.size();
    }

    /**
     * 最重要的一个方法，它根据读到的歌词内容
     * 进行初始化，比如把歌词一句一句分开并计算好时间
     * @param content 歌词内容
     */
    private void init(String content) {
        //如果歌词的内容为空,则后面就不用执行了
        //直接显示歌曲名就可以了
        if (content == null || content.trim().equals("")) {
            //list.add(new Sentence(info.getFormattedName(), Integer.MIN_VALUE, Integer.MAX_VALUE));
            return;
        }
        try {
        	StringBuffer sb=new StringBuffer();
        	int l=content.length();
        	for(int i=0;i<l;i++){
        		if(content.charAt(i)!='\r'&&content.charAt(i)!='\n'){
        			sb.append(content.charAt(i));
        		}
        		else{
        			if(sb.length()>0){
        				parseLine(sb);
            			sb.setLength(0);	
        			}
        		}
        	}
        	if(sb.length()>0){
        		parseLine(sb);
        	}
        	
            //读进来以后就排序了
            Collections.sort(list, new Comparator<Sentence>() {

                public int compare(Sentence o1, Sentence o2) {
                    return (int) (o1.getFromTime() - o2.getFromTime());
                }
            });
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
  
    private static final Pattern pattern = Pattern.compile("(?<=\\[)([0-5]{0,1}\\d)\\:([0-5]{0,1}\\d)(\\.(\\d{1,2})){0,1}(\\])");
	
    private void parseLine(StringBuffer line){
    	//System.out.println("line:"+line);
    	if(line.length()<2)return;
    	while(line.length()>0&&Character.isWhitespace(line.charAt(0))){
    		line.deleteCharAt(0);
    	}
    	if(line.charAt(0)!='['||line.length()<2)return;
    	if(!Character.isDigit(line.charAt(1))&&line.length()>9){//是否数字开头，数字开头则为时间标签，否则为标识标签
    		if(line.substring(0, 8).equalsIgnoreCase("[offset:")){
    			int e=line.indexOf("]");
    			if(e>8){
    				String n=line.substring(8, e);
    				if(n.matches("^\\d+$"))
    				offset=Integer.parseInt(n);
    			}
    			return;
    		}
    	}
    	Matcher matcher =pattern.matcher(line);
    	long t = 0;
    	int index=list.size();
    	int e=0;
    	while(matcher.find()){
    		t=0;
    		t+=getInteger(matcher.group(1))*60000L;
    		t+=getInteger(matcher.group(2))*1000L;
    		t+=matcher.group(3)!=null?getInteger(matcher.group(4))*10:0;
    		e=matcher.end(5);
    		list.add(new Sentence("",t));
    	}
    	if(list.size()>index){
    		String text=line.substring(e);
    		//if(text.trim().length()>0)
    		for(;index<list.size();index++){
    			list.get(index).setContent(text);
    			//System.out.println("s:"+list.get(index).getFromTime()+"::"+list.get(index).getContent());
    		}
    	}
    }
    
    public static int getInteger(String text){
		int l=text.length();
		if(l>0){
			int i=0;
			int r=0;
			while(--l>=0){
				r+=(text.charAt(l)-48)*Math.pow(10, i);
				i++;
			}
			return r;
		}
		return 0;
	}
  
    
    /**
     * 当前播放时间
     * @return 下标
     */
    private int getNowSentenceIndex(long t) {
    	//System.out.println("size()="+size()+",recent_position="+recent_position);
    	if(t>=(list.get(recent_position).getFromTime()+offset)){
    		while(++recent_position<size()){
    			if(t<(list.get(recent_position).getFromTime()+offset)){
    				return --recent_position;
    			}
    		}
    		return recent_position=size()-1;
    	}
    	else{
    		while(--recent_position>=0){
    			if(t>=(list.get(recent_position).getFromTime()+offset)){
    				return recent_position;
    			}
    		}
    		recent_position=0;
    	}
        return 0;
    }

    public Sentence getNowSen(long t,int offset)
    {
    	return list.get(getNowSentenceIndex(t));
    }
    
    public int getNowSenPosition(long t){
    	//System.out.println("play_time:"+t);
    	return getNowSentenceIndex(t);
    }
    
    public List<Sentence> getList() {
		return list;
	}
    
}
