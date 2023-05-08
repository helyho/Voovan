package org.voovan.http.server.module.monitor;

import org.voovan.http.server.HttpFilter;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.tools.FastThreadLocal;


/**
 * 监控用过滤器
 */
public class HttpMonitorFilter implements HttpFilter {
	private final static FastThreadLocal<Long> REQUEST_START_TIME = new FastThreadLocal<Long>();

	@Override
	public Object onRequest(HttpFilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult ) {
		REQUEST_START_TIME.set(System.currentTimeMillis());
		return "";
	}

	@Override
	public Object onResponse(HttpFilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult ) {
		//处理请求分析数据
		requestAnalysis(request);

		//处理IP数据分析
		ipAddressAnalysis(request);

		return "";
	}

	/**
	 * 请求数据的分析
	 * @param request 请求对象
	 */
	public void requestAnalysis(HttpRequest request){
		Long startTime = REQUEST_START_TIME.get();

		if(startTime!=null) {
			String requestPath = request.protocol().getPath();
			long dealTime = (System.currentTimeMillis() - startTime);

			RequestAnalysis requestAnalysis = null;
			if (MonitorGlobal.REQUEST_ANALYSIS.containsKey(requestPath)) {
				requestAnalysis = MonitorGlobal.REQUEST_ANALYSIS.get(requestPath);
			} else {
				requestAnalysis = new RequestAnalysis(requestPath);
				MonitorGlobal.REQUEST_ANALYSIS.put(requestPath, requestAnalysis);
			}

			requestAnalysis.addRequestTime(Long.valueOf(dealTime).intValue());
		}
		REQUEST_START_TIME.set(null);
	}


	/**
	 * ip 数据的分析
	 * @param request Http请求对象
	 */
	public void ipAddressAnalysis(HttpRequest request){
		String requestPath = request.protocol().getPath();
		String ipAddress = request.getRemoteAddress();

		if(ipAddress==null){
			ipAddress = "unknown or disconnect";
		}

		IPAnalysis ipAnalysis = null;
		if(MonitorGlobal.IP_ANALYSIS.containsKey(ipAddress)){
			ipAnalysis = MonitorGlobal.IP_ANALYSIS.get(ipAddress);
		} else {
			ipAnalysis = new IPAnalysis(ipAddress);
			MonitorGlobal.IP_ANALYSIS.put(ipAddress, ipAnalysis);
		}

		ipAnalysis.addRequest(requestPath);
	}
}
