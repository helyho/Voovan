package org.voovan.test.tools.serialize;

import junit.framework.TestCase;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.TObject;
import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.serialize.ProtoStuffSerialize;
import org.voovan.tools.serialize.TSerialize;

import java.text.ParseException;
import java.util.*;

/**
 * 类文字命名
 *
 * @author helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class TSerializationUnit extends TestCase {

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

        byte[] bytes = TSerialize.serialize(vvv);
        Object object = TSerialize.unserialize(bytes);
        System.out.println(object);

        Object o = TReflect.getObjectFromMap(TestObject.class, TObject.asMap(), false);
        System.out.println(object);
    }

    public void testProtoStuffMapCollection() {
        TSerialize.SERIALIZE = new ProtoStuffSerialize();
        byte[] mm;
        Object mk;

        HashMap m = new HashMap();
//        m.putAll(TObject.asMap("a", "123123", "1", 123));
        mm =TSerialize.serialize(m);
        mk = TSerialize.unserialize(mm);
        System.out.println(mk);

        List m1 = new ArrayList();
        m1.add("123123");
        m1.add(123123);
        mm =TSerialize.serialize(m1);
        mk = TSerialize.unserialize(mm);
        System.out.println(mk);

        HashSet hashSet = new HashSet();
        hashSet.add("345345");
        mm =TSerialize.serialize(hashSet);
        mk = TSerialize.unserialize(mm);
        System.out.println(mk);

        TestObject testObject = new TestObject();
        testObject.setString("helyho");
        mm =TSerialize.serialize(testObject);
        mk = TSerialize.unserialize(mm);
        System.out.println(JSON.toJSON(mk));


    }
}
