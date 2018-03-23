package org.voovan.network.aio;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.network.exception.RestartException;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritePendingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * NIO 会话连接对象
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AioSession extends IoSession<AioSocket> {

    private AsynchronousSocketChannel	socketChannel;


    /**
     * 构造函数
     *
     * @param socket AioSocket对象
     */
    public AioSession(AioSocket socket) {
        super(socket);
        if (socket != null) {
            this.socketChannel = socket.socketChannel();
        } else {
            Logger.error("SocketChannel is null, please check it.");
        }
    }

    @Override
    public String localAddress() {
        if (this.isConnected()) {
            try {
                InetSocketAddress socketAddress = (InetSocketAddress)socketChannel.getLocalAddress();
                return socketAddress.getHostName();
            } catch (IOException e) {
                Logger.error("Get SocketChannel local address failed",e);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public int loaclPort() {
        if (this.isConnected()) {
            try {
                InetSocketAddress socketAddress = (InetSocketAddress)socketChannel.getLocalAddress();
                return socketAddress.getPort();
            } catch (IOException e) {
                Logger.error("Get SocketChannel local port failed",e);
                return -1;
            }
        } else {
            return -1;
        }
    }

    @Override
    public String remoteAddress() {
        if (this.isConnected()) {
            try {
                InetSocketAddress socketAddress = (InetSocketAddress)socketChannel.getRemoteAddress();
                return socketAddress.getHostString();
            } catch (IOException e) {
                Logger.error("Get SocketChannel remote address failed",e);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public int remotePort() {
        if (this.isConnected()) {
            try {
                InetSocketAddress socketAddress = (InetSocketAddress)socketChannel.getRemoteAddress();
                return socketAddress.getPort();
            } catch (IOException e) {
                Logger.error("Get SocketChannel remote port failed.",e);
                return -1;
            }
        } else {
            return -1;
        }
    }

    @Override
    protected int read0(ByteBuffer buffer) throws IOException {
        int readSize = 0;
        if (buffer != null && !this.getByteBufferChannel().isReleased()) {
            readSize = this.getByteBufferChannel().readHead(buffer);
        }
        return readSize;
    }

    @Override
    protected synchronized int send0(ByteBuffer buffer) throws IOException {
        int totalSendByte = 0;
        if (isConnected() && buffer != null) {
            //循环发送直到全部内容发送完毕
            while(isConnected() && buffer.remaining()!=0){
                try {
                    Future<Integer> sendResult = socketChannel.write(buffer);
                    try {
                        while(isConnected()) {
                            //这里会阻赛当前的发送线程
                            try {
                                Integer sentLength = sendResult.get(socketContext().getSendTimeout(), TimeUnit.MILLISECONDS);
                                if (sentLength != null) {
                                    totalSendByte += sentLength;
                                    break;
                                }
                            }catch (TimeoutException e){
                                Logger.error("AioSession send timeout, Socket will be close", e);
                                close();
                                return -1;
                            }
                        }
                    } catch ( ExecutionException e) {
                        break;
                    } catch (InterruptedException  e){
                        close();
                    }

                }catch(WritePendingException e){
                    continue;
                }
            }
        }
        return totalSendByte;
    }

    @Override
    protected MessageSplitter getMessagePartition() {
        return this.socketContext().messageSplitter();
    }

    @Override
    public boolean isConnected() {
        return this.socketContext().isConnected();
    }

    /**
     * 会话是否打开
     *
     * @return true: 打开,false: 关闭
     */
    @Override
    public boolean isOpen() {
        return this.socketContext().isOpen();
    }

    @Override
    public boolean close() {
        this.cancelIdle();

        // 关闭 socket
        if(isConnected()) {
            return this.socketContext().close();
        }else{
            return false;
        }
    }

    /**
     * 重连当前连接
     * @throws IOException IO 异常
     * @throws RestartException 重新启动的异常
     */
    public void restart() throws IOException, RestartException {
        socketContext().restart();
    }

    @Override
    public String toString() {
        return "[" + this.localAddress() + ":" + this.loaclPort() + "] -> [" + this.remoteAddress() + ":" + this.remotePort() + "]";
    }
}
