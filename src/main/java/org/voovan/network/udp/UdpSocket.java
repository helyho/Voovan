package org.voovan.network.udp;

import org.voovan.Global;
import org.voovan.network.ConnectModel;
import org.voovan.network.SocketContext;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.handler.SynchronousHandler;
import org.voovan.network.messagesplitter.TimeOutMesssageSplitter;
import org.voovan.network.messagesplitter.TrasnferSplitter;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
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
public class UdpSocket extends SocketContext{

    private SelectorProvider provider;
    private Selector selector;
    private DatagramChannel datagramChannel;
    private UdpSession session;


    /**
     * socket 连接
     * @param host      监听地址
     * @param port		监听端口
     * @param readTimeout   超时事件
     * @throws IOException	IO异常
     */
    public UdpSocket(String host, int port, int readTimeout) throws IOException{
        super(host, port, readTimeout);
        this.readTimeout = readTimeout;
        provider = SelectorProvider.provider();
        datagramChannel = provider.openDatagramChannel();
        datagramChannel.socket().setSoTimeout(this.readTimeout);
        this.connectModel = connectModel;
        InetSocketAddress address = new InetSocketAddress(this.host, this.port);
        datagramChannel.connect(address);
        session = new UdpSession(this,address);
        datagramChannel.configureBlocking(false);

        this.handler = new SynchronousHandler();
        init();
    }

    /**
     * 构造函数
     * @param parentSocketContext 父 SocketChannel 对象
     * @param socketAddress SocketAddress 对象
     */
    protected UdpSocket(SocketContext parentSocketContext,InetSocketAddress socketAddress){
        try {
            provider = SelectorProvider.provider();
            this.datagramChannel = ((UdpServerSocket)parentSocketContext).datagramChannel();
            this.copyFrom(parentSocketContext);
            session = new UdpSession(this, socketAddress);
            connectModel = ConnectModel.SERVER;
        } catch (Exception e) {
            Logger.error("Create socket channel failed",e);
        }
    }

    /**
     * 初始化函数
     */
    private void init()  {
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

    @Override
    public void start() throws IOException {
        //如果没有消息分割器默认使用透传分割器
        if(messageSplitter == null){
            messageSplitter = new TrasnferSplitter();
        }

        init();

        if(datagramChannel!=null && datagramChannel.isOpen()){
            UdpSelector udpSelector = new UdpSelector(selector,this);
            udpSelector.eventChose();
        }
    }

    /**
     * 启动同步的上下文连接,同步读写时使用
     */
    public void syncStart(){
        Global.getThreadPool().execute(()->{
            try {
                start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
            return datagramChannel.isConnected();
        }else{
            return false;
        }
    }

    @Override
    public boolean close() {
        session.getByteBufferChannel().free();

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
