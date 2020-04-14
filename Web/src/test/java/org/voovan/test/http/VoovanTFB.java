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
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;


public class VoovanTFB {
	private static final byte[] HELLO_WORLD = "Hello, World!".getBytes();

	public static class Message {

		private final String message;

		public Message(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public int hashCode() {
			return Objects.hash(message);
		}
	}

	public static void main(String[] args) {
		TReflect.register(Message.class);

		WebServerConfig webServerConfig = WebContext.buildWebServerConfig();
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

		//性能测试请求;
		webServer.get("/plaintext", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.header().put(HttpStatic.CONTENT_TYPE_STRING, HttpStatic.TEXT_PLAIN_STRING);
				resp.write(HELLO_WORLD);
			}
		});
		//性能测试请求
		webServer.get("/vjson", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.header().put(HttpStatic.CONTENT_TYPE_STRING, HttpStatic.APPLICATION_JSON_STRING);
				resp.write(JSON.toJSON(new Message("Hello, World!"), false, false));
			}
		});
		//性能测试请求
		webServer.get("/json", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.header().put(HttpStatic.CONTENT_TYPE_STRING, HttpStatic.APPLICATION_JSON_STRING);
				resp.write(serializeMsg(new Message("Hello, World!")));
			}
		});

		Logger.setEnable(true);

		webServer.serve();
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
