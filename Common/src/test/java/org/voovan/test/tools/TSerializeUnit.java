package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.TObject;
import org.voovan.tools.TSerialize;
import org.voovan.tools.reflect.TReflect;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class TSerializeUnit extends TestCase {

    public static void testJSON() throws ParseException, ReflectiveOperationException {
        TestObject m = new TestObject();
        m.setBint(111);
        m.setString("str");
        Vector<Object> v = new Vector<Object>();
        v.add("v1111");
        v.add("v2222");
        m.setList(v);
        HashMap<String, Object> k = new HashMap<>();
        k.put("m1", "v1");
        k.put("m2", "v2");
        m.setMap(k);

        Map<String, TestObject> vvv = new HashMap<>();
        vvv.putIfAbsent("mm", m);

        byte[] bytes = TSerialize.serializeJSON(vvv);
        Object object = TSerialize.unserializeJSON(bytes);
        System.out.println(object);

        Object o = TReflect.getObjectFromMap(TestObject.class, TObject.asMap(), false);
        System.out.println(object);
    }
}
