package org.voovan.tools.cache;

import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import javassist.compiler.ast.ASTList;
import redis.clients.jedis.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 *
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class RedisHashsWithSortedSet implements Closeable {
    private JedisPool redisPool;
    private String name = null;
    private int dbIndex = 0;

    private HashMap<String, String> scriptHashMap = new HashMap<String, String>();

    /**
     * 构造函数
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     * @param name        在 redis 中的 HashMap的名称
     * @param password    redis 服务密码
     */
    public RedisHashsWithSortedSet(String host, int port, int timeout, int poolsize, String name, String password){
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
    public RedisHashsWithSortedSet(String host, int port, int timeout, int poolsize, String name){
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
    public RedisHashsWithSortedSet(String name){
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

    /**
     * 执行 jedis 的脚本
     * @param jedis
     * @param command
     * @param zsetName
     * @param values
     * @return
     */
    public Object eval(Jedis jedis, String command, String zsetName,Object ... values){
        String params = ", ";

        String cachedKey = command+values.length;
        String scriptHash = scriptHashMap.get(cachedKey);

        for (int i = 0; i < values.length; i++) {
            if(scriptHash==null) {
                params = params + "ARGV[" + (i + 3) + "], ";
            }
            values[i] = values[i].toString();
        }

        List valueList = TObject.asList(this.name, zsetName);
        valueList.addAll(TObject.asList(values));

        if(scriptHash==null){


            params = TString.removeSuffix(params.trim());

            String script = "local zsetKey = redis.call('HEXISTS', ARGV[1], ARGV[2]);\n" +
                    "if (zsetKey == 0) then\n" +
                    "    zsetKey = tostring(ARGV[1])..'-'..tostring(ARGV[2]);\n" +
                    "    redis.call('hset', ARGV[1], zsetKey, zsetKey);\n" +
                    "else \n" +
                    "    zsetKey = tostring(ARGV[1])..'-'..tostring(ARGV[2]);\n" +
                    "end\n" +
                    "return redis.call('" + command + "', zsetKey" + params + ");";
            scriptHash =jedis.scriptLoad(script);
            scriptHashMap.put(cachedKey, scriptHash);

            Logger.fremawork("Create " + cachedKey + ": " + scriptHash);
        }


        return jedis.evalsha(scriptHash, Collections.emptyList(), valueList);
    }

    private ArrayList convertScoreMembersToArrays(Map<String, Double> scoreMembers) {
        ArrayList args = new ArrayList(scoreMembers.size() * 2);
        Iterator iterator = scoreMembers.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<String, Double> entry = (Map.Entry)iterator.next();
            args.add(entry.getValue().toString());
            args.add(entry.getKey());
        }

        return args;
    }

    /**
     * 新增一个元素
     * @param values 新的元素
     * @return 新增元素的数量
     */
    public long addAll(String zsetName, Map<String, Double> values){
        try (Jedis jedis = getJedis()) {
            return (Long)eval(jedis, "zadd", zsetName, convertScoreMembersToArrays(values).toArray());
        }
    }

    /**
     * 新增一个元素
     * @param score 元素的分
     * @param value 新的元素
     * @return 新增元素的数量
     */
    public long add(String zsetName, double score, String value){
        try (Jedis jedis = getJedis()) {
            return (Long)eval(jedis, "zadd", zsetName, score, value);
        }
    }

    /**
     * 对 Score 进行自增
     * @param value 进行自增操作的元素
     * @param score 增加值
     * @return 自增后的 score
     */
    public double increase(String zsetName, String value, double score){
        try (Jedis jedis = getJedis()) {
            return Double.valueOf((String)eval(jedis, "zincrby", zsetName, score, value));
        }
    }

    /**
     * 获取前集合的大小
     * @return 集合的大小
     */
    public long size(){
        try (Jedis jedis = getJedis()) {
            return jedis.hlen(this.name);
        }
    }

    /**
     * 获取前集合的大小
     * @return 集合的大小
     */
    public long size(String zsetName){
        try (Jedis jedis = getJedis()) {
            return (Long)eval(jedis, "zcard", zsetName);
        }
    }

    /**
     * 某一个特定的 score 范围内的成员数量, 包含 min 和 max 的数据
     * @param min score 的最小值
     * @param max score 的最大值
     * @return 成员的数量
     */
    public long scoreRangeCount(String zsetName, double min, double max){
        try (Jedis jedis = getJedis()) {
            return (Long)eval(jedis, "zcount", zsetName, min, max);
        }
    }

    /**
     * 某一个成员区间内的成员数量, 包含 min 和 max 的数据
     * @param min value 的最小值
     * @param max value 的最大值
     * @return 成员的数量
     */
    public long valueRangeCount(String zsetName, String min, String max){
        try (Jedis jedis = getJedis()) {
            return (Long)eval(jedis, "zlexcount", zsetName, min, max);
        }
    }

    /**
     * 某一个特定倒序索引区间内的所有成员, 包含 start 和 end 的数据
     * @param start 索引起始位置
     * @param end value 索引结束位置
     * @return 成员对象的集合
     */
    public Set<String> rangeByIndex(String zsetName, long start, long end){
        try (Jedis jedis = getJedis()) {
            Set<String> result = new HashSet<String>();
            result.addAll((Collection<String>)eval(jedis, "zrange", zsetName, start, end));
            return result;
        }
    }


    /**
     * 某一个特定倒序索引区间内的所有成员, 包含 start 和 end 的数据
     * @param start 索引起始位置
     * @param end value 索引结束位置
     * @return 成员对象的集合
     */
    public Set<String> revRangeByIndex(String zsetName, long start, long end){
        try (Jedis jedis = getJedis()) {
            Set<String> result = new HashSet<String>();
            result.addAll((Collection<String>)eval(jedis, "zrevrange", zsetName, start, end));
            return result;
        }
    }

    /**
     * 某一个特定值区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最小值
     * @param end value 的最大值
     * @return 成员对象的集合
     */
    public Set<String> rangeByValue(String zsetName, String start, String end){
        try (Jedis jedis = getJedis()) {
            Set<String> result = new HashSet<String>();
            result.addAll((Collection<String>)eval(jedis, "zrangeByLex", zsetName, start, end));
            return result;
        }
    }


    /**
     * 某一个特定值区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最小值
     * @param end value 的最大值
     * @param offset 偏移量
     * @param size 数量
     * @return 成员对象的集合
     */
    public Set<String> rangeByValue(String zsetName, String start, String end, int offset, int size){
        try (Jedis jedis = getJedis()) {
            Set<String> result = new HashSet<String>();
            result.addAll((Collection<String>)eval(jedis, "zrangeByLex", zsetName, start, end, "LIMIT", offset, size));
            return result;
        }
    }

    /**
     * 某一个特定值倒序区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最大值
     * @param end value 的最小值
     * @return 成员对象的集合
     */
    public Set<String> revRangeByValue(String zsetName, String start, String end){
        try (Jedis jedis = getJedis()) {
            Set<String> result = new HashSet<String>();
            result.addAll((Collection<String>)eval(jedis, "zrevrangeByLex", zsetName, start, end));
            return result;
        }
    }

    /**
     * 某一个特定值倒序区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最大值
     * @param end value 的最小值
     * @param offset 偏移量
     * @param size 数量
     * @return 成员对象的集合
     */
    public Set<String> revRangeByValue(String zsetName, String start, String end, int offset, int size){
        try (Jedis jedis = getJedis()) {
            Set<String> result = new HashSet<String>();
            result.addAll((Collection<String>)eval(jedis, "zrevrangeByLex", zsetName, start, end, "LIMIT", offset, size));
            return result;
        }
    }

    /**
     * 某一个Score区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最小值
     * @param end value 的最大值
     * @return 成员对象的集合
     */
    public Set<String> rangeByScore(String zsetName, double start, double end){
        try (Jedis jedis = getJedis()) {
            Set<String> result = new HashSet<String>();
            result.addAll((Collection<String>)eval(jedis, "zrangeByScore", zsetName, start, end));
            return result;
        }
    }

    /**
     * 某一个Score区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最小值
     * @param end value 的最大值
     * @param offset 偏移量
     * @param size 数量
     * @return 成员对象的集合
     */
    public Set<String> rangeByScore(String zsetName, double start, double end, int offset, int size){
        try (Jedis jedis = getJedis()) {
            Set<String> result = new HashSet<String>();
            result.addAll((Collection<String>)eval(jedis, "zrangeByScore", zsetName, start, end, "LIMIT", offset, size));
            return result;
        }
    }

    /**
     * 某一个Score倒序区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最大值
     * @param end value 的最小值
     * @return 成员对象的集合
     */
    public Set<String> revRangeByScore(String zsetName, double start, double end){
        try (Jedis jedis = getJedis()) {
            Set<String> result = new HashSet<String>();
            result.addAll((Collection<String>)eval(jedis, "zrevrangeByScore", zsetName, start, end));
            return result;
        }
    }

    /**
     * 某一个Score倒序区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最大值
     * @param end value 的最小值
     * @param offset 偏移量
     * @param size 数量
     * @return 成员对象的集合
     */
    public Set<String> revRangeByScore(String zsetName, double start, double end, int offset, int size){
        try (Jedis jedis = getJedis()) {
            Set<String> result = new HashSet<String>();
            result.addAll((Collection<String>)eval(jedis, "zrevrangeByScore", zsetName, start, end, "LIMIT", offset, size));
            return result;
        }
    }

    /**
     * 获得当前值的索引位置
     * @param value 值
     * @return 索引诶只
     */
    public long indexOf(String zsetName, String value){
        try (Jedis jedis = getJedis()) {
            return (long)eval(jedis, "zrank", zsetName, value);
        }
    }

    /**
     * 获得当前值的倒序索引位置
     * @param value 值
     * @return 索引诶只
     */
    public long revIndexOf(String zsetName, String value){
        try (Jedis jedis = getJedis()) {
            return (long)eval(jedis, "zrevrank", zsetName, value);
        }
    }

    /**
     * 移除某个特定 value
     * @return 移除元素的索引
     */
    public long remove(String zsetName, String value){
        try (Jedis jedis = getJedis()) {
            return (long)eval(jedis, "zrem", zsetName, value);
        }
    }

    /**
     * 移除某个特定 value 区间的数据
     * @param start value 的最大值
     * @param end value 的最小值
     * @return 移除元素的数量
     */
    public long removeRangeByValue(String zsetName,String start, String end){
        try (Jedis jedis = getJedis()) {
            return (long)eval(jedis, "zremrangeByLex", zsetName, start, end);
        }
    }

    /**
     * 移除某个特定索引区间的数据
     * @param start 索引起始位置
     * @param end value 索引结束位置
     * @return 移除元素的数量
     */
    public long removeRangeByIndex(String zsetName,int start, int end){
        try (Jedis jedis = getJedis()) {
            return (long)eval(jedis, "zremrangeByRank", zsetName, start, end);
        }
    }

    /**
     * 移除某个特定Score区间的数据
     * @param min score 的最小值
     * @param max score 的最大值
     * @return 移除元素的数量
     */
    public long removeRangeByScore(String zsetName,double min, double max){
        try (Jedis jedis = getJedis()) {
            return (long)eval(jedis, "zremrangeByScore", zsetName, min, max);
        }
    }

    /**
     * 获取某个特定值的 Score
     * @param value 值
     * @return 对应的 Score
     */
    public double getScore(String zsetName,String value){
        try (Jedis jedis = getJedis()) {
            return Double.valueOf((String)eval(jedis, "zscore", zsetName, value));
        }
    }

    public ScanedObject scan(String zsetName, String cursor, String matchValue, Integer count){
        try (Jedis jedis = getJedis()) {

            ArrayList paramList = new ArrayList();
            paramList.add(cursor);
            ScanParams scanParams = new ScanParams();
            if(matchValue!=null) {
                paramList.add("MATCH");
                paramList.add(matchValue);
            } else if(count!=null) {
                paramList.add("Count");
                paramList.add(count);
            }

            ArrayList<ArrayList> resultList = (ArrayList<ArrayList> )eval(jedis, "zscan", zsetName, paramList.toArray());

            Object cursorValue = resultList.get(0);
            ScanedObject scanedObject = new ScanedObject((String)cursorValue);
            for(Object object : resultList.get(1)){
                scanedObject.getResultList().add((String)object);
            }
            return scanedObject;
        }
    }

    @Override
    public void close() throws IOException {
        redisPool.close();
    }
}
