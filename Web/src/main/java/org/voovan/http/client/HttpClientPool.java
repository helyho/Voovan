package org.voovan.http.client;

import org.voovan.tools.TPerformance;
import org.voovan.tools.pool.ObjectPool;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * httpclient 连接池
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class HttpClientPool {
    ObjectPool<HttpClient> pool;
    private static int MIN_SIZE = TPerformance.getProcessorCount();
    private static int MAX_SIZE = TPerformance.getProcessorCount();

    public HttpClientPool(String host, Integer timeout, Integer minSize, Integer maxSize) {
        pool = new ObjectPool<HttpClient>()
            .minSize(minSize).maxSize(maxSize)
            .validator(httpClient -> httpClient.isConnect())
            .supplier(()->{
                return new HttpClient(host, timeout);
            }).destory(httpClient -> {
                httpClient.close();
                return true;
            }).create();
    }

    public HttpClientPool(String host, Integer timeout) {
        this(host, timeout, MIN_SIZE, MAX_SIZE);
    }

    public ObjectPool<HttpClient> getPool() {
        return pool;
    }

    public HttpClient getHttpClient(long timeout, TimeUnit timeUnit) throws TimeoutException {
        HttpClient httpClient = null;
        timeout = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        long start = System.currentTimeMillis();
        do {
            if(System.currentTimeMillis() - start > timeout) {
                throw new TimeoutException("get httpclient timeout");
            }
            httpClient = pool.borrow();
        } while (httpClient==null);

        return httpClient;
    }

    public HttpClient getHttpClient() {
       return pool.borrow();
    }

    public void restitution(HttpClient httpClient) {
        pool.restitution(httpClient);
    }
}
