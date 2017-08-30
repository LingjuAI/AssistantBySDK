package com.lingju.assistant.wxapi;


import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.player.audio.model.AudioRepository;
import com.lingju.assistant.social.weibo.Constants;
import com.lingju.model.PlayMusic;
import com.lingju.model.User;
import com.lingju.model.dao.UserManagerDao;
import com.lingju.util.MusicUtils;
import com.lingju.util.QClient;
import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.ConstantsAPI;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.SendAuth;
import com.tencent.mm.sdk.openapi.SendAuth.Resp;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
	private final static String TAG="WXEntryActivity";
	
	public final static String TYPE="type";
	public final static int LOGIN=1;
    private IWXAPI api;
	private Intent it;
    private String state;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wechat_login_dialog);
    	api = WXAPIFactory.createWXAPI(this, Constants.WECHAT_APPID, false);
    	api.registerApp(Constants.WECHAT_APPID);    	
        api.handleIntent(getIntent(), this);
        it=getIntent();
        int type=0;
        if(it!=null){
        	type=it.getIntExtra(TYPE, 0);
        }
        if(type==LOGIN){
        	SendAuth.Req req = new SendAuth.Req();
        	req.scope = "snsapi_userinfo";
        	req.state = state=Long.toString(System.currentTimeMillis());
        	api.sendReq(req);
        }
        findViewById(R.id.wechat_close).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
    }

	@Override
	protected void onNewIntent(Intent intent) {
		Log.i(TAG, "onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);
        api.handleIntent(intent, this);
	}

	@Override
	public void onReq(BaseReq req) {
		Log.i(TAG, "onReq:"+req+",req.getType()="+req.getType());
		switch (req.getType()) {
		case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
			break;
		case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
			break;
		default:
			break;
		}
	}

	@Override
	public void onResp(BaseResp resp) {
		Log.i(TAG, "onResp:"+resp+",resp.errCode="+resp.errCode);
		switch (resp.errCode) {
		case BaseResp.ErrCode.ERR_OK:
			if(resp instanceof SendAuth.Resp){
				SendAuth.Resp res=(Resp) resp;
				Log.i(TAG, "res.resultUrl="+res.resultUrl);
				Log.i(TAG, "res.token="+res.token);
				Log.i(TAG, "res.userName="+res.userName);
				Log.i(TAG, "res.transaction="+res.transaction);

				new GetTokenTask(((SendAuth.Resp)resp).token).execute();
			}
			else{
				finish();
			}
			break;
		case BaseResp.ErrCode.ERR_USER_CANCEL:
		case BaseResp.ErrCode.ERR_AUTH_DENIED:
		default:
			finish();
			break;
		}
	}
	
	class GetTokenTask extends AsyncTask<Void, Void, Integer>{
		private String code;
		
		public GetTokenTask(String code){
			this.code=code;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			String url="https://api.weixin.qq.com/sns/oauth2/access_token?appid="+Constants.WECHAT_APPID+"&secret="
					+Constants.WECHAT_AppSecret+"&code="+code+"&grant_type=authorization_code";
			JSONObject jo= MusicUtils.getJsonByUrl(url);
			/**jo正确返回格式
			 * { 
				"access_token":"ACCESS_TOKEN", 
				"expires_in":7200, 
				"refresh_token":"REFRESH_TOKEN",
				"openid":"OPENID", 
				"scope":"SCOPE","unionid":"o6_bmasdasdsad6_2sgVt7hMZOPfL"
				}
				错误返回格式：{"errcode":40029,"errmsg":"invalid code"}
			 */
			try {
				if(null!=jo&&!TextUtils.isEmpty(jo.getString("access_token"))){
					url="https://api.weixin.qq.com/sns/userinfo?access_token="+jo.getString("access_token")+"&openid="+jo.getString("openid");
					jo= MusicUtils.getJsonByUrl(url);
					UserManagerDao userDao= UserManagerDao.getInstance();
					AudioRepository repository = AudioRepository.create(WXEntryActivity.this);
					User user=new User();
					boolean r= MusicUtils.loginByWeChat(user,jo);
					if(r){
						user.setCreated(new Date());
						userDao.insertUser(user);
						AppConfig app=(AppConfig)getApplication();
						app.user=user;
						repository.resetFavoriteMusics();
						int pullCount=repository.pullFavoriteMusicsFromCloud(user);
						List<PlayMusic> list=repository.getFavoriteUnSyn();
						if(list.size()>0){
							for(PlayMusic m:list){
								if(MusicUtils.favoriteMusic(m, user)){
									m.setSynchronize(true);
									repository.update(m);
								}
							}
						}
						if(pullCount>0){
							LingjuAudioPlayer.get().getPlayList(IBatchPlayer.PlayListType.FAVORITE).clear();
							LingjuAudioPlayer.get().getPlayList(IBatchPlayer.PlayListType.FAVORITE).addAll(repository.getFavorite());
						}
					}
					else{
						if(QClient.STATUS.get()!=0){
							return QClient.STATUS.get();
						}
					}
					return r?0:4;
				}
			} catch (JSONException e) {
				return 4;
			}
			return 4;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			if(result!=0){
				Log.i(TAG, "result="+result);
				String text="";
				switch(result){
				case QClient.BAD_NETWORK:text="网络不给力，请重试";break;
				case QClient.NO_NETWORK:text="网络不可用";break;
				case QClient.INTERRUPTED:text="异常打断";break;
				case 4:
				default:text="登录出错";break;
				}
				((TextView)findViewById(R.id.wechat_tips)).setText(text);
				findViewById(R.id.wechat_bottom).setVisibility(View.VISIBLE);
			}
			else{
				Log.i(TAG, "result==0");
				//startActivity(new Intent(WXEntryActivity.this, MainActivity.class));
				setResult(1);
				finish();
			}
		}
		
	}
	
}