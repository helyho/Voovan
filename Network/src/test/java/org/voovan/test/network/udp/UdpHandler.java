package org.voovan.test.network.udp;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class UdpHandler implements IoHandler{
    @Override
    public Object onConnect(IoSession session) {
        Logger.simple("onConnect");
        return "asdfadsfaf";
    }

    @Override
    public void onDisconnect(IoSession session) {
        Logger.simple("onDisconnect");
    }

    @Override
    public Object onReceive(IoSession session, Object obj) {
        Logger.simple("onReceive");
        Logger.simple(obj.toString());
        return null;
    }

    @Override
    public void onSent(IoSession session, Object obj) {
        Logger.simple("onSent");
    }

    @Override
    public void onException(IoSession session, Exception e) {
        Logger.simple("onException");
    }
}
