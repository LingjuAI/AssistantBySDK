package com.lingju.assistant.activity.index.model;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.MainActivity;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.IntroduceShowEvent;
import com.lingju.assistant.service.AssistantService;
import com.lingju.model.temp.speech.SpeechMsg;
import com.lingju.common.log.Log;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Dyy on 2017/2/15.
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private Context mContext;
    private LayoutInflater inflater;

    public ExpandableListAdapter(Context context) {
        this.mContext = context;
        inflater = LayoutInflater.from(context);
    }

    private Object[][] groups = new Object[][]{
            {R.drawable.phone, "打电话", "解放双手呼出电话"},
            {R.drawable.sms, "发短信", "驾车时都可以轻松收发短信"},
            {R.drawable.navigation, "导航", "安心开车，用嘴巴就可以控制导航了"},
            {R.drawable.account, "记账", "一句话随口记账"},
            {R.drawable.note, "备忘", "语音轻松添加备忘"},
            {R.drawable.remind, "提醒", "语音轻松添加提醒"},
            {R.drawable.alarm, "闹钟", "语音轻松设置闹钟"},
            {R.drawable.music, "播放音乐", "想听什么音乐对我说"},
            {R.drawable.news, "新闻", "让我读新闻给你听"},
            {R.drawable.wealther, "天气", "从人的角度为你播报天气"},
            {R.drawable.story, "故事", "为你故事"},
            {R.drawable.joke, "笑话", "为你笑话"},
            {R.drawable.poem, "诗词", "能和你对诗"},
            {R.drawable.calendar_black, "日历", "算日子问时间"},
            {R.drawable.video, "影视", "询问影视信息"},
            {R.drawable.knew, "闲聊百科", "各种闲聊多种百科知识"},
            {R.drawable.game, "游戏", "玩成语接龙、飞花令"},
            {R.drawable.stady, "学习调教", "你可以教我说话了"},
            {R.drawable.calc, "算术", "四则运算难不倒我"},
            {R.drawable.equity, "股票", "快速了解股市行情"},
            {R.drawable.triffic, "交通", "获取票价信息"},
            {R.drawable.nearby, "地图周边", "吃喝玩乐我都在行"},
            {R.drawable.lottery, "彩票", "一句话获取开奖信息"},
            {R.drawable.sport, "赛事", "体坛赛事快讯"},
            {R.drawable.basket, "购物", "帮你找最优惠的价格"},
            {R.drawable.hand, "怎么使用", "不会用可以问问我"}

    };


    private String[][] datas = new String[][]{
            {"拨打电话",
                    "呼叫张三",
                    "打给10086",
                    "用免提打电话给张三",
                    "呼叫186三个零1234",
                    "打电话给工行",
                    "报警",
                    "叫救护车",
                    "打给张伟伟大的伟",
                    "打电话给张三的座机",
                    "拨打张三136的号码",
                    "打电话给张三5346的手机",
                    "打电话给百度的张三",
                    "刚才是谁的电话",
                    "回拨刚才的电话",
                    "我有什么未接来电",
                    "回拨电话",
                    "给张三回一个电话"
            },

            {
                    "发送短信",
                    "发短信给张三",
                    "发短信给张三告诉他早点回家吃饭",
                    "发短信给张三移动的号码",
                    "发短信给张三的136的号码",
                    "发短信给张三4321的号码",
                    "发短信给百度的张三",
                    "刚才是谁的短信",
                    "回复刚才的短信",
                    "读下刚才的两条短信",
                    "朗读全部短信",
                    "我有什么短信",
                    "回复短信",
                    "回短信给张三",

            },
            //导航
            {
                    "导航回家",
                    "导航去附近的加油站",
                    "在保利世贸附近找一个停车场",
                    "导航回家途经人民广场",
                    "导航去白云山躲避拥堵",
                    "导航去白云山选择最短路径",
                    "导航去白云山高速优先",
                    "导航去白云山选择收费最少路线",
                    "导航去白云山不用躲避拥堵",
                    "设置家的位置",
                    "<导航中>更改目的地",
                    "<导航中>改目的地为广州塔",
                    "<导航中>查询费用",
                    "<导航中>费用最少的路线",
                    "<导航中>最短路线",
                    "<导航中>高速优先",
                    "<导航中>躲避拥堵",
                    "<导航中>不用躲避拥堵",
                    "<导航中>全程多少公里",
                    "<导航中>全程要多少时间",
                    "<导航中>查看全程",
                    "<导航中>还有多久才到",
                    "<导航中>还有多远",
                    "<导航中>添加途经点白云山",
                    "<导航中>有没有经过人民公园",
                    "<导航中>不要经过人民公园",
                    "<导航中>放大/缩小地图",
                    "<导航中>途径了哪些地方",
                    "<导航中>选择方案一",
                    "<导航中>路上找一个加油站",
                    "<导航中>我要去白云山路上找一个加油站",
                    "<导航中>人民大道堵车吗",
                    "<导航中>前面堵车吗",
                    "<导航中>上班堵车吗",
                    "<导航中>回家堵车吗",
                    "<导航中>去白云山的路堵不堵",
                    "<导航中>躲开人民大道的拥堵",
                    "<导航中>路上哪些地方堵车",
                    "<导航中>现在车速是多少",
                    "<导航中>现在什么路",
                    "<导航中>前面路况怎样",
                    "<导航中>现在是去白云山吗",
                    "<导航中>目的地是哪里",
                    "<导航中>暂停导航",
                    "<导航中>继续导航"
            },

            //记账
            {
                    "昨天买衣服花了356元",
                    "查看记账",
                    "查询本月的记账",
                    "查看上个月吃饭的记账",
                    "删除记账",
                    "删除全部记账",
                    "删除本月的记账",
                    "删除200块以下的记账",
                    "删除项目为吃饭的记账",
                    "删除备注为和朋友吃饭的记账",
                    "撤销删除",
                    "播报记账",
                    "播报本月的记账",
                    "改为251块",
                    "改为午餐",
                    "把备注改为旅行的意义",
                    "改为昨天下午3点"
            },

            //备忘
            {"新建备忘",
                    "添加备忘今天天气不错",
                    "进入添加模式",
                    "退出添加模式",
                    "查看备忘",
                    "查看会议开头的备忘",
                    "查询内容为开会的备忘",
                    "查询刚才修改的备忘",
                    "查询昨天创建的备忘",
                    "删除备忘",
                    "删除含有开会的备忘",
                    "撤销删除",
                    "播报备忘",
                    "播报昨天的备忘"
            },

            //提醒
            {"新建提醒",
                    "提醒我明天下午3点洗衣服",
                    "每年5月5日提醒我老婆生日",
                    "查询提醒",
                    "查询含有开会的提醒",
                    "查询刚才修改的提醒",
                    "查询昨天开会的提醒",
                    "查询昨天下午三点内容为开会的提醒",
                    "查询每月/周/天重复的提醒",
                    "查询每天9点重复的提醒",
                    "查询1小时内的提醒",
                    "删除提醒",
                    "删除昨天的提醒",
                    "撤销删除",
                    "播报提醒",
                    "播报昨天下午三点内容为开会的提醒",
                    "改为明天下午8点",
                    "改为每周一",
                    "将内容改为吃饭"
            },

            //闹钟
            {"设置闹钟",
                    "设置一个8点的闹钟",
                    "明天8点叫我起床",
                    "查询闹钟",
                    "查询明天的闹钟",
                    "查询每周一重复的闹钟",
                    "查询1小时内的闹钟",
                    "删除闹钟",
                    "删除刚才的闹钟",
                    "撤销删除",
                    "播报闹钟",
                    "播报每天9点重复的闹钟",
                    "打开/关闭闹钟",
                    "打开/关闭明天的闹钟",
                    "改为9点"
            },

            //播放音乐
            {"放首歌",
                    "播放摇滚乐",
                    "我想听旅行的意义",
                    "来一首王菲的歌",
                    "播放周杰伦的爱情废柴",
                    "点播英文歌",
                    "播放手机里的歌",
                    "<播放中>上一首",
                    "<播放中>下一首",
                    "<播放中>换一首",
                    "<播放中>换一批",
                    "<播放中>循环播放",
                    "<播放中>随机播放",
                    "<播放中>单曲循环",
                    "<播放中>顺序播放"
            },

            //新闻
            {"最近有什么新闻",
                    "有什么周星驰的新闻",
                    "读一下第二条",
                    "第一条的全文"
            },

            //天气
            {"今天天气怎样",
                    "今天几度",
                    "北京明天下雨吗",
                    "广州最近天气如何",
                    "今天空气质量怎样"
            },
            //故事
            {"讲个故事",
                    "讲个成语故事"
            },
            //笑话
            {"讲个笑话",
                    "讲个会变声的笑话"},
            //诗词
            {"背首诗",
                    "背诵静夜思",
                    "背一首王之涣的登鹳雀楼",
                    "床前明月光的下一句"
            },
            //日历
            {"今年除夕是几号",
                    "植树节是哪一天",
                    "距离春节还有多少天"},
            //电影
            {"最近有什么电影",
                    "最近有什么好看的动作片",
                    "最近有什么韩剧",
            },
            //闲聊百科
            {"毛泽东的老家在哪里",
                    "姚明和郭敬明谁高",
                    "世界上最大的淡水湖是什么",
                    "世界第一高峰"

            },
            //游戏
            {"我要玩游戏",
                    "我要玩成语接龙",
                    "玩飞花令"
            },
            /* 学习调教 */
            {"记住你的名字叫小灵",
                    "你记住当我说有客人来了你要说欢迎欢迎",
                    "你记住当我说天黑了你要打开灯",
                    "跟我说新年快乐"
            },
            //算术
            {
                    "1+1等于几",
                    "长方形的长是3宽是1周长是多少",
                    "面积是3的三角形底边是3高是多少",
                    "身高170cm的女生标准体重是多少"

            },
            //股票
            {
                    "最近股市怎样",
                    "大盘现在多少点",
                    "云南白药现在的股价是多少",
                    "科大讯飞有没有涨停"

            },
            //交通
            {
                    "明天早上到北京的航班",
                    "从广州到北京的火车有哪些",
                    "到深圳坐大巴要多少钱"
            },
            //地图周边
            {
                    "附近有什么加油站",
                    "在附近找一家餐馆"
            },
            //彩票
            {
                    "本期双色球开奖号码",
                    "2017013期双色球开奖号码",
                    "大乐透怎么玩",
                    "双色球开奖频率",
                    "双色球多少钱一注"
            },
            //赛事
            {
                    "今天有什么足球比赛",
                    "西甲积分榜",
                    "今天有湖人的比赛吗"

            },
            //购物
            {
                    "iphone7卖多少钱",
                    "iphone7哪里最便宜"
            },
            //怎么使用
            {"怎么播放音乐",
                    "怎么打电话",
                    "怎么发短信",
                    "怎么使用唤醒功能"}


    };

    @Override
    public int getGroupCount() {
        return groups.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return datas[groupPosition].length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groupPosition;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return groupPosition * 10 + childPosition;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * 10 + childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.example_list_item, null);
        }
        RelativeLayout l = (RelativeLayout) ((LinearLayout) convertView).getChildAt(1);
        ((ImageView) l.getChildAt(0)).setImageResource((Integer) groups[groupPosition][0]);
        LinearLayout box = (LinearLayout) l.getChildAt(1);
        TextView title = (TextView) box.getChildAt(0);
        title.setText(groups[groupPosition][1].toString());
        ((TextView) box.getChildAt(1)).setText(groups[groupPosition][2].toString());
        if (isExpanded) {
            title.setTextColor(mContext.getResources().getColorStateList(R.color.base_blue));
            ((ImageView) l.getChildAt(2)).setImageResource(R.drawable.more_up);
            if (groupPosition > 0)
                ((LinearLayout) convertView).getChildAt(0).setVisibility(View.VISIBLE);
        } else {
            title.setTextColor(mContext.getResources().getColorStateList(R.color.new_text_color_first));
            ((ImageView) l.getChildAt(2)).setImageResource(R.drawable.more_dowm);
            ((LinearLayout) convertView).getChildAt(0).setVisibility(View.INVISIBLE);
        }
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.example_list_item2, null);
        }
        LinearLayout l = (LinearLayout) convertView;
        ((TextView) l.getChildAt(0)).setText(datas[groupPosition][childPosition]);
        if (isLastChild) {
            l.getChildAt(1).setVisibility(View.VISIBLE);
        } else {
            l.getChildAt(1).setVisibility(View.INVISIBLE);
        }
        final TextView textView = (TextView) convertView.findViewById(R.id.child_text);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("introduceFragment", "text is >>>>" + textView.getText());
                ((MainActivity)mContext).quitLongRecordMode();
                String text = textView.getText().toString();
                //隐藏introduce界面
                EventBus.getDefault().post(new IntroduceShowEvent(false));
                //添加到聊天界面
                EventBus.getDefault().post(new ChatMsgEvent(new SpeechMsg(text), null, null, null));
                //2.发送到AssistantService的robot处理
                Intent intent = new Intent(mContext, AssistantService.class);
                intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT);
                intent.putExtra(AssistantService.TEXT, text);
                intent.putExtra(AssistantService.INPUT_TYPE, AssistantService.INPUT_VOICE);
                mContext.startService(intent);
            }
        });
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

}
