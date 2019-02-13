package org.voovan.test.http;

import org.voovan.http.message.HttpStatic;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;
import org.voovan.http.server.WebServer;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.tools.log.Logger;


public class VoovanTFB {

	public static void main(String[] args) {
		WebServerConfig webServerConfig = WebContext.getWebServerConfig();
		webServerConfig.setGzip(false);
		webServerConfig.setAccessLog(false);
		webServerConfig.setKeepAliveTimeout(1000);
		webServerConfig.setHost("0.0.0.0");
		webServerConfig.setPort(28080);
		webServerConfig.setCache(true);
		webServerConfig.getModuleonfigs().clear();
		webServerConfig.getRouterConfigs().clear();
		WebServer webServer = WebServer.newInstance(webServerConfig);
		Logger.setEnable(false);

		//性能测试请求
		webServer.get("/plaintext", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.header().remove(HttpStatic.CONNECTION_STRING);
				resp.write("Hello, World!");
			}
		});
		//性能测试请求
		webServer.get("/json", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.header().remove(HttpStatic.CONNECTION_STRING);
				resp.write("Hello, World!");
			}
		});

		webServer.serve();
	}
}