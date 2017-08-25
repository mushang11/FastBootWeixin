package com.mxixm.fastboot.weixin.config.token;

import com.mxixm.fastboot.weixin.config.invoker.WxUrlProperties;
import com.mxixm.fastboot.weixin.controller.invoker.executor.WxApiInvoker;
import com.mxixm.fastboot.weixin.config.invoker.WxVerifyProperties;
import com.mxixm.fastboot.weixin.exception.WxAccessTokenException;
import com.mxixm.fastboot.weixin.exception.WxAppException;
import com.mxixm.fastboot.weixin.module.token.WxAccessToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxixm.fastboot.weixin.module.user.WxUser;
import com.mxixm.fastboot.weixin.web.WxWebUser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * FastBootWeixin  WxTokenServer
 * 注意拦截调用异常，如果是token过期，重新获取token并重试
 *
 * @author Guangshan
 * @summary FastBootWeixin  WxTokenServer
 * @Copyright (c) 2017, Guangshan Group All Rights Reserved
 * @since 2017/7/23 17:14
 */
public class WxTokenServer {

    private static final Log logger = LogFactory.getLog(MethodHandles.lookup().lookupClass());

    private WxApiInvoker wxApiInvoker;

    private WxVerifyProperties wxVerifyProperties;

    private WxUrlProperties wxUrlProperties;

    private final ObjectMapper jsonConverter = new ObjectMapper();

    public WxTokenServer(WxApiInvoker wxApiInvoker, WxVerifyProperties wxVerifyProperties, WxUrlProperties wxUrlProperties) {
        this.wxApiInvoker = wxApiInvoker;
        this.wxVerifyProperties = wxVerifyProperties;
        this.wxUrlProperties = wxUrlProperties;
    }

    public WxAccessToken refreshToken() {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme("https").host(wxUrlProperties.getHost()).path(wxUrlProperties.getRefreshToken())
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", wxVerifyProperties.getAppid())
                .queryParam("secret", wxVerifyProperties.getAppsecret());
        String result = wxApiInvoker.getForObject(builder.toUriString(), String.class);
        if (WxAccessTokenException.hasException(result)) {
            throw new WxAccessTokenException(result);
        } else {
            try {
                return jsonConverter.readValue(result, WxAccessToken.class);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new WxAppException("获取Token时转换Json失败");
            }
        }
    }

    public WxWebUser getWxWebUserByCode(String code) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme("https").host(wxUrlProperties.getHost()).path(wxUrlProperties.getGetUserAccessTokenByCode())
                .queryParam("grant_type", "authorization_code")
                .queryParam("appid", wxVerifyProperties.getAppid())
                .queryParam("secret", wxVerifyProperties.getAppsecret())
                .queryParam("code", code);
        return getWxWebUserByBuilder(builder);
    }

    public WxWebUser getWxWebUserByRefreshToken(String refreshToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme("https").host(wxUrlProperties.getHost()).path(wxUrlProperties.getGetUserAccessTokenByCode())
                .queryParam("grant_type", "authorization_code")
                .queryParam("appid", wxVerifyProperties.getAppid())
                .queryParam("secret", wxVerifyProperties.getAppsecret())
                .queryParam("refresh_token", refreshToken);
        return getWxWebUserByBuilder(builder);
    }

    private WxWebUser getWxWebUserByBuilder(UriComponentsBuilder builder) {
        String result = wxApiInvoker.getForObject(builder.toUriString(), String.class);
        if (WxAccessTokenException.hasException(result)) {
            throw new WxAccessTokenException(result);
        } else {
            try {
                return jsonConverter.readValue(result, WxWebUser.class);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new WxAppException("获取Token时转换Json失败");
            }
        }
    }

    public WxUser getWxUserByWxWebUser(WxWebUser wxWebUser) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme("https").host(wxUrlProperties.getHost()).path(wxUrlProperties.getGetUserAccessTokenByCode())
                .queryParam("access_token", wxWebUser.getAccessToken())
                .queryParam("openid", wxWebUser.getOpenId())
                .queryParam("lang", "zh_CN");
        String result = wxApiInvoker.getForObject(builder.toUriString(), String.class);
        if (WxAccessTokenException.hasException(result)) {
            throw new WxAccessTokenException(result);
        } else {
            try {
                return jsonConverter.readValue(result, WxUser.class);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new WxAppException("获取Token时转换Json失败");
            }
        }
    }

    public boolean isVerifyUserAccessToken(WxWebUser wxWebUser) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme("https").host(wxUrlProperties.getHost()).path(wxUrlProperties.getGetUserAccessTokenByCode())
                .queryParam("access_token", wxWebUser.getAccessToken())
                .queryParam("openid", wxWebUser.getOpenId());
        String result = wxApiInvoker.getForObject(builder.toUriString(), String.class);
        if (WxAccessTokenException.hasException(result)) {
            return false;
        } else {
            return true;
        }
    }

}