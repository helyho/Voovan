package org.voovan.tools.serialize;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.voovan.tools.TByte;
import org.voovan.tools.TPerformance;
import org.voovan.tools.collection.ThreadObjectPool;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * ProtoStuff 的序列化实现
 *
 * @author: helyho
 * ignite-test Framework.
 * WebSite: https://github.com/helyho/ignite-test
 * Licence: Apache v2 License
 */
public class ProtoStuffSerialize implements Serialize {

    static Schema PROTOSTUFF_WARP = getSchema(ProtoStuffWrap.class);

    ThreadObjectPool<LinkedBuffer> threadBufferPool = new ThreadObjectPool<LinkedBuffer>(TPerformance.getProcessorCount());

    public static Schema getSchema(Class clazz) {
        return RuntimeSchema.getSchema(clazz);
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
            LinkedBuffer buffer = threadBufferPool.get(()->LinkedBuffer.allocate(512));
            try {
                Object wrapObj = obj;
                if(obj instanceof Map) {
                    wrapObj = new ProtoStuffWrap((Map) obj, null);
                    schema = PROTOSTUFF_WARP;

                }

                if(obj instanceof Collection) {
                    wrapObj = new ProtoStuffWrap(null, (Collection) obj);
                    schema = PROTOSTUFF_WARP;
                }

                buf = ProtostuffIOUtil.toByteArray(wrapObj, schema, buffer);
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
                    Object wrapObj = obj;

                    if(obj instanceof Map || obj instanceof Collection) {
                        wrapObj = new ProtoStuffWrap();
                        schema = PROTOSTUFF_WARP;
                    }

                    ProtostuffIOUtil.mergeFrom(valueBytes, 0, valueBytes.length, wrapObj, schema);

                    if(obj instanceof Map) {
                        ((ProtoStuffWrap)wrapObj).feed((Map)obj);
                    }

                    if(obj instanceof Collection) {
                        ((ProtoStuffWrap)wrapObj).feed((Collection)obj);
                    }
                }

                return (T) obj;
            } else {
                return (T) bytes;
            }
        } catch (Exception e) {
            Logger.error(e);
        }

        return null;
    }

}
