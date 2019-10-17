package org.voovan.tools.pool;

/**
 * 池化对象需要实现的接口
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public interface IPooledObject {
    public long getPoolObjectId();

    public void setPoolObjectId(long objectId);
}
