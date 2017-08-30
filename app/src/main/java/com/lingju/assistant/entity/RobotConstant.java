package com.lingju.assistant.entity;

import java.util.HashMap;

/**
 * Created by Ken on 2017/5/10.
 */
public class RobotConstant {
    /** 语义动作对象ID部分 **/
    /**
     * 音频
     **/
    public final static int ACTION_AUDIO = 100;
    /**
     * 视频
     **/
    public final static int ACTION_VIDEO = 200;
    /**
     * 播放器
     **/
    public final static int ACTION_PLAYER = 300;
    /**
     * 收藏夹
     **/
    public final static int ACTION_FAVOR = 305;
    /**
     * 备忘
     **/
    public final static int ACTION_MEMO = 307;
    /**
     * 提醒
     **/
    public final static int ACTION_REMIND = 308;
    /**
     * 闹钟
     **/
    public final static int ACTION_ALARM = 309;
    /**
     * 账单
     **/
    public final static int ACTION_ACCOUNTING = 314;
    /**
     * 选择对话框
     **/
    public final static int ACTION_DIALOG = 310;
    /**
     * 语音引擎
     **/
    public final static int ACTION_VOICE_ENGINE = 311;
    /**
     * 唤醒
     **/
    public final static int ACTION_AWAKEN = 313;
    /**
     * 录音
     **/
    public final static int ACTION_TAPE = 315;
    /**
     * 专辑
     **/
    public final static int ACTION_ALBUM = 316;
    /**
     * 联系人
     **/
    public final static int ACTION_CONTACT = 317;
    /**
     * 通话记录
     **/
    public final static int ACTION_CALL_LOG = 318;
    /**
     * 电话
     **/
    public final static int ACTION_CALL = 319;
    /**
     * 短信
     **/
    public final static int ACTION_SMS = 320;
    /**
     * 短信记录
     **/
    public final static int ACTION_SMS_LOG = 321;
    /**
     * 号码
     **/
    public final static int ACTION_PHONE_NUM = 322;
    /**
     * 导航引擎
     **/
    public final static int ACTION_NAVIGATION = 323;
    /**
     * 路线
     **/
    public final static int ACTION_ROUTE = 324;
    /**
     * 地址
     **/
    public final static int ACTION_ADDRESS = 325;
    /**
     * 路枢
     **/
    public final static int ACTION_ROUTENODE = 326;
    /**
     * 地图
     **/
    public final static int ACTION_PLAT = 327;

    /** 导航状态 **/
    public enum NaviStatus {
        OPEN, CLOSE, PAUSE, CONTINUE, PLAN, TIME_CONTINUE
    }

    /**
     * 语义动作值
     **/
    public final static HashMap<String, Integer> ActionMap = new HashMap<>();
    public final static int VIEW = 0;       //查看，展示（查询结果输出）
    public final static int QUERY = 1;      //查询，告诉开发者可自行查询标记 
    public final static int SEND = 2;       //发送 
    public final static int CALL = 3;       //拨打 
    public final static int ANSWER = 4;     //接听 
    public final static int HANG = 5;       //挂断 
    public final static int DOACTION = 6;   //做动作（表情展示也归类为动作）
    public final static int CREATE = 7;     //新建 
    public final static int INSERT = 8;     //插入 
    public final static int RESTART = 9;    //重启 
    public final static int CANCEL = 10;    //取消 
    public final static int RETURN = 11;    //返回 
    public final static int MODIFY = 12;    //编辑 
    public final static int DELETE = 13;    //删除 
    public final static int COPY = 14;      //复制 
    public final static int PASTE = 15;     //粘贴 
    public final static int MOVE = 16;      //移动、剪切 
    public final static int ROLL = 17;      //滚动 
    public final static int OPEN = 18;      //打开 
    public final static int CLOSE = 19;     //关闭 
    public final static int REFRESH = 20;   //刷新 
    public final static int SET = 21;       //设置 
    public final static int SHARE = 22;     //分享 
    public final static int RECOMMEND = 23; //推荐 
    public final static int DOWNLOAD = 24;  //下载 
    public final static int SELECT = 25;    //选择 
    public final static int CLEAR = 26;     //清空 
    public final static int TRANSLATE = 27; //翻译 
    public final static int SUBSCRIBE = 28; //订阅、预约 
    public final static int BOOK = 29;      //预定、订购 
    public final static int ORDER = 30;     //购买、下单 
    public final static int APPEND = 31;    //追加、附加
    public final static int READ = 31;      //朗读

