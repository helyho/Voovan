package org.voovan.http.server.module.monitor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求分析信息对象
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RequestAnalysis {
    private String path;    //请求路径
    private AtomicLong count;     //请求数量
    private int avgTime;   //平均请求时间
    private int maxTime;   //最大请求时间
    private int minTime;   //最小请求时间

    public RequestAnalysis(String path){
        this.path = path;
        count = new AtomicLong(0);
        avgTime = 0;
        maxTime = 0;
        minTime = 0;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getCount() {
        return count.get();
    }

    public int getAvgTime() {
        return avgTime;
    }

    public int getMaxTime() {
        return maxTime;
    }

    public int getMinTime() {
        return minTime;
    }


    /**
     * 增加请求时间
     * @param time 增加的时间参数
     */
    public void addRequestTime(int time){
        count.addAndGet(1);

        if (avgTime == 0 ){
            avgTime = time;
        }

        avgTime = (avgTime+time)/2;

        if(maxTime==0){
            maxTime = time;
        }
        if(minTime==0){
            minTime = time;
        }

        if(maxTime<time){
            maxTime = time;
        }
        if(minTime>time){
            minTime = time;
        }
    }
}
