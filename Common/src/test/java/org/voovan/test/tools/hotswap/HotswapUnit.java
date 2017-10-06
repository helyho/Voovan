package org.voovan.test.tools.hotswap;

import junit.framework.TestCase;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.compiler.DynamicCompiler;
import org.voovan.tools.hotswap.Hotswaper;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HotswapUnit extends TestCase{

    public static void main(String[] args) throws Exception {

        //构造 Hotswaper 对象,然后定时调用 autoReload 方法即可

        Hotswaper hotSwaper = new Hotswaper();
        hotSwaper.autoReload(1);

        System.out.println("=============Run=============");
        for (int i = 0; i < 2; i++) {
            System.out.println("\r\n=============Run " + i + "=============");
            long startTime = System.currentTimeMillis();
            TestSay testSay = new TestSay();
            Object result = testSay.say();
            Logger.info("==>RunTime: " + (System.currentTimeMillis() - startTime) + "\r\n==>Result: " + result);
            //运行脚本
            TEnv.sleep(1000);

            if (i == 0) {
                DynamicCompiler dynamicCompiler = new DynamicCompiler(null, null, "/Users/helyho/Work/Java/Voovan/Common/classes");
                dynamicCompiler.compileCode(TObject.asList("/Users/helyho/Work/Java/Voovan/Common/src/test/java/org/voovan/test/tools/hotswap/TestSay.java"));
                TEnv.sleep(1000);
            }
        }
    }
}
