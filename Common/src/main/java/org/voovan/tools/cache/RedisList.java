package org.voovan.tools.cache;

import redis.clients.jedis.BinaryClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * 基于 Redis 的 List 实现
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisList<V> implements List<V>, Deque<V>, Closeable {
    private JedisPool redisPool;
    private String name = null;
    private int dbIndex = 0;

    /**
     * 构造函数
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     * @param name        在 redis 中的 HashMap的名称
     * @param password    redis 服务密码
     */
    public RedisList(String host, int port, int timeout, int poolsize, String name, String password){
        super();

        //如果没有指定JedisPool的配置文件,则使用默认的
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(poolsize);
        poolConfig.setMaxTotal(poolsize);

        //如果没有指定密码,则默认不需要密码
        if(password==null) {
            redisPool = new JedisPool(poolConfig, host, port, timeout);
        }else {
            redisPool = new JedisPool(poolConfig, host, port, timeout, password);
        }
        this.name = name;
    }

    /**
     * 构造函数
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     * @param name        在 redis 中的 HashMap的名称
     */
    public RedisList(String host, int port, int timeout, int poolsize, String name){
        super();

        //如果没有指定JedisPool的配置文件,则使用默认的
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(poolsize);
        poolConfig.setMaxTotal(poolsize);

        redisPool = new JedisPool(poolConfig, host, port, timeout);
        this.name = name;
    }

    /**
     * 构造函数
     * @param name 在 redis 中的 HashMap的名称
     */
    public RedisList(String name){
        this.redisPool = CacheStatic.getRedisPool();
        this.name = name;
    }

    /**
     * 获取当前选择的数据集
     * @return 数据集序号
     */
    public int getDbIndex() {
        return dbIndex;
    }

    private Jedis getJedis(){
        Jedis Jedis = redisPool.getResource();
        Jedis.select(dbIndex);
        return Jedis;
    }

    /**
     * 选择当前数据集
     * @param dbIndex 数据集序号
     */
    public void setDbIndex(int dbIndex) {
        this.dbIndex = dbIndex;
    }

    @Override
    public void close() throws IOException {
        redisPool.close();
    }

    @Override
    public int size() {
        try(Jedis jedis = getJedis()) {
            return Long.valueOf(jedis.llen(name)).intValue();
        }
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<V> iterator() {
        return (Iterator<V>)new RedisListIterator(this, false);
    }

    @Override
    public Iterator<V> descendingIterator() {
        return (Iterator<V>)new RedisListIterator(this, true);
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offerFirst(V s) {
        try (Jedis jedis = getJedis()) {
            jedis.lpush(name.getBytes(), CacheStatic.serialize(s));
            return true;
        }
    }

    @Override
    public boolean offerLast(V s) {
        try (Jedis jedis = getJedis()) {
            jedis.rpush(name.getBytes(), CacheStatic.serialize(s));
            return true;
        }
    }

    @Override
    public void addFirst(V s) {
        offerFirst(s);
    }

    @Override
    public void addLast(V s) {
        offerLast(s);
    }

    @Override
    public V removeFirst() {
        try (Jedis jedis = getJedis()) {
            return (V)CacheStatic.unserialize(jedis.lpop(name.getBytes()));
        }
    }

    @Override
    public V removeLast() {
        try (Jedis jedis = getJedis()) {
            return (V)CacheStatic.unserialize(jedis.rpop(name.getBytes()));
        }
    }

    public List<V> removeFirst(int timeout) {
        try (Jedis jedis = getJedis()) {
            ArrayList<V> result = new ArrayList<V>();
            List<byte[]> queryResult = jedis.blpop(timeout, name.getBytes());
            for(byte[] bytes : queryResult){
                result.add((V)CacheStatic.unserialize(bytes));
            }
            return result;
        }
    }

    public List<V> removeLast(int timeout) {
        try (Jedis jedis = getJedis()) {
            ArrayList<V> result = new ArrayList<V>();
            List<byte[]> queryResult = jedis.blpop(timeout, name.getBytes());
            for(byte[] bytes : queryResult){
                result.add((V)CacheStatic.unserialize(bytes));
            }
            return result;
        }
    }

    @Override
    public V pollFirst() {
        return removeFirst();
    }

    @Override
    public V pollLast() {
        return removeLast();
    }

    @Override
    public V getFirst() {
        try (Jedis jedis = getJedis()) {
            return (V)CacheStatic.unserialize(jedis.lindex(name.getBytes(), 0));
        }
    }

    @Override
    public V getLast() {
        try (Jedis jedis = getJedis()) {
            return (V)CacheStatic.unserialize(jedis.lindex(name.getBytes(), -1));
        }
    }

    @Override
    public V peekFirst() {
        return getFirst();
    }

    @Override
    public V peekLast() {
        return getLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        try (Jedis jedis = getJedis()) {
            int rmCount =  Long.valueOf(jedis.lrem(name, 1, o.toString())).intValue();
            return rmCount > 0 ;
        }
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        try (Jedis jedis = getJedis()) {
            int rmCount =  Long.valueOf(jedis.lrem(name, -1, o.toString())).intValue();
            return rmCount > 0 ;
        }
    }

    @Override
    public boolean add(V s) {
        return offerLast(s);
    }

    @Override
    public boolean offer(V s) {
        return offerLast(s);
    }

    @Override
    public V remove() {
        return removeFirst();
    }

    @Override
    public V poll() {
        return pollFirst();
    }

    @Override
    public V element() {
        return getFirst();
    }

    @Override
    public V peek() {
        return peekFirst();
    }

    @Override
    public void push(V s) {
        addFirst(s);
    }

    @Override
    public V pop() {
        return removeFirst();
    }

    @Override
    public boolean remove(Object o) {
        try (Jedis jedis = getJedis()) {
            int rmCount =  Long.valueOf(jedis.lrem(name, 0, o.toString())).intValue();
            return rmCount > 0 ;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        for(V item : c){
            add(item);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
        try (Jedis jedis = getJedis()) {
            byte[] pivot = jedis.lindex(name.getBytes(), index);
            for(V item : c){
                jedis.linsert(name.getBytes(), BinaryClient.LIST_POSITION.AFTER, pivot, CacheStatic.serialize(item));
            }
        }

        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        try (Jedis jedis = getJedis()) {
            for(Object item : c){
                remove(c);
            }
        }

        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public V get(int index) {
        try (Jedis jedis = getJedis()) {
            return (V)CacheStatic.unserialize(jedis.lindex(name.getBytes(), index));
        }
    }

    @Override
    public V set(int index, V element) {
        try (Jedis jedis = getJedis()) {
            jedis.lset(name.getBytes(), index, CacheStatic.serialize(element));
            return element;
        }
    }

    @Override
    public void add(int index, V element) {
        try (Jedis jedis = getJedis()) {
            byte[] pivot = jedis.lindex(name.getBytes(), index);
            jedis.linsert(name.getBytes(), BinaryClient.LIST_POSITION.AFTER, pivot, CacheStatic.serialize(element));
        }
    }

    @Override
    public V remove(int index) {
        try (Jedis jedis = getJedis()) {
            V value = get(index);
            jedis.lrem(name.getBytes(), 1, CacheStatic.serialize(value));
            return value;
        }
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<V> listIterator() {
        return new RedisListIterator(this, false);
    }

    @Override
    public ListIterator<V> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<V> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    /**
     * 修剪当前 list
     * @param start 起始偏移量,  0 是列表里的第一个元素（表头），1 是第二个元素,-1 表示列表里的最后一个元素， -2 表示倒数第二个
     * @param end  结束偏移量, 0 是列表里的第一个元素（表头），1 是第二个元素,-1 表示列表里的最后一个元素， -2 表示倒数第二个
     * @return 修剪后的 list 长度
     */
    public boolean trim(int start, int end){
        try (Jedis jedis = getJedis()) {
            return jedis.ltrim(name, Long.valueOf(start), Long.valueOf(end)).contains("OK");
        }
    }

    /**
     * 返回存储在列表里指定范围内的元素
     * @param start 起始偏移量,  0 是列表里的第一个元素（表头），1 是第二个元素,-1 表示列表里的最后一个元素， -2 表示倒数第二个
     * @param end  结束偏移量, 0 是列表里的第一个元素（表头），1 是第二个元素,-1 表示列表里的最后一个元素， -2 表示倒数第二个
     * @return 修剪后的 list 长度
     */
    public List<V> range(int start, int end){
        try (Jedis jedis = getJedis()) {
            ArrayList<V> result = new ArrayList<V>();
            List<byte[]> queryResult = jedis.lrange(name.getBytes(), Long.valueOf(start), Long.valueOf(end));
            for(byte[] bytes : queryResult){
                result.add((V)CacheStatic.unserialize(bytes));
            }

            return result;
        }
    }

    /**
     * RedisList 的迭代器
     */
    public class RedisListIterator implements ListIterator<V> {

        private RedisList redisList;
        private int size = 0;
        private int position = 0;
        private boolean isDesc;

        public RedisListIterator(RedisList redisList, boolean isDesc){
            this.isDesc = isDesc;
            if(isDesc){
                this.size = redisList.size();
                position = this.size;
            } else {
                this.size = redisList.size();
                this.redisList = redisList;
            }
        }

        @Override
        public boolean hasNext() {
            if(isDesc){
                return position > 0;
            } else {
                return position < size();
            }
        }

        @Override
        public V next() {
            if(position==size){
                return null;
            }

            V value = (V)redisList.get(position);
            if(isDesc){
                position--;
            } else {
                position++;
            }
            return value;
        }

        @Override
        public boolean hasPrevious() {
            if(isDesc){
                return position < size();
            } else {
                return position > 0;
            }
        }

        @Override
        public V previous() {
            if(position==0){
                return null;
            }

            V value = (V)redisList.get(position);
            if(isDesc){
                position++;
            } else {
                position--;
            }
            return value;
        }

        @Override
        public int nextIndex() {
            if(isDesc){
                return position-1 < 0 ? -1 : position-1;
            } else {
                int size = size();
                return position+1 > size ? size : position+1 ;
            }
        }

        @Override
        public int previousIndex() {
            if(isDesc){
                int size = size();
                return position+1 > size ? size : position+1 ;
            } else {
                return position-1 < -1 ? 0 : position-1;
            }
        }

        @Override
        public void remove() {
            redisList.remove(position);
        }

        @Override
        public void set(V s) {
            redisList.set(position, s);
        }

        @Override
        public void add(V s) {
            redisList.add(position, s);
        }
    }
}
