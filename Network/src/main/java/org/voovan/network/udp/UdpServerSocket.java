package org.voovan.network.udp;

import org.voovan.Global;
import org.voovan.network.SocketContext;
import org.voovan.network.handler.SynchronousHandler;
import org.voovan.network.messagesplitter.TransferSplitter;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
public class UdpServerSocket extends SocketContext {

    private SelectorProvider provider;
    private Selector selector;
    private DatagramChannel datagramChannel;
    private UdpSession session;


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
        InetSocketAddress address = new InetSocketAddress(this.host, this.port);
        datagramChannel.bind(new InetSocketAddress(this.host, this.port));
        datagramChannel.configureBlocking(false);
        this.handler = new SynchronousHandler();
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

    /**
     * 初始化函数
     */
    private void registerSelector()  {
        try{
            selector = provider.openSelector();
            datagramChannel.register(selector, SelectionKey.OP_READ);
        }catch(IOException e){
            Logger.error("init SocketChannel failed by openSelector",e);
        }
    }


    /**
     * 获取 Session 对象
     * @return Session 对象
     */
    public UdpSession getSession(){
        return session;
    }

    public DatagramChannel datagramChannel(){
        return this.datagramChannel;
    }

    /**
     * 启动同步监听
     * 		非阻赛方法
     * @throws IOException  IO 异常
     */
    @Override
    public void start() throws IOException {
        //如果没有消息分割器默认使用透传割器
        if(messageSplitter == null){
            messageSplitter = new TransferSplitter();
        }

        registerSelector();

        if(datagramChannel!=null && datagramChannel.isOpen()){
            UdpSelector udpSelector = new UdpSelector(selector,this);
            udpSelector.eventChose();
        }
    }

    /**
     * 启动同步监听
     * 		非阻赛方法
     * @throws IOException  IO 异常
     */
    @Override
    public void syncStart() throws IOException {
        Global.getThreadPool().execute(new Runnable(){
            public void run() {
                try {
                    start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void acceptStart() throws IOException {

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
            return datagramChannel.isConnected();
        }else{
            return false;
        }
    }

    @Override
    public boolean close() {
        if(datagramChannel!=null){
            try{
                datagramChannel.close();
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
