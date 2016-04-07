package org.voovan.http.server;

import org.voovan.http.server.WebServerConfig.FilterConfig;

/**
 * Http 服务过滤器接口
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface HttpBizFilter {
	public void onRequest(FilterConfig filterConfig, HttpRequest request, HttpResponse response);
	public void onResponse(FilterConfig filterConfig, HttpRequest request, HttpResponse response);
}
