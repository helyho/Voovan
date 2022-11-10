package org.voovan.tools.serialize;

import org.voovan.tools.log.Logger;

import java.io.*;

/**
 * 默认 JDK 序列化的实现
 *
 * @author helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class DefaultJDKSerialize implements Serialize {
    @Override
    public byte[] serialize(Object obj) {
        if(!(obj instanceof Serializable)){
            throw new IllegalArgumentException("object must be implement Serializable");
        }

        ObjectOutputStream objectOutputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;

        byte[] result = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            result = byteArrayOutputStream.toByteArray();
        } catch (Exception e){
            Logger.error("TSerialize.serializeJDK error: ", e);
            result = null;
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    Logger.error(e);
                }
            }

            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    Logger.error(e);
                }
            }
        }

        return result;
    }

    @Override
    public <T> T unserialize(byte[] bytes) {
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
                    Logger.error(e);
                }
            }

        }

        return (T)result;
    }
}
