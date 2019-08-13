package org.voovan.tools;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高速ID生成器
 * 参考 snowflake 算法快速生成 ID
 * 在 2109-05-15 15:35:11:103 可 保证无重复
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class UniqueId {
    private static final int SEQ_DEFAULT = 0;
    private static final int RADIX = 62;
    private static final int SEQUENCE_LEFT = 11; //一毫秒生成 2048 个 id
    private static final int SIGNID_LEFT = 11;   //可有 2048 个数据中心
    private static final int MAX_SIGNID = 1 << SIGNID_LEFT;
    private static final int MAX_SEQUENCE = 1 << SEQUENCE_LEFT;

    private volatile AtomicInteger orderedIdSequence = new AtomicInteger(SEQ_DEFAULT);
    private Long lastTime = 0L;
    private int workId = 0;
    private int step = 1;

    /**
     * 构造函数
     */
    public UniqueId() {
        int workId = (new SecureRandom()).nextInt(MAX_SIGNID);
        if(workId > MAX_SIGNID || workId < 0){
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_SIGNID));
        }
        workId = 0;
    }

    /**
     *  构造函数
     * @param signId 标识 ID
     */
    public UniqueId(int signId) {
        if(signId >= MAX_SIGNID || signId < 0){
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_SIGNID));
        }
        workId = signId;
    }

    /**
     *  构造函数
     * @param signId 标识 ID
     * @param step 每次自增的步长
     */
    public UniqueId(int signId, int step) {
        this(signId);
        this.step = step;
    }

    /**
     * 获取下一个 id
     * @return 返回 id
     */
    public long nextNumber(){
        return generateId();
    }

    /**
     * 获取下一个 id
     * @return 返回 id
     */
    public String nextString(){
        return TString.radixConvert(generateId(), RADIX);
    }

    /**
     * 生成带顺序的 ID 序列
     * @return ID字符串
     */
    private synchronized long generateId(){
        long currentTime = System.currentTimeMillis();

        if(lastTime < currentTime){
            orderedIdSequence.set(SEQ_DEFAULT);
        }else if(lastTime > currentTime){
            throw new RuntimeException("Clock moved backwards.");
        }

        if(orderedIdSequence.get() >= MAX_SEQUENCE){
            TEnv.sleep(1);
            currentTime = System.currentTimeMillis();
            orderedIdSequence.set(SEQ_DEFAULT);
        }

        long resultId = (currentTime << (SEQUENCE_LEFT + SIGNID_LEFT) ) | (workId << SEQUENCE_LEFT) | orderedIdSequence.getAndAdd(step);

        lastTime = System.currentTimeMillis();
        return resultId;
    }

    /**
     * 获取指定时间的 id
     * @param timeMills 指定的时间, 毫秒
     * @return 返回 id
     */
    public long getNumber(Long timeMills){
        return generateId(timeMills);
    }

    /**
     * 获取指定时间的 id
     * @param timeMills 指定的时间, 毫秒
     * @return 返回 id
     */
    public String getString(Long timeMills){
        return TString.radixConvert(generateId(timeMills), RADIX);
    }


    /**
     * 获取某个时间点产生的第一个 id
     *          用于检索某个特定时间的结果
     * @param timeMills 时间点的毫秒
     * @return 当前时间点的 id
     */
    private long generateId(Long timeMills){
        return (timeMills << (SEQUENCE_LEFT + SIGNID_LEFT) ) | (workId << SEQUENCE_LEFT) | 1;
    }

    public static Long getMillis(Long uniqueId){
        return uniqueId==null ? null : uniqueId >> 22;
    }

    public static Long getMillis(String uniqueId) {
        return uniqueId==null ? null : TString.radixUnConvert(uniqueId, RADIX) >> 22;
    }

    public static Long getSignId(Long uniqueId) {
        return uniqueId==null ? null : uniqueId << 42 >> 53;
    }

    public static Long getSignId(String uniqueId) {
        return uniqueId==null ? null : TString.radixUnConvert(uniqueId, RADIX)  << 42 >> 53;
    }

    public static Long getSequence(Long uniqueId) {
        return uniqueId==null ? null : uniqueId << 53 >> 53;
    }

    public static Long getSequence(String uniqueId) {
        return uniqueId==null ? null : TString.radixUnConvert(uniqueId, RADIX)  << 53 >> 53;
    }
}
