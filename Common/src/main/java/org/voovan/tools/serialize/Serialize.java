package org.voovan.tools.serialize;

/**
 * 序列化实现接口
 *
 * @author helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public interface Serialize {
    public byte[] serialize(Object obj);

    /**
     * 反序列化方法
     * @param <T> 范型类型
     * @param bytes 反序列化后的字节
     * @return 反序列化后的目标对象
     */
     public <T> T unserialize(byte[] bytes);
}
