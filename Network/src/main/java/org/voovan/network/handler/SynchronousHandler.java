package org.voovan.network.handler;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Socket 同步通信 handler
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SynchronousHandler implements IoHandler {
    //Socket 响应队列
    private LinkedBlockingDeque<Object> socketResponses  = new LinkedBlockingDeque<Object>();

    /**
     * 增加响应对象
     * @param obj 响应对象
     */
    public void addResponse(Object obj){
        socketResponses.add(obj);
    }

    @Override
    public Object onConnect(IoSession session) {
        return  null;
    }

    @Override
    public void onDisconnect(IoSession session) {
    }

    @Override
    public Object onReceive(IoSession session, Object obj) {
        addResponse(obj);
        return null;
    }

    @Override
    public void onSent(IoSession session, Object obj) {

    }

    @Override
    public void onFlush(IoSession session) {

    }

    @Override
    public void onException(IoSession session, Exception e) {
        socketResponses.addLast(e);
    }

    @Override
    public void onIdle(IoSession session) {

    }

    /**
     * 获取下一个响应对象
     * @param  timeout 超时时间
     * @return 响应对象
     * @throws TimeoutException 超时异常
     */
    public Object getResponse(int timeout) throws TimeoutException {
        try {
            return socketResponses.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new TimeoutException("SynchronousHandler.getResponse timeout");
        }
    }

    /**
     * 获取响应对象数量
     * @return 获取响应对象数量
     */
    public int responseCount(){
        return socketResponses.size();
    }

    /**
     * 是否存在下一个响应对象
     * @return true: 存在, false: 不存在
     */
    public boolean hasNextResponse(){
        return responseCount() > 0;
    }
}
