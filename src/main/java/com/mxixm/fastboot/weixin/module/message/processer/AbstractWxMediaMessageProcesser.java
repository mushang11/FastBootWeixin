package com.mxixm.fastboot.weixin.module.message.processer;

import com.mxixm.fastboot.weixin.module.web.WxRequest;
import com.mxixm.fastboot.weixin.module.media.WxMedia;
import com.mxixm.fastboot.weixin.module.media.WxMediaManager;
import com.mxixm.fastboot.weixin.module.message.WxMessage;
import com.mxixm.fastboot.weixin.module.message.WxMessageProcesser;
import com.mxixm.fastboot.weixin.util.WxUrlUtils;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * FastBootWeixin  WxCommonMessageProcesser
 *
 * @author Guangshan
 * @summary FastBootWeixin  WxCommonMessageProcesser
 * @Copyright (c) 2017, Guangshan Group All Rights Reserved
 * @since 2017/8/20 22:53
 */
public abstract class AbstractWxMediaMessageProcesser<T extends WxMessage> implements WxMessageProcesser<T> {

    protected WxMediaManager wxMediaManager;

    public AbstractWxMediaMessageProcesser(WxMediaManager wxMediaManager) {
        this.wxMediaManager = wxMediaManager;
    }

    protected WxMessage.MediaBody processBody(WxRequest wxRequest, WxMessage.MediaBody body) {
        if (body.getMediaId() == null) {
            // 优先使用path
            if (body.getMediaPath() != null) {
                String mediaId = wxMediaManager.addTempMedia(WxMedia.Type.IMAGE, new File(body.getMediaPath()));
                body.setMediaId(mediaId);
            } else if (body.getMediaUrl() != null) {
                String url = WxUrlUtils.mediaUrl(wxRequest.getRequestURL().toString(), body.getMediaUrl());
                String mediaId = wxMediaManager.addTempMediaByUrl(WxMedia.Type.IMAGE, url);
                body.setMediaId(mediaId);
            }
        }
        return body;
    }

    public boolean supports(WxRequest wxRequest, T wxMessage) {
        Type type = this.getClass().getGenericSuperclass();
        if (!(type instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Class userClass =(Class)(parameterizedType.getActualTypeArguments()[0]);
        if (userClass == null) {
            return false;
        }
        return userClass.isAssignableFrom(wxMessage.getClass());
    }

}
