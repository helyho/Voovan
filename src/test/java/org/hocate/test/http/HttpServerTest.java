package org.hocate.test.http;

import java.io.IOException;


import org.hocate.http.message.packet.Cookie;
import org.hocate.http.server.HttpServer;
import org.hocate.log.Logger;

public class HttpServerTest {
	public static void main(String[] args) {
		try {
			//HttpServer httpServer = new HttpServer("0.0.0.0",2080,100,"/Users/helyho/Downloads");
			HttpServer httpServer = HttpServer.newInstance();
			httpServer.get("/", (req,resp)->{
												Logger.simple(req);
												Logger.simple("{{{{{{{{{");
												Cookie cookie = new Cookie();
												cookie.setName("name");
												cookie.setValue("helyho");
												resp.cookies().add(cookie);
												resp.body().writeString("<b>This is HTTP test!</b><br>"+req.getQueryString());
											}
			);
			httpServer.Serve();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
