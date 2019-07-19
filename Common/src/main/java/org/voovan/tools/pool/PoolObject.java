package org.voovan.tools.pool;

/**
 * 池化对象需要实现的结果
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public interface PoolObject {
    public long getPoolObjectId();

    public void setPoolObjectId(long objectId);
}
