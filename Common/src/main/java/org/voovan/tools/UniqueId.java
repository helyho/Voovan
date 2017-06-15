package org.voovan.tools;

import org.voovan.Global;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 高速ID生成器
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class UniqueId {
    private static final int SEQ_DEFAULT = 3844;
    private static final int RADIX = 62;
    private Integer orderedIdSequence = SEQ_DEFAULT;
    private Long oldTime = 0L;
    private ConcurrentLinkedDeque<String> idDeque;
    private String prefix;
    private String suffix;
    private int cacheSize = 10000;
    private GenerateThread generateThread;

    /**
     * 构造函数
     * 默认 id 缓存数为10000
     */
    public UniqueId() {
        prefix = "";
        suffix = "";
        init();
        autoIdGenerater();
    }

    /**
     * 构造函数
     * @param prefix id 前缀
     * @param suffix id 后缀
     * @param cacheSize id 缓存池的数量
     */
    public UniqueId(String prefix, String suffix, int cacheSize) {
        init();
        autoIdGenerater();
        this.prefix = prefix;
        this.suffix = suffix;
        this.cacheSize = cacheSize;
    }

    /**
     * 初始化 id 缓存
     */
    private void init(){
        idDeque = new ConcurrentLinkedDeque<String>();
        for(int i=0;i<cacheSize;i++){
            idDeque.offerLast(generateId());
        }
    }

    /**
     * id 生成线程
     */
    private class GenerateThread implements Runnable{
        private boolean running = true;

        @Override
        public void run() {
            int sizeOfId = idDeque.size();
            for(int i=0; i<cacheSize - sizeOfId; i++){
                idDeque.offerLast(generateId());
            }

            running = false;
        }

        public boolean isRunning(){
            return running;
        }
    };

    /**
     * 封装的自动生成 id 的方法
     */
    private void autoIdGenerater(){
        if(generateThread==null || !generateThread.isRunning()) {
            generateThread = new GenerateThread();
            ThreadPoolExecutor threadPoolExecutor = Global.getThreadPool();
            if(!threadPoolExecutor.isShutdown()) {
                Global.getThreadPool().execute(generateThread);
            }
        }
    }

    /**
     * 获取系一个 id
     * @return 返回 id
     */
    public String nextId(){
        String id;
        if(idDeque.size() < cacheSize/4){
            autoIdGenerater();
        }

        if(idDeque.size()==0){
            id = generateId();
        }else{
            id = idDeque.poll();
        }

        return prefix + id + suffix;
    }

    /**
     * 生成带顺序的 ID 序列
     * @return ID字符串
     */
    public synchronized String generateId(){
        StringBuilder result = new StringBuilder();
        long currentTime = System.currentTimeMillis();
        if(oldTime < currentTime){
            orderedIdSequence = SEQ_DEFAULT;
        }
        long tmp = Long.parseLong( currentTime + "" + orderedIdSequence);
        result.append(TString.radixConvert(tmp, RADIX));

        orderedIdSequence++;

        oldTime = currentTime;
        return result.toString();
    }
}
