package org.voovan.test.http;


import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.tools.log.Logger;

import junit.framework.TestCase;

public class HttpClientUnit extends TestCase {

	HttpClient httpClient;
	Response response;
	public HttpClientUnit(String name){
		super(name);
		httpClient = new HttpClient("http://www.baidu.com/s");
		httpClient.putParameters("wd", "测试");
	}

	public void testHttpClient(){
		assertNotNull(httpClient);
	}

	public void testGetHeader() {
		assertEquals(httpClient.getHeader().get("Host"),"www.baidu.com");
	}

	public void testParameters() {
		assertEquals(httpClient.getParameters().get("wd"), "测试");
	}
	
	public void testConnect() throws Exception{
		response = httpClient.Connect();
		Logger.simple("Response body Length: "+ response.body().getBodyBytes().length+"\r\n"+response.body().toString());
		assertTrue(response.header().size()>5);
	}
}
