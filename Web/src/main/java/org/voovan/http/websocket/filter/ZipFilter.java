package org.voovan.http.websocket.filter;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.voovan.http.websocket.WebSocketFilter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.tools.TZip;
import org.voovan.tools.buffer.TByteBuffer;
import org.voovan.tools.log.Logger;

public class ZipFilter implements WebSocketFilter{

    @Override
    public Object decode(WebSocketSession session, Object object) {
        if(object instanceof ByteBuffer){
            ByteBuffer byteBuffer = (ByteBuffer)object;
            byte[] bytes = TByteBuffer.toArray(byteBuffer);
            try {
                return TZip.decodeZip(bytes);
            } catch (IOException e) {
                Logger.errorf("ZipFilter decode failed", e);
            }
		}
		return object;
        
    }

    @Override
    public Object encode(WebSocketSession session, Object object) {
        return object;
    }
    
}
