package org.voovan.test;

import org.junit.rules.TestRule;
import org.voovan.tools.cmd.GnuCommander;
import org.voovan.tools.cmd.annotation.Command;
import org.voovan.tools.cmd.annotation.Option;
import org.voovan.tools.json.JSON;

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
        System.out.println(System.getenv("USER"));
    }
}
