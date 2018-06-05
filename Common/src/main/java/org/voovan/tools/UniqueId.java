package org.voovan.tools;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高速ID生成器
 * 采用 snowflake 算法快速生成 ID
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class UniqueId {
    private static final int SEQ_DEFAULT = 0;
    private static final int RADIX = 62;
    private static final int SEQUENCE_LEFT = 12;
    private static final int SIGNID_LEFT = 10;
    private static final int MAX_SIGNID = 1 << SIGNID_LEFT;

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
        if(signId > MAX_SIGNID || signId < 0){
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_SIGNID));
        }
        workId = signId;
    }

    /**
     * 获取下一个 id
     * @return 返回 id
     */
    public long nextInt(){
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

        if(orderedIdSequence.get() > 2096){
            TEnv.sleep(1);
        }

        long resultId = (currentTime << (SEQUENCE_LEFT + SIGNID_LEFT) ) | (workId << SEQUENCE_LEFT) | orderedIdSequence.getAndAdd(1);

        lastTime = currentTime;
        return resultId;
    }
}
