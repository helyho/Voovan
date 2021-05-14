package org.voovan.network.plugin;

import org.voovan.network.*;
import org.voovan.tools.buffer.ByteBufferChannel;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * SSL 插件用于支持 SSL 加密通信
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SSLPlugin implements IoPlugin {
    private SSLManager sslManager;
    private SSLParser sslParser;

    public SSLPlugin(SSLManager sslManager) {
        this.sslManager = sslManager;
    }

    @Override
    public void init(IoSession session) {
        try {
            SocketContext socketContext = session.socketContext();
            if (sslManager != null && socketContext.getConnectModel() == ConnectModel.SERVER) {
                sslParser = sslManager.createServerSSLParser(session);
            } else if (sslManager != null && socketContext.getConnectModel() == ConnectModel.CLIENT) {
                sslParser = sslManager.createClientSSLParser(session);
            }
        } catch (SSLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void prepare(IoSession session) {
        //客户端模式主动发起 SSL 握手
        if(session.socketContext().getConnectModel() == ConnectModel.CLIENT) {
            sslParser.doHandShake();
        }

        while(session.isConnected() && !sslParser.isHandShakeDone()) {
            session.socketSelector().select();

            if(sslParser.getSSlByteBufferChannel().size()>0) {
                sslParser.doHandShake();
            }
        }
    }

    @Override
    public ByteBufferChannel getReadBufferChannel(IoSession session) {
        return sslParser.getSSlByteBufferChannel();
    }

    @Override
    public ByteBuffer warp(IoSession session, ByteBuffer byteBuffer) {
        try {
            sslParser.warp(byteBuffer);
        } catch (SSLException e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void unwarp(IoSession session) {
        try {
            sslParser.unwarpByteBufferChannel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release(IoSession session) {
        sslParser.release();
    }
}
