package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;

import java.nio.ByteBuffer;

/**
 * 透传处理器
 * 直接将数据进行透传,收到即调用过滤器,然后调用  IoHandler.onRecive 方法
 *         使用这个分割器的时候不推荐使用过滤器,应为报文并不完整.
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TransferSplitter implements MessageSplitter {
    @Override
    public int canSplite(IoSession session, ByteBuffer byteBuffer) {
        return byteBuffer.limit();
    }
}
