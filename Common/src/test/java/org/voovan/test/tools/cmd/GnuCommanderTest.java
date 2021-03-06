package org.voovan.test.tools.cmd;

import org.voovan.test.Other;
import org.voovan.tools.cmd.GnuCommander;
import org.voovan.tools.cmd.annotation.Command;
import org.voovan.tools.cmd.annotation.Option;
import org.voovan.tools.json.JSON;

import java.util.List;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class GnuCommanderTest {
    public static void main(String[] args) throws Exception {
        System.out.println(JSON.toJSON(args));
        GnuCommander<TestOption> gnuCommander = new GnuCommander(TestOption.class);

        System.out.println();
        gnuCommander.printUsage(12);

        System.out.println();

        TestOption testOption = gnuCommander.parser(args);
        System.out.println(JSON.toJSON(testOption));
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

        @Option(name="c", longName = "contuine", usage = "contuine usage toStringadfadf")
        boolean c;

        @Option(name="d", longName = "longName", usage = "longName usage toStringadfadf")
        boolean d;

        @Option(name="e", usage = "gnuCommander usage toStringadfadf")
        List<Integer> e;

        @Option(name = "f", usage = "active usage toStringadfadf")
        List<Integer> f;
    }
}
