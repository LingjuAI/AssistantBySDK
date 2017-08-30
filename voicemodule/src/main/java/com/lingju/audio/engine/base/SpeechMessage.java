package com.lingju.audio.engine.base;

import org.json.JSONObject;

/**
 * Created by Administrator on 2016/11/1.
 */
public class SpeechMessage {

    /**
     * 如果当前正在识别，打断之
     */
    public final static int PRIORITY_ABOVE_RECOGNIZE=1;
    /**
     * 如果当前正在识别，丢弃本次合成
     */
    public final static int PRIORITY_BELOW_RECOGNIZE=0;

    public final static int ORIGIN_FINAL=-1;
    public final static int ORIGIN_DEFAULT=0;
    public final static int ORIGIN_COMMON=1;
    public final static int ORIGIN_NAVI=2;



    private long id=0;
    String text;
    CallBack callBack;
    boolean invalid;
    int errorCount;
    JSONObject sections;
    /**
     * 合成优先级，
     * 取值：{@linkplain SpeechMessage#PRIORITY_ABOVE_RECOGNIZE PRIORITY_ABOVE_RECOGNIZE} 默认值;
     *      {@linkplain SpeechMessage#PRIORITY_BELOW_RECOGNIZE PRIORITY_BELOW_RECOGNIZE}
     */
    int priority=1;
    /**
     * 被打断后可恢复次数
     */
    int repeatCntInterrupted;
    /**
     * 合成文本来源
     */
    int origin;
    /**
     * 是否强制使用离线引擎
     */
    boolean forceLocalEngine;


    public SpeechMessage(String text){
        this(text,1,0);
    }
    public SpeechMessage(String text,int priority){
        this(text,priority,0);
    }
    public SpeechMessage(String text,int priority,int repeatCntInterrupted){
        this.text=text;
        this.priority=priority;
        this.repeatCntInterrupted=repeatCntInterrupted;
        this.id=System.currentTimeMillis();
    }

    public SpeechMessage(String text,int priority,int repeatCntInterrupted,CallBack callBack){
        this(text,priority,repeatCntInterrupted);
        this.callBack=callBack;
    }

    public SpeechMessage(String text,CallBack callBack){
        this.text=text;
        this.callBack=callBack;
        this.id=System.currentTimeMillis();
    }

    public SpeechMessage setOrigin(int origin){
        this.origin=origin;
        return this;
    }

    public SpeechMessage setForceLocalEngine(boolean forceLocalEngine) {
        this.forceLocalEngine = forceLocalEngine;
        return this;
    }

    public SpeechMessage setSections(JSONObject sections) {
        this.sections = sections;
        return this;
    }

    public JSONObject getSections() {
        return sections;
    }

    public String getText() {
        return text.replaceAll("\\<br\\/{0,1}\\>", "。");
    }
    public void setText(String text) {
        this.text = text;
    }
    public CallBack getCallBack() {
        return callBack;
    }
    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }
    public long getId() {
        return id;
    }

    public int getAndAddErrorCount() {
        return errorCount++;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public int getRepeatCntInterrupted() {
        return repeatCntInterrupted;
    }

    public void setRepeatCntInterrupted(int repeatCntInterrupted) {
        this.repeatCntInterrupted = repeatCntInterrupted;
    }

    public void decreaseRepeatCntInterrupted(){
        this.repeatCntInterrupted--;
    }

		/*public void setId(long id) {
			this.id = id;
		}*/

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public int getOrigin() {
        return origin;
    }

    public static abstract class CallBack{
        /**
         * 合成回调完成后，是否终止后续流程{@linkplain SynthesizerBase#continueWakeupMode() continueWakeupMode} ，true=是，不再继续调用，默认=true
         */
        private boolean end=true;
        /**
         * 合成被打断时，是否舍弃本回调方法{@linkplain CallBack#onComplete() onComplete} ,true=是，默认=true
         */
        private boolean endWhenInterrupted=true;

        public CallBack(){
        }
        public CallBack(boolean end){
            this.end=end;
        }
        public CallBack(boolean end,boolean endWhenInterrupted){
            this.end=end;
            this.endWhenInterrupted=endWhenInterrupted;
        }
        public boolean isEnd() {
            return end;
        }

        public boolean isEndWhenInterrupted() {
            return endWhenInterrupted;
        }

        public void setEndWhenInterrupted(boolean endWhenInterrupted) {
            this.endWhenInterrupted = endWhenInterrupted;
        }

        public abstract void onComplete();
    }

}
