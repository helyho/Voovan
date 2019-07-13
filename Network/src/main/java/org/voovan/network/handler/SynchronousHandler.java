package org.voovan.network.handler;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;

import java.util.concurrent.LinkedBlockingDeque;
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
    private Object lock = new Object();

    //Socket 响应队列
    private LinkedBlockingDeque<Object> socketResponses  = new LinkedBlockingDeque<Object>();

    public void tryLock(int timeout) throws TimeoutException {
        synchronized (this) {
            if (!hasNextResponse()) {
                synchronized (lock) {
                    try {
                        lock.wait(timeout);
                    } catch (InterruptedException e) {
                        throw new TimeoutException();
                    }
                }
            }
        }
    }

    public void unlock(){
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * 增加响应对象
     * @param obj 响应对象
     */
    public void addResponse(Object obj){
        unlock();
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
     * @return 响应对象
     */
    public Object getResponse(int timeout) throws TimeoutException {
        tryLock(timeout);
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
        return responseCount() > 0;
    }
}
