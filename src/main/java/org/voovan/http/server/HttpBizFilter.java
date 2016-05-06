package org.voovan.http.server;

import org.voovan.http.server.FilterConfig;

/**
 * Http 服务过滤器接口
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface HttpBizFilter {
	/**
	 * 请求过滤器,在请求之前
	 * @param filterConfig 过滤器配置对象
	 * @param request  请求对象
	 * @param response 响应对象
	 * @param prevFilterResult 上一个过滤器的结果,可用来传递控制过滤器
     */
	public Object onRequest(FilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult );

	/**
	 * 响应过滤器,在响应之后
	 * @param filterConfig 过滤器配置对象
	 * @param request  请求对象
	 * @param response 响应对象
	 * @param prevFilterResult 上一个过滤器的结果,可用来传递控制过滤器
     */
	public Object onResponse(FilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult);
}
