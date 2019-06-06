package org.voovan.tools.serialize;

import org.voovan.tools.TProperties;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

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
        String serializeType = TProperties.getString("framework", "SerializeType").trim();
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
     * @param clazz 反序列化的目标对象类型
     * @return 反序列化的对象
     */
    public static Object unserialize(byte[] bytes, Class clazz) {
        return bytes == null ? null : SERIALIZE.unserialize(bytes, clazz);
    }

    /**
     * 反序列化对象
     * @param bytes 序列化后的字节
     * @return 反序列化的对象
     */
    public static Object unserialize(byte[] bytes){
        return unserialize(bytes, null);
    }

    static ConcurrentHashMap<Class, String> CLASS_AND_SIMPLE_NAME = new ConcurrentHashMap<Class, String>();
    static ConcurrentHashMap<String, Class> SIMPLE_NAME_AND_CLASS = new ConcurrentHashMap<String, Class>();

    static {
        CLASS_AND_SIMPLE_NAME.put(int.class, "I");
        CLASS_AND_SIMPLE_NAME.put(Integer.class, "I");
        CLASS_AND_SIMPLE_NAME.put(byte.class, "B");
        CLASS_AND_SIMPLE_NAME.put(Byte.class, "B");
        CLASS_AND_SIMPLE_NAME.put(short.class, "S");
        CLASS_AND_SIMPLE_NAME.put(Short.class, "S");
        CLASS_AND_SIMPLE_NAME.put(long.class, "L");
        CLASS_AND_SIMPLE_NAME.put(Long.class, "L");
        CLASS_AND_SIMPLE_NAME.put(float.class, "F");
        CLASS_AND_SIMPLE_NAME.put(Float.class, "F");
        CLASS_AND_SIMPLE_NAME.put(double.class, "D");
        CLASS_AND_SIMPLE_NAME.put(Double.class, "D");
        CLASS_AND_SIMPLE_NAME.put(char.class, "C");
        CLASS_AND_SIMPLE_NAME.put(Character.class, "C");
        CLASS_AND_SIMPLE_NAME.put(boolean.class, "T");
        CLASS_AND_SIMPLE_NAME.put(Boolean.class, "T");

        SIMPLE_NAME_AND_CLASS.put("I", int.class);
        SIMPLE_NAME_AND_CLASS.put("I", Integer.class);
        SIMPLE_NAME_AND_CLASS.put("B", byte.class);
        SIMPLE_NAME_AND_CLASS.put("B", Byte.class);
        SIMPLE_NAME_AND_CLASS.put("S", short.class);
        SIMPLE_NAME_AND_CLASS.put("S", Short.class);
        SIMPLE_NAME_AND_CLASS.put("L", long.class);
        SIMPLE_NAME_AND_CLASS.put("L", Long.class);
        SIMPLE_NAME_AND_CLASS.put("F", float.class);
        SIMPLE_NAME_AND_CLASS.put("F", Float.class);
        SIMPLE_NAME_AND_CLASS.put("D", double.class);
        SIMPLE_NAME_AND_CLASS.put("D", Double.class);
        SIMPLE_NAME_AND_CLASS.put("C", char.class);
        SIMPLE_NAME_AND_CLASS.put("C", Character.class);
        SIMPLE_NAME_AND_CLASS.put("T", boolean.class);
        SIMPLE_NAME_AND_CLASS.put("T", Boolean.class);
    }

    protected static String getSimpleNameByClass(Class clazz){
       String className = CLASS_AND_SIMPLE_NAME.get(clazz);
       if(className == null) {
           className = TReflect.getClassName(clazz);
       }

       return className;
    }

    protected static Class getClassBySimpleName(String className) throws ClassNotFoundException {
        Class clazz = SIMPLE_NAME_AND_CLASS.get(className);
        if(clazz == null) {
            clazz = TReflect.getClassByName(className);
        }

        return clazz;
    }
}
