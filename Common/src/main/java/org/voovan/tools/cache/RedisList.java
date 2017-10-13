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
public class RedisList implements List<String>, Deque<String>, Closeable {
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

    /**
     * 选择当前数据集
     * @param dbIndex 数据集序号
     */
    public void setDbIndex(int dbIndex) {
        this.dbIndex = dbIndex;
    }

    private Jedis getJedis(){
        Jedis Jedis = redisPool.getResource();
        Jedis.select(dbIndex);
        return Jedis;
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
    public Iterator<String> iterator() {
        return (Iterator<String>)new RedisListIterator(this, false);
    }

    @Override
    public Iterator<String> descendingIterator() {
        return (Iterator<String>)new RedisListIterator(this, true);
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
    public boolean offerFirst(String s) {
        try (Jedis jedis = getJedis()) {
            jedis.lpush(name, s);
            return true;
        }
    }

    @Override
    public boolean offerLast(String s) {
        try (Jedis jedis = getJedis()) {
            jedis.rpush(name, s);
            return true;
        }
    }

    @Override
    public void addFirst(String s) {
        offerFirst(s);
    }

    @Override
    public void addLast(String s) {
        offerLast(s);
    }

    @Override
    public String removeFirst() {
        try (Jedis jedis = getJedis()) {
            return jedis.lpop(name);
        }
    }

    @Override
    public String removeLast() {
        try (Jedis jedis = getJedis()) {
            return jedis.rpop(name);
        }
    }

    @Override
    public String pollFirst() {
        return removeFirst();
    }

    @Override
    public String pollLast() {
        return removeLast();
    }

    @Override
    public String getFirst() {
        try (Jedis jedis = getJedis()) {
            return jedis.lindex(name, 0);
        }
    }

    @Override
    public String getLast() {
        try (Jedis jedis = getJedis()) {
            return jedis.lindex(name, -1);
        }
    }

    @Override
    public String peekFirst() {
        return getFirst();
    }

    @Override
    public String peekLast() {
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
    public boolean add(String s) {
        return offerLast(s);
    }

    @Override
    public boolean offer(String s) {
        return offerLast(s);
    }

    @Override
    public String remove() {
        return removeFirst();
    }

    @Override
    public String poll() {
        return pollFirst();
    }

    @Override
    public String element() {
        return getFirst();
    }

    @Override
    public String peek() {
        return peekFirst();
    }

    @Override
    public void push(String s) {
        addFirst(s);
    }

    @Override
    public String pop() {
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
    public boolean addAll(Collection<? extends String> c) {
        for(String item : c){
            add(item);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends String> c) {
        try (Jedis jedis = getJedis()) {
            String pivot = get(index);
            for(String item : c){
                jedis.linsert(name, BinaryClient.LIST_POSITION.AFTER, pivot, item);
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
    public String get(int index) {
        try (Jedis jedis = getJedis()) {
            return jedis.lindex(name, index);
        }
    }

    @Override
    public String set(int index, String element) {
        try (Jedis jedis = getJedis()) {
            return jedis.lset(name, index, element);
        }
    }

    @Override
    public void add(int index, String element) {
        try (Jedis jedis = getJedis()) {
            String pivot = get(index);
            jedis.linsert(name, BinaryClient.LIST_POSITION.AFTER, pivot, element);
        }
    }

    @Override
    public String remove(int index) {
        try (Jedis jedis = getJedis()) {
            String value = get(index);
            jedis.lrem(name, 1, value);
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
    public ListIterator<String> listIterator() {
        return new RedisListIterator(this, false);
    }

    @Override
    public ListIterator<String> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> subList(int fromIndex, int toIndex) {
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
    public List<String> range(int start, int end){
        try (Jedis jedis = getJedis()) {
            return jedis.lrange(name, Long.valueOf(start), Long.valueOf(end));
        }
    }

    /**
     * RedisList 的迭代器
     */
    public class RedisListIterator implements ListIterator<String> {

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
        public String next() {
            if(position==size){
                return null;
            }

            String value = redisList.get(position);
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
        public String previous() {
            if(position==0){
                return null;
            }

            String value = redisList.get(position);
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
        public void set(String s) {
            redisList.set(position, s);
        }

        @Override
        public void add(String s) {
            redisList.add(position, s);
        }
    }
}
