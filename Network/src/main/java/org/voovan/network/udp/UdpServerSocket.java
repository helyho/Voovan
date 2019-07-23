package org.voovan.network.udp;

import org.voovan.network.*;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
public class UdpServerSocket extends SocketContext<DatagramChannel, UdpSession> {

    private SelectorProvider provider;
    private DatagramChannel datagramChannel;

    //用来阻塞当前Socket
    private Object waitObj = new Object();

    /**
     * socket 连接
     *      默认不会出发空闲事件, 默认发超时时间: 1s
     * @param host      监听地址
     * @param port		监听端口
     * @param readTimeout   超时时间, 单位:毫秒
     * @throws IOException	IO异常
     */
    public UdpServerSocket(String host, int port, int readTimeout) throws IOException{
        super(host, port, readTimeout);
        init();
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
    public UdpServerSocket(String host, int port, int readTimeout, int idleInterval) throws IOException{
        super(host, port, readTimeout, idleInterval);
        init();
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
    public UdpServerSocket(String host, int port, int readTimeout, int sendTimeout, int idleInterval) throws IOException{
        super(host, port, readTimeout, sendTimeout, idleInterval);
        init();
    }

    private void init() throws IOException {
        provider = SelectorProvider.provider();
        datagramChannel = provider.openDatagramChannel();
        datagramChannel.socket().setSoTimeout(this.readTimeout);
        datagramChannel.configureBlocking(false);
        connectModel = ConnectModel.LISTENER;
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

    @Override
    public UdpSession getSession() {
        return null;
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
        datagramChannel.bind(new InetSocketAddress(this.host, this.port));

        bindToSocketSelector(SelectionKey.OP_READ);
    }

    @Override
    protected void acceptStart() throws IOException {
        throw new UnsupportedEncodingException();
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
                synchronized (waitObj) {
                    waitObj.notify();
                }

                return true;
            } catch(IOException e){
                Logger.error("Close SocketChannel failed",e);
                return false;
            }
        }else{
            return true;
        }
    }
}
