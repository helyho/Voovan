package org.voovan.http.client;

import org.voovan.http.message.Response;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.handler.SynchronousHandler;
import org.voovan.tools.log.Logger;

import java.util.function.Consumer;

/**
 * HttpClient 异步 IoHandler 实现
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class AsyncHandler implements IoHandler {
    public HttpClient httpClient;
    public Consumer<Response> async;
    public boolean running = false;
    public SynchronousHandler synchronousHandler;

    public AsyncHandler(HttpClient httpClient) {
        this.httpClient = httpClient;
        if(httpClient.getSocket().handler() instanceof SynchronousHandler) {
            this.synchronousHandler = (SynchronousHandler)httpClient.getSocket().handler();
        }
    }

    public Consumer<Response> getAsync() {
        return async;
    }

    public void setAsync(Consumer<Response> async) {
        this.async = async;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public Object onConnect(IoSession session) {
        return null;
    }

    @Override
    public void onDisconnect(IoSession session) {

    }

    @Override
    public Object onReceive(IoSession session, Object obj) {
        Response response = (Response) obj;
        async.accept(response);

        httpClient.finished(response);
        running = false;
        if (synchronousHandler != null) {
            session.socketContext().handler(synchronousHandler);
        }
        return null;
    }

    @Override
    public void onSent(IoSession session, Object obj) {
        running = true;
    }

    @Override
    public void onFlush(IoSession session) {

    }

    @Override
    public void onException(IoSession session, Exception e) {
        Logger.error(e);
    }

    @Override
    public void onIdle(IoSession session) {

    }
}
