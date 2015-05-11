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
	public void doFilter(FilterConfig filterConfig,HttpRequest request,HttpResponse response);
}
