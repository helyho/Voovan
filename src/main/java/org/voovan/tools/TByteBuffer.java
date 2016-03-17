package org.voovan.tools;

import java.nio.ByteBuffer;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Java Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class TByteBuffer {

    public static byte[] toArray(ByteBuffer bytebuffer){
        if(!bytebuffer.hasArray()) {
            int oldPosition = bytebuffer.position();
            bytebuffer.position(0);
            int size = bytebuffer.limit();
            byte[] buffers = new byte[size];
            bytebuffer.get(buffers);
            bytebuffer.position(oldPosition);
            return buffers;
        }else{
            return bytebuffer.array();
        }
    }
}
