package org.voovan.test.http;


import junit.framework.TestCase;
import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.http.message.packet.Part;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.filter.StringFilter;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;

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

	public void testGetHeader() throws IOException {
		HttpClient httpClient = new HttpClient("http://127.0.0.1:28080");
		httpClient.putParameters("name", "测试");
		assertEquals(httpClient.getHeader().get("Host"),"127.0.0.1");
		httpClient.close();
	}

	public void testParameters() throws IOException {
		HttpClient httpClient = new HttpClient("http://127.0.0.1:28080");
		httpClient.putParameters("name", "测试");
		assertEquals(httpClient.getParameters().get("name"), "测试");
		httpClient.close();
	}
	
	public void testGet() throws Exception{
		HttpClient getClient = new HttpClient("http://127.0.0.1:28080","GB2312", 60);
		Response response  = getClient.setMethod("GET")
			.putParameters("name", "测试Get")
			.putParameters("age", "32").send();
		Logger.simple(response.body().getBodyString("GB2312"));
		assertTrue(response.protocol().getStatus()!=500);
		getClient.close();
	}

	public void testPost() throws Exception {
		HttpClient postClient = new HttpClient("http://127.0.0.1:28080","GB2312",60);
		Response response = postClient.setMethod("POST") 
			.putParameters("name", "测试Post")
			.putParameters("age", "32").send();
		Logger.simple(response.body().getBodyString("GB2312"));
		assertTrue(response.protocol().getStatus() != 500);
		postClient.close();
	}
	
	public void testMultiPart() throws Exception {
		HttpClient mpClient = new HttpClient("http://127.0.0.1:28080", 60);
		Response response = mpClient.setMethod("POST")
			.addPart(new Part("name","测试MultiPart","GB2312"))
			.addPart(new Part("age","23","GB2312")).send();
		
		Logger.simple(response.body().getBodyString("GB2312"));
		assertTrue(response.protocol().getStatus()!=500);
		mpClient.close();
	}

	public void testFileUpload() throws Exception {
		HttpClient ulClient = new HttpClient("http://127.0.0.1:28080", 60);
		ulClient.setMethod("POST");
		ulClient.addPart(new Part("name","测试Upload","GB2312"));
		ulClient.uploadFile("file",new File("./pom.xml"));
		Response response = ulClient.send("/upload");
		Logger.simple(response.body().getBodyString("GB2312"));
		assertTrue(response.protocol().getStatus()!=500);
		ulClient.close();
	}

	public void testHTTPSRequest() throws Exception {
		HttpClient httpClient = new HttpClient("https://www.oschina.net/","UTF-8", 60);
		httpClient.putHeader("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");
		System.out.println(httpClient.send("/").body().getBodyString());
		httpClient.close();
	}
	
	public void testSeriesRequest() throws Exception {
		HttpClient httpClient = new HttpClient("http://127.0.0.1:28080","GBK2312",10000);
		Logger.simple(httpClient.send("/").body().getBodyString());
		Logger.simple("=========================================");
		Logger.simple(httpClient.send("/").body().getBodyString());
		Logger.simple("=========================================");
		Logger.simple(httpClient.send("/").body().getBodyString());
		Logger.simple("=========================================");
		Logger.simple(httpClient.send("/").body().getBodyString());
		httpClient.close();
	}

	public void testWebSocket() throws Exception {
		HttpClient httpClient = new HttpClient("ws://127.0.0.1:28080/","GBK2312",60);
		httpClient.webSocket("/websocket", new WebSocketRouter() {

			@Override
			public Object onOpen(WebSocketSession webSocketSession) {
				Logger.simple("WebSocket open");
				return "OPEN_MSG";
			}

			int count = 0;
			@Override
			public Object onRecived(WebSocketSession webSocketSession, Object message) {
				Logger.simple("Recive: "+message);

				if(count==0){
					count++;
					return "RECIVE_MSG";
				}

				if(((String)message).contains("RECIVE_MSG")){
					webSocketSession.close();
				}

				return null;
			}

			@Override
			public void onSent(WebSocketSession webSocketSession, Object message){
				Logger.simple("Send: " + message);
			}

			@Override
			public void onClose(WebSocketSession webSocketSession) {
				Logger.simple("WebSocket close");
			}
		}.addFilterChain(new StringFilter()));

	}


	public void testMulGet() throws Exception{
		HttpClient getClient = new HttpClient("http://127.0.0.1:28080","GB2312", 5);
		for(int i=0;i<20;i++) {
			Logger.simple(i);
			Response response = getClient.setMethod("GET").send("/annon/index");
			Logger.simple(response.body().getBodyString("GB2312"));
			assertTrue(response.protocol().getStatus() != 500);
			TEnv.sleep(100);
		}
		getClient.close();
	}

	public void testMulPost() throws Exception{
		HttpClient getClient = new HttpClient("http://127.0.0.1:28080","GB2312", 5);
		for(int i=0;i<20;i++) {
			Logger.simple(i);
			Response response = getClient.setMethod("POST").setData("this is post body").send("/annon/body");
			Logger.simple(response.body().getBodyString("GB2312"));
			assertTrue(response.protocol().getStatus() != 500);
			TEnv.sleep(100);
		}
		getClient.close();
	}

}
