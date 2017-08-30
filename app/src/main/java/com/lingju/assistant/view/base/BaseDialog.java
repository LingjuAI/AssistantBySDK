package com.lingju.assistant.view.base;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.lingju.assistant.R;
import com.lingju.assistant.social.weibo.AccessTokenKeeper;
import com.lingju.assistant.social.weibo.Constants;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.common.log.Log;
import com.lingju.util.AnimationUtils;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.openapi.legacy.StatusesAPI;
import com.sina.weibo.sdk.openapi.models.ErrorInfo;
import com.sina.weibo.sdk.openapi.models.User;
import com.tencent.connect.share.QQShare;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXWebpageObject;
import com.tencent.mm.sdk.platformtools.Util;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONObject;

import java.text.SimpleDateFormat;

public class BaseDialog extends DialogFragment implements View.OnClickListener {
    private final static String TAG = "BaseDialog";
    private static Context mContext;
    private String shareText = "";
    private String shareLink = "http://ass.lingjuai.com";
    private String wechatShareLink = "http://ass.lingjuai.com";
    private IWXAPI wAPI;
    /**
     * 封装了 "access_token"，"expires_in"，"refresh_token"，并提供了他们的管理功能
     */
    private Oauth2AccessToken mAccessToken;

    /**
     * 注意：SsoHandler 仅当 SDK 支持 SSO 时有效
     */
    private SsoHandler mSsoHandler;

    private Tencent mTencent;

