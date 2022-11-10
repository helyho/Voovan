package org.voovan.network.plugin;

import org.voovan.network.*;
import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.log.Logger;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

/**
 * SSL 插件用于支持 SSL 加密通信
 *
 * @author helyho
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

    public SSLPlugin(){
        try {
            this.sslManager = new SSLManager("TLS");
        } catch (NoSuchAlgorithmException e) {
            Logger.error("SSLPlugin construct failed", e);
        }
    }

    @Override
    public void init(IoSession session) {
        //准备 SSL 证书
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

        //等待握手完成
        while(session.isConnected() && !sslParser.isHandShakeDone()) {
            session.socketSelector().select();

            if(sslParser.getSSlByteBufferChannel().size()>0) {
                sslParser.doHandShake();
            }
        }
    }

    @Override
    public ByteBufferChannel getReadBufferChannel(IoSession session) {
        //使用 SSLParser 中的缓冲区
        return sslParser.getSSlByteBufferChannel();
    }

    @Override
    public ByteBuffer wrap(IoSession session, ByteBuffer byteBuffer) {

        try {
            sslParser.warp(byteBuffer);
        } catch (SSLException e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void unwrap(IoSession session) {
        //将 SSLParser 的缓冲区转换到 IoSession 的缓冲区
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
