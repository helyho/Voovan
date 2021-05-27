package org.voovan.network;

import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.collection.Chain;

import java.nio.ByteBuffer;

/**
 * 插件实现接口
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface IoPlugin {
    /**
     * 初始化插件信息
     * @param session IoSession 会话对象
     */
    public void init(IoSession session);

    /**
     * 插件准备截断, 例如:握手初始化
     * @param session IoSession 会话对象
     */
    public void prepare(IoSession session);

    /**
     * 获取默认的读取 ByteBufferChannel
     * @param session IoSession 会话对象
     * @return 用户读取的ByteBufferChannel
     */
    public ByteBufferChannel getReadBufferChannel(IoSession session);

    /**
     * 数据打包
     * @param session IoSession 会话对象
     * @param byteBuffer 需要发送的数据
     * @return 转换给下一个 plugin 的 ByteBuffer 对象
     */
    public ByteBuffer warp(IoSession session, ByteBuffer byteBuffer);

    /**
     * 数据解包
     * @param session IoSession 会话对象
     */
    public void unwarp(IoSession session);

    /**
     * 过滤器释放
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

    public static void warpChain(SocketContext socketContext, ByteBuffer byteBuffer) {
        Chain<IoPlugin> pluginChain = (Chain<IoPlugin>) socketContext.pluginChain().clone();
        pluginChain.rewind();
        while (pluginChain.hasPrevious()) {
            IoPlugin plugin = pluginChain.previous();
            plugin.warp(socketContext.getSession(), byteBuffer);
        }
    }

    public static void unwarpChain(SocketContext socketContext) {
        Chain<IoPlugin> pluginChain = (Chain<IoPlugin>) socketContext.pluginChain().clone();
        pluginChain.rewind();
        while (pluginChain.hasNext()) {
            IoPlugin plugin = pluginChain.next();
            plugin.unwarp(socketContext.getSession());
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
