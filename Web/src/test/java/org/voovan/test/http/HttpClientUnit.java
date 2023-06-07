package org.voovan.test.http;


import junit.framework.TestCase;
import org.junit.Test;
import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.http.message.packet.Part;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.filter.StringFilter;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
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
	static {
//		System.getProperties().put("socksProxySet","true");
//		System.getProperties().put("socksProxyHost","127.0.0.1");
//		System.getProperties().put("socksProxyPort","1080");
//
//		System.getProperties().setProperty("proxySet", "true");
//		System.getProperties().setProperty("http.proxyHost", "127.0.0.1");
//		System.getProperties().setProperty("http.proxyPort", "1087");
	}

	public HttpClientUnit(String name){
		super(name);
	}

	@Test
	public void testGetHeader() throws IOException {
		HttpClient httpClient = new HttpClient("http://webserver.voovan.org");
		httpClient.putParameter("name", "测试");
		assertEquals(httpClient.getHeader().get("Host"),"webserver.voovan.org");
		httpClient.close();
	}

	public void testParameters() throws IOException {
		HttpClient httpClient = new HttpClient("http://webserver.voovan.org");
		httpClient.putParameter("name", "测试");
		assertEquals(httpClient.getParameters().get("name"), "测试");
		httpClient.close();
	}

	public void testGet() throws Exception{
		HttpClient getClient = new HttpClient("http://127.0.0.1:28080/","GB2312", 60);
		Response response  = getClient.setMethod("GET")
				.putParameter("name", "测试Get")
				.putParameter("age", "32").send();
		System.out.println(response.body().getBodyString("GB2312"));
		assertTrue(response.protocol().getStatus()!=500);
		getClient.close();
	}

	public void testAsync() throws Exception{
		HttpClient getClient = new HttpClient("http://webserver.voovan.org","GB2312", 60);
		getClient.setMethod("GET")
				.putParameter("name", "测试Get")
				.putParameter("age", "32").asyncSend(resp->{
			System.out.println(resp.body().getBodyString());
		});
		TEnv.sleep(3000);
		getClient.setMethod("GET")
				.putParameter("name", "测试Get")
				.putParameter("age", "42").asyncSend(resp->{
			System.out.println(resp.body().getBodyString());
		});
		TEnv.sleep(3000);
		getClient.close();

	}

	public void testPost() throws Exception {
		HttpClient postClient = new HttpClient("http://127.0.0.1:28080/test",60);
		Response response = postClient.setMethod("POST")
				.putParameter("name", "测试Get")
				.putParameter("age", "32")
				.send();
		new Response().copyFrom(response);
		System.out.println(response.body().getBodyString());
		assertTrue(response.protocol().getStatus() != 500);
		postClient.close();
	}

	public void testMultiPart() throws Exception {
		HttpClient mpClient = new HttpClient("http://webserver.voovan.org", 60);
		Response response = mpClient.setMethod("POST")
			.addPart(new Part("name","测试MultiPart","GB2312"))
			.addPart(new Part("age","23","GB2312")).send();

		System.out.println(response.body().getBodyString("GB2312"));
		assertTrue(response.protocol().getStatus()!=500);
		mpClient.close();
	}

	public void testFileUpload() throws Exception {
		HttpClient ulClient = new HttpClient("http://webserver.voovan.org", 60);
		ulClient.setMethod("POST");
		ulClient.addPart(new Part("name","测试Upload","GB2312"));
		ulClient.uploadFile("file",new File("./pom.xml"));
		Response response = ulClient.send("/upload");
		System.out.println(response.body().getBodyString("GB2312"));
		assertTrue(response.protocol().getStatus()!=500);
		ulClient.close();
	}

	public void testHTTPSRequest() throws Exception {
		HttpClient httpClient = new HttpClient("https://www.baidu.com","UTF-8", 600000);
		try {
			httpClient.putHeader("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");
			TEnv.measure("111", ()->{
				try {
					System.out.println(httpClient.send("/").body().getBodyString().length());
				} catch (SendMessageException e) {
					e.printStackTrace();
				} catch (ReadMessageException e) {
					e.printStackTrace();
				}
			});

			TEnv.measure("111", ()->{
				try {
					System.out.println(httpClient.send("/").body().getBodyString().length());
				} catch (SendMessageException e) {
					e.printStackTrace();
				} catch (ReadMessageException e) {
					e.printStackTrace();
				}
			});

			long ss = System.currentTimeMillis();
			httpClient.asyncSend(response -> {
				System.out.println("asss: " + (System.currentTimeMillis()-ss));
				System.out.println(response.body().getBodyString().length());

			});

			TEnv.sleep(10000);
		} catch (Exception e) {
			e.printStackTrace();
		}  finally {
			httpClient.close();
		}


	}

	public void testSeriesRequest() throws Exception {
		HttpClient httpClient = new HttpClient("http://webserver.voovan.org","GBK2312",10000);
		System.out.println(httpClient.send("/").body().getBodyString());
		System.out.println("1=========================================");
		System.out.println(httpClient.send("/").body().getBodyString());
		System.out.println("2=========================================");
		System.out.println(httpClient.send("/").body().getBodyString());
		System.out.println("3=========================================");
		System.out.println(httpClient.send("/").body().getBodyString());
		TEnv.sleep(10);
		httpClient.close();
	}

	public static  WebSocketSession session ;

	public void testWebSocket() throws Exception {
		HttpClient httpClient = new HttpClient("ws://webserver.voovan.org/websocket","GBK2312",60);

		httpClient.webSocket("/websocket", new WebSocketRouter() {

			@Override
			public Object onOpen(WebSocketSession webSocketSession) {
				System.out.println("WebSocket open");
				HttpClientUnit.session = webSocketSession;
				return "OPEN_MSG";
			}

			int count = 0;
			@Override
			public Object onRecived(WebSocketSession webSocketSession, Object message) {
				System.out.println("Recive: "+message);

				if(count==0){
					count++;
					return "RECIVE_MSG";
				}

//				if(((String)message).contains("RECIVE_MSG")){
//					webSocketSession.close();
//				}

				return null;
			}

			@Override
			public void onSent(WebSocketSession webSocketSession, Object message){
				System.out.println("Send: " + message);
			}

			@Override
			public void onClose(WebSocketSession webSocketSession) {
				System.out.println("WebSocket close");
			}
		}.addFilterChain(new StringFilter()));

		TEnv.sleep(5000);

		HttpClientUnit.session.close();

		TEnv.sleep(3000);
	}


	public void testMulGet() throws Exception{
		HttpClient getClient = new HttpClient("http://webserver.voovan.org","GB2312", 5);
		for(int i=0;i<20;i++) {
			System.out.println(i);
			Response response = getClient.setMethod("GET").send("/ar/annon");
			System.out.println(response.body().getBodyString("GB2312"));
			assertTrue(response.protocol().getStatus() != 500);
			TEnv.sleep(100);
		}
		getClient.close();
	}

	public void testMulPost() throws Exception{
		HttpClient getClient = new HttpClient("http://webserver.voovan.org","GB2312", 5);
		for(int i=0;i<20;i++) {
			System.out.println(i);
			Response response = getClient.setMethod("POST").setData("this is post body").send("/ar/annon/body");
			System.out.println(response.body().getBodyString("GB2312"));
			assertTrue(response.protocol().getStatus() != 500);
			TEnv.sleep(100);
		}
		getClient.close();
	}
}
