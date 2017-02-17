package org.voovan.network.udp;

import org.voovan.network.SocketContext;
import org.voovan.network.handler.SynchronousHandler;
import org.voovan.network.messagesplitter.TimeOutMesssageSplitter;
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
public class UdpServerSocket extends SocketContext{

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
    public UdpServerSocket(String host, int port, int readTimeout) throws IOException{
        super(host, port, readTimeout);
        this.readTimeout = readTimeout;
        provider = SelectorProvider.provider();
        datagramChannel = provider.openDatagramChannel();
        datagramChannel.socket().setSoTimeout(this.readTimeout);
        this.connectModel = connectModel;
        InetSocketAddress address = new InetSocketAddress(this.host, this.port);
        datagramChannel.bind(new InetSocketAddress(this.host, this.port));
        datagramChannel.configureBlocking(false);
        this.handler = new SynchronousHandler();
        init();
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
        //如果没有消息分割器默认使用读取超时时间作为分割器
        if(messageSplitter == null){
            messageSplitter = new TimeOutMesssageSplitter();
        }

        if(datagramChannel!=null && datagramChannel.isOpen()){
            UdpSelector udpSelector = new UdpSelector(selector,this);
            udpSelector.eventChose();
        }
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
