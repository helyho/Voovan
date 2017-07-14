package org.voovan.test.tools.compiler.function;

import junit.framework.TestCase;
import org.voovan.tools.TEnv;
import org.voovan.tools.compiler.function.DynamicFunction;

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
    private String subFunCode;

    public void setUp(){
        code =  "import java.util.ArrayList;\n\n" +
                "ArrayList list = new ArrayList();\n" +
                "System.out.println(temp1+ temp2); \n" +
                "SubFunCode();\n" +
                "return list;\n";
        System.out.println("=============Source code=============");
        System.out.println(code);

        subFunCode = "System.out.println(\"this is subcode\");";
        try {
            DynamicFunction function = new DynamicFunction("SubFunCode",subFunCode);
            function.compileCode();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public void testRun() throws Exception{
        DynamicFunction dynamicFunction = new DynamicFunction("TestCode",code);  //字符串形式的脚本
//        File codeFile = new File("./src/test/java/org/voovan/test/tools/compiler/function/TestFunction.vct");
//        DynamicFunction dynamicFunction = new DynamicFunction( codeFile, "UTF-8");   // 文件形式的脚本

        //增加默认导入
        dynamicFunction.addImport(ArrayList.class);

        //增加对其他动态函数的引用
        dynamicFunction.addImportFunction("SubFunCode");

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
            System.out.println("==>name:" +dynamicFunction.getName());
            System.out.println("==>classname:" +dynamicFunction.getClassName());

            long startTime = System.currentTimeMillis();
            //运行脚本
            List list = dynamicFunction.call("hely ", "ho");

            System.out.println("==>RunTime: "+(System.currentTimeMillis()-startTime )+"\r\n==>Result: " + list);
            TEnv.sleep( 1000 );
        }
    }
}
