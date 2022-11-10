package org.voovan.http.server;

import org.voovan.http.HttpRequestType;

/**
 * HttpSession状态类
 *
 * @author helyho
 * voovan-framework Framework.
 * WebSite: https://github.com/helyho/voovan-framework
 * Licence: Apache v2 License
 */
public class HttpSessionState {
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;
    private HttpRequestType type;
    private boolean isKeepAlive = false;
    private long keepAliveTimeout = -1;

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    public void setHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public HttpRequestType getType() {
        return type;
    }

    public void setType(HttpRequestType type) {
        this.type = type;
    }

    public boolean isHttp() {
        return type == HttpRequestType.HTTP;
    }

    public boolean isUpgrade() {
        return type == HttpRequestType.UPGRADE;
    }

    public boolean isWebSocket() {
        return type == HttpRequestType.WEBSOCKET;
    }

    public boolean isKeepAlive() {
        return isKeepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        isKeepAlive = keepAlive;
    }

    public long getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(long keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }
}
