package org.voovan.test.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.voovan.http.server.HttpServer;
import org.voovan.http.server.websocket.WebSocketBizHandler;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TFile;
import org.voovan.tools.log.Logger;

public class HttpServerDemo {
	private static byte[] fileContent = TFile.loadResource("org/voovan/test/http/test.htm");
	
	
	public static void main(String[] args) {
		try {
			HttpServer httpServer = HttpServer.newInstance();
			
			//带路劲参数的 GET 请求
			httpServer.get("/:name/:age", (req, resp) -> {
				if (req.getSession() != null && req.getSession().getAttributes("Time") != null) {
					Logger.simple("Session saved time is: " + req.getSession().getAttributes("Time"));
				}
				req.getSession().setAttribute("Time", TDateTime.now());
				resp.write(fileContent);
				resp.write("{\r\n\t\"name\":\""+req.getParameter("name")+"\",\r\n\t\"age\":\""+req.getParameter("age")+"\"\r\n}");
			});
			
			//普通 GET 请求
			httpServer.get("/", (req, resp) -> {
				if (req.getSession() != null && req.getSession().getAttributes("Time") != null) {
					Logger.simple("Session saved time is: " + req.getSession().getAttributes("Time"));
				}
				req.getSession().setAttribute("Time", TDateTime.now());

				resp.write(fileContent);
				resp.write("{\r\n\t\"name\":\""+req.getParameter("name")+"\",\r\n\t\"age\":\""+req.getParameter("age")+"\"\r\n}");
			});
			
			// 重定向
			httpServer.get("/redirect", (req, resp) -> {
				resp.redirct("http://www.baidu.com");
			});

			//普通 POST 请求
			httpServer.post("/", (req, resp) -> {
				if (req.getSession() != null && req.getSession().getAttributes("Time") != null) {
					Logger.simple("Session saved time is: " + req.getSession().getAttributes("Time"));
				}
				req.getSession().setAttribute("Time", TDateTime.now());
				resp.write(fileContent);
				resp.write("{\r\n\t\"name\":\""+req.getParameter("name")+"\",\r\n\t\"age\":\""+req.getParameter("age")+"\"\r\n}");
			});
			
			httpServer.socket("/websocket", new WebSocketBizHandler() {
				
				@Override
				public ByteBuffer onRecived(Map<String, String> params, ByteBuffer message) {
					Logger.info(new String(message.array()));
					return ByteBuffer.wrap("hello helyho".getBytes());
				}
				
				@Override
				public void onOpen(Map<String, String> params) {
					Logger.info("WebSocket connect!");
				}
				
				@Override
				public void onClose() {
					Logger.info("WebSocket close!");
					
				}
			});
			
			httpServer.Serve();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
