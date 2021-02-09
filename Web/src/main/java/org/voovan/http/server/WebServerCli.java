package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.client.HttpClient;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.filter.StringFilter;
import org.voovan.tools.*;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebServer ClI 工具类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebServerCli {
    public static void registerRouter(WebServer webServer) {

        //Http Status 命令
        webServer.otherMethod("ADMIN", "/voovan/admin/status", new HttpRouter() {
            @Override
            public void process(HttpRequest request, HttpResponse response) throws Exception {
                String status = "RUNNING";

                String authToken = request.header().get("AUTH-TOKEN");
                if(authToken!=null && authToken.equals(WebContext.AUTH_TOKEN)) {
                    if(WebContext.PAUSE){
                        status = "PAUSE";
                    }
                    response.write(status);
                } else {
                    request.getSession().close();
                }
            }
        });

        //WebSocket 管理命令
        webServer.socket("/voovan/admin", new WebSocketRouter() {
            String tips = TString.tokenReplace("{}:# ", WebContext.getWebServerConfig().getServerName());

            @Override
            public Object onOpen(WebSocketSession session) {
                if(!TPerformance.getLocalIpAddrs().contains(session.getRemoteAddress())){
                    session.close();
                }

                session.setAttribute("authed", false);
                Logger.simplef("[{}] WebSocket admin connect: {}:{}", TDateTime.now() ,session.getRemoteAddress(), session.getRemotePort());
                return tips;
            }

            @Override
            public Object onRecived(WebSocketSession session, Object obj) {
                String[] cmds = ((String)obj).split(" ");

                String response = "";
                switch (cmds[0]) {
                    //鉴权
                    case "auth" : {
                        //重置 AUTH_TOKEN
                        if(cmds.length<1 || TString.isNullOrEmpty(cmds[1])) {
                            response =  "(error) Need token for Authentication";
                        }

                        //重置 AUTH_TOKEN
                        if(!WebContext.AUTH_TOKEN.equals(cmds[1])) {
                            response =  "(error) Token is invalide";
                        }

                        session.setAttribute("authed", true);

                        response =  "Authentication success";
                        break;
                    }

                    //退出
                    case "exit" : {
                        response =  "Bye, see you later";

                        Global.getHashWheelTimer().addTask(()->{
                            session.close();
                        }, 1);

                        return null;
                    }

                    //鉴权成功可以执行的命令
                    default: {
                        if (!((Boolean) session.getAttribute("authed"))) {
                            response = "(error) Authentication required";
                        } else {

                            switch (cmds[0]) {
                                //查看服务状态
                                case "status": {
                                    String status = "RUNNING";
                                    if (WebContext.PAUSE) {
                                        status = "PAUSE";
                                    }
                                    response = "Web server is " + status;
                                    break;
                                }

                                //停止服务
                                case "shutdown": {
                                    webServer.stop();
                                    response = "Web server is stoped";
                                    break;
                                }

                                //暂停服务
                                case "pause": {
                                    WebContext.PAUSE = true;
                                    response = "Web server is paused";
                                    break;
                                }

                                //服务恢复
                                case "unpause": {
                                    WebContext.PAUSE = false;
                                    response = "Web server is running";
                                    break;
                                }

                                //重读配置
                                case "reload": {
                                    String config = (cmds.length == 1 || TString.isNullOrEmpty(cmds[1])) ? null : cmds[1];
                                    webServer.reload(config);
                                    response = "Web server reload success";
                                    break;
                                }

                                //列出当前配置
                                case "config": {
                                    response = "\r\n" + JSON.formatJson(JSON.toJSON(webServer.getWebServerConfig()));
                                    break;
                                }

                                //修改鉴权 Token
                                case "changeToken": {
                                    //重置 AUTH_TOKEN
                                    if (cmds.length < 1 || TString.isNullOrEmpty(cmds[1])) {
                                        response = "(error) Token is invalide";
                                    }
                                    WebContext.AUTH_TOKEN = cmds[1];
                                    File tokenFile = new File("logs" + File.separator + ".token");
                                    TFile.writeFile(tokenFile, false, WebContext.AUTH_TOKEN.getBytes());
                                    response = "Token is changed";
                                    break;
                                }
                            }
                        }
                    }
                }

                response = response.length()==0 ? "" : TString.tokenReplace("{}\r\n", response);
                return TString.tokenReplace("{}{}",response, tips);
            }

            @Override
            public void onSent(WebSocketSession session, Object obj) {

            }

            @Override
            public void onClose(WebSocketSession session) {

            }
        }.addFilterChain(new StringFilter()));
    }


    public static void remoteCli(String[] args, int argsIdx){
        Logger.setEnable(false);
        AtomicReference<WebSocketSession> sessionRef = new AtomicReference<WebSocketSession>();

        try {
            String url = args[argsIdx + 1].trim();
            String path = url.substring(url.indexOf("/", 8));
            HttpClient client = HttpClient.newInstance(url, 60);
            client.webSocket(path, new WebSocketRouter() {
                @Override
                public Object onOpen(WebSocketSession session) {
                    System.out.println("[" + TDateTime.now() + "] connect to " + url.toString() + "\r\n\r\n");
                    sessionRef.set(session);
                    return null;
                }

                @Override
                public Object onRecived(WebSocketSession session, Object obj) {
                    System.out.print(obj);
                    return null;
                }

                @Override
                public void onSent(WebSocketSession session, Object obj) {

                }

                @Override
                public void onClose(WebSocketSession session) {

                }
            }.addFilterChain(new StringFilter()));


            TEnv.wait(()->client.isConnect() && sessionRef.get()==null);

            WebSocketSession webSocketSession = sessionRef.get();

            while(client.isConnect()) {
                int size = System.in.available();
                if(size>0) {
                    byte[] commandBytes = new byte[size];
                    System.in.read(commandBytes);
                    String command = new String(commandBytes).trim();

                    webSocketSession.send(command);
                }
                TEnv.sleep(500);
            }
        } catch (Exception e) {
            System.out.println(args);
            e.printStackTrace();
        }

        System.exit(1);
    }
}
