package org.voovan.network.udp;

import org.voovan.network.*;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.RestartException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;

/**
 * UdpSocket 连接
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class UdpSocket extends SocketContext<DatagramChannel, UdpSession> {

    private SelectorProvider provider;
    private DatagramChannel datagramChannel;
    private UdpSession session;

    //用来阻塞当前Socket
    private Object waitObj = new Object();


    /**
     * socket 连接
     *      默认不会出发空闲事件, 默认发超时时间: 1s
     * @param host      监听地址
     * @param port		监听端口
     * @param readTimeout   超时时间, 单位: 毫秒
     * @throws IOException	IO异常
     */
    public UdpSocket(String host, int port, int readTimeout) throws IOException{
        super(host, port, readTimeout);
    }

    /**
     * socket 连接
     *      默认发超时时间: 1s
     * @param host      监听地址
     * @param port		监听端口
     * @param idleInterval	空闲事件触发时间, 单位: 秒
     * @param readTimeout   超时时间, 单位: 毫秒
     * @throws IOException	IO异常
     */
    public UdpSocket(String host, int port, int readTimeout, int idleInterval) throws IOException{
        super(host, port, readTimeout, idleInterval);
    }

    /**
     * socket 连接
     * @param host      监听地址
     * @param port		监听端口
     * @param idleInterval	空闲事件触发时间, 单位: 秒
     * @param readTimeout   超时时间, 单位: 毫秒
     * @param sendTimeout 发超时时间, 单位: 毫秒
     * @throws IOException	IO异常
     */
    public UdpSocket(String host, int port, int readTimeout, int sendTimeout, int idleInterval) throws IOException{
        super(host, port, readTimeout, sendTimeout, idleInterval);
    }

    private void init() throws IOException {
        provider = SelectorProvider.provider();
        datagramChannel = provider.openDatagramChannel();
        datagramChannel.socket().setSoTimeout(this.readTimeout);

        InetSocketAddress address = new InetSocketAddress(this.host, this.port);
        session = new UdpSession(this, address);
        connectModel = ConnectModel.CLIENT;
    }

    /**
     * 构造函数
     * @param parentSocketContext 父 SocketChannel 对象
     * @param datagramChannel UDP通信对象
     * @param socketAddress SocketAddress 对象
     */
    public UdpSocket(SocketContext parentSocketContext, DatagramChannel datagramChannel, InetSocketAddress socketAddress){
        try {
            provider = SelectorProvider.provider();
            this.datagramChannel = datagramChannel;
            this.copyFrom(parentSocketContext);
            session = new UdpSession(this, socketAddress);
            datagramChannel.configureBlocking(false);
            connectModel = ConnectModel.SERVER;
        } catch (Exception e) {
            Logger.error("Create socket channel failed",e);
        }
    }


    @Override
    public void setIdleInterval(int idleInterval) {
        this.idleInterval = idleInterval;
    }

    /**
     * 设置 Socket 的 Option 选项
     *
     * @param name   SocketOption类型的枚举, 参照:DatagramChannel.setOption的说明
     * @param value  SocketOption参数
     * @throws IOException IO异常
     */
    public <T> void setOption(SocketOption<T> name, T value) throws IOException {
        datagramChannel.setOption(name, value);
    }

    @Override
    public DatagramChannel socketChannel() {
        return datagramChannel;
    }

    /**
     * 初始化函数
     */
    private void registerSelector(int ops)  {
        EventRunner eventRunner = EventRunnerGroup.EVENT_RUNNER_GROUP.choseEventRunner();
        SocketSelector socketSelector = (SocketSelector)eventRunner.attachment();
        socketSelector.register(this, ops);
        if(ops!=0) {
            eventRunner.addEvent(() -> {
                if (datagramChannel.isOpen()) {
                    socketSelector.eventChose();
                }
            });
        }
    }


    /**
     * 获取 Session 对象
     * @return Session 对象
     */
    public UdpSession getSession(){
        return session;
    }

    @Override
    public void start() throws IOException {
        syncStart();

        synchronized (waitObj){
            try {
                waitObj.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 启动同步的上下文连接,同步读写时使用
     */
    public void syncStart() throws IOException {
	    init();

        datagramChannel.connect(new InetSocketAddress(this.host, this.port));
        datagramChannel.configureBlocking(false);

        registerSelector(SelectionKey.OP_READ);

        EventTrigger.fireConnect(session);
    }

    @Override
    public void acceptStart() throws IOException {
		registerSelector(0);
    }

    /**
     * 重连当前连接
     * @return UdpSocket对象
     * @throws IOException IO 异常
     * @throws RestartException 重新启动的异常
     */
    public UdpSocket restart() throws IOException, RestartException {
        if(this.connectModel == ConnectModel.CLIENT) {
            init();
            this.start();
            return this;
        }else{
            throw new RestartException("Can't invoke reStart method in server mode");
        }
    }

    /**
     * 重连当前连接
     *      同步模式
     * @return UdpSocket对象
     * @throws IOException IO 异常
     * @throws RestartException 重新启动的异常
     */
    public UdpSocket syncRestart() throws IOException, RestartException {
        if(this.connectModel == ConnectModel.CLIENT) {
            init();
            this.syncStart();
            return this;
        }else{
            throw new RestartException("Can't invoke reStart method in server mode");
        }
    }

    /**
     * 同步读取消息
     * @return 读取出的对象
     * @throws ReadMessageException 读取消息异常
     */
    public Object synchronouRead() throws ReadMessageException {
        return session.syncRead();
    }

    /**
     * 同步发送消息
     * @param obj  要发送的对象
     * @throws SendMessageException  消息发送异常
     */
    public void synchronouSend(Object obj) throws SendMessageException {
        session.syncSend(obj);
    }


    @Override
    public boolean isOpen() {
        if(datagramChannel!=null){
            return datagramChannel.isOpen();
        }else{
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        if(datagramChannel!=null){
            return datagramChannel.isOpen();
        }else{
            return false;
        }
    }

    @Override
    public boolean close() {

        if(datagramChannel!=null){
            try{
                datagramChannel.close();

                session.getSelectionKey().attach(null);
                session.getSelectionKey().cancel();
                session.getReadByteBufferChannel().release();
                session.getSendByteBufferChannel().release();
                synchronized (waitObj) {
                    waitObj.notify();
                }
                return true;
            } catch(IOException e){
                Logger.error("Close SocketChannel failed",e);
                return false;
            }
        }else{
            synchronized (waitObj) {
                waitObj.notify();
            }
            return true;
        }
    }
}
