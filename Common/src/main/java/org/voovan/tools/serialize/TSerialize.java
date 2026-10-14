package org.voovan.tools.serialize;

import org.voovan.tools.TProperties;
import org.voovan.tools.exception.SerializeException;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.security.THash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDK 序列化和反序列化封装
 *
 * @author helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class TSerialize {
    public static Serialize SERIALIZE;
    static {
        String serializeType = TProperties.getString("framework", "SerializeType", "JDK").trim();
        if("JSON".equalsIgnoreCase(serializeType.trim())){
            serializeType = "org.voovan.tools.serialize.DefaultJSONSerialize";
        } else if("JDK".equalsIgnoreCase(serializeType.trim())){
            serializeType = "org.voovan.tools.serialize.DefaultJDKSerialize";
        } else if("ProtoStuff".equalsIgnoreCase(serializeType.trim())){
            serializeType = "org.voovan.tools.serialize.ProtoStuffSerialize";
        } else if(serializeType == null){
            serializeType = "org.voovan.tools.serialize.DefaultJDKSerialize";
        }

        try {
            Class serializeClazz = Class.forName(serializeType);
            TReflect.isImp(serializeClazz, Serialize.class);
            SERIALIZE = (Serialize) TReflect.newInstance(serializeClazz);
        } catch (ReflectiveOperationException e) {
            Logger.error(e);
        }

        Logger.simple("[FRAMEWRORK] serialize type: " + serializeType);
    }

    /**
     * 序列化对象
     * @param object 待序列化的对象
     * @return 序列化的字节
     */
    public static byte[] serialize(Object object) {
        return object == null ? null : SERIALIZE.serialize(object);
    }

    /**
     * 反序列化对象
     * @param bytes 序列化后的字节
     * @return 反序列化的对象
     */
    public static Object unserialize(byte[] bytes){
        return bytes == null ? null : SERIALIZE.unserialize(bytes);
    }

    static ConcurrentHashMap<Class, Integer> CLASS_AND_HASH = new ConcurrentHashMap<Class, Integer>();
    static ConcurrentHashMap<Integer, Class> HASH_AND_CLASS = new ConcurrentHashMap<Integer, Class>();

    /**
     * 注册一个 Class 名称简写
     * @param clazz 类对象
     * @return 注册的 hashcode
     */
    public static int register(Class clazz){
        int hashcode = THash.HashFNV1(clazz.getName());
        register(hashcode, clazz);
        return hashcode;
    }

    /**
     * 注册一个 Class 名称简写
     * @param mark  Class 别名
     * @param clazz 类对象
     * @return 注册的 hashcode
     */
    public static int register(String mark, Class clazz){
        int hashcode = THash.HashFNV1(mark);
        register(hashcode, clazz);
        return hashcode;
    }

    /**
     * 注册一个 Class 名称简写
     * @param code  简写代码
     * @param clazz 类对象
     */
    public static void register(Integer code, Class clazz){
        if(HASH_AND_CLASS.containsKey(code)) {
            if(!HASH_AND_CLASS.get(code).equals(clazz)) {
                throw new SerializeException("TSerialize.registerClassWithSimpleName failed, because class or simplename is registerd");
            }
        } else {
            CLASS_AND_HASH.put(clazz, code);
            HASH_AND_CLASS.put(code, clazz);
        }
    }

    /**
     * 判断两个类是否为 primitive 与其包装类的合法 hash 共享
     * @param a 类 a
     * @param b 类 b
     * @return true: 两个类可以共享同一个 hash
     */
    private static boolean isLegalHashSharing(Class a, Class b) {
        if(a.equals(b)) {
            return true;
        }

        Class primitive = a.isPrimitive() ? a : (b.isPrimitive() ? b : null);
        if(primitive == null) {
            return false;
        }

        Class other = a.isPrimitive() ? b : a;
        try {
            // 包装类的 TYPE 静态字段指向对应的 primitive 类
            return other.getField("TYPE").get(null).equals(primitive);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /**
     * 注册内置类型, 注册的 hash 与已有 hash 冲突时抛出异常
     * @param clazz 类对象
     * @param code  hash 码
     */
    private static void putBuiltinClass(Class clazz, int code) {
        for (Map.Entry<Class, Integer> entry : CLASS_AND_HASH.entrySet()) {
            if (entry.getValue() == code && !isLegalHashSharing(entry.getKey(), clazz)) {
                throw new SerializeException("TSerialize hash conflict, class [" + clazz.getName()
                        + "] and class [" + entry.getKey().getName() + "] has the same hash [" + code + "]");
            }
        }

        CLASS_AND_HASH.put(clazz, code);
    }

    static {
        putBuiltinClass(int.class,       THash.HashFNV1(Integer.class.getName()));
        putBuiltinClass(Integer.class,   THash.HashFNV1(Integer.class.getName()));
        putBuiltinClass(byte.class,      THash.HashFNV1(Byte.class.getName()));
        putBuiltinClass(Byte.class,      THash.HashFNV1(Byte.class.getName()));
        putBuiltinClass(short.class,     THash.HashFNV1(Short.class.getName()));
        putBuiltinClass(Short.class,     THash.HashFNV1(Short.class.getName()));
        putBuiltinClass(long.class,      THash.HashFNV1(Long.class.getName()));
        putBuiltinClass(Long.class,      THash.HashFNV1(Long.class.getName()));
        putBuiltinClass(float.class,     THash.HashFNV1(Float.class.getName()));
        putBuiltinClass(Float.class,     THash.HashFNV1(Float.class.getName()));
        putBuiltinClass(double.class,    THash.HashFNV1(Double.class.getName()));
        putBuiltinClass(Double.class,    THash.HashFNV1(Double.class.getName()));
        putBuiltinClass(char.class,      THash.HashFNV1(Character.class.getName()));
        putBuiltinClass(Character.class, THash.HashFNV1(Character.class.getName()));
        putBuiltinClass(boolean.class,   THash.HashFNV1(Boolean.class.getName()));
        putBuiltinClass(Boolean.class,   THash.HashFNV1(Boolean.class.getName()));
        putBuiltinClass(String.class,    THash.HashFNV1(String.class.getName()));
        putBuiltinClass(ArrayList.class, THash.HashFNV1(ArrayList.class.getName()));

        // 注册所有基础类型及包装类型的数组形式
        Class[] arrayClasses = {
            int[].class,      Integer[].class,
            byte[].class,     Byte[].class,
            short[].class,    Short[].class,
            long[].class,     Long[].class,
            float[].class,    Float[].class,
            double[].class,   Double[].class,
            char[].class,     Character[].class,
            boolean[].class,  Boolean[].class,
            String[].class
        };
        for (Class arrayClass : arrayClasses) {
            putBuiltinClass(arrayClass, THash.HashFNV1(arrayClass.getName()));
        }
        putBuiltinClass(HashMap.class,    THash.HashFNV1(HashMap.class.getName()));
        putBuiltinClass(HashSet.class,    THash.HashFNV1(HashSet.class.getName()));
        putBuiltinClass(BigDecimal.class, THash.HashFNV1(BigDecimal.class.getName()));
//        CLASS_AND_HASH.put(byte[].class,    THash.HashFNV1(byte[].class.getName()));

        for(Map.Entry<Class, Integer> entry : CLASS_AND_HASH.entrySet()) {

            if(!entry.getKey().isPrimitive()) {
                Class oldClass = HASH_AND_CLASS.put(entry.getValue(), entry.getKey());
                if(oldClass != null && !oldClass.equals(entry.getKey())) {
                    throw new SerializeException("TSerialize hash conflict, hash [" + entry.getValue()
                            + "] already used by class [" + oldClass.getName() + "]");
                }
            }
        }
    }

    protected static Integer getHashByClass(Class clazz){
        Integer hashcode = CLASS_AND_HASH.get(clazz);

        if (hashcode == null) {
            hashcode = register(clazz);
        }

        return hashcode;
    }

    protected static Class getClassByHash(Integer hashcode) throws ClassNotFoundException {
        
        Class clazz = hashcode == null  ? null : HASH_AND_CLASS.get(hashcode);

        if(clazz == null) {
//            Logger.error("Class hash " + hashcode + " is not found, please register this class first");
            return null;
        }

        return clazz;
    }
}
