package org.voovan.test.http;

import java.util.Map.Entry;

import org.voovan.http.server.HttpBizFilter;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.WebServerConfig.FilterConfig;
import org.voovan.tools.log.Logger;

public class HttpBizFilterTest implements HttpBizFilter {

	@Override
	public void doFilter(FilterConfig filterConfig, HttpRequest request, HttpResponse response) {
		String msg = "["+filterConfig.getName()+"] ";
		for(Entry<String, Object> entry : filterConfig.getParameters().entrySet()){
			msg+=entry.getKey()+" = "+entry.getValue()+" ";
		}
		Logger.simple(msg);
	}

}
