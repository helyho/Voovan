package org.voovan.http.server;

import org.voovan.tools.TString;

/**
 * 路由信息包裹类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
class RouterWrap<T> {
    protected String method;
    protected String regexPath;
    protected String routePath;
    protected Boolean hasUrlParam;
    protected T router;

    public RouterWrap(String method, String routePath, T router) {
        this.method = method;
        this.regexPath = HttpDispatcher.routePath2RegexPath(routePath);
        this.routePath = routePath;
        this.hasUrlParam = TString.searchByRegex(routePath,":[^:?/]*").length > 0;
        this.router = router;
    }

    public String getMethod() {
        return method;
    }

    public String getRegexPath() {
        return regexPath;
    }

    public String getRoutePath() {
        return routePath;
    }

    public Boolean getHasUrlParam() {
        return hasUrlParam;
    }

    public T getRouter() {
        return router;
    }
}
