package org.voovan.test.tools.complier.function;

import junit.framework.TestCase;
import org.voovan.tools.TEnv;
import org.voovan.tools.complier.function.DynamicFunction;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.util.List;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DynamicFunctionUnit extends TestCase{

    private String code;

    public void setUp(){
        code =  "import java.util.ArrayList;\n\n" +
                "ArrayList asssssl = new ArrayList();\n" +
                "System.out.println(temp1+ temp2);\n" +
                "return asssssl;\n";
        System.out.println("=============Source code=============");
        System.out.println(code);
    }

    public void testName() throws Exception {
    }

    public void testRun() throws Exception{
//        DynamicFunction function = new DynamicFunction("TestCode",code);
        DynamicFunction function = new DynamicFunction("TestCode",
                new File("./src/test/java/org/voovan/test/tools/complier/function/TestScript.jf"),
                "UTF-8");

        function.addPrepareArg(0, "String", " temp1");
        function.addPrepareArg(1, "String", " temp2");

        System.out.println("=============Args list=============");
        System.out.println("arg0 -> 0 String temp1 = hely ");
        System.out.println("arg1 -> 0 String temp2 = ho \n");

        System.out.println("============= result =============");

        for(int i=0;i<4;i++) {
            long startTime = System.currentTimeMillis();
            List list = function.call("hely ", "ho");
            Logger.simple( (System.currentTimeMillis()-startTime )+" Result: " + list);
            TEnv.sleep( 1000 );
        }
    }
    
}
