package org.voovan.test.http;

import org.voovan.Global;
import org.voovan.http.message.HttpStatic;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;
import org.voovan.http.server.WebServer;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.http.websocket.filter.StringFilter;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.tcp.TcpServerSocket;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;


public class WebServerDemo {
	private static byte[] fileContent = TFile.loadFileFromContextPath("WEBAPP/index.htm");

	public static void main(String[] args) {
		WebServer webServer = WebServer.newInstance(args);
		webServer.getWebServerConfig().setCache(true);
        //性能测试请求
		webServer.get("/test", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.header().put(HttpStatic.CONTENT_TYPE_STRING, HttpStatic.TEXT_HTML_STRING);
				resp.write("OK");
			}
		});

		webServer.get("/restart", new HttpRouter() {
			@Override
			public void process(HttpRequest request, HttpResponse response) throws Exception {
				((TcpServerSocket)webServer.getServerSocket()).restart();
				System.out.println("restart ....");
			}
		});

		webServer.post("/test", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.write("OK");
			}
		});

		//性能测试请求
		webServer.post("/upload", new HttpRouter(){
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				try {
					req.saveUploadedFile("file", "./" + req.getParameter("file"));
					resp.write("Success");
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		});

		//性能测试请求
		webServer.get("/async", new HttpRouter(){
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				HttpResponse asyncResponse = resp.getAsyncResponse();
				Global.async(()->{
					TEnv.sleep(100);
					asyncResponse.write("async response");
					try {
						asyncResponse.send();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
		});

		//普通 GET 请求
		webServer.get("/",  new HttpRouter(){
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
				Logger.simple("Request info: " + req.protocol());
				//Session 测试
				{
					String now = TDateTime.now();
					if (req.getSession() != null && req.getSession().getAttribute("Time") != null) {
						Logger.simple("Session "+ req.getSession().getId() +" saved time is: " + req.getSession().getAttribute("Time") + " SavedTime: " + now);
					}
					req.getSession().setAttribute("Time", now);
				}
				resp.write(fileContent);
				resp.write("{"
						+ "\"Method\":\"NormalGET\","
						+ "\"name\":\"" + req.getParameter("name") + "\","
						+ "\"age\":\"" + req.getParameter("age") + "\""
						+ "}");
			}
		});

		//带路径参数的 GET 请求
		webServer.get("/Star/:name/:age",  new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
				Logger.simple("Request info: " + req.protocol());
				resp.write(fileContent);
				resp.write("{"
						+ "\"Method\":\"PathGET\","
						+ "\"name\":\"" + req.getParameter("name") + "\","
						+ "\"age\":\"" + req.getParameter("age") + "\""
						+ "}");
			}
		});

		//路径模糊匹配
		webServer.get("/test/t*t/kkk/*",  new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
				Logger.simple("Request info: " + req.protocol());
				resp.write(fileContent);
				resp.write("{"
						+ "\"Method\":\"FuzzyMatching\""
						+ "}");
			}
		});


		// 重定向
		webServer.get("/redirect",  new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
				Logger.simple("Request info: " + req.protocol());
				resp.redirct("http://www.baidu.com");
			}
		});

		//普通 POST 请求
		webServer.post("/",  new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
				Logger.simple("Request info: " + req.protocol());
				resp.write(fileContent);
				String contentType = req.header().get("Content-Type").split(";")[0];
				resp.write("{"
						+ "\"Method\":\"" + contentType + "\","
						+ "\"name\":\"" + req.getParameter("name") + "\","
						+ "\"age\":\"" + req.getParameter("age") + "\""
						+ "}");
			}
		});

		//自定义方法测试
		webServer.otherMethod("LOCK","/:test", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.body().write("User Defined HTTP method is " + req.protocol().getMethod());
				Logger.simple("Query");
			}
		});

		webServer.socket("/websocket", new WebSocketRouter() {

			@Override
			public Object onOpen(WebSocketSession webSocketSession) {
				Logger.info("onOpen: WebSocket connect!");

				//调用发送函数发送
				try {

					String mm= new String("opened");
					webSocketSession.send(mm);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return "Server writeToChannel: onOpen";
			}

			@Override
			public Object onRecived(WebSocketSession webSocketSession, Object obj) {
				String msg = (String)obj;
				Logger.info("onRecive: "+msg);
				msg = "This is server message. Client message: \r\n\t\""+msg+"\"";

				//调用发送函数发送
				try {
					webSocketSession.send("Send by writeToChannel method in onRecive");
				} catch (SendMessageException | WebSocketFilterException e) {
					e.printStackTrace();
				}
				return msg;
			}

			@Override
			public void onSent(WebSocketSession webSocketSession, Object message) {
				Logger.info("----> onSend: " + message);
			}

			@Override
			public void onClose(WebSocketSession webSocketSession) {
				Logger.info("WebSocket close!");
			}
		}.addFilterChain(new StringFilter()));

//		Global.schedual(()->{
//			((TcpServerSocket)webServer.getServerSocket()).restart();;
//			System.out.println("restart ....");
//		}, 3);

		webServer.serve();
	}
}
