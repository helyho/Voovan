package org.voovan.tools.serialize;

/**
 * 序列化实现接口
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public interface Serialize {
    public byte[] serialize(Object obj);

    /**
     * 反序列化方法
     * @param bytes 反序列化后的字节
     * @param clazz 反序列化后的目标对象类型
     * @return 反序列化后的目标对象
     */
    public Object unserialize(byte[] bytes, Class clazz);
}
