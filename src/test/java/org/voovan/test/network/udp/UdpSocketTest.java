package org.voovan.test.network.udp;

import org.voovan.network.ConnectModel;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.udp.UdpSocket;
import org.voovan.test.network.udp.ClientHandlerTest;

import java.io.IOException;

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

    public static void main(String[] args) throws IOException {
        UdpSocket udpSocket = new UdpSocket("127.0.0.1",60000,50);
        udpSocket.messageSplitter(new LineMessageSplitter());
        udpSocket.filterChain().add(new StringFilter());
        udpSocket.handler(new ClientHandlerTest());
        udpSocket.start();
    }
}
