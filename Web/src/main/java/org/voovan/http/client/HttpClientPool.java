package org.voovan.http.client;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.voovan.tools.TEnv;
import org.voovan.tools.TPerformance;
import org.voovan.tools.log.Logger;
import org.voovan.tools.pool.ObjectPool;

/**
 * httpclient 连接池
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class HttpClientPool {
    ObjectPool<HttpClient> pool;
    private static int MIN_SIZE = TPerformance.getProcessorCount();
    private static int MAX_SIZE = TPerformance.getProcessorCount();

    public HttpClientPool(String host, Integer timeout, Integer minSize, Integer maxSize, int maxBorrow) {
        pool = new ObjectPool<HttpClient>()
            .maxBorrow(maxBorrow)
            .minSize(minSize).maxSize(maxSize)
            .validator(httpClient -> httpClient.isConnect())
            .supplier(()->{
                try {
                    return HttpClient.newInstance(host, timeout);
                } catch (Exception e) {
                    Logger.error("Create HttpClient " + host + " error:", e);
                    TEnv.sleep(timeout * 1000);
                    return null;
                }
            }).destory(httpClient -> {
                httpClient.close();
                return true;
            }).create();
    }

    public HttpClientPool(String host, Integer timeout, Integer minSize, Integer maxSize) {
        this(host, timeout, minSize, maxSize, 0);
    }

    public HttpClientPool(String host, Integer timeout, Integer maxBorrow) {
        this(host, timeout, MIN_SIZE, MAX_SIZE, maxBorrow);
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
        httpClient = pool.borrow(timeout);
        if(httpClient!=null) {
            httpClient.getSocket().updateLastTime();
        }
        return httpClient;
    }

    public HttpClient getHttpClient() {
       return pool.borrow();
    }

    public void restitution(HttpClient httpClient) {
        if(httpClient!=null) {
            httpClient.reset();
        }

        pool.restitution(httpClient);
    }

    public <T> T send(ExFunction<HttpClient, T> function) {
        HttpClient httpClient = this.getHttpClient();
        try {
            return function.apply(httpClient);
        } catch (Throwable e) {
            Logger.error(e);
            return null;
        } finally {
            this.restitution(httpClient);
        }
    }

    public interface ExFunction<T, R> {
        R apply(T t) throws Exception;
    }
}
