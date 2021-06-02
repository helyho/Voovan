package org.voovan.network.plugin;

import org.voovan.network.*;
import org.voovan.tools.buffer.ByteBufferChannel;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 默认插件, SocketContext 无须手工初始化, SocketContext初始化时会作为默认的第一个插件初始化到 PluginChain 中
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DefaultPlugin implements IoPlugin {
    @Override
    public void init(IoSession session) {

    }

    @Override
    public void prepare(IoSession session) {
    }

    @Override
    public ByteBufferChannel getReadBufferChannel(IoSession session) {
        return session.getReadByteBufferChannel();
    }

    @Override
    public ByteBuffer warp(IoSession session, ByteBuffer byteBuffer) {
        if(byteBuffer!=null && byteBuffer.hasRemaining()) {
            session.sendToBuffer(byteBuffer);
        }

        return byteBuffer;
    }

    @Override
    public void unwarp(IoSession session) {

    }

    @Override
    public void release(IoSession session) {

    }
}
