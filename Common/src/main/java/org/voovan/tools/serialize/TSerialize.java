package org.voovan.tools.serialize;

import org.voovan.tools.TProperties;
import org.voovan.tools.exception.SerializeException;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.security.THash;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDK 序列化和反序列化封装
 *
 * @author: helyho
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
            TReflect.isImpByInterface(serializeClazz, Serialize.class);
            SERIALIZE = (Serialize) TReflect.newInstance(serializeClazz);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }

        Logger.simple("[SYSTEM] serialize type: " + serializeType);
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
     */
    public static void register(Class clazz){
        int hashcode = THash.HashFNV1(clazz.getName());
        register(hashcode, clazz);
    }

    /**
     * 注册一个 Class 名称简写
     * @param code  简写代码
     * @param clazz 类对象
     */
    public static void register(Integer code, Class clazz){
        if(HASH_AND_CLASS.contains(code)) {
            throw new RuntimeException("simple name is exists");
        }

        if(CLASS_AND_HASH.containsKey(clazz) || HASH_AND_CLASS.containsKey(code)) {
            throw new SerializeException("TSerialize.registerClassWithSimpleName failed, because class or simplename is registerd");
        } else {
            CLASS_AND_HASH.put(clazz, code);
            HASH_AND_CLASS.put(code, clazz);
        }
    }

    static {
        CLASS_AND_HASH.put(int.class,       THash.HashFNV1(Integer.class.getName()));
        CLASS_AND_HASH.put(Integer.class,   THash.HashFNV1(Integer.class.getName()));
        CLASS_AND_HASH.put(byte.class,      THash.HashFNV1(Byte.class.getName()));
        CLASS_AND_HASH.put(Byte.class,      THash.HashFNV1(Byte.class.getName()));
        CLASS_AND_HASH.put(short.class,     THash.HashFNV1(Short.class.getName()));
        CLASS_AND_HASH.put(Short.class,     THash.HashFNV1(Short.class.getName()));
        CLASS_AND_HASH.put(long.class,      THash.HashFNV1(Long.class.getName()));
        CLASS_AND_HASH.put(Long.class,      THash.HashFNV1(Long.class.getName()));
        CLASS_AND_HASH.put(float.class,     THash.HashFNV1(Float.class.getName()));
        CLASS_AND_HASH.put(Float.class,     THash.HashFNV1(Float.class.getName()));
        CLASS_AND_HASH.put(double.class,    THash.HashFNV1(Double.class.getName()));
        CLASS_AND_HASH.put(Double.class,    THash.HashFNV1(Double.class.getName()));
        CLASS_AND_HASH.put(char.class,      THash.HashFNV1(Character.class.getName()));
        CLASS_AND_HASH.put(Character.class, THash.HashFNV1(Character.class.getName()));
        CLASS_AND_HASH.put(boolean.class,   THash.HashFNV1(Boolean.class.getName()));
        CLASS_AND_HASH.put(Boolean.class,   THash.HashFNV1(Boolean.class.getName()));
        CLASS_AND_HASH.put(String.class,    THash.HashFNV1(String.class.getName()));
        CLASS_AND_HASH.put(byte[].class,    THash.HashFNV1(byte[].class.getName()));

        for(Map.Entry<Class, Integer> entry : CLASS_AND_HASH.entrySet()) {

            if(!entry.getKey().isPrimitive()) {
                HASH_AND_CLASS.put(entry.getValue(), entry.getKey());
            }
        }

    }

    protected static Integer getHashByClass(Class clazz){
       Integer hashcode = CLASS_AND_HASH.get(clazz);
       if(hashcode == null) {
          register(clazz);
       }

       return hashcode;
    }

    protected static Class getClassByHash(Integer hashcode) throws ClassNotFoundException {
        Class clazz = HASH_AND_CLASS.get(hashcode);
        if(clazz == null) {
            return null;
        }

        return clazz;
    }
}
