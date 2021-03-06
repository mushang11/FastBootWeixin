package com.mxixm.fastboot.weixin.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录校验通过之后执行一些业务逻辑
 */
public interface WxOAuth2Callback {

    void after(WxOAuth2Context context) throws Exception;

    final class WxOAuth2Context {

        private WxWebUser wxWebUser;
        private String state;
        private HttpServletResponse response;
        private HttpServletRequest request;

        public WxOAuth2Context(WxWebUser wxWebUser, String state,
                               HttpServletResponse response, HttpServletRequest request) {
            super();
            this.state = state;
            this.wxWebUser = wxWebUser;
            this.response = response;
            this.request = request;
        }

        /**
         * 当前登录用户
         *
         * @return the wxWebUser
         */
        public WxWebUser getWxWebUser() {
            return wxWebUser;
        }

        public String getState() {
            return state;
        }

        /**
         * http response
         *
         * @return the response
         */
        public HttpServletResponse getResponse() {
            return response;
        }

        /**
         * http request
         *
         * @return the request
         */
        public HttpServletRequest getRequest() {
            return request;
        }
    }
}