package org.voovan.test.tools.compiler.clazz;

import junit.framework.TestCase;
import org.voovan.tools.TEnv;
import org.voovan.tools.compiler.DynamicCompilerManager;
import org.voovan.tools.compiler.sandbox.DynamicCompilerSecurityManager;
import org.voovan.tools.compiler.clazz.DynamicClass;
import org.voovan.tools.compiler.sandbox.SecurityModel;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.File;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DynamicClassUnit extends TestCase{

    private String code;

    public void setUp(){
        SecurityModel securityModel = new SecurityModel();
        securityModel.setExit(false);
        System.setSecurityManager( new DynamicCompilerSecurityManager(securityModel));
        code =  "package org.hocate.test;\r\n\r\n"
                + "import org.voovan.tools.TString;\r\n"
                + "public class testSay {\r\n"
                + "\t public String say(){\r\n"
                + "\t\t System.out.println(\"helloword\");\r\n"
                + "\t\t return TString.removePrefix(\"finished\"); \r\n"
                + "\t }\r\n"
                + "}\r\n";
        System.out.println("=============Source code=============");
        System.out.println(code);

    }

    public void testRun() throws Exception{
//        DynamicClass dynamicClass = new DynamicClass("TestCode",code);  //字符串形式的脚本
        File codeFile = new File("./src/test/java/org/voovan/test/tools/compiler/clazz/TestClass.vct");
        DynamicClass dynamicClass = new DynamicClass(codeFile, "UTF-8");   // 文件形式的脚本

        for(int i=0;i<4;i++) {
            System.out.println("\r\n=============Run "+i+"=============");
            long startTime = System.currentTimeMillis();
            //运行脚本
            Class clazz = dynamicClass.getClazz();
            System.out.println("==>name:" +dynamicClass.getName());
            System.out.println("==>classname:" +dynamicClass.getClassName());
            Object testSay = DynamicCompilerManager.newInstance("TestClass",null);
            try {
                Object obj = TReflect.invokeMethod(testSay, "say");
                System.out.println("==>RunTime: " + (System.currentTimeMillis()-startTime )+"\r\n==>Result: " + obj);
            }catch (Exception e){
                Logger.error(e);
            }
            TEnv.sleep( 1000 );
        }

    }
}
