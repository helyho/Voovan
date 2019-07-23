package org.voovan.test.http;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;
import org.voovan.http.server.WebServer;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;


public class VoovanWebServerBench {

	public static void main(String[] args) {
		WebServerConfig webServerConfig = WebContext.getWebServerConfig();
		webServerConfig.setGzip(false);
		webServerConfig.setAccessLog(false);
		WebServer webServer = WebServer.newInstance(webServerConfig);

		//性能测试请求
		webServer.get("/test", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.write("OK");
			}
		});

		webServer.post("/test", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.write("OK");
			}
		});

//		Logger.setEnable(false);

		webServer.serve();


	}
}
