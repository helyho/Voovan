package org.hocate.test.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;

import org.hocate.http.server.HttpServer;
import org.hocate.http.server.websocket.WebSocketHandler;
import org.hocate.log.Logger;
import org.hocate.tools.TFile;

public class HttpServerTest {
	private static byte[] fileContent = TFile.loadResource("org/hocate/test/http/test.htm");
	
	
	public static void main(String[] args) {
		try {
			HttpServer httpServer = HttpServer.newInstance();
			
			//带路劲参数的 GET 请求
			httpServer.get("/:name", (req, resp) -> {
				if (req.getSession() != null && req.getSession().getAttributes("Time") != null) {
					Logger.simple("Session saved time is: " + req.getSession().getAttributes("Time"));
				}
				Logger.simple(req.getRemoteAddres() + " " + req.getRemotePort());
				Logger.simple("QueryString:"+req.getQueryString());
				req.getSession().setAttribute("Time", new Date().toString());

				resp.write(fileContent);
				resp.write(req.getParameter("name"));
			});
			
			//普通 GET 请求
			httpServer.get("/", (req, resp) -> {
				if (req.getSession() != null && req.getSession().getAttributes("Time") != null) {
					Logger.simple("Session saved time is: " + req.getSession().getAttributes("Time"));
				}
				//Logger.simple(req.getRemoteAddres() + " " + req.getRemotePort());
				//Logger.simple("QueryString:"+req.getQueryString());
				req.getSession().setAttribute("Time", new Date().toString());

				resp.write(fileContent);
				resp.write(req.getParameter("name"));
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
				Logger.simple(req.getRemoteAddres() + " " + req.getRemotePort());
				Logger.simple(req.getQueryString());
				req.getSession().setAttribute("Time", new Date().toString());
				Logger.simple(req.getQueryString());
				resp.write(fileContent);
				resp.write(req.getParameter("name"));
			});
			
			httpServer.socket("/ws", new WebSocketHandler() {
				
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
