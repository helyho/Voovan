package org.voovan.http.server.filter;

import org.voovan.http.server.HttpFilter;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.context.HttpFilterConfig;

/**
 * 支持 JSONP 的过滤方式
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class JsonpFilter implements HttpFilter{

    @Override
    public Object onRequest(HttpFilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult) {
        return true;
    }

    @Override
    public Object onResponse(HttpFilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult) {
        String functionParamName = (String) filterConfig.getParameter("functionParamName");
        String functionName = request.getParameter(functionParamName);
        if(functionName!=null){
            String jsonpResponse = functionName+"(" + response.body().getBodyString()+")";
            response.clear();
            response.body().write(jsonpResponse.getBytes());
        }
        return true;
    }
}
