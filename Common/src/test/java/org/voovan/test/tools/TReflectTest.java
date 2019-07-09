package org.voovan.test.tools;

import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.compiler.function.DynamicFunction;
import org.voovan.tools.reflect.TReflect;

/**
 * 类文字命名
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class TReflectTest {

    public static void main(String[] args) throws Exception {
        TestObject obj = new TestObject();
        obj.setBint(12312313);
        obj.setString("starstattt");
        obj.getList().add("1111");
        obj.getMap().put("key", "value");

//        TReflect.genFieldWriter(obj);
//        TReflect.genFieldReader(obj);
//
//        DynamicFunction dynamicFunctionReader = TReflect.FIELD_READER.get(TestObject.class.getCanonicalName());
//        System.out.println((Integer) dynamicFunctionReader.call(obj, "bint"));
//
//        DynamicFunction dynamicFunctionWriter = TReflect.FIELD_WRITER.get(TestObject.class.getCanonicalName());
//        dynamicFunctionWriter.call(obj, "string", "456456456string");
//
//        System.out.println((String) dynamicFunctionReader.call(obj, "string"));

        TReflect.genMethodInvoker(obj);

        DynamicFunction dynamicFunctionMethod= TReflect.METHOD_INVOKE.get(TestObject.class.getCanonicalName());
        System.out.println((Object)dynamicFunctionMethod.call(obj, "getData", new Object[]{"aaaa", 111}));
    }
}
