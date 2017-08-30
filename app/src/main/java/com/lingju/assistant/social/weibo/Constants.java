/*
 * Copyright (C) 2010-2013 The SINA WEIBO Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lingju.assistant.social.weibo;

/**
 * 该类定义了第三方SDK的相关常量参数。
 *
 * @author SINA
 * @since 2013-09-29
 */
public interface Constants {

    /**
     * 当前 DEMO 应用的回调页，第三方应用可以使用自己的回调页。
     * <p/>
     * <p>
     * 注：关于授权回调页对移动客户端应用来说对用户是不可见的，所以定义为何种形式都将不影响，
     * 但是没有定义将无法使用 SDK 认证登录。
     * 建议使用默认回调页：https://api.weibo.com/oauth2/default.html
     * </p>
     */
    String REDIRECT_URL = "https://api.weibo.com/oauth2/default.html";

    /**
     * Scope 是 OAuth2.0 授权机制中 authorize 接口的一个参数。通过 Scope，平台将开放更多的微博
     * 核心功能给开发者，同时也加强用户隐私保护，提升了用户体验，用户在新 OAuth2.0 授权页中有权利
     * 选择赋予应用的功能。
     * <p/>
     * 我们通过新浪微博开放平台-->管理中心-->我的应用-->接口管理处，能看到我们目前已有哪些接口的
     * 使用权限，高级权限需要进行申请。
     * <p/>
     * 目前 Scope 支持传入多个 Scope 权限，用逗号分隔。
     * <p/>
     * 有关哪些 OpenAPI 需要权限申请，请查看：http://open.weibo.com/wiki/%E5%BE%AE%E5%8D%9AAPI
     * 关于 Scope 概念及注意事项，请查看：http://open.weibo.com/wiki/Scope
     */
    public static final String SCOPE = "";
            /*"email,direct_messages_read,direct_messages_write,"
            + "friendships_groups_read,friendships_groups_write,statuses_to_me_read,"
            + "follow_app_official_microblog," + "invitation_write";*/
    /**
     * 微博appkey
     */
    String WEIBO_APPKEY = "你的appKey";

    /**
     * 微信
     **/
    String WECHAT_APPID = "你的appId";
    String WECHAT_AppSecret = "你的appSecret";

    /**
     * 腾讯qq
     **/
    String TENCENT_APPID = "你的appId";
    String TENCENT_AppSecret = "你的appSecret";

    /**
     * 讯飞语音
     **/
    String XUNFEI_APPID = "你的appId";

    /**
     * 喜马拉雅api的appsecret
     **/
    String XIMALAYA_APPKEY = "你的appKey";
    String XIMALAYA_APPSECRET = "你的appSecret";
    String XIMALAYA_REDIRECT_URL = "https://api.ximalaya.com/openapi-collector-app/get_access_token";
    /**
     * 灵聚SDK
     **/
    String LINGJU_APPKEY = "你的appKey";
}
