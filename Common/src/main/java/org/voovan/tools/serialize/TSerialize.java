package org.voovan.tools.serialize;

import org.voovan.tools.TByte;
import org.voovan.tools.TProperties;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
    static ConcurrentHashMap<Class, Function<?,?>> CLASS_SERIALIZE_METHOD = new ConcurrentHashMap<Class, Function<?,?>>();
    static ConcurrentHashMap<Class, Function<?,?>> CLASS_UNSERIALIZE_METHOD = new ConcurrentHashMap<Class, Function<?,?>>();

    /**
     * 注册一个 Class 名称简写
     * @param simpleName Class 名称简写
     * @param clazz 类对象
     */
    public static void registerClassWithSimpleName(String simpleName, Class clazz){
        if(SIMPLE_NAME_AND_CLASS.contains(simpleName)) {
            throw new RuntimeException("simple name is exists");
        }
        CLASS_AND_SIMPLE_NAME.put(clazz, simpleName);
        SIMPLE_NAME_AND_CLASS.put(simpleName, clazz);
    }

    static {
        CLASS_AND_SIMPLE_NAME.put(int.class, "0");
        CLASS_AND_SIMPLE_NAME.put(Integer.class, "0");
        CLASS_AND_SIMPLE_NAME.put(byte.class, "1");
        CLASS_AND_SIMPLE_NAME.put(Byte.class, "1");
        CLASS_AND_SIMPLE_NAME.put(short.class, "2");
        CLASS_AND_SIMPLE_NAME.put(Short.class, "2");
        CLASS_AND_SIMPLE_NAME.put(long.class, "3");
        CLASS_AND_SIMPLE_NAME.put(Long.class, "3");
        CLASS_AND_SIMPLE_NAME.put(float.class, "4");
        CLASS_AND_SIMPLE_NAME.put(Float.class, "4");
        CLASS_AND_SIMPLE_NAME.put(double.class, "5");
        CLASS_AND_SIMPLE_NAME.put(Double.class, "5");
        CLASS_AND_SIMPLE_NAME.put(char.class, "6");
        CLASS_AND_SIMPLE_NAME.put(Character.class, "6");
        CLASS_AND_SIMPLE_NAME.put(boolean.class, "7");
        CLASS_AND_SIMPLE_NAME.put(Boolean.class, "7");
        CLASS_AND_SIMPLE_NAME.put(String.class, "8");
        CLASS_AND_SIMPLE_NAME.put(byte[].class, "9");

        SIMPLE_NAME_AND_CLASS.put("0", int.class);
        SIMPLE_NAME_AND_CLASS.put("0", Integer.class);
        SIMPLE_NAME_AND_CLASS.put("1", byte.class);
        SIMPLE_NAME_AND_CLASS.put("1", Byte.class);
        SIMPLE_NAME_AND_CLASS.put("2", short.class);
        SIMPLE_NAME_AND_CLASS.put("2", Short.class);
        SIMPLE_NAME_AND_CLASS.put("3", long.class);
        SIMPLE_NAME_AND_CLASS.put("3", Long.class);
        SIMPLE_NAME_AND_CLASS.put("4", float.class);
        SIMPLE_NAME_AND_CLASS.put("4", Float.class);
        SIMPLE_NAME_AND_CLASS.put("5", double.class);
        SIMPLE_NAME_AND_CLASS.put("5", Double.class);
        SIMPLE_NAME_AND_CLASS.put("6", char.class);
        SIMPLE_NAME_AND_CLASS.put("6", Character.class);
        SIMPLE_NAME_AND_CLASS.put("7", boolean.class);
        SIMPLE_NAME_AND_CLASS.put("7", Boolean.class);
        SIMPLE_NAME_AND_CLASS.put("8", String.class);
        SIMPLE_NAME_AND_CLASS.put("9", byte[].class);
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
