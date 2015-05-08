package org.voovan.test.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.voovan.http.server.HttpRequest;
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
			
			//普通 GET 请求
			httpServer.get("/", (req, resp) -> {
				Logger.info("Client info: "+req.getRemoteAddres()+":"+req.getRemotePort());
				Logger.simple("Request info: "+req.protocol());
				if (req.getSession() != null && req.getSession().getAttributes("Time") != null) {
					Logger.simple("Session saved time is: " + req.getSession().getAttributes("Time"));
				}
				req.getSession().setAttribute("Time", TDateTime.now());

				resp.write(fileContent);
				resp.write("{"
						+ "\"Method\":\"NormalGET\","
						+ "\"name\":\""+req.getParameter("name")+"\","
						+ "\"age\":\""+req.getParameter("age")+"\""
				 + "}");
			});
			
			//带路劲参数的 GET 请求
			httpServer.get("/:name/:age", (req, resp) -> {
				Logger.info("Client info: "+req.getRemoteAddres()+":"+req.getRemotePort());
				Logger.simple("Request info: "+req.protocol());
				if (req.getSession() != null && req.getSession().getAttributes("Time") != null) {
					Logger.simple("Session saved time is: " + req.getSession().getAttributes("Time"));
				}
				req.getSession().setAttribute("Time", TDateTime.now());
				resp.write(fileContent);
				resp.write("{"
								+ "\"Method\":\"PathGET\","
								+ "\"name\":\""+req.getParameter("name")+"\","
								+ "\"age\":\""+req.getParameter("age")+"\""
						 + "}");
			});
			
			
			// 重定向
			httpServer.get("/redirect", (req, resp) -> {
				Logger.info("Client info: "+req.getRemoteAddres()+":"+req.getRemotePort());
				Logger.simple("Request info: "+req.protocol());
				resp.redirct("http://www.baidu.com");
			});

			//普通 POST 请求
			httpServer.post("/", (req, resp) -> {
				Logger.info("Client info: "+req.getRemoteAddres()+":"+req.getRemotePort());
				Logger.simple("Request info: "+req.protocol());
				if (req.getSession() != null && req.getSession().getAttributes("Time") != null) {
					Logger.simple("Session saved time is: " + req.getSession().getAttributes("Time"));
				}
				req.getSession().setAttribute("Time", TDateTime.now());
				resp.write(fileContent);
				String contentType = req.header().get("Content-Type").split(";")[0];
				resp.write("{"
						+ "\"Method\":\""+contentType+"\","
						+ "\"name\":\""+req.getParameter("name")+"\","
						+ "\"age\":\""+req.getParameter("age")+"\""
				 + "}");
			});
			
			httpServer.socket("/websocket", new WebSocketBizHandler() {
				
				@Override
				public ByteBuffer onRecived(HttpRequest upgradeRequest, ByteBuffer message) {
					Logger.info(new String(message.array()));
					String msg = "This is server message. Client message: \r\n\t\""+new String(message.array())+"\"";
					return ByteBuffer.wrap(msg.getBytes());
				}
				
				@Override
				public void onOpen(HttpRequest upgradeRequest) {
					Logger.info("WebSocket connect!");
				}
				
				@Override
				public void onClose() {
					Logger.info("WebSocket close!");
					
				}
			});
			
			httpServer.Serve();
		} catch (IOException e) {
			Logger.error(e);
		}
	}
}
