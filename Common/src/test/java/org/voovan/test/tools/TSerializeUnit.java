package org.voovan.test.tools;

import org.voovan.tools.security.THash;
import org.voovan.tools.serialize.TSerialize;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * TSerialize hash 注册与冲突检查测试
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TSerializeUnit {
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        checkInitSize();
        checkBuiltinLoop();
        checkSerializeLoop();
        checkLegalSharing();
        checkConflict();
        checkArrayConflict();

        if (failed > 0) {
            throw new IllegalStateException("TSerializeUnit failed: " + failed);
        }
        System.out.println("[TSerializeUnit] all passed");
    }

    static void assertTrue(boolean cond, String name) {
        System.out.println((cond ? "[OK] " : "[FAIL] ") + name);
        if (!cond) {
            failed++;
        }
    }

    /** 静态初始化完成, 数量与设计一致 */
    static void checkInitSize() throws Exception {
        Map classAndHash = getClassAndHash();
        Map hashAndClass = getHashAndClass();
        assertTrue(classAndHash.size() == 38, "CLASS_AND_HASH.size == 38, actual: " + classAndHash.size());
        assertTrue(hashAndClass.size() == 30, "HASH_AND_CLASS.size == 30, actual: " + hashAndClass.size());
    }

    /** 内置类型的 hash 与反查环路 */
    static void checkBuiltinLoop() throws Exception {
        Method getHash = TSerialize.class.getDeclaredMethod("getHashByClass", Class.class);
        Method getClass = TSerialize.class.getDeclaredMethod("getClassByHash", Integer.class);
        getHash.setAccessible(true);
        getClass.setAccessible(true);

        Integer intHash = (Integer) getHash.invoke(null, int.class);
        assertTrue(intHash.equals(THash.HashFNV1(Integer.class.getName())), "int.class share hash with Integer.class");
        assertTrue(Integer.class.equals(getClass.invoke(null, intHash)), "int hash resolve to Integer.class");

        int[] array = new int[0];
        Integer arrayHash = (Integer) getHash.invoke(null, int[].class);
        assertTrue(int[].class.equals(getClass.invoke(null, arrayHash)), "int[].class hash loop");

        Integer mapHash = (Integer) getHash.invoke(null, HashMap.class);
        assertTrue(HashMap.class.equals(getClass.invoke(null, mapHash)), "HashMap hash loop");
    }

    /** 序列化/反序列化不受改动影响 */
    static void checkSerializeLoop() {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("a", 1);
        map.put("b", 2);
        Object mapObj = TSerialize.unserialize(TSerialize.serialize(map));
        assertTrue(mapObj instanceof HashMap && ((HashMap) mapObj).size() == 2,
                "serialize/unserialize loop (Map)");

        Object strObj = TSerialize.unserialize(TSerialize.serialize("hello"));
        assertTrue("hello".equals(strObj), "serialize/unserialize loop (String)");

        ArrayList<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        Object listObj = TSerialize.unserialize(TSerialize.serialize(list));
        assertTrue(listObj instanceof ArrayList && ((ArrayList) listObj).size() == 2
                && "a".equals(((ArrayList) listObj).get(0)),
                "serialize/unserialize loop (ArrayList)");

        HashSet<String> set = new HashSet<String>();
        set.add("x");
        set.add("y");
        Object setObj = TSerialize.unserialize(TSerialize.serialize(set));
        assertTrue(setObj instanceof HashSet && ((HashSet) setObj).size() == 2
                && ((HashSet) setObj).contains("x"),
                "serialize/unserialize loop (HashSet)");
    }

    /** primitive 与包装类的合法共享不报错 */
    static void checkLegalSharing() throws Exception {
        Method put = TSerialize.class.getDeclaredMethod("putBuiltinClass", Class.class, int.class);
        put.setAccessible(true);
        put.invoke(null, int.class, THash.HashFNV1(Integer.class.getName()));
        System.out.println("[OK] legal primitive/box sharing");
    }

    /** 非 primitive 类 hash 冲突报错 */
    static void checkConflict() throws Exception {
        Method put = TSerialize.class.getDeclaredMethod("putBuiltinClass", Class.class, int.class);
        put.setAccessible(true);
        try {
            put.invoke(null, TSerializeUnit.class, THash.HashFNV1(String.class.getName()));
            assertTrue(false, "hash conflict not detected");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof org.voovan.tools.exception.SerializeException,
                    "conflict throws SerializeException: " + e.getCause().getMessage());
        }
    }

    /** 数组类之间冲突同样报错 */
    static void checkArrayConflict() throws Exception {
        Method put = TSerialize.class.getDeclaredMethod("putBuiltinClass", Class.class, int.class);
        put.setAccessible(true);
        try {
            put.invoke(null, String[].class, THash.HashFNV1(int[].class.getName()));
            assertTrue(false, "array hash conflict not detected");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof org.voovan.tools.exception.SerializeException,
                    "array conflict throws SerializeException: " + e.getCause().getMessage());
        }
    }

    static Map getClassAndHash() throws Exception {
        Field field = TSerialize.class.getDeclaredField("CLASS_AND_HASH");
        field.setAccessible(true);
        return (Map) field.get(null);
    }

    static Map getHashAndClass() throws Exception {
        Field field = TSerialize.class.getDeclaredField("HASH_AND_CLASS");
        field.setAccessible(true);
        return (Map) field.get(null);
    }
}
