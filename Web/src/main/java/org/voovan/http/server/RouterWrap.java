package org.voovan.http.server;

import org.voovan.tools.TString;

import java.util.regex.Pattern;

/**
 * 路由信息包裹类
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
class RouterWrap<T> {
    protected String    method;
    protected String    regexPath;
    protected String    routePath;
    protected Boolean   hasPathParam;
    protected String    compareRoutePath;
    protected String    routePathMathchRegex;
    protected String[]  paramNames;
    protected T router;

    public RouterWrap(String method, String routePath, T router) {
        this.method         = method;
        this.regexPath      = HttpDispatcher.routePath2RegexPath(routePath);
        this.routePath      = compareRoutePath = routePath;
        this.paramNames     = TString.searchByRegex(routePath, ":[^:?/]*");
        this.hasPathParam   = paramNames.length > 0;
        this.router         = router;

        this.routePathMathchRegex = routePath;
        for (int i = 0; i < paramNames.length; i++) {
            paramNames[i] = TString.removePrefix(paramNames[i]);
            String name = paramNames[i];
            //拼装通过命名抽取数据的正则表达式
            routePathMathchRegex = routePathMathchRegex.replace(":" + name, "(?<" + name + ">.*)");
        }

        compareRoutePath   = compareRoutePath.charAt(compareRoutePath.length()-1)=='*' ? TString.removeSuffix(compareRoutePath) : compareRoutePath;
        compareRoutePath   = compareRoutePath.charAt(compareRoutePath.length()-1)=='/' ? TString.removeSuffix(compareRoutePath) : compareRoutePath;
    }

    public String[] getParamNames() {
        return paramNames;
    }

    public String getRoutePathMathchRegex() {
        return routePathMathchRegex;
    }

    public String getCompareRoutePath() {
        return compareRoutePath;
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

    public Boolean getHasPathParam() {
        return hasPathParam;
    }

    public T getRouter() {
        return router;
    }
}
