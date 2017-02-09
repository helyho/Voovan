package org.voovan.test.http;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.WebServer;
import org.voovan.http.server.websocket.WebSocketRouter;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TFile;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;

public class WebServerDemo {
	private static byte[] fileContent = TFile.loadFileFromContextPath("WEBAPP/index.htm");
	
	public static void main(String[] args) {
		WebServer webServer = WebServer.newInstance();

		//性能测试请求
        webServer.get("/test", (req, resp) -> {
			resp.body().write("OK");
		});

		//普通 GET 请求
        webServer.get("/", (req, resp) -> {
			Logger.info("Client info: "+req.getRemoteAddres()+":"+req.getRemotePort());
			Logger.simple("Request info: "+req.protocol());
			//Session 测试
			{
				String now = TDateTime.now();
				if (req.getSession() != null && req.getSession().getAttributes("Time") != null) {
					Logger.simple("Session saved time is: " + req.getSession().getAttributes("Time")+" SavedTime: "+now);
				}
				req.getSession().setAttribute("Time", now);
			}
			resp.write(fileContent);
			resp.write("{"
					+ "\"Method\":\"NormalGET\","
					+ "\"name\":\""+req.getParameter("name")+"\","
					+ "\"age\":\""+req.getParameter("age")+"\""
			 + "}");
		});

		//带路劲参数的 GET 请求
        webServer.get("/Star/:name/:age", (req, resp) -> {
			Logger.info("Client info: "+req.getRemoteAddres()+":"+req.getRemotePort());
			Logger.simple("Request info: "+req.protocol());
			resp.write(fileContent);
			resp.write("{"
							+ "\"Method\":\"PathGET\","
							+ "\"name\":\""+req.getParameter("name")+"\","
							+ "\"age\":\""+req.getParameter("age")+"\""
					 + "}");
		});

		//带路劲参数的 GET 请求
        webServer.get("/test/t*t/kkk/*", (req, resp) -> {
			Logger.info("Client info: "+req.getRemoteAddres()+":"+req.getRemotePort());
			Logger.simple("Request info: "+req.protocol());
			resp.write(fileContent);
			resp.write("{"
							+ "\"Method\":\"FuzzyMatching\","
							+ "\"name\":\""+req.getParameter("name")+"\","
							+ "\"age\":\""+req.getParameter("age")+"\""
					 + "}");
		});


		// 重定向
        webServer.get("/redirect", (req, resp) -> {
			Logger.info("Client info: "+req.getRemoteAddres()+":"+req.getRemotePort());
			Logger.simple("Request info: "+req.protocol());
			resp.redirct("http://www.baidu.com");
		});

		//普通 POST 请求
        webServer.post("/", (req, resp) -> {
			Logger.info("Client info: "+req.getRemoteAddres()+":"+req.getRemotePort());
			Logger.simple("Request info: "+req.protocol());
			resp.write(fileContent);
			String contentType = req.header().get("Content-Type").split(";")[0];
			resp.write("{"
					+ "\"Method\":\""+contentType+"\","
					+ "\"name\":\""+req.getParameter("name")+"\","
					+ "\"age\":\""+req.getParameter("age")+"\""
			 + "}");
		});

		//自定义方法测试
        webServer.otherMethod("LOCK","/:test",(request,response)->{
			response.body().write("User Defined HTTP method is "+request.protocol().getMethod());
			Logger.simple("Query");
		});

        webServer.socket("/websocket", new WebSocketRouter() {

			@Override
			public ByteBuffer onRecived(HttpRequest upgradeRequest, ByteBuffer message) {
				Logger.info(new String(message.array()));
				String msg = "This is server message. Client message: \r\n\t\""+new String(message.array())+"\"";
				return ByteBuffer.wrap(msg.getBytes());
			}

			@Override
			public ByteBuffer onOpen(HttpRequest upgradeRequest) {
				Logger.info("WebSocket connect!");
				return null;
			}

			@Override
			public void onClose() {
				Logger.info("WebSocket close!");

			}
		});

        webServer.serve();

	}
}
