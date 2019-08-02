package org.voovan.tools.serialize;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.voovan.tools.TByte;
import org.voovan.tools.collection.ObjectThreadPool;
import org.voovan.tools.reflect.TReflect;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProtoStuff 的序列化实现
 *
 * @author: helyho
 * ignite-test Framework.
 * WebSite: https://github.com/helyho/ignite-test
 * Licence: Apache v2 License
 */
public class ProtoStuffSerialize implements Serialize {

    ObjectThreadPool<LinkedBuffer> objectThreadPool = new ObjectThreadPool<LinkedBuffer>(128);

    Map<Class, Schema> SCHEMAS = new ConcurrentHashMap<Class, Schema>();

    public Schema getSchema(Class clazz) {
        Schema schema = SCHEMAS.get(clazz);
        if(schema == null) {
            schema = RuntimeSchema.getSchema(clazz);
        }

        return schema;
    }

    @Override
    public byte[] serialize(Object obj) {
        //字节数组直接返回
        if(obj instanceof byte[]) {
            return (byte[])obj;
        }

        byte[] buf = null;
        buf = TByte.toBytes(obj);
        if(buf==null) {
            Schema schema = getSchema(obj.getClass());
            LinkedBuffer buffer =objectThreadPool.get(()->LinkedBuffer.allocate(512));
            try {
                buf = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
            } finally {
                buffer.clear();
            }
        }

        byte[] type = TByte.getBytes(TSerialize.getHashByClass(obj.getClass()));
        buf = TByte.byteArrayConcat(type, type.length, buf, buf.length);

        return buf;
    }

    @Override
    public <T> T unserialize(byte[] bytes) {
        try {
            Integer hashcode = null;
            if(bytes.length >= 4) {
                hashcode = TByte.getInt(bytes);
            }

            Class innerClazz = hashcode==null ? null : TSerialize.getClassByHash(hashcode);

            //如果没有明确的类指示,则直接返回字节数组
            if(innerClazz != null) {
                byte[] valueBytes = Arrays.copyOfRange(bytes, 4, bytes.length);

                Object obj = TByte.toObject(valueBytes, innerClazz);

                if (obj == null) {
                    Schema schema = getSchema(innerClazz);
                    obj = TReflect.newInstance(innerClazz);
                    ProtostuffIOUtil.mergeFrom(valueBytes, 0, valueBytes.length, obj, schema);
                }

                return (T) obj;
            } else {
                return (T) bytes;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
