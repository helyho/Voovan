package org.voovan.test.tools.reflect;

import junit.framework.TestCase;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.TEnv;
import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.ClassModel;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ClassModelUnit extends TestCase {

    public void test(){
        System.out.println(ClassModel.getClazzModel(String.class));
        System.out.println(ClassModel.getClazzModel(HashMap.class));
        System.out.println(ClassModel.getClazzModel(ArrayList.class));
        System.out.println(ClassModel.getClazzModel(new int[]{}.getClass()));

        String jsonModel = ClassModel.getClazzModel(TestObject.class);
        System.out.println(JSON.formatJson(jsonModel));

        ClassModel.buildClass(jsonModel);

        System.out.println(ClassModel.CLASS_CODE.get("TestObject"));
        System.out.println(ClassModel.CLASS_CODE.get("TestObject2"));

        ClassModel.buildClass("java.lang.String");
        ClassModel.buildClass((String)null);
        TEnv.sleep(1000000);
    }
}
