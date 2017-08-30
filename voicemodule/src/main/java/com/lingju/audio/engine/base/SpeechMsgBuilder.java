package com.lingju.audio.engine.base;

import org.json.JSONArray;

/**
 * Created by Administrator on 2016/11/3.
 */
public class SpeechMsgBuilder {

    private long id = 0;
    private String text;
    boolean invalid;
    int errorCount;
    JSONArray sections;
    /**
     * 合成优先级，
     * 取值：{@linkplain SpeechMsg#PRIORITY_ABOVE_RECOGNIZE PRIORITY_ABOVE_RECOGNIZE} 默认值;
     * {@linkplain SpeechMsg#PRIORITY_BELOW_RECOGNIZE PRIORITY_BELOW_RECOGNIZE}
     */
    int priority = 1;
    /**
     * 被打断后可恢复次数
     */
    int retryTimes;
    /**
     * 合成文本来源
     */
    int origin;
    /**
     * 上下文模式：当合成朗读结束时作出的语音控制
     * 取值：{@linkplain SpeechMsg#CONTEXT_AUTO CONTEXT_AUTO},默认值
     * {@linkplain SpeechMsg#CONTEXT_KEEP_RECOGNIZE CONTEXT_KEEP_RECOGNIZE}；
     * {@linkplain SpeechMsg#CONTEXT_KEEP_AWAKEN CONTEXT_KEEP_AWAKEN}
     */
    int contextMode;
    /**
     * 是否强制使用离线引擎
     */
    boolean forceLocalEngine;

    public static SpeechMsgBuilder create(String text) {
        return new SpeechMsgBuilder(text);
    }

    public SpeechMsgBuilder(String text) {
        this.id = System.currentTimeMillis();
        this.text = text;
    }

    public SpeechMsgBuilder setSections(JSONArray sections) {
        this.sections = sections;
        return this;
    }

    public SpeechMsgBuilder setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public SpeechMsgBuilder setOrigin(int origin) {
        this.origin = origin;
        return this;
    }

    public SpeechMsgBuilder setContextMode(int contextMode) {
        this.contextMode = contextMode;
        return this;
    }

    public SpeechMsgBuilder setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
        return this;
    }

    public SpeechMsgBuilder setForceLocalEngine(boolean forceLocalEngine) {
        this.forceLocalEngine = forceLocalEngine;
        return this;
    }

    public SpeechMsgBuilder setText(String text) {
        this.text = text;
        return this;
    }

    public String getText() {
        return text;
    }

    public SpeechMsg build() {
        return new DefaultSpeechMsg();
    }


    class DefaultSpeechMsg implements SpeechMsg {
        private State state = State.Idle;
        private long pauseTime;

        DefaultSpeechMsg() {

        }

        public SpeechMsg clone() {
            DefaultSpeechMsg r = new DefaultSpeechMsg();
            r.state = this.state;
            r.pauseTime = this.pauseTime;
            return r;
        }

        @Override
        public SpeechMsgBuilder getBuilder() {
            return SpeechMsgBuilder.this;
        }

        @Override
        public long id() {
            return id;
        }

        @Override
        public State state() {
            return state;
        }

        @Override
        public SpeechMsg setState(State state) {
            this.state = state;
            switch (state) {
                case OnPaused:
                    pauseTime = System.currentTimeMillis();
                    break;
                case OnResume:
                    pauseTime = 0;
                    break;
            }
            return this;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public int origin() {
            return origin;
        }

        @Override
        public String text() {
            return text;
        }

        @Override
        public int errorCount() {
            return errorCount;
        }

        @Override
        public int contextMode() {
            return contextMode;
        }

        @Override
        public JSONArray sections() {
            return sections;
        }

        @Override
        public int retryTimes() {
            return retryTimes;
        }

        @Override
        public boolean forceLocalEngine() {
            return forceLocalEngine;
        }

        @Override
        public boolean invalid() {
            return invalid;
        }

        @Override
        public void setInvalid(boolean invalid) {
            SpeechMsgBuilder.this.invalid = invalid;
        }

        @Override
        public SpeechMsg increaseError() {
            SpeechMsgBuilder.this.errorCount++;
            return this;
        }

        @Override
        public SpeechMsg decreaseRetryTimes() {
            SpeechMsgBuilder.this.retryTimes--;
            return this;
        }

        @Override
        public int pausedTime() {
            return (int) (pauseTime == 0 ? pauseTime : System.currentTimeMillis() - pauseTime);
        }
    }

}
