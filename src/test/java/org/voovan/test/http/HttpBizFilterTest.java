package org.voovan.test.http;

import org.voovan.http.server.HttpBizFilter;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.WebServerConfig.FilterConfig;
import org.voovan.tools.log.Logger;

import java.util.Map.Entry;

public class HttpBizFilterTest implements HttpBizFilter {

	@Override
	public void onRequest(FilterConfig filterConfig, HttpRequest request, HttpResponse response) {
		String msg = "["+filterConfig.getName()+"] ";
		for(Entry<String, Object> entry : filterConfig.getParameters().entrySet()){
			msg+=entry.getKey()+" = "+entry.getValue()+" " + request.protocol().getPath();
		}
		Logger.simple("ON_REQUEST: "+msg);
	}

	@Override
	public void onResponse(FilterConfig filterConfig, HttpRequest request, HttpResponse response) {
		String msg = "["+filterConfig.getName()+"] ";
		for(Entry<String, Object> entry : filterConfig.getParameters().entrySet()){
			msg+=entry.getKey()+" = "+entry.getValue()+" " + request.protocol().getPath();
		}
		Logger.simple("ON_RESPONSE: "+msg);
	}

}
