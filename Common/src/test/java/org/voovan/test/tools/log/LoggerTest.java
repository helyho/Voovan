package org.voovan.test.tools.log;

import org.voovan.tools.log.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class LoggerTest {

    private static String record_100_byte = "Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.";   //100字节
    private static String record_200_byte = "Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.";   //200字节，为方便展示此处以"..."代替
    private static String record_400_byte = "Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.Performance Testing.";   //400字节，为方便展示此处以"..."代替
    private static AtomicInteger messageCount = new AtomicInteger(0);
    private static int count = 1000000;  //基准数值，以messageCount为准
    private static int threadNum = 8;  //1,2,4,8,16,32

    public static void main(String[] argu) throws Exception {
        final CountDownLatch latch = new CountDownLatch(threadNum);

        long st = System.currentTimeMillis();
        for(int i=0; i<threadNum; i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(messageCount.get() < count){
                        Logger.info(record_400_byte);
                        messageCount.incrementAndGet();
                    }
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        long et = System.currentTimeMillis();

        System.out.println("messageCount=" + messageCount.get() + ", threadNum=" + threadNum + ", costTime=" + (et-st) +"ms, throughput=" + (1*1000*messageCount.get()/(et-st)));
        System.exit(0);
    }
}
