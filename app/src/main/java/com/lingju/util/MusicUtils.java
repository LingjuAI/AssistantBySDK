package com.lingju.util;

import android.text.TextUtils;

import com.lingju.common.log.Log;
import com.lingju.config.Setting;
import com.lingju.model.PlayMusic;
import com.lingju.model.User;
import com.lingju.model.Version;
import com.lingju.robot.AndroidChatRobotBuilder;
import com.ximalaya.ting.android.player.MD5;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 音乐工具类
 *
 * @author Leung
 */
public class MusicUtils {

    private final static String TAG = "MusicUtils";
    public static String userid = "0212b08f9026345f169d4c3da73fdce3";


/*	private static String getTestUpdateVersionJson(){
        return "{\n" +
				"    \"status\": \"0\",\n" +
				"    \"last_version\": \"v3.1.5.2\",\n" +
				"    \"update_app\": \"\",\n" +
				"    \"update_files\": [\n" +
				"        {\n" +
				"         \"update_file\":\"http://192.168.2.58/cdb.dat\"\n" +
				"        },\n" +
				"        {\n" +
				"         \"update_file\": \"http://192.168.2.58/set.dat\"\n" +
				"        },\n" +
				"        {\n" +
				"         \"update_file\": \"http://192.168.2.58/pydic.dat\"\n" +
				"        },\n" +
				"        {\n" +
				"         \"update_file\": \"http://192.168.2.58/AiBase.jar\"\n" +
				"        }\n" +
				"    ]\n" +
				"}";
		*//*return "{\n" +
                "    \"status\": \"0\",\n" +
				"    \"last_version\": \"v3.1.5\",\n" +
				"    \"update_app\": \"http://192.168.2.58/app-debug.apk\",\n" +
				"    \"update_files\": [\n" +
				"    ]\n" +
				"}";*//*
	}*/

