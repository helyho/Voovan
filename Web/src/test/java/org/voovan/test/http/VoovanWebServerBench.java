package org.voovan.test.http;

import org.voovan.http.extend.engineio.Config;
import org.voovan.http.extend.engineio.EIODispatcher;
import org.voovan.http.extend.engineio.EIOHandler;
import org.voovan.http.extend.socketio.SIODispatcher;
import org.voovan.http.extend.socketio.SIOHandler;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;
import org.voovan.http.server.WebServer;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.http.websocket.filter.StringFilter;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TFile;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;


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

		webServer.serve();

	}
}
