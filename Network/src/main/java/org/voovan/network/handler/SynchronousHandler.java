package org.voovan.network.handler;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;

import java.util.concurrent.LinkedBlockingDeque;

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
        socketResponses.addLast(obj);
        return null;
    }

    @Override
    public void onSent(IoSession session, Object obj) {

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
     * @return 响应对象
     */
    public Object getResponse(){
        return socketResponses.pollFirst();
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
        return socketResponses.size() > 0;
    }
}