    /**
     * 播放器动作
     **/
    public final static HashMap<String, Integer> PlayerMap = new HashMap<>();
    public final static int MODE_ORDER = 30001;            //顺序
    public final static int MODE_RANDOM = 30002;           //随机
    public final static int MODE_SINGLE_CYCLE = 30003;     //单曲循环
    public final static int MODE_ORDER_CYCLE = 30004;      //列表循环
    public final static int CONTROL_PLAY = 30005;          //播放
    public final static int CONTROL_PAUSE = 30006;         //暂停
    public final static int CONTROL_RESUME = 30007;        //恢复播放
    public final static int CONTROL_END = 30008;           //结束播放
    public final static int CONTROL_PRE = 30009;           //播放上一个
    public final static int CONTROL_NEXT = 30010;          //播放下一个
    public final static int CONTROL_FF = 30011;            //快进
    public final static int CONTROL_FR = 30012;             //快退
    public final static int CONTROL_REPLAY = 30013;        //重播

    /**
     * 发音人
     **/
    public final static HashMap<String, String> VoiceMap = new HashMap<>();
    public final static String VOICE_DEFAULT = "默认";
    public final static String VOICE_GRANDPA = "爷爷";
    public final static String VOICE_GRANDMA = "奶奶";
    public final static String VOICE_MAN = "中年男人";
    public final static String VOICE_WOMAN = "中年女人";
    public final static String VOICE_BOY = "男孩";
    public final static String VOICE_GIRL = "女孩";
    public final static String VOICE_RANDOM = "随机";

    static {
        ActionMap.put("VIEW", VIEW);
        ActionMap.put("QUERY", QUERY);
        ActionMap.put("SEND", SEND);
        ActionMap.put("CALL", CALL);
        ActionMap.put("ANSER", ANSWER);
        ActionMap.put("HANG", HANG);
        ActionMap.put("DOACTION", DOACTION);
        ActionMap.put("CREATE", CREATE);
        ActionMap.put("INSERT", INSERT);
        ActionMap.put("RESTART", RESTART);
        ActionMap.put("CANCEL", CANCEL);
        ActionMap.put("RETURN", RETURN);
        ActionMap.put("MODIFY", MODIFY);
        ActionMap.put("DELETE", DELETE);
        ActionMap.put("COPY", COPY);
        ActionMap.put("PASTE", PASTE);
        ActionMap.put("MOVE", MOVE);
        ActionMap.put("ROLL", ROLL);
        ActionMap.put("OPEN", OPEN);
        ActionMap.put("CLOSE", CLOSE);
        ActionMap.put("REFRESH", REFRESH);
        ActionMap.put("SET", SET);
        ActionMap.put("SHARE", SHARE);
        ActionMap.put("RECOMMEND", RECOMMEND);
        ActionMap.put("DOWNLOAD", DOWNLOAD);
        ActionMap.put("SELECT", SELECT);
        ActionMap.put("CLEAR", CLEAR);
        ActionMap.put("TRANSLATE", TRANSLATE);
        ActionMap.put("SUBSCRIBE", SUBSCRIBE);
        ActionMap.put("BOOK", BOOK);
        ActionMap.put("ORDER", ORDER);
        ActionMap.put("APPEND", APPEND);
        ActionMap.put("READ", READ);

        PlayerMap.put("ORDER", MODE_ORDER);
        PlayerMap.put("RANDOM", MODE_RANDOM);
        PlayerMap.put("SINGLE_CYCLE", MODE_SINGLE_CYCLE);
        PlayerMap.put("ORDER_CYCLE", MODE_ORDER_CYCLE);
        PlayerMap.put("PLAY", CONTROL_PLAY);
        PlayerMap.put("PAUSE", CONTROL_PAUSE);
        PlayerMap.put("RESUME", CONTROL_RESUME);
        PlayerMap.put("END", CONTROL_END);
        PlayerMap.put("PRE", CONTROL_PRE);
        PlayerMap.put("NEXT", CONTROL_NEXT);
        PlayerMap.put("FF", CONTROL_FF);
        PlayerMap.put("FR", CONTROL_FR);
        PlayerMap.put("REPLAY", CONTROL_REPLAY);

        VoiceMap.put("DEFAULT", VOICE_DEFAULT);
        VoiceMap.put("GRANDPA", VOICE_GRANDPA);
        VoiceMap.put("GRANDMA", VOICE_GRANDMA);
        VoiceMap.put("MAN", VOICE_MAN);
        VoiceMap.put("WOMAN", VOICE_WOMAN);
        VoiceMap.put("BOY", VOICE_BOY);
        VoiceMap.put("GIRL", VOICE_GIRL);
        VoiceMap.put("RANDOM", VOICE_RANDOM);
    }
}
