package org.voovan.test;

import org.junit.rules.TestRule;
import org.voovan.tools.cmd.GnuCommander;
import org.voovan.tools.cmd.annotation.Option;
import org.voovan.tools.json.JSON;

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
        System.out.println(JSON.toJSON(args));
        GnuCommander<TestOption> gnuCommander = new GnuCommander(TestOption.class);

        gnuCommander.printUsage(4);

        TestOption testOption = gnuCommander.parser(args);
        System.out.println(JSON.toJSON(testOption));
    }


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
