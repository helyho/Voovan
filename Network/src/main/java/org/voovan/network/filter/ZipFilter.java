package org.voovan.network.filter;

import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.network.exception.IoFilterException;
import org.voovan.tools.TZip;
import org.voovan.tools.log.Logger;

import java.io.IOException;

/**
 * Some description
 *
 * @author: helyho
 * Project: DBase
 * Create: 2017/11/3 21:08
 */
public class ZipFilter implements IoFilter{
    @Override
    public Object decode(IoSession session, Object object) throws IoFilterException {
        if(object.getClass() == ByteFilter.BYTE_ARRAY_CLASS){
            try {
                return TZip.decodeGZip((byte[])object);
            } catch (IOException e) {
                Logger.error("ZipFilter decode error, socket will be close");
                session.close();
            }
        }
        return null;
    }

    @Override
    public Object encode(IoSession session, Object object) throws IoFilterException {
        if(object.getClass() == ByteFilter.BYTE_ARRAY_CLASS){
            try {
                return TZip.encodeGZip((byte[])object);
            } catch (IOException e) {
                Logger.error("ZipFilter encode error, socket will be close");
                session.close();
            }
        }
        return null;
    }
}
