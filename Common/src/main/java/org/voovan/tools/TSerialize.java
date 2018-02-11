package org.voovan.tools;

import java.io.*;

/**
 * JDK 序列化和反序列化封装
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class TSerialize {
    public static byte[] serialize(Object object) {
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

    public static Object unserialize(byte[] bytes) {
        ByteArrayInputStream byteArrayInputStream = null;

        Object result = null;
        try {
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(byteArrayInputStream);
            result = ois.readObject();
        } catch (Exception e){
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
}
