package org.voovan.tools;

import java.security.SecureRandom;
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
    private static final int SEQUENCE_LEFT = 11; //一毫秒生成 4096 个 id
    private static final int SIGNID_LEFT = 11;   //可有 4096 个数据中心
    private static final int MAX_SIGNID = 1 << SIGNID_LEFT;
    private static final int MAX_SEQUENCE = 1 << SEQUENCE_LEFT;

    private volatile AtomicInteger orderedIdSequence = new AtomicInteger(SEQ_DEFAULT);
    private Long lastTime = 0L;
    private int workId = 0;

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
    public synchronized long generateId(){
        long currentTime = System.currentTimeMillis();

        if(lastTime < currentTime){
            orderedIdSequence.set(SEQ_DEFAULT);
        }else if(lastTime > currentTime){
            throw new RuntimeException("Clock moved backwards.");
        }

        if(orderedIdSequence.get() >= MAX_SEQUENCE){
            TEnv.sleep(1);
            orderedIdSequence.set(SEQ_DEFAULT);
        }

        long resultId = (currentTime << (SEQUENCE_LEFT + SIGNID_LEFT) ) | (workId << SEQUENCE_LEFT) | orderedIdSequence.getAndAdd(1);

        lastTime = System.currentTimeMillis();
        return resultId;
    }
}
