package org.voovan.test.tools.compiler.function;

import junit.framework.TestCase;
import org.voovan.tools.TEnv;
import org.voovan.tools.compiler.function.DynamicFunction;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.util.ArrayList;
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

    public void testRun() throws Exception{
//        DynamicFunction function = new DynamicFunction("TestCode",code);  //字符串形式的脚本
        File codeFile = new File("./src/test/java/org/voovan/test/tools/compiler/function/TestFunction.vct");
        DynamicFunction dynamicFunction = new DynamicFunction( codeFile, "UTF-8");   // 文件形式的脚本

        //增加默认导入
        dynamicFunction.addImport(ArrayList.class);

        //启用脚本中的 import 导入
        dynamicFunction.enableImportInCode(true);

        //准备脚本的默认参数
        dynamicFunction.addPrepareArg(0, String.class, " temp1");
        dynamicFunction.addPrepareArg(1, String.class, " temp2");

        System.out.println("=============Args list=============");
        System.out.println("arg0 -> 0 String temp1 = hely ");
        System.out.println("arg1 -> 0 String temp2 = ho \n");

        System.out.println("============= result =============");

        for(int i=0;i<4;i++) {
            System.out.println("\r\n=============Run "+i+"=============");
            System.out.println("name:" +dynamicFunction.getName());
            System.out.println("classname:" +dynamicFunction.getClassName());

            long startTime = System.currentTimeMillis();
            //运行脚本
            List list = dynamicFunction.call("hely ", "ho");

            Logger.simple( (System.currentTimeMillis()-startTime )+" Result: " + list);
            TEnv.sleep( 1000 );
        }
    }
    
}
