package org.voovan.test.tools.ioc;

import junit.framework.TestCase;
import org.voovan.tools.ioc.Config;
import org.voovan.tools.ioc.Container;
import org.voovan.tools.ioc.annotation.Value;
import org.voovan.tools.reflect.TReflect;

/**
 * Class name
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class ContainerUnit extends TestCase {
    public void testAll() throws ReflectiveOperationException {
        Container container = new Container("defalut");

        //========test========
        System.out.println(container.getByExpression("Filters[0].Name", ""));
        System.out.println("============================");

        container.addBeanValue("abcd", 123123);
        System.out.println(container.getByExpression("abcd", 123));
        System.out.println("============================");

        container.getDefinitions().addBeanDefinition("config", Config.class, true, false, false);
        Config c = container.getByExpression("co", null);
        System.out.println("Value: " + c);
        System.out.println("============================");

        container.getDefinitions().addBeanDefinition("parent", Config.class, true, false, false);
        container.getDefinitions().addMethodDefinition("tttt1", "parent", TReflect.findMethod(Config.class, "getConfig"), false, false, false);
        System.out.println("method1 PathValue: " + container.getByExpression("tttt1.ServerName", null));
        System.out.println("============================");

        //静态方法测试
        container.getDefinitions().addMethodDefinition("tttt2", null, TReflect.findMethod(ContainerUnit.class, "test1", 1)[0], false, false, false);
        System.out.println("static method2 PathValue: " + container.getByExpression("tttt2.name", null));
        System.out.println("============================");

        container.getDefinitions().addMethodDefinition("tttt3", null, TReflect.findMethod(ContainerUnit.class, "test2"), false, false, false);
        System.out.println("static method3 PathValue: " + container.getByExpression("tttt3", null));
        System.out.println("============================");

        container.getDefinitions().addMethodDefinition("tttt4", null, TReflect.findMethod(ContainerUnit.class, "test2"), true, false, false);
        System.out.println("static method4 singleton PathValue: " + container.getByExpression("tttt4", null));
        System.out.println("static method4 singleton PathValue: " + container.getByExpression("tttt4", null));
        System.out.println("static method4 singleton PathValue: " + container.getByExpression("tttt4", null));
        System.out.println("static method4 singleton PathValue: " + container.getByExpression("tttt4", null));
    }

    public static Config test1(@Value("config1") Config config){
        return config;
    }

    static int i=0;
    public static String test2(){
        return "aaaa " + ++i;
    }

}
