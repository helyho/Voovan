package org.voovan.test.http;

import org.voovan.http.server.WebServer;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.nio.ByteBuffer;

public class WebServerDemo {
	private static byte[] fileContent = TFile.loadFileFromContextPath("WEBAPP/index.htm");
	
	public static void main(String[] args) {
		WebServer webServer = WebServer.newInstance();

		//性能测试请求
        webServer.get("/test", (req, resp) -> {
			resp.write("OK");
		});

		//性能测试请求
		webServer.post("/upload", (req, resp) -> {
			req.saveUploadedFile("file","./upload_file.xml");
			if(new File("./upload_file.xml").exists()){
				resp.write("Success");
			}
		});

		//普通 GET 请求
        webServer.get("/", (req, resp) -> {
			Logger.info("Client info: "+req.getRemoteAddres()+":"+req.getRemotePort());
			Logger.simple("Request info: "+req.protocol());
			//Session 测试
			{
				String now = TDateTime.now();
				if (req.getSession() != null && req.getSession().getAttribute("Time") != null) {
					Logger.simple("Session saved time is: " + req.getSession().getAttribute("Time")+" SavedTime: "+now);
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
			public ByteBuffer onOpen() {
				Logger.info("onOpen: WebSocket connect!");

				WebSocketRouter webSocketRouter = this.persistent();
                //调用发送函数发送
                webSocketRouter.send(ByteBuffer.wrap("Send by persistent Object's send method in onOpen".getBytes()));

				send(ByteBuffer.wrap("Send by send method in onOpen".getBytes()));
				return ByteBuffer.wrap("Server send: onOpen".getBytes());
			}

			@Override
			public ByteBuffer onRecived(ByteBuffer message) {
				String msg = TByteBuffer.toString(message);
				Logger.info("onRecive: "+TByteBuffer.toString(message));
				msg = "This is server message. Client message: \r\n\t\""+msg+"\"";

				//调用发送函数发送
				send(ByteBuffer.wrap("Send by send method in onRecive".getBytes()));
				return ByteBuffer.wrap(msg.getBytes());
			}

			@Override
			public void onSent(ByteBuffer message) {
				Logger.info("onSend: "+TByteBuffer.toString(message));
			}

				@Override
			public void onClose() {
				Logger.info("WebSocket close!");
			}
		});

        webServer.serve();

	}
}
