package org.voovan.tools;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

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
    private static SecureRandom secureRandom = new SecureRandom();
    private static final int SEQ_DEFAULT = 0;
    private static final int RADIX = 62;
    private static final int SEQUENCE_LEFT = 11; //一毫秒生成 2048 个 id
    private static final int SIGNID_LEFT = 11;   //可有 2048 个数据中心
    private static final int MAX_SIGNID = 1 << SIGNID_LEFT;
    private static final int MAX_SEQUENCE = 1 << SEQUENCE_LEFT;

    private AtomicInteger orderedIdSequence = new AtomicInteger(SEQ_DEFAULT);
    private Long lastTime = 0L;
    private int signId = -1;
    private int step = 1;

    private ReentrantLock lock = new ReentrantLock();

    /**
     * 构造函数
     */
    public UniqueId() {}

    /**
     *  构造函数
     * @param signId 标识 ID
     */
    public UniqueId(int signId) {
        if(signId >= MAX_SIGNID || signId < 0){
            throw new IllegalArgumentException(String.format("Sign Id can't be greater than %d or less than 0", MAX_SIGNID));
        }
        this.signId = signId;
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
    private long generateId(){
        try {
            lock.lock();
            long currentTime = System.currentTimeMillis();

            if (lastTime < currentTime) {
                orderedIdSequence.set(SEQ_DEFAULT);
            } else if (lastTime > currentTime) {
                throw new RuntimeException("Clock moved backwards.");
            }

            if (orderedIdSequence.get() >= MAX_SEQUENCE) {
                TEnv.sleep(1);
                currentTime = System.currentTimeMillis();
                orderedIdSequence.set(SEQ_DEFAULT);
            }

            long resultId = generateId(currentTime, signId, orderedIdSequence.getAndAdd(step));

            lastTime = System.currentTimeMillis();
            return resultId;
        } finally {
            if(lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    /**
     * 生成带顺序的 ID 序列
     * @param timeMills 时间戳
     * @param signId 种子id
     * @param sequence 序号
     * @return id
     */
    public static long generateId(long timeMills, int signId, int sequence){
        if(signId < 0) {
            signId = secureRandom.nextInt(MAX_SIGNID - 1);
        }

        return (timeMills << (SEQUENCE_LEFT + SIGNID_LEFT) ) | (signId << SEQUENCE_LEFT) | sequence;
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
        return generateId(timeMills, signId, 1);
    }

    public static Long getMillis(Long uniqueId){
        return uniqueId==null ? null : uniqueId >> 22;
    }

    public static Long getMillis(String uniqueId) {
        return uniqueId==null ? null : TString.radixUnConvert(uniqueId, RADIX) >> 22;
    }

    public static Integer getSignId(Long uniqueId) {
        int signId = (int)(uniqueId << 42 >> 53);
        signId = signId < 0 ? 2048 + signId : signId;
        return uniqueId==null ? null : signId;
    }

    public static Integer getSignId(String uniqueId) {
        long longUniqueId =TString.radixUnConvert(uniqueId, RADIX);
        int signId = (int)(longUniqueId << 42 >> 53);
        signId = signId < 0 ? 2048 + signId : signId;
        return uniqueId==null ? null : signId;
    }

    public static Integer getSequence(Long uniqueId) {
        return uniqueId==null ? null :(int)( uniqueId << 53 >> 53);
    }

    public static Integer getSequence(String uniqueId) {
        return uniqueId==null ? null : (int)(TString.radixUnConvert(uniqueId, RADIX)  << 53 >> 53);
    }

    public static long adjust(long id, Integer adjustMills, Integer sequence) {
        long time = UniqueId.getMillis(id) + (adjustMills == null ? 0L : adjustMills);
        int signId = UniqueId.getSignId(id);
        sequence = sequence==null ? UniqueId.getSequence(id) : sequence;
        return UniqueId.generateId(time, signId, sequence);
    }

    public static long adjust(String uniqueId, Integer adjustMills, Integer sequence) {
        long id = TString.radixUnConvert(uniqueId, RADIX);
        long time = UniqueId.getMillis(id) + (adjustMills == null ? 0L : adjustMills);
        int signId = UniqueId.getSignId(id);
        sequence = sequence==null ? UniqueId.getSequence(id) : sequence;
        return UniqueId.generateId(time, signId, sequence);
    }
}