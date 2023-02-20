package org.voovan.network;

import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.collection.Chain;

import java.nio.ByteBuffer;

/**
 * 插件实现接口
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface IoPlugin {
    /**
     * 初始化插件信息<br>
     * 在 SocketContext.syncStart/acceptStart  内触发
     * @param session IoSession 会话对象
     */
    public void init(IoSession session);

    /**
     * 插件准备阶段, 例如:握手初始化
     * 在 SocketContext.unhold 内触发, 在 epoll 的注册以后
     * @param session IoSession 会话对象
     */
    public void prepare(IoSession session);

    /**
     * 获取默认的读取 ByteBufferChannel<br>
     * 在 SocketSelector.tcpReadFromChannel/udpReadFromChannel 触发
     * 默认会使用这个方法返回的 ByteBufferChannel 来接收 os.tcp 缓冲区的数据.
     * 例如: ssl 使用了 SSLParser中的 ByteBufferChannel 来接手数据, 并在 unwarp 中转换到 IoSession.getReadByteBufferChannel 中来支持框架后续行为
     *
     * @param session IoSession 会话对象
     * @return 用户读取的ByteBufferChannel, 返回 null 则使用默认的 IoSession.getReadByteBuffer()
     */
    public ByteBufferChannel getReadBufferChannel(IoSession session);

    /**
     * 数据打包
     * IoSession.send 内触发<br>
     * @param session IoSession 会话对象
     * @param byteBuffer 需要发送的数据
     * @return 转换给下一个 plugin 的 ByteBuffer 对象
     */
    public ByteBuffer wrap(IoSession session, ByteBuffer byteBuffer);

    /**
     * 数据解包<br>
     * SocketSelector.loadAndPrepare 内触发
     * @param session IoSession 会话对象
     */
    public void unwrap(IoSession session);

    /**
     * 过滤器释放<br>
     * IoSession.release  内触发
     * @param session IoSession 会话对象
     */
    public void release(IoSession session);

    public static void initChain(SocketContext socketContext) {
        Chain<IoPlugin> pluginChain = (Chain<IoPlugin>) socketContext.pluginChain().clone();
        pluginChain.rewind();
        while (pluginChain.hasNext()) {
            IoPlugin plugin = pluginChain.next();
            plugin.init(socketContext.getSession());
        }
    }

    public static void prepareChain(SocketContext socketContext) {
        Chain<IoPlugin> pluginChain = (Chain<IoPlugin>) socketContext.pluginChain().clone();
        pluginChain.rewind();
        while (pluginChain.hasNext()) {
            IoPlugin plugin = pluginChain.next();
            plugin.prepare(socketContext.getSession());
        }
    }

    public static ByteBufferChannel getReadBufferChannelChain(SocketContext socketContext) {
        Chain<IoPlugin> pluginChain = (Chain<IoPlugin>) socketContext.pluginChain().clone();
        pluginChain.rewind();
        ByteBufferChannel byteBufferChannel = null;
        while (pluginChain.hasNext()) {
            IoPlugin plugin = pluginChain.next();
            ByteBufferChannel tmpByteBufferChannel = plugin.getReadBufferChannel(socketContext.getSession());
            if(tmpByteBufferChannel!=null) {
                byteBufferChannel = tmpByteBufferChannel;
            }
        }

        return byteBufferChannel;
    }

    public static void wrapChain(SocketContext socketContext, ByteBuffer byteBuffer) {
        Chain<IoPlugin> pluginChain = (Chain<IoPlugin>) socketContext.pluginChain().clone();
        pluginChain.rewind();
        while (pluginChain.hasPrevious()) {
            IoPlugin plugin = pluginChain.previous();
            ByteBuffer tmpByteBuffer = plugin.wrap(socketContext.getSession(), byteBuffer);
            if(tmpByteBuffer!=null){
                byteBuffer = tmpByteBuffer;
            }
        }
    }

    public static void unwrapChain(SocketContext socketContext) {
        Chain<IoPlugin> pluginChain = (Chain<IoPlugin>) socketContext.pluginChain().clone();
        pluginChain.rewind();
        while (pluginChain.hasNext()) {
            IoPlugin plugin = pluginChain.next();
            plugin.unwrap(socketContext.getSession());
        }
    }

    public static void releaseChain(SocketContext socketContext) {
        Chain<IoPlugin> pluginChain = (Chain<IoPlugin>) socketContext.pluginChain().clone();
        pluginChain.rewind();
        while (pluginChain.hasPrevious()) {
            IoPlugin plugin = pluginChain.previous();
            plugin.release(socketContext.getSession());
        }
    }
}