    public static BaseDialog newInstance(Context context) {

        Bundle args = new Bundle();
        mContext = context;
        BaseDialog fragment = new BaseDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.BOTTOM;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.base_dialog, container, false);
        AnimationUtils.slideToUp(view);
        view.findViewById(R.id.share_sinaweibo_box).setOnClickListener(this);
        view.findViewById(R.id.share_wechat_friend_box).setOnClickListener(this);
        view.findViewById(R.id.share_weichat_timeline_box).setOnClickListener(this);
        view.findViewById(R.id.share_qq_box).setOnClickListener(this);
        registerToWx();
        mTencent = Tencent.createInstance(Constants.TENCENT_APPID, mContext);
        shareText = mContext.getResources().getString(R.string.shareText);
        shareLink = wechatShareLink;
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.share_sinaweibo_box:
                shareToWeibo();
                break;
            case R.id.share_wechat_friend_box:
                shareToWechat(false);
                break;
            case R.id.share_weichat_timeline_box:
                shareToWechat(true);
                break;
            case R.id.share_qq_box:
                shareToQQ();
                break;
            default:
                break;
        }
    }

    private void shareToQQ() {
        if (isPkgInstalled("com.tencent.mobileqq")) {

            final Bundle params = new Bundle();
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
            params.putString(QQShare.SHARE_TO_QQ_TITLE, shareText);
            params.putString(QQShare.SHARE_TO_QQ_SUMMARY, mContext.getResources().getString(R.string.shareDescription));
            params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, shareLink);
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, "http://www.lingjuai.com/product/images/logo.png");
            params.putString(QQShare.SHARE_TO_QQ_APP_NAME, "灵聚智能助理");
            mTencent.shareToQQ((Activity) mContext, params, new BaseUiListener());
        } else {
            new CommonDialog(getActivity(), "温馨提示", "未检测到“QQ”应用", "确定").show();
        }
    }

    private void registerToWx() {
        wAPI = WXAPIFactory.createWXAPI(mContext, Constants.WECHAT_APPID, true);
        wAPI.registerApp(Constants.WECHAT_APPID);
    }

    private void shareToWeibo() {
        mAccessToken = AccessTokenKeeper.readAccessToken(mContext);
        if (mAccessToken != null) {
            StatusesAPI statusAPI = new StatusesAPI(mContext, Constants.WEIBO_APPKEY, mAccessToken);
            statusAPI.update(shareText + mContext.getResources().getString(R.string.shareDescription) + shareLink, "0.0", "0.0", weiboListener);
            //statusAPI.uploadUrlText("分享一个音乐播放器，http://www.lingjutech.com", "http://tp3.sinaimg.cn/1706684510/50/22818070132/1", "", "0.0", "0.0", weiboListener);
        } else {
            AccessTokenKeeper.clear(mContext);
            Log.i(TAG, "Context=" + mContext);
            AuthInfo authInfo = new AuthInfo(mContext, Constants.WEIBO_APPKEY, Constants.REDIRECT_URL, Constants.SCOPE);
            //			WeiboAuth weiboAuth = new WeiboAuth(mContext, Constants.APP_KEY, Constants.REDIRECT_URL, Constants.SCOPE);
            mSsoHandler = new SsoHandler((Activity) mContext, authInfo);
            mSsoHandler.authorize(new AuthListener());
        }
    }


    private void shareToWechat(boolean timeline) {
        if (isPkgInstalled("com.tencent.mm")) {
            // 初始化一个WXTextObject对象
            Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);
            //WXImageObject imgObj = new WXImageObject(bmp);

            WXWebpageObject webObj = new WXWebpageObject();
            webObj.webpageUrl = shareLink;


            WXMediaMessage msg = new WXMediaMessage();
            msg.mediaObject = webObj;

            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true);
            bmp.recycle();
            msg.thumbData = Util.bmpToByteArray(thumbBmp, true);  // 设置缩略图
            msg.description = mContext.getResources().getString(R.string.shareDescription);
            msg.title = shareText;

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = buildTransaction("webpage");
            req.message = msg;

            req.scene = timeline ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;

            // 调用api接口发送数据到微信
            wAPI.sendReq(req);
        } else {
            new CommonDialog(getActivity(), "温馨提示", "未检测到“微信”应用", "确定").show();
        }
    }

    public SsoHandler getmSsoHandler() {
        return mSsoHandler;
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    private RequestListener weiboListener = new RequestListener() {

        @Override
        public void onWeiboException(WeiboException e) {
            Log.e(TAG, "weiboListener.onWeiboException:" + e.getMessage());
            ErrorInfo ei = ErrorInfo.parse(e.getMessage());
            if (ei.error_code.equals("21332") || ei.error_code.equals("21327")) {
                AccessTokenKeeper.clear(mContext);
                shareToWeibo();
            } else {
                new CommonDialog(mContext, "微博分享", "分享失败，错误码：" + ei.error_code, "确定").show();
            }
        }

        @Override
        public void onComplete(String s) {
            Log.e(TAG, "weiboListener.onComplete:");
            new CommonDialog(mContext, "微博分享", "分享成功", "确定").show();
        }
    };

    /**
     * 登入按钮的监听器，接收授权结果。
     */
    private class AuthListener implements WeiboAuthListener {
        @Override
        public void onComplete(Bundle values) {
            Log.i(TAG, "AuthListener.onComplete");
            Oauth2AccessToken accessToken = Oauth2AccessToken.parseAccessToken(values);
            if (accessToken != null && accessToken.isSessionValid()) {
                //UsersAPI userAPI=new UsersAPI(accessToken);
                //userAPI.show(Long.parseLong(accessToken.getUid()), userListener);

                String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(
                        new java.util.Date(accessToken.getExpiresTime()));
                System.out.println("uid=" + accessToken.getUid());
                System.out.println("access_token=" + accessToken.getToken());
                System.out.println("expiresTime=" + date);
                AccessTokenKeeper.writeAccessToken(mContext, accessToken);
                shareToWeibo();
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            Log.e(TAG, "AuthListener.onWeiboException:" + e.getMessage());
        }

        @Override
        public void onCancel() {
            Log.e(TAG, "AuthListener.onCancel");
        }
    }

    private RequestListener userListener = new RequestListener() {

        @Override
        public void onWeiboException(WeiboException e) {
            Log.e(TAG, "userListener.onWeiboException:" + e.getMessage());
        }

        @Override
        public void onComplete(String s) {
            Log.i(TAG, "userListener.onComplete:" + s);
            if (!TextUtils.isEmpty(s)) {
                // 调用 User#parse 将JSON串解析成User对象
                User user = User.parse(s);

            }
        }
    };

    /**
     * QQ分享回调
     */
    private class BaseUiListener implements IUiListener {

        @Override
        public void onComplete(Object response) {
            if (null == response) {
                new CommonDialog(getActivity(), "温馨提示", "分享失败", "确定").show();
                return;
            }
            JSONObject jsonResponse = (JSONObject) response;
            if (null != jsonResponse && jsonResponse.length() == 0) {
                new CommonDialog(getActivity(), "温馨提示", "分享失败", "确定").show();
                return;
            }
            new CommonDialog(getActivity(), "温馨提示", "分享成功", "确定").show();
            doComplete((JSONObject) response);
        }

        protected void doComplete(JSONObject values) {

        }

        @Override
        public void onError(UiError e) {
            new CommonDialog(getActivity(), "温馨提示", "分享失败，" + e.errorDetail, "确定").show();
        }

        @Override
        public void onCancel() {
            Log.i("BaseUiListener", "onCancel: ");
        }
    }

    private boolean isPkgInstalled(String pkgName) {
        PackageInfo packageInfo;
        try {
            packageInfo = getActivity().getPackageManager().getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
        }
        return packageInfo != null;
    }

}