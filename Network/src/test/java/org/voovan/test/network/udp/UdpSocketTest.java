package org.voovan.test.network.udp;

import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.udp.UdpSocket;
import org.voovan.test.network.ClientHandlerTest;
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
public class UdpSocketTest {

    public static void main(String[] args) throws Exception {
        UdpSocket udpSocket = new UdpSocket("127.0.0.1",60000,50000, 1);
        udpSocket.messageSplitter(new LineMessageSplitter());
        udpSocket.filterChain().add(new StringFilter());
        udpSocket.handler(new ClientHandlerTest());
        udpSocket.start();
    }
}
