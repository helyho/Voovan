package org.voovan.test;

import org.junit.rules.TestRule;
import org.voovan.tools.cmd.GnuCommander;
import org.voovan.tools.cmd.annotation.Command;
import org.voovan.tools.cmd.annotation.Option;
import org.voovan.tools.json.JSON;

import java.math.BigDecimal;
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
       BigDecimal b = new BigDecimal("1.00");
        BigDecimal c = new BigDecimal("1.00000");
        System.out.println(b.compareTo(c));

        System.out.println(b);
    }


    @Command(usage = "option -abcded [... file]", description = "option os 123123123123123",
            version = "1.0.0", copyright = "helyho@copyright", author = "helyho",
    contact = "helyho@gmail.com", licence = "apache v2")
    static public class TestOption {
        //--active mmm -cd -b '12 -3 -123' -f 123 123 -system
        @Option(name="a", longName = "active", usage = "aaa usage asdfasdffffffff", required = true)
        String a;
        @Option(name="b", usage = "bbbb usage TestOption")
        String b;
        @Option(name = "c",longName = "contuine", usage = "contuine usage toStringadfadf")
        boolean c;
        @Option(name = "d", longName = "longName", usage = "longName usage toStringadfadf")
        boolean d;
        @Option(name = "s", usage = "gnuCommander usage toStringadfadf")
        List<Integer> e;

        @Option(name = "f", usage = "active usage toStringadfadf")
        List<Integer> f;
    }
}
