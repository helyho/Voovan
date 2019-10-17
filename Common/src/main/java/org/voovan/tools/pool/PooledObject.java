package org.voovan.tools.pool;

/**
 * 池化对象需要继承的对象
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class PooledObject implements IPooledObject {
    private long poolObjectId;

    public long getPoolObjectId() {
        return poolObjectId;
    }

    public void setPoolObjectId(long poolObjectId){
        this.poolObjectId = poolObjectId;
    }
}
