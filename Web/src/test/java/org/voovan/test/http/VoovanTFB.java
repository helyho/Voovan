package org.voovan.test.http;

import com.jsoniter.output.JsonStream;
import com.jsoniter.output.JsonStreamPool;
import com.jsoniter.spi.JsonException;
import org.voovan.http.message.HttpStatic;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;
import org.voovan.http.server.WebServer;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.tools.TObject;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;


public class VoovanTFB {
	private static final byte[] HELLO_WORLD = "Hello, World!".getBytes();

	static class Message {

		private final String message;

		public Message(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

	}

	public static void main(String[] args) {

		WebServerConfig webServerConfig = WebContext.getWebServerConfig();
		webServerConfig.setGzip(false);
		webServerConfig.setAccessLog(false);
		webServerConfig.setKeepAliveTimeout(1000);
		webServerConfig.setHost("0.0.0.0");
		webServerConfig.setPort(8080);
		webServerConfig.setHotSwapInterval(0);
		webServerConfig.setCache(true);
		webServerConfig.getModuleonfigs().clear();
		webServerConfig.getRouterConfigs().clear();
		WebServer webServer = WebServer.newInstance(webServerConfig);
		Logger.setEnable(false);

		//性能测试请求;
		webServer.get("/plaintext", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.header().put(HttpStatic.CONTENT_TYPE_STRING, "text/plain");
				resp.write(HELLO_WORLD);
			}
		});
		//性能测试请求
		webServer.get("/vjson", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.header().put("Content-Type", "application/json");
				resp.write(JSON.toJSON(TObject.asMap("message", "Hello, World!"), false, false));
			}
		});
		//性能测试请求
		webServer.get("/json", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.header().put("Content-Type", "application/json");
				resp.write(serializeMsg(new Message("Hello, World!")));
			}
		});

		webServer.syncServe();
	}

	private static byte[] serializeMsg(Message obj) {
		JsonStream stream = JsonStreamPool.borrowJsonStream();
		try {
			stream.reset(null);
			stream.writeVal(Message.class, obj);
			return Arrays.copyOfRange(stream.buffer().data(), 0, stream.buffer().tail());
		} catch (IOException e) {
			throw new JsonException(e);
		} finally {
			JsonStreamPool.returnJsonStream(stream);
		}
	}
}
