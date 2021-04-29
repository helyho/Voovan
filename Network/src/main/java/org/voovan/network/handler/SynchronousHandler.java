package org.voovan.network.handler;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;

import java.util.concurrent.*;

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
    private final Object lock = new Object();

    private volatile Object socketResponses = null;


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
        addResponse(e);
    }

    @Override
    public void onIdle(IoSession session) {

    }

    /**
     * 增加响应对象
     * @param obj 响应对象
     */
    public void addResponse(Object obj){
        socketResponses = obj;
        unhold();
    }

    /**
     * 获取下一个响应对象
     * @param  timeout 超时时间
     * @return 响应对象
     * @throws TimeoutException 超时异常
     */
    public synchronized Object getResponse(int timeout) throws Exception {
        try {
            hold(timeout);
            return socketResponses;
        } finally {
            reset();
        }
    }

    public void reset(){
        socketResponses = null;
    }

    public void hold(int timeout) throws TimeoutException {
        try {
            synchronized (lock) {
                //socketResponses 有数据则不触发 hold
                if(socketResponses==null) {
                    long start = System.currentTimeMillis();
                    lock.wait(timeout);
                    long cost = System.currentTimeMillis() - start;
                    if (cost >= timeout && socketResponses == null) {
                        throw new TimeoutException();
                    }
                }
            }

        } catch (InterruptedException e) {
            throw new TimeoutException(e.getMessage());
        }
    }

    public void unhold(){
        synchronized (lock) {
            lock.notifyAll();
        }
    }
}
