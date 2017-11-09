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
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.http.websocket.filter.StringFilter;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TFile;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.io.File;


public class WebServerDemo {
	private static byte[] fileContent = TFile.loadFileFromContextPath("WEBAPP/index.htm");
	
	public static void main(String[] args) {
		WebServer webServer = WebServer.newInstance();

		//性能测试请求
        webServer.get("/test", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.write("OK");
			}
		});

		//性能测试请求
		webServer.post("/upload", new HttpRouter(){
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				req.saveUploadedFile("file", "./upload_file.xml");
				if (new File("./upload_file.xml").exists()) {
					resp.write("Success");
				}
			}
		});

		//普通 GET 请求
        webServer.get("/",  new HttpRouter(){
            public void process(HttpRequest req, HttpResponse resp) throws Exception {
                Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
                Logger.simple("Request info: " + req.protocol());
                //Session 测试
                {
                    String now = TDateTime.now();
                    if (req.getSession() != null && req.getSession().getAttribute("Time") != null) {
                        Logger.simple("Session saved time is: " + req.getSession().getAttribute("Time") + " SavedTime: " + now);
                    }
                    req.getSession().setAttribute("Time", now);
                }
                resp.write(fileContent);
                resp.write("{"
                        + "\"Method\":\"NormalGET\","
                        + "\"name\":\"" + req.getParameter("name") + "\","
                        + "\"age\":\"" + req.getParameter("age") + "\""
                        + "}");
            }
		});

		//带路径参数的 GET 请求
        webServer.get("/Star/:name/:age",  new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
				Logger.simple("Request info: " + req.protocol());
				resp.write(fileContent);
				resp.write("{"
						+ "\"Method\":\"PathGET\","
						+ "\"name\":\"" + req.getParameter("name") + "\","
						+ "\"age\":\"" + req.getParameter("age") + "\""
						+ "}");
			}
		});

		//带分割参数的 GET 请求, 这种形式针对 seo 优化
		webServer.get("/split_:name_:age",  new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
				Logger.simple("Request info: " + req.protocol());
				resp.write(fileContent);
				resp.write("{"
						+ "\"Method\":\"PathGET\","
						+ "\"name\":\"" + req.getParameter("name") + "\","
						+ "\"age\":\"" + req.getParameter("age") + "\""
						+ "}");
			}
		});

		//路径模糊匹配
        webServer.get("/test/t*t/kkk/*",  new HttpRouter() {
            public void process(HttpRequest req, HttpResponse resp) throws Exception {
                Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
                Logger.simple("Request info: " + req.protocol());
                resp.write(fileContent);
                resp.write("{"
                        + "\"Method\":\"FuzzyMatching\""
                        + "}");
            }
		});


		// 重定向
        webServer.get("/redirect",  new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
				Logger.simple("Request info: " + req.protocol());
				resp.redirct("http://www.baidu.com");
			}
		});

		//普通 POST 请求
        webServer.post("/",  new HttpRouter() {
            public void process(HttpRequest req, HttpResponse resp) throws Exception {
                Logger.info("Client info: " + req.getRemoteAddres() + ":" + req.getRemotePort());
                Logger.simple("Request info: " + req.protocol());
                resp.write(fileContent);
                String contentType = req.header().get("Content-Type").split(";")[0];
                resp.write("{"
                        + "\"Method\":\"" + contentType + "\","
                        + "\"name\":\"" + req.getParameter("name") + "\","
                        + "\"age\":\"" + req.getParameter("age") + "\""
                        + "}");
            }
		});

		//自定义方法测试
        webServer.otherMethod("LOCK","/:test", new HttpRouter() {
			public void process(HttpRequest req, HttpResponse resp) throws Exception {
				resp.body().write("User Defined HTTP method is " + req.protocol().getMethod());
				Logger.simple("Query");
			}
		});

        webServer.socket("/websocket", new WebSocketRouter() {

        	@Override
			public Object onOpen(WebSocketSession webSocketSession) {
				Logger.info("onOpen: WebSocket connect!");

                //调用发送函数发送
				try {
					webSocketSession.send("Send by persistent Object's send method in onOpen");
					webSocketSession.send("Send by send method in onOpen");
				} catch (SendMessageException | WebSocketFilterException e) {
					e.printStackTrace();
				}

				return "Server send: onOpen";
			}

			@Override
			public Object onRecived(WebSocketSession webSocketSession, Object obj) {
				String msg = (String)obj;
				Logger.info("onRecive: "+msg);
				msg = "This is server message. Client message: \r\n\t\""+msg+"\"";

				//调用发送函数发送
				try {
					webSocketSession.send("Send by send method in onRecive");
				} catch (SendMessageException | WebSocketFilterException e) {
					e.printStackTrace();
				}
				return msg;
			}

			@Override
			public void onSent(WebSocketSession webSocketSession, Object message) {
				Logger.info("----> onSend: " + message);
			}

				@Override
			public void onClose(WebSocketSession webSocketSession) {
				Logger.info("WebSocket close!");
			}
		}.addFilterChain(new StringFilter()));

		//engine.io 测试用例
		webServer.socket("/engine.io", new EIODispatcher(new Config())
				.on("connection", new EIOHandler() {
					@Override
					public String execute(String msg) {
						try {
							this.send("asdfasdf");
						} catch (SendMessageException e) {
							e.printStackTrace();
						} catch (WebSocketFilterException e) {
							e.printStackTrace();
						}
						Logger.simple("connected");
						return "server connected";
					}
				}).on("close", new EIOHandler() {
					@Override
					public String execute(String msg) {
						Logger.simple("closed");
						return null;
					}
				}).on("message", new EIOHandler() {
					@Override
					public String execute(String msg) {
						Logger.simple("message: " + msg);
						return "server "+msg;
					}
				}).on("ping", new EIOHandler() {
					@Override
					public String execute(String msg) {
						Logger.simple("ping");
						return null;
					}
				}).on("pong", new EIOHandler() {
					@Override
					public String execute(String msg) {
						Logger.simple("pong");
						return null;
					}
				}));

		//socket.io 测试用例
		webServer.socket("/socket.io", new SIODispatcher(new Config())
            .on("connect", new SIOHandler() {
                @Override
                public String execute(Object... args) {
                    Logger.simple("connect");
                    return null;
                }
            })
            .on("disconnect", new SIOHandler() {
                @Override
                public String execute(Object... args) {
                    Logger.simple("disconnect");
                    return null;
                }
            })
            .on("hello", new SIOHandler() {
                @Override
                public Object execute(Object... args) {
                    Logger.simple("hello: "+ JSON.toJSON(args));

                    //触发前端的事件
					try {
						//事件名, 回掉, 事件参数
						emit("show", new SIOHandler() {
                            @Override
                            public Object execute(Object... args) {
                                Logger.simple(args);
                                return null;
                            }
                        }, "aaaa");
					} catch (SendMessageException e) {
						e.printStackTrace();
					} catch (WebSocketFilterException e) {
						e.printStackTrace();
					}

					return "hello back message";
                }
            })
		);

        webServer.serve();

	}
}
