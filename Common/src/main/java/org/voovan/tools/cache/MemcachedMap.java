package org.voovan.tools.cache;

import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.voovan.tools.cache.CacheStatic.defaultPoolSize;

/**
 * 基于 Memcached 的 Map 实现
 *      简单实现,key 和 value 都是 String 类型
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class MemcachedMap implements Map<String, String> , Closeable {
    private MemcachedClientBuilder memcachedClientBuilder;
    private MemcachedClient memcachedClient = null;
    private int size;

    /**
     * 构造函数
     * @param host        Memcached 服务地址
     * @param port        Memcached 服务端口
     * @param timeout     Memcached 超时时间
     * @param poolSize    Memcached 服务密码
     */
    public MemcachedMap(String host, int port, int timeout, int poolSize){
        super();
        if(memcachedClientBuilder == null) {
            try {

                if (poolSize == 0) {
                    poolSize = defaultPoolSize();
                }

                memcachedClientBuilder = new XMemcachedClientBuilder(
                        AddrUtil.getAddresses(host + ":" + port));
                memcachedClientBuilder.setFailureMode(true);
                memcachedClientBuilder.setCommandFactory(new BinaryCommandFactory());
                memcachedClientBuilder.setConnectionPoolSize(poolSize);
                memcachedClientBuilder.setConnectTimeout(timeout);
            }catch (Exception e){
                Logger.error("Read ./classes/Memcached.properties error");
            }
        }
        this.size = 0;
    }

    /**
     * 构造函数
     * @param memcachedClientBuilder Memcached 连接池对象
     */
    public MemcachedMap(MemcachedClientBuilder memcachedClientBuilder){
        this.memcachedClientBuilder = memcachedClientBuilder;
    }

    /**
     * 构造函数
     */
    public MemcachedMap(){
        this.memcachedClientBuilder = CacheStatic.getMemcachedPool();
        this.size = 0;
    }

    /**
     * 获取Memcached连接
     * @return Memcached连接
     */
    private MemcachedClient getMemcachedClient(){
        if(memcachedClient == null || memcachedClient.isShutdown()) {
            try {
                memcachedClient = memcachedClientBuilder.build();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return memcachedClient;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            return memcachedClient.get((String) key)!=null;
        }catch (Exception e){
            Logger.error(e);
            return false;
        }
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String get(Object key) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            return memcachedClient.get((String) key);
        }catch (Exception e){
            Logger.error(e);
            return null;
        }
    }

    /**
     * 获取 memcached 中的对象
     * @param key key 名称
     * @param clazz 对象类型
     * @param <T> 范型
     * @return 对象
     */
    public <T> T getObj(Object key, Class<T> clazz) {
        return (T)JSON.toObject(get(key), clazz);
    }

    @Override
    public String put(String key, String value) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            if(memcachedClient.set(key, 0, value)) {
                return value;
            }else{
                return null;
            }
        }catch (Exception e){
            Logger.error(e);
            return null;
        }
    }

    /**
     * 向 memcached 中放置字符串数据
     * @param key key 名称
     * @param value 数据
     * @param expire 超时事件
     * @return true: 成功, false:失败
     */
    public boolean put(String key, String value, long expire) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            return memcachedClient.set(key, 0, value, expire);
        }catch (Exception e){
            Logger.error(e);
            return false;
        }
    }

    /**
     * 像 memcached 中放置对象数据
     * @param key key 名称
     * @param value 数据
     * @return true: 成功, false:失败
     */
    public String putObj(String key, Object value) {
        return put(key, JSON.toJSON(value));
    }

    /**
     * 像 memcached 中放置对象数据
     * @param key key 名称
     * @param value 数据
     * @param expire 超时时间
     * @return true: 成功, false:失败
     */
    public boolean putObj(String key, Object value, int expire) {
        return put(key, JSON.toJSON(value), expire);
    }

    /**
     * 像 memcached 中放置对象数据
     *      不管数据存在不存在都会将目前设置的数据存储的 memcached，但不等待返回确认
     * @param key key 名称
     * @param value 数据
     * @param expire 超时时间
     * @return true: 成功, false:失败
     */
    public Boolean putWithNoReply(String key, Object value, int expire) {
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            memcachedClient.setWithNoReply(key, expire, value);
        } catch (Exception e) {
            Logger.error(e);
            return false;
        }
        return true;
    }

    @Override
    public String remove(Object key) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            String value = memcachedClient.get(key.toString());
            memcachedClient.delete(key.toString());
            return value;
        }catch (Exception e){
            Logger.error(e);
            return null;
        }
    }

    /**
     * 删除指定的 key,不等待返回
     * @param key
     */
    public void removeWithNoReply(Object key) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            String value = memcachedClient.get(key.toString());
            memcachedClient.deleteWithNoReply(key.toString());
        }catch (Exception e){
            Logger.error(e);
        }
    }

    @Override
    public void putAll(Map map) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            for (Object obj : map.entrySet()) {
                Entry entry = (Entry) obj;
                memcachedClient.set(entry.getKey().toString(), 0,entry.getValue().toString());
            }
        }catch (Exception e){
            Logger.error(e);
        }
    }

    @Override
    public void clear() {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            memcachedClient.flushAll();
        }catch (Exception e){
            Logger.error(e);
        }
    }

    /**
     * 原子减少操作
     * @param key  key 名称
     * @param value 值
     * @return 自增后的结果
     */
    public long decr(String key, long value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.decr(key , value);
        } catch (Exception e) {
            Logger.error(e);
        }
        return 0;
    }

    /**
     * 原子减少操作
     * @param key  key 名称
     * @param value 值
     * @param initValue 默认值
     * @return 自增后的结果
     */
    public long decr(String key, long value , long initValue){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.decr(key , value , initValue);
        } catch (Exception e) {
            Logger.error(e);
        }
        return 0;
    }

    /**
     * 原子增加操作
     * @param key  key 名称
     * @param value 值
     * @return 自增后的结果
     */
    public long incr(String key, long value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.incr(key , value);
        } catch (Exception e) {
            Logger.error(e);
        }
        return 0;
    }

    /**
     * 原子增加操作
     * @param key  key 名称
     * @param value 值
     * @param initValue 默认值
     * @return 自增后的结果
     */
    public long incr(String key, long value , long initValue){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.incr(key , value , initValue);
        } catch (Exception e) {
            Logger.error(e);
        }
        return 0;
    }

    /**
     * 向指定的 key 尾部追加数据
     * @param key key 名称
     * @param value 数据
     * @return
     */
    public boolean append(String key, Object value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.append(key , value);
        } catch (Exception e) {
            Logger.error(e);
        }
        return false;
    }

    /**
     * 向指定的 key 头部追加数据
     * @param key key 名称
     * @param value 数据
     * @return
     */
    public boolean prepend(String key, Object value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.prepend(key , value);
        } catch (Exception e) {
            Logger.error(e);
        }
        return false;
    }

    /**
     * 向指定的 key 尾部追加数据,不等待返回
     * @param key key 名称
     * @param value 数据
     * @return
     */
    public void appendWithNoReply(String key, Object value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            memcachedClient.appendWithNoReply(key , value);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    /**
     * 向指定的 key 头部追加数据,不等待返回
     * @param key key 名称
     * @param value 数据
     * @return
     */
    public void prependWithNoReply(String key, Object value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            memcachedClient.prependWithNoReply(key , value);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    /**
     * 原子操作
     * @param key key 名称
     * @param value 值
     * @param version 数据版本
     * @return true: 成功, false:失败
     */
    public boolean cas(String key, Object value,long version) {
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return  memcachedClient.cas(key, 0, value, version);
        } catch (Exception e) {
            Logger.error(e);
            return false;
        }
    }

    /**
     * 原子操作
     * @param key key 名称
     * @param value 值
     * @param time 超时
     * @param version 数据版本
     * @return true: 成功, false:失败
     */
    public boolean cas(String key, Object value, int time, long version) {
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return  memcachedClient.cas(key, time, value, version);
        } catch (Exception e) {
            Logger.error(e);
            return false;
        }
    }

    @Override
    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        memcachedClient.shutdown();
    }
}
