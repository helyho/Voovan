package org.voovan.test.http;

import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.tools.log.Logger;

public class HttpClientTest {
	
	public static void main(String[] args) throws Exception {
		HttpClient client = new HttpClient("http://www.sohu.com/");
		client.setMethod("GET");
		long t = System.currentTimeMillis();
		Logger.simple("start :"+System.currentTimeMillis());
		Response response = client.Connect();
		Logger.simple("====");
		Logger.simple(System.currentTimeMillis()-t);
		
		String bodyString = response.body().toString();
		Logger.debug("body length:"+bodyString.length());
	}
}