    /**
     * @param versionName
     * @return
     */
    public static Version checkUpdateVersion(String versionName) {
        if (versionName == null)
            return null;
        //return new Version(getTestUpdateVersionJson());
        try {
            Map<String, String> message = new SocketMap<String, String>();
            message.put("command", "CheckVersion");
            message.put("version", "v" + versionName);
            message.put("appid", "3");
			/*{"command":"102","version":"v0.9.11","appid":"2"}
			QMessage message = new QMessage();
			message.setCommand(1020);
			message.setItem("8");
			message.setUserid(""); //账户，也即表中userid
			List list = new ArrayList();
			list.add(versionName); //提交最新版本号，例如:v1.0.0
			list.add("2");
			message.setData(list);
			String temp = QUtils.changeObjectToString(message);*/
            Log.i("checkUpdateVersion", "versionName:v" + versionName);
            if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
                return null;
            }
            String temp = QClient.getInstance().sendMessage(message);
            Log.i("checkUpdateVersion", "result:" + temp);
            //QClient.STATUS.remove();
            //{"last_version":"v0.9.12","update_app":"http://www.360008.com/software/app_music/v0.9.12/,LingjuMusicv0.9.12.apk"}
            //String res[] = (String[]) QUtils.changeStringToObject(temp);
            return new Version(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据指定musicid搜索指定歌曲的相关信息
     *
     * @param musicid
     * @return 歌曲的信息，json格式的文本，格式:{"url":"http://xxx.mp3","lyricist":"歌词"}。<br>
     * 歌词格式如下：<br>
     * [00:01.50]单车<br>
     * [00:07.50]<br>
     * [00:16.80]不要不要假设我知道<br>
     * [00:20.77]一切一切也都是为我而做<br>
     * [00:25.47]为何这么伟大<br>
     * [00:29.14]如此感觉不到<br>
     * [00:31.66]不说一句的爱有多好<br>
     * [00:35.33]只有一次记得实在接触到<br>
     * [00:40.04]骑着单车的我俩<br>
     * [00:43.90]怀紧贴背的拥抱<br>
     * [00:45.37]<br>
     * [00:46.01]难离难舍想抱紧些<br>
     * ...
     */
    public static String queryByMusicid(String musicid) {
        /*Map<String, String> message = new SocketMap<String, String>();
        //{"command":"Music_Url_Lyrics","musicid":"MUSIC_1565921"}
        message.put("command", "GetMusicLyrics");
        message.put("musicid", musicid);
        message.put("userid", userid);
        Log.i("queryByMusicid", "queryByMusicid::::musicid=" + musicid);
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return null;
        }
        String temp = QClient.getInstance().sendMessage(message.toString());
        Log.i("queryByMusicid", "queryByMusicid::::status:" + QClient.STATUS.get());
        return temp != null ? temp.trim().replaceAll("<br>", "\n") : null;*/
        return AndroidChatRobotBuilder.get().robot().actionTargetAccessor().searchLyrics(musicid);
    }

    /**
     * 发送匿名登录日志
     */
    public static int sendLoginLog() {
        QClient client = QClient.getInstance();
        Map<String, String> message = new SocketMap<String, String>();
        //{"command":"UserLoginNone","lingjuappkey":"XXXXXXXX","lingjumodel":"XXX","userid":"f86434f484e4c1af55ba68aa8428b978","imei":"869274011849263","mac":"78:f5:fd:91:62:51",
        //"serial":"021YHB2133059225"}

        message.put("command", "UserLoginNone");
        message.put("lingjuappkey", "LINGJU_ASS");
        message.put("lingjumodel", "mobile"); //业务类型，目前为:BBJIA或MUSIC
        String imei = AndroidChatRobotBuilder.get().getImei();
        String mac = DeviceUtils.openWifiAndGetMacAddress();
        String series = AndroidChatRobotBuilder.get().getSeries();
        String userId = MD5.md5("LINGJU_ASS" + imei + mac + series);
        message.put("userid", userId); //用户账户=MD5(lingjuappkey+imei+mac+serial) 32位
        message.put("imei", imei); //终端机器码
        message.put("mac", mac); //终端网卡地址
        message.put("serial", series); //终端序列号

        Log.d(TAG, "sendLoginLog>>>>" + message.toString());
        String temp = client.sendMessage(message);
        try {
            if (!TextUtils.isEmpty(temp.trim()) && !"{}".equals(temp)) {
                JSONObject json = new JSONObject(temp);
                return json.getInt("status");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "sendLoginLog result==" + temp);
        return -1;
    }

    /**
     * 根据给定歌名和歌手搜索歌曲的歌词.</br>
     *
     * @param songTitle 歌曲名称
     * @param singer    歌曲演唱者
     * @return 歌词文本，格式如下：<br>
     * [00:01.50]单车<br>
     * [00:07.50]<br>
     * [00:16.80]不要不要假设我知道<br>
     * [00:20.77]一切一切也都是为我而做<br>
     * [00:25.47]为何这么伟大<br>
     * [00:29.14]如此感觉不到<br>
     * [00:31.66]不说一句的爱有多好<br>
     * [00:35.33]只有一次记得实在接触到<br>
     * [00:40.04]骑着单车的我俩<br>
     * [00:43.90]怀紧贴背的拥抱<br>
     * [00:45.37]<br>
     * [00:46.01]难离难舍想抱紧些<br>
     * ...
     */
    public static String searchLyric(String songTitle, String singer) {
        /*Map<String, String> message = new SocketMap<String, String>();
        //{"command":"Music_Singer_Name_Lyrics","singer":"刘德华","name":"忘情水"}
        message.put("userid", userid);
        message.put("command", "SearchMusicLyrics");
        message.put("singer", singer);
        message.put("name", songTitle);
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return null;
        }
        String temp = QClient.getInstance().sendMessage(message.toString());
        //QClient.STATUS.remove();
	*//*	String[] res = (String[]) QUtils.changeStringToObject(temp);
		if(res!=null&&res.length>0){
			return res[0];
		}*//*
        return temp != null && temp.length() > 1 ? temp.replaceAll("<br>", "\n") : null;*/
        return AndroidChatRobotBuilder.get().robot().actionTargetAccessor().searchLyrics(songTitle, singer);
    }

    public static Pattern pt = Pattern.compile("[\\p{Punct}\\p{Sc}，。？‘’”“：；、\\/〈〉《》【】〈（）￥#·！~`]");//标点符号正则

    public static String[] formatSongFileName(String fileName, String title, String singer, boolean isLocalMode) {
        if (fileName.startsWith("/")) {
            if (pt.matcher(title).find() || pt.matcher(singer.replaceAll("[\\&\\/\\,\\，]", "")).find() || singer.contains("未知")
                    || (noChinese(title) && noChinese(singer))) {
                Log.i("formatSongFileName", "formatSongFileName:" + fileName);
                int b = fileName.lastIndexOf("/") + 1;
                int e = fileName.lastIndexOf(".");
                if (e < b)
                    return null;
                fileName = fileName.substring(b, e);
                e = fileName.indexOf("-");
                if (e > 0 && e != fileName.length() - 1) {
                    String[] rs = new String[2];
                    rs[1] = fileName.substring(0, e).trim();
                    rs[0] = fileName.substring(e + 1).trim();
                    e = rs[0].indexOf("-");
                    if (e > 0) {
                        rs[0] = rs[0].substring(0, e).trim();
                    }
                    rs[1] = rs[1].replaceAll("\\[.*\\]", "");
                    if (rs[1].contains(singer))
                        rs[1] = singer;
                    if (rs[0].contains(title))
                        rs[0] = title;
                    return rs;
                }
                if (!isLocalMode) {
                    //System.out.println("remote formatSongFileName:"+fileName);
                    String result = fileName.replaceAll("专辑|曲目艺人|主题曲|大比拼|电视剧", "");
                    Map<String, String> message = new SocketMap<String, String>();
                    message.put("command", "Music_Local_Name");
                    message.put("music_name", result);
                    //{"command":"Music_Local_Name","music_name":"忘情水"}
                    if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
                        return null;
                    }
                    result = QClient.getInstance().sendMessage(message);
                    if (result != null && result.contains("|")) {
                        return result.split("\\|", 2);
                    }
                }
            }
        }
        return null;
    }

    static Pattern p = Pattern.compile("[\u4E00-\u9FA5]");

    public static boolean noChinese(String text) {
        if (text == null || text.length() == 0)
            return true;
        return !p.matcher(text).find();
    }

    /**
     * @param user
     * @return "0"：成功："1"， 已经注册，"2"：密码位数太短（6-12位合适）；"3"：邮箱不正确，"4"：其他原因
     */
    public static int register(User user) {
        int r;
        if (QClient.getInstance().isSessionInvalid() && (r = sendLoginLog()) != 0) {
            return r;
        }
        QClient client = QClient.getInstance();
        //{"command":"UserRegister","email":"kong_gz@163.com","password":"kong555","sex":"m","provice":"安徽","city":"合肥"}
        Map<String, String> message = new SocketMap<String, String>();
        message.put("command", "UserRegister");
        message.put("email", user.getEmail());
        message.put("password", user.getPassword());
        message.put("sex", user.getSex());
        message.put("provice", user.getProvinces());
        message.put("city", user.getCity());

        Log.i(TAG, "register:email:" + user.getEmail() + ",psw:" + user.getPassword() + ",sex:" + user.getSex() + ",province:" + user.getProvinces() + ",city:" + user.getCity());
        String temp = client.sendMessage(message);
        Log.i(TAG, "register:result:" + temp);
        if (temp == null) {
            return 4;
        } else {
            try {
                JSONObject json = new JSONObject(temp);
                if (json.getInt("status") == 0) {
                    user.setUserid(json.getString("userid"));
                    return 0;
                } else {
                    return json.getInt("status");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return 8;
    }

    public static boolean submitFeedback(String content, User user) {
        String userid;
        if (user != null && user.getUserid() != null) {
            userid = user.getUserid();
        } else
            userid = Setting.machineMsg;
        Map<String, String> message = new SocketMap<String, String>();
        message.put("command", "Suggest");
        message.put("type", "ASSISTANT");
        message.put("content", content);
        message.put("userid", userid);
        Log.i("submitFeedback", "MSG:" + message.toString());
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return false;
        }
        String temp = QClient.getInstance().sendMessage(message);
        Log.i("submitFeedback", "result:" + temp);
        if (!TextUtils.isEmpty(temp.trim()) && !"{}".equals(temp)) {
            try {
                return new JSONObject(temp).getInt("status") == 0;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean login(User user) {
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return false;
        }
        QClient client = QClient.getInstance();
        //{"command":"UserLogin","uid":"kong_gz@163.com","password":"kong555"}
        Map<String, String> message = new SocketMap<String, String>();
        message.put("command", "UserLogin");
        message.put("uid", user.getEmail());
        message.put("password", user.getPassword());
        Log.i(TAG, "login:uid:" + user.getEmail());
        String temp = client.sendMessage(message);
        Log.i(TAG, "login:result:" + temp);
        if (temp == null || temp.length() == 0 || temp.trim().equals("0")) {
            return false;
        }
        try {
            JSONObject json = new JSONObject(temp);
            if (json.getInt("status") == 0) {
                user.setUserid(json.getString("userid"));
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
	
	/*public static boolean loginByWeibo(User user,com.sina.weibo.sdk.openapi.models.User wUser){
		if(user==null||wUser==null)return false;
		if(QClient.getInstance().isSessionInvalid()&&sendLoginLog()!=0){
			return false;
		}
		QClient client = QClient.getInstance();
		//{"command":"UserLoginThird","uid":"kong_gz22@163.com","type":"0","sex":"m","provice":"安徽","city":"合肥"}
		boolean reg=user.getUserid()==null;
		Map<String,String> message=new SocketMap<String, String>();
		message.put("command", "UserLoginThird");
		message.put("uid", wUser.id);
		if(reg)
			message.put("type", "0"); //类别，0：第一次登录相当于注册，下面三个资料需要，1：已经存在账户的登录，下面三个资料可以为空
		else
			message.put("type", "1");
		message.put("sex",  wUser.gender);
		String[] location=wUser.location.split("\\s",2);
		if(location.length==2){
			message.put("provice", location[0]); //省/自治区   --可以为空
			message.put("city", location[1]); //地市     --可以为空
		}
		else{
			if(location[0].length()>1){
				message.put("provice", location[0]);
			}
			else{
				message.put("provice", "");
			}
			message.put("city", "");
		}
		
		Log.i(TAG, "loginByWeibo:uid="+wUser.id+",type:"+message.get("type")+","
				+ "sex:"+message.get("sex")+",province:"+message.get("provice")+",city="+message.get("city"));
		String temp = client.sendMessage(message.toString());
		Log.i(TAG, "loginByWeibo:result:"+temp);
		if(temp==null||temp.trim().equals("")){
			return false;
		}
		try {
			JSONObject json=new JSONObject(temp);
			if(json.getInt("status")==0) {
				if(reg){
					user.setName(wUser.screen_name);
					user.setUserid(json.getString("userid"));
					user.setEmail(wUser.id);
					user.setPassword("");
					user.setSex(wUser.gender);
					user.setProvinces(message.get("provice").toString());
					user.setCity(message.get("city").toString());
				}
				return true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return false;
	}*/

    /**
     * @param user
     * @param jo   json格式：
     *             {
     *             "openid":"OPENID",//普通用户的标识，对当前开发者帐号唯一
     *             "nickname":"NICKNAME",
     *             "sex":1,//1为男性，2为女性
     *             "province":"PROVINCE",
     *             "city":"CITY",
     *             "country":"COUNTRY",
     *             "headimgurl": "http://wx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     *             //用户头像，最后一个数值代表正方形头像大小（有0、46、64、96、132数值可选，0代表640*640正方形头像），用户没有头像时该项为空
     *             "privilege":[
     *             "PRIVILEGE1",
     *             "PRIVILEGE2"
     *             ],//用户特权信息，json数组，如微信沃卡用户为（chinaunicom）
     *             "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"//用户统一标识。针对一个微信开放平台帐号下的应用，同一用户的unionid是唯一的。
     *             }
     * @return
     */
    public static boolean loginByWeChat(User user, JSONObject jo) {
        if (user == null || jo == null)
            return false;
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return false;
        }
        QClient client = QClient.getInstance();
        //{"command":"Info_Login_Third","uid":"kong_gz22@163.com","type":"0","sex":"m","provice":"安徽","city":"合肥"}
        boolean reg = user.getUserid() == null;
        Map<String, String> message = new SocketMap<String, String>();
        try {
            message.put("command", "UserLoginThird");
            message.put("uid", jo.getString("unionid"));
            if (reg)
                message.put("type", "0"); //类别，0：第一次登录相当于注册，下面三个资料需要，1：已经存在账户的登录，下面三个资料可以为空
            else
                message.put("type", "1");
            message.put("sex", jo.getString("sex").equals("1") ? "m" : jo.getString("sex").equals("2") ? "f" : "n");
            message.put("provice", jo.getString("province")); //省/自治区   --可以为空
            message.put("city", jo.getString("city")); //地市     --可以为空

            String temp = client.sendMessage(message);
            Log.i(TAG, "loginByWeChat:result:" + temp);
            if (temp == null || temp.trim().equals("")) {
                return false;
            }
            try {
                JSONObject json = new JSONObject(temp);
                if (json.getInt("status") == 0) {
                    if (reg) {
                        user.setUserid(json.getString("userid"));
                        user.setName(jo.getString("nickname"));
                        user.setEmail(jo.getString("unionid"));
                        user.setPassword("");
                        user.setSex(message.get("sex").toString());
                        user.setProvinces(message.get("provice").toString());
                        user.setCity(message.get("city").toString());
                    }
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean loginByQQ(User user, JSONObject jo, String uid) {
        if (user == null || jo == null)
            return false;
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return false;
        }
        QClient client = QClient.getInstance();
        //{"command":"Info_Login_Third","uid":"kong_gz22@163.com","type":"0","sex":"m","provice":"安徽","city":"合肥"}
        boolean reg = user.getUserid() == null;
        Map<String, String> message = new SocketMap<String, String>();
        try {
            message.put("command", "UserLoginThird");
            message.put("uid", uid);
            if (reg)
                message.put("type", "0"); //类别，0：第一次登录相当于注册，下面三个资料需要，1：已经存在账户的登录，下面三个资料可以为空
            else
                message.put("type", "1");
            message.put("sex", jo.getString("gender").equals("男") ? "m" : jo.getString("gender").equals("女") ? "f" : "n");
            message.put("provice", jo.getString("province")); //省/自治区   --可以为空
            message.put("city", jo.getString("city")); //地市     --可以为空

            String temp = client.sendMessage(message);
            Log.i(TAG, "loginByQQ:result:" + temp);
            if (temp == null || temp.trim().equals("")) {
                return false;
            }
            try {
                JSONObject json = new JSONObject(temp);
                if (json.getInt("status") == 0) {
                    if (reg) {
                        user.setName(jo.getString("nickname"));
                        user.setUserid(json.getString("userid"));
                        user.setEmail(uid);
                        user.setPassword("");
                        user.setSex(message.get("sex").toString());
                        user.setProvinces(message.get("provice").toString());
                        user.setCity(message.get("city").toString());
                    }
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean lookForPsw(String email) {
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return false;
        }
        QClient client = QClient.getInstance();
        //{"command":"ForgetPassword","email":"kong_gz@163.com"}
        Map<String, String> message = new SocketMap<String, String>();
        message.put("command", "ForgetPassword");
        message.put("email", email);

        String temp = client.sendMessage(message);
        Log.i(TAG, "lookForPsw:result=" + temp);
        if (TextUtils.isEmpty(temp))
            return false;
        try {
            return new JSONObject(temp).getInt("status") == 0;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean updatePsw(User user, String oldPassword) {
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return false;
        }
        QClient client = QClient.getInstance();
        //{"command":"EditPassword ","email":"kong_gz@163.com","password":"kong555111","oldpassword":"kong555"}
        Map<String, String> message = new SocketMap<String, String>();
        message.put("command", "EditPassword");
        message.put("email", user.getEmail());
        message.put("password", user.getPassword());
        message.put("oldpassword", oldPassword);

        Log.i(TAG, "updatePsw:email=" + user.getEmail() + ",psw=" + user.getPassword() + ",opsw=" + oldPassword);
        String temp = client.sendMessage(message);
        Log.i(TAG, "updatePsw:result=" + temp);
        if (TextUtils.isEmpty(temp))
            return false;
        try {
            return new JSONObject(temp).getInt("status") == 0;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean favoriteMusic(PlayMusic m, User user) {
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return false;
        }
        QClient client = QClient.getInstance();
        //{"command":"Music_Save_Collection","userid":"100","musicname":"忘情水A","singer":"刘德华","times":"100","album":"金曲","type":"抒情","musicid":"MUSIC_1565922","islocal":"1"}
        Map<String, String> message = new SocketMap<String, String>();
        message.put("command", "SaveMusicCollection");
        message.put("userid", user.getUserid());
        message.put("musicname", m.getTitle()); //歌名
        message.put("singer", m.getSinger()); //歌手
        message.put("times", String.valueOf(m.getDuration())); //时长，单位毫秒
        message.put("album", m.getAlbum() == null ? "" : m.getAlbum()); //专辑名称
        message.put("type", m.getType() == null ? "" : m.getType()); //歌曲类型
        message.put("musicid", m.getCloud() ? m.getMusicid() : ""); //歌曲位置
        message.put("islocal", m.getCloud() ? "1" : "0"); //是否本地，0：本地，1：网络

        Log.i(TAG, "favoriteMusic:musicid=" + m.getMusicid());
        String temp = client.sendMessage(message);
        Log.i(TAG, "favoriteMusic:result=" + temp);
        if (TextUtils.isEmpty(temp))
            return false;
        try {
            return new JSONObject(temp).getInt("status") == 0;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean unFavoriteMusic(PlayMusic m, User u) {
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return false;
        }
        QClient client = QClient.getInstance();
        //{"command":"Music_Delete_Collection","userid":"100","musicname":"忘情水"}
        Map<String, String> message = new SocketMap<String, String>();
        message.put("command", "DeleteMusicCollection");
        message.put("userid", u.getUserid());
        message.put("musicname", m.getTitle());

        Log.i(TAG, "unFavoriteMusic:musicid=" + m.getMusicid() + ",title=" + m.getTitle() + ",userid=" + u.getUserid());
        String temp = client.sendMessage(message);
        Log.i(TAG, "unFavoriteMusic:result=" + temp);
        if (TextUtils.isEmpty(temp))
            return false;
        try {
            return new JSONObject(temp).getInt("status") == 0;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @param user
     * @return 格式：{"counts":"2","data":[{"musicname":"忘情水","singer":"刘德华","times":"1000","album":"哈哈","type":"抒情","musicid":"暂无1","islocal":"0"},{"musicname":"男人哭吧不是罪","singer":"刘德华","times":"2000","album":"好的","type":"","musicid":"","islocal":"1"}]}
     * @throws JSONException
     */
    public static String getFavoriteMusicFromCloud(User user) {
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return null;
        }
        QClient client = QClient.getInstance();
        //{"command":"Music_Get_Collection","userid":"100"}
        Map<String, String> message = new SocketMap<String, String>();
        message.put("command", "GetMusicCollection");
        message.put("userid", user.getUserid());
        String temp = client.sendMessage(message);
        Log.i(TAG, "getFavoriteMusicFromCloud:" + temp);
        if (temp == null || temp.trim().length() == 0)
            return null;
        return temp;
    }

    public static boolean sendPlayLog(PlayMusic m, String userid) {
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return false;
        }
        QClient client = QClient.getInstance();
        //{"command":"Music_Play_Log","userid":"kong","type":"MUSIC","musicid":"MUSIC_325771","singer":"刘德华","name":"忘情水"}
        Map<String, String> message = new SocketMap<String, String>();
        message.put("command", "MusicPlayLog");
        message.put("userid", userid);
        message.put("type", "ASSISTANT"); //业务类型，目前为:BBJIA或MUSIC
        if (m.getCloud())
            message.put("musicid", m.getMusicid()); //歌曲编码，比如"MUSIC_1543"等；
        message.put("singer", m.getSinger()); //歌手
        message.put("name", m.getTitle()); //歌曲名称

        Log.i(TAG, "sendPlayLog >>>>" + message.toString());
        String temp = client.sendMessage(message);
        Log.i(TAG, "sendPlayLog result>>>>" + temp);
        if (TextUtils.isEmpty(temp))
            return false;
        try {
            return new JSONObject(temp).getInt("status") == 0;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getPushMusics(User user) {
        if (QClient.getInstance().isSessionInvalid() && sendLoginLog() != 0) {
            return null;
        }
        QClient client = QClient.getInstance();
        //{"command":"Music_Recommend","userid":"aaaaa","type":"MUSIC"}
        Map<String, String> message = new SocketMap<String, String>();
        message.put("command", "MusicRecommend");
        message.put("type", "ASSISTANT"); //业务类型，目前为:BBJIA或MUSIC
        message.put("userid", user == null || TextUtils.isEmpty(user.getUserid()) ? Setting.machineMsg : user.getUserid()); //用户账户=MD5(imei+mac+serial) 16位
        if (message.get("userid").toString().equals(Setting.machineMsg) && Setting.machineMsg.equals(Setting.UNKNOWN)) {
            return null;
        }
        Log.i(TAG, "getPushMusics >>>>" + message.toString());
        String temp = client.sendMessage(message);
        //Log.i(TAG, "getPushMusics result>>>>"+temp);
        return temp;
    }


    private static String findUrlsBegin =
            "http://antiserver.kuwo.cn/anti.s?response=url&format=aac%7Cmp3&type=convert_url&rid=";
    private static String findUrlsEnd = "&qq-pf-to=pcqq.c2c";
    private static HttpURLConnection conn = null;
    private static int retryGetMusicTimes = 3;

    public static String getMusicOnlineUri(String musicid) {
        String result = null;
        try {
            retryGetMusicTimes = 3;
            URL urls = new URL(findUrlsBegin + musicid + findUrlsEnd);
            while (retryGetMusicTimes-- > 0) {
                try {
                    result = getURLSource(urls);
                    Log.i(TAG, "getMusicOnlineUri(" + musicid + ")>>" + result);
                    if (result != null && result.trim().startsWith("http")) {
                        if (result.indexOf("?") > -1) {
                            result = result.substring(0, result.lastIndexOf("?"));
                        }
                        break;
                    } else {
                        result = null;
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    result = null;
                }
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            result = null;
        }
        return result;
    }

    public static void stopGetMusicOnlineUri() {
        if (conn != null) {
            System.out.println("stopGetMusicOnlineUri>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            retryGetMusicTimes = 0;
            conn.disconnect();
            conn = null;
        }
    }

    public static JSONObject getJsonByUrl(String url) {
        JSONObject jo = null;
        System.out.println("getJsonByUrl:url=" + url);
        try {
            retryGetMusicTimes = 3;
            URL urls = new URL(url);
            String result = null;
            while (retryGetMusicTimes-- > 0) {
                try {
                    result = getURLSource(urls);
                    if (result != null)
                        break;
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    result = null;
                }
            }
            if (!TextUtils.isEmpty(result)) {
                System.out.println("getJsonByUrl:result=" + result);
                jo = new JSONObject(result.trim());
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            jo = null;
        }
        return jo;
    }

    private static String getURLSource(URL urls) throws IOException {
        try {
            conn = (HttpURLConnection) urls.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Language", "zh-CN");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setConnectTimeout(10000);
            conn.connect();
            if (conn.getResponseCode() == 200) {
                InputStream inStream = conn.getInputStream(); // 通过输入流获取html二进制数据
                byte[] data = readInputStream(inStream); // 把二进制数据转化为byte字节数据
                String htmlSource = new String(data);
                htmlSource = htmlSource.trim();
                return htmlSource;
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }
        return null;
    }
	
	
	
/*
	private static String getURLSource(URL url,String encode) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(10000);
		InputStream inStream = conn.getInputStream(); // 通过输入流获取html二进制数据
		byte[] data = readInputStream(inStream); // 把二进制数据转化为byte字节数据
		String htmlSource = new String(data,encode);
		return htmlSource;
	}*/

    private static byte[] readInputStream(InputStream instream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = instream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        instream.close();
        return outStream.toByteArray();
    }
	
/*	public static void main(String args[]){
		String text="MUSIC_1037664|MUSIC_3912775|MUSIC_487224|MUSIC_3372483|MUSIC_3403156|MUSIC_132845|MUSIC_96537|MUSIC_3372506|MUSIC_107763|MUSIC_3372486|MUSIC_143503";
		final String[] ts=text.split("\\|");
		for(int i=0;i<ts.length;i++){
			final int index=i;
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					queryByMusicid(ts[index]);
				}
			}).start();
		}
	}*/

    static class SocketMap<K, V> extends HashMap<K, V> {
        public SocketMap() {
        }
    }
}
