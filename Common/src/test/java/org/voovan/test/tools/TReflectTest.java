package org.voovan.test.tools;

import com.jsoniter.JsonIterator;
import org.voovan.test.tools.json.TestObject;
import org.voovan.test.tools.json.TestObject2;
import org.voovan.tools.TEnv;
import org.voovan.tools.compiler.function.DynamicFunction;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.JSONDecode;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
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
        TestObject obj = TReflect.newInstance(TestObject.class, null);
        obj.setBint(12312313);
        obj.setString("starstattt");
        obj.getList().add("1111");
        obj.getMap().put("key", "value");

        TReflect.genFieldReader(obj);
        TReflect.genFieldWriter(obj);

        //get
        String val = TReflect.getFieldValueNatvie(obj, "string");
        System.out.println("native get: "+val);

        //set
        TReflect.setFieldValueNatvie(obj, "string", "nativeFieldSet");
        System.out.println("native set");

        //get
        val = TReflect.getFieldValueNatvie(obj, "string");
        System.out.println("native get: "+ val);


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

        Method method = TReflect.findMethod(TestObject.class, "getData", new Class[]{String.class, Integer.class});

        Object[] objs = new Object[]{"aaaa", 111};

        System.out.println("==========================get==========================");


        System.out.println("direct: " + TEnv.measureTime(()->{
            for(int i=0;i<1000000;i++){
                try {
                    obj.getString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("reflect: " + TEnv.measureTime(()->{
            for(int i=0;i<1000000;i++){
                try {
                    TReflect.getFieldValue(obj, "string");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("native: " + TEnv.measureTime(()->{
            for(int i=0;i<1000000;i++){
                try {
                    TReflect.getFieldValueNatvie(obj, "string");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("==========================set==========================");

        System.out.println("direct: " + TEnv.measureTime(()->{
            for(int i=0;i<1000000;i++){
                try {
                    obj.setString("123123");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("reflect: " + TEnv.measureTime(()->{
            for(int i=0;i<1000000;i++){
                try {
                    TReflect.setFieldValue(obj, "string", "123123");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("native: " + TEnv.measureTime(()->{
            for(int i=0;i<1000000;i++){
                try {
                    TReflect.setFieldValueNatvie(obj, "string", "123123");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("==========================invoke==========================");
        System.out.println("direct: " + TEnv.measureTime(()->{
            for(int i=0;i<1000000;i++){
                try {
                    obj.getData("aaaa", 111);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("reflect: " + TEnv.measureTime(()->{
            for(int i=0;i<1000000;i++){
                try {
                    TReflect.invokeMethod(obj, method, objs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);



    }
}
