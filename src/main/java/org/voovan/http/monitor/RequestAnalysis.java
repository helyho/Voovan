package org.voovan.http.monitor;

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
    private long count;     //请求数量
    private long avgTime;   //平均请求时间
    private long maxTime;   //最大请求时间
    private long minTime;   //最小请求时间

    public RequestAnalysis(String path){
        this.path = path;
        count = 0;
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
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getAvgTime() {
        return avgTime;
    }

    public void setAvgTime(long avgTime) {
        this.avgTime = avgTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    public long getMinTime() {
        return minTime;
    }

    public void setMinTime(long minTime) {
        this.minTime = minTime;
    }

    /**
     * 增加请求时间
     * @param time
     */
    public void add(long time){
        count++;
        avgTime = (avgTime+time)/count;
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
