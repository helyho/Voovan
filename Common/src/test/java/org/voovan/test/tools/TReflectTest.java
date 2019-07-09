package org.voovan.test.tools;

import com.jsoniter.JsonIterator;
import org.voovan.test.tools.json.TestObject;
import org.voovan.test.tools.json.TestObject2;
import org.voovan.tools.compiler.function.DynamicFunction;
import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.TReflect;

import java.util.List;

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

        TReflect.genMethodInvoker(obj);

        DynamicFunction dynamicFunctionMethod= TReflect.METHOD_INVOKE.get(TestObject.class.getCanonicalName());


        //get
        String val = TReflect.getFieldValueNatvie(obj, "string");
        System.out.println("native get: "+val);

        //set
        TReflect.setFieldValueNatvie(obj, "string", "nativeFieldSet");
        System.out.println("native set");

        //get
        val = TReflect.getFieldValueNatvie(obj, "string");
        System.out.println("native get: "+ val);


        String result = TReflect.invokeMethodNative(obj, "getData", new Object[]{"aaaa", 111});
        System.out.println("native invoke: " + result);

        //get tb2
        TestObject2 tb2 = TReflect.getFieldValueNatvie(obj, "tb2");
        tb2.getList().add("999999999");
        System.out.println("native get: "+obj.getTb2().getList());
        System.out.println(JSON.toJSON(obj));

        //get tb2
        tb2 = new TestObject2();
        tb2.setString("new tb2");
        TReflect.setFieldValueNatvie(obj, "tb2", tb2);
        System.out.println("native get: "+obj.getTb2().getString());

        System.out.println(JSON.toJSON(obj));
    }
}
