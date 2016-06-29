package org.voovan.test.http;


import junit.framework.TestCase;
import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.http.message.packet.Part;
import org.voovan.tools.log.Logger;

/**
 * 测试用例中的方法需要单独执行
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpClientUnit extends TestCase {

	public HttpClientUnit(String name){
		super(name);
	}

	public void testGetHeader() {
		HttpClient httpClient = new HttpClient("http://127.0.0.1:28080");
		httpClient.putParameters("name", "测试");
		assertEquals(httpClient.getHeader().get("Host"),"127.0.0.1");
		httpClient.close();
	}

	public void testParameters() {
		HttpClient httpClient = new HttpClient("http://127.0.0.1:28080");
		httpClient.putParameters("name", "测试");
		assertEquals(httpClient.getParameters().get("name"), "测试");
		httpClient.close();
	}
	
	public void testGet() throws Exception{
		HttpClient getClient = new HttpClient("http://127.0.0.1:28080","GB2312");
		Response response  = getClient.setMethod("GET")
			.putParameters("name", "测试Get")
			.putParameters("age", "32").send();
		Logger.simple(response.body().getBodyString("GB2312"));
		assertTrue(response.protocol().getStatus()!=500);
		getClient.close();
	}

	public void testPost() throws Exception {
		HttpClient postClient = new HttpClient("http://127.0.0.1:28080","GB2312");
		Response response = postClient.setMethod("POST") 
			.putParameters("name", "测试Post")
			.putParameters("age", "32").send();
		Logger.simple(response.body().getBodyString("GB2312"));
		assertTrue(response.protocol().getStatus() != 500);
		postClient.close();
	}
	
	public void testMultiPart() throws Exception {
		HttpClient mpClient = new HttpClient("http://127.0.0.1:28080");
		Response response = mpClient.setMethod("POST")
			.addPart(new Part("name","测试MultiPart","GB2312"))
			.addPart(new Part("age","23","GB2312")).send();
		
		Logger.simple(response.body().getBodyString("GB2312"));
		assertTrue(response.protocol().getStatus()!=500);
		mpClient.close();
	}
	
	public void testSeriesRequest() throws Exception {
		HttpClient httpClient = new HttpClient("http://www.oschina.net/","GBK2312",10000);
		Logger.simple(httpClient.send("/").body().getBodyString());
		Logger.simple("=========================================");
		Logger.simple(httpClient.send("/").body().getBodyString());
		httpClient.close();
	}
}
