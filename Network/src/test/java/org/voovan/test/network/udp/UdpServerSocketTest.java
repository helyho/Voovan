package org.voovan.test.network.udp;

import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.udp.UdpServerSocket;
import org.voovan.test.network.ServerHandlerTest;

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
public class UdpServerSocketTest {

    public static void main(String[] args) throws IOException {
        UdpServerSocket udpServerSocket = new UdpServerSocket("127.0.0.1",60000,5000, 1);
        udpServerSocket.messageSplitter(new LineMessageSplitter());
        udpServerSocket.filterChain().add(new StringFilter());
        udpServerSocket.handler(new ServerHandlerTest());
        udpServerSocket.start();
    }
}
