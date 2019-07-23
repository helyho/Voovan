package org.voovan.test.tools;

import org.voovan.tools.TEnv;
import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        SimpleObject obj = TReflect.newInstance(SimpleObject.class, null);
        obj.setValueS("xxxxxxx");
        obj.setValueI(123123);

        TReflect.register(SimpleObject.class);

        //get
        String val = TReflect.getFieldValueNatvie(obj, "valueS");
        System.out.println("native get: "+val);

        //set
        Boolean value = TReflect.setFieldValueNatvie(obj, "valueS", "nativeFieldSet");
        System.out.println("native set:" + value);

        //get
        val = TReflect.getFieldValueNatvie(obj, "valueS");
        System.out.println("native get: "+ val);

        //set
        value = TReflect.setFieldValueNatvie(obj, "valueS", null);
        System.out.println("native set:" + value);

        //get
        val = TReflect.getFieldValueNatvie(obj, "valueS");
        System.out.println("native get: "+ val);


        System.out.println(JSON.toJSON(obj));

        Method method = TReflect.findMethod(SimpleObject.class, "getData", new Class[]{String.class, Integer.class});

        Object[] objs = new Object[]{"aaaa", 111};

        System.out.println("==========================newInstance==========================");

        System.out.println("direct: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    SimpleObject simpleObject = new SimpleObject();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        clear();
        TEnv.measure("reflect:", ()->{
            for(int i=0;i<500000;i++){
                try {
                    TReflect.newInstance(SimpleObject.class, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, TimeUnit.MILLISECONDS);

        gen();
        System.out.println("native: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    TReflect.newInstanceNative(SimpleObject.class, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);


        System.out.println("==========================get==========================");

        System.out.println("direct: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    obj.getValueS();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        clear();
        System.out.println("reflect: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    TReflect.getFieldValue(obj, "valueS");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        gen();
        System.out.println("native: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    TReflect.getFieldValueNatvie(obj, "valueS");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("==========================set==========================");

        System.out.println("direct: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    obj.setValueS("123123");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        clear();
        System.out.println("reflect: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    TReflect.setFieldValue(obj, "valueS", "123123");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        gen();
        System.out.println("native: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    TReflect.setFieldValueNatvie(obj, "valueS", "123123");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("==========================getMapfromObject==========================");
        clear();
        System.out.println("reflect: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    TReflect.getMapfromObject(obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        gen();
        System.out.println("native: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    TReflect.getMapfromObject(obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("==========================getObjectFromMap==========================");
        Map map = TReflect.getMapfromObject(obj);
        clear();
        System.out.println("reflect: " + TEnv.measure(()->{
            for(int i=0;i<50000;i++){
                try {
                    TReflect.getObjectFromMap(SimpleObject.class, map, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        gen();
        System.out.println("native: " + TEnv.measure(()->{
            for(int i=0;i<50000;i++){
                try {
                    TReflect.getObjectFromMap(SimpleObject.class, map, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);


        System.out.println("==========================JSON.toJSON==========================");
        clear();
        System.out.println("reflect: " + TEnv.measure(()->{
            for(int i=0;i<50000;i++){
                try {
                   JSON.toJSON(obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        gen();
        System.out.println("native: " + TEnv.measure(()->{
            for(int i=0;i<50000;i++){
                try {
                    JSON.toJSON(obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("==========================JSON.toObject==========================");
        String json = JSON.toJSON(obj);
        clear();
        System.out.println("reflect: " + TEnv.measure(()->{
            for(int i=0;i<50000;i++){
                try {
                    JSON.toObject(json, SimpleObject.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        gen();
        System.out.println("native: " + TEnv.measure(()->{
            for(int i=0;i<50000;i++){
                try {
                    JSON.toObject(json, SimpleObject.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        System.out.println("==========================invoke==========================");
        System.out.println("direct: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    obj.getData("aaaa", 111);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);

        clear();
        System.out.println("reflect: " + TEnv.measure(()->{
            for(int i=0;i<500000;i++){
                try {
                    TReflect.invokeMethod(obj, method, objs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })/1000000f);
    }

    public static void clear(){
        TReflect.clearRegister();;
    }

    public static void gen() throws ReflectiveOperationException {
        TReflect.register(SimpleObject.class);
    }
}
