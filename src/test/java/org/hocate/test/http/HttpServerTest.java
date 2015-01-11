package org.hocate.test.http;

import java.io.IOException;

import org.hocate.http.server.HttpServer;

public class HttpServerTest {
	public static void main(String[] args) {
		try {
			HttpServer httpServer = new HttpServer("0.0.0.0",2080,100,"/Users/helyho/Downloads");
			httpServer.get("/", (req,resp)->{
												System.out.println(req);
												resp.body().writeString("<b>This is HTTP test!</b>");
												System.out.println("=================================");
											}
			);
			httpServer.Serve();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
