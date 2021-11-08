package org.voovan.test;

import org.junit.rules.TestRule;
import org.voovan.tools.TEnv;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * none
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Other {
    public static void main(String[] args) throws Exception {
        Logger.infof(System.getenv("USER"));

        TEnv.sleep(100);
    }
}
