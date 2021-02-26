package org.voovan.http.server.filter;

import org.voovan.http.server.HttpFilter;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

import java.util.List;

/**
 * 根据 Url 限定 ip 访问的过滤器
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class UrlLimitIpFilter implements HttpFilter {
    @Override
    public Object onRequest(HttpFilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult) {
        List<String> ipList = (List<String>) filterConfig.getParameter("IpList");
        List<String> urlList= (List<String>) filterConfig.getParameter("UrlList");
        Boolean invalidClose = (Boolean) TObject.nullDefault(filterConfig.getParameter("invalidClose"), true);

        //TODO: 未授权的 ip 访问加入黑名单,一段时间不允许访问
        String remoteAddress = request.getSocketSession().remoteAddress();
        String path = request.protocol().getPath();

        //检查地址
        boolean matchUrl = false;
        for (String url : urlList) {
            if (path.toLowerCase().startsWith(url.toLowerCase() )) {
                matchUrl = true;
                break;
            }
        }

        //检查 ip
        boolean matchIp = false;
        if(matchUrl) {
            for (String address : ipList) {
                if (remoteAddress.startsWith(address)) {
                    matchIp = true;
                }
            }
        }


        if(matchUrl) {
            if(matchIp) {
                return true;
            } else {
                if (invalidClose) {
                    Logger.warn("Deny access: [url=" + path+ ", ip="+remoteAddress+"]");
                    request.getSession().close();
                }
                return null;
            }
        } else {
            return true;
        }
    }

    @Override
    public Object onResponse(HttpFilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult) {
        return null;
    }
}
