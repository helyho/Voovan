package org.voovan.tools.cache;

import java.util.concurrent.TimeoutException;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public interface Bucket {
    public boolean acquire();
    public void acquire(int timeout) throws TimeoutException;
}
