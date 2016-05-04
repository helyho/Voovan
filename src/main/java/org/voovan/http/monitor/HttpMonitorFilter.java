package org.voovan.http.monitor;

import org.voovan.http.server.HttpBizFilter;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.WebServerConfig.FilterConfig;
import org.voovan.tools.TEnv;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控用过滤器
 */
public class HttpMonitorFilter implements HttpBizFilter {
	private static Map<String,RequestAnalysis> requestInfos= new HashMap<String,RequestAnalysis>();

	/**
	 * 获取请求分析对象
	 * @return 返回的请求信息
     */
	public static Map<String, RequestAnalysis> getRequestInfos() {
		return requestInfos;
	}

	@Override
	public void onRequest(FilterConfig filterConfig, HttpRequest request, HttpResponse response) {
		request.getSession().setAttribute("VOOVAN_REQSTART",System.currentTimeMillis());
		TEnv.sleep(20);
	}

	@Override
	public void onResponse(FilterConfig filterConfig, HttpRequest request, HttpResponse response) {
		Long startTime = (Long)request.getSession().getAttributes("VOOVAN_REQSTART");
		if(startTime!=null) {
			long dealTime = (System.currentTimeMillis() - startTime);
			String path = request.protocol().getPath();
			if (requestInfos.containsKey(path)) {
				requestInfos.get(path).add(dealTime);
			} else {
				RequestAnalysis requestAnalysis = new RequestAnalysis(path);
				requestAnalysis.add(dealTime);
				requestInfos.put(path, requestAnalysis);
			}
		}
	}
}
