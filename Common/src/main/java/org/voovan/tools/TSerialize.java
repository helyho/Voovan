package org.voovan.tools;

import org.voovan.tools.json.JSON;
import org.voovan.tools.json.JSONPath;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * JDK 序列化和反序列化封装
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class TSerialize {
    public final static String SERIALIZE_TYPE = TProperties.getString("framework", "SerializeType");
    static {
        Logger.simple("[SYSTEM] Cache serialize type is " + SERIALIZE_TYPE);
    }

    /**
     * 使用 JDK 序列化对象
     * @param object 待序列化的对象
     * @return 序列化的字节
     */
    public static byte[] serializeJDK(Object object) {
        if(!(object instanceof Serializable)){
            throw new IllegalArgumentException("object must be implement Serializable");
        }

        ObjectOutputStream objectOutputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;

        byte[] result = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            result = byteArrayOutputStream.toByteArray();
        } catch (Exception e){
            Logger.error("TSerialize.serializeJDK error: ", e);
            result = null;
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    /**
     * 使用 JDK 反序列化对象
     * @param bytes 序列化后的字节
     * @return 反序列化的对象
     */
    public static Object unserializeJDK(byte[] bytes) {
        ByteArrayInputStream byteArrayInputStream = null;

        Object result = null;
        try {
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(byteArrayInputStream);
            result = ois.readObject();
        } catch (Exception e){
            Logger.error("TSerialize.unserializeJDK error: ", e);
            result = null;
        } finally {
            if (byteArrayInputStream != null) {
                try {
                    byteArrayInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    /**
     * 使用 JDK 序列化对象
     * @param object 待序列化的对象
     * @return 序列化的字节
     */
    public static byte[] serializeJSON(Object object) {
        try{
            Class clazz = object.getClass();
            Class[] genericClazzs = TReflect.getGenericClass(object);

            return JSON.toJSON(TObject.asMap("T",clazz.getCanonicalName(),  "G", genericClazzs, "V", object)).getBytes();
        } catch (Exception e){
            Logger.error("TSerialize.serializeJDK error: ", e);
            return null;
        }
    }

    /**
     * 使用 JSON 反序列化对象
     * @param bytes 序列化后的字节
     * @return 反序列化的对象
     */
    public static Object unserializeJSON(byte[] bytes) {
        try {

            Class mainClazz = null;

            JSONPath jsonPath = new JSONPath(new String(bytes));

            mainClazz = Class.forName(jsonPath.value("/T", String.class));
            List<String> genericClazzStrs = jsonPath.listObject("/G", String.class);
            Class[] genericClazzs = new Class[genericClazzStrs.size()];

            for (int i = 0; i < genericClazzStrs.size(); i++) {
                genericClazzs[i] = Class.forName(genericClazzStrs.get(i));
            }

            genericClazzs = genericClazzs.length == 0 ? null : genericClazzs;


            return TReflect.getObjectFromMap(mainClazz, jsonPath.mapObject("/V", genericClazzs), true);
        } catch (Exception e){
            Logger.error("TSerialize.serializeJDK error: ", e);
            return null;
        }
    }


    /**
     * 序列化对象
     * @param object 待序列化的对象
     * @return 序列化的字节
     */
    public static byte[] serialize(Object object) {
        if("JDK".equalsIgnoreCase(SERIALIZE_TYPE)){
            return serializeJDK(object);
        } else if("JSON".equalsIgnoreCase(SERIALIZE_TYPE)){
            return serializeJSON(object);
        } else {
            return serializeJDK(object);
        }
    }

    /**
     * 反序列化对象
     * @param bytes 序列化后的字节
     * @return 反序列化的对象
     */
    public static Object unserialize(byte[] bytes) {
        if("JDK".equalsIgnoreCase(SERIALIZE_TYPE)){
            return unserializeJDK(bytes);
        } else if("JSON".equalsIgnoreCase(SERIALIZE_TYPE)){
            return unserializeJSON(bytes);
        } else {
            return unserializeJDK(bytes);
        }
    }
}
