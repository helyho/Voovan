package org.voovan.network.udp;

import org.voovan.network.*;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.tcp.TcpSession;
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
        this.connectType = ConnectType.UDP;
    }

    /**
     * 构造函数
     * @param parentSocketContext 父 SocketChannel 对象
     * @param datagramChannel UDP通信对象
     * @param socketAddress SocketAddress 对象
     */
    public UdpSocket(SocketContext parentSocketContext, DatagramChannel datagramChannel, InetSocketAddress socketAddress){
        try {
            this.provider = SelectorProvider.provider();
            this.datagramChannel = datagramChannel;
            this.copyFrom(parentSocketContext);
            this.session = new UdpSession(this, socketAddress);
            this.datagramChannel.configureBlocking(false);
            this.connectModel = ConnectModel.SERVER;
            this.connectType = ConnectType.UDP;
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
                Logger.error(e);
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

        bindToSocketSelector(SelectionKey.OP_READ);
    }

    @Override
    public void acceptStart() throws IOException {
		bindToSocketSelector(0);
    }

    /**
     * 同步读取消息
     * @return 读取出的对象
     * @throws ReadMessageException 读取消息异常
     */
    public Object syncRead() throws ReadMessageException {
        return session.syncRead();
    }

    /**
     * 同步发送消息
     * @param obj  要发送的对象
     * @throws SendMessageException  消息发送异常
     */
    public void syncSend(Object obj) throws SendMessageException {
        session.syncSend(obj);
    }


    @Override
    public boolean isOpen() {
        if (datagramChannel.isOpen()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        if (datagramChannel.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean close() {

        if(datagramChannel!=null){

			session.release();

			synchronized (waitObj) {
				waitObj.notify();
			}
			return true;
        }else{
            synchronized (waitObj) {
                waitObj.notify();
            }
            return true;
        }
    }
}
