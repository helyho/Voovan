package org.voovan.http.monitor;

/**
 * 请求分析信息对象
 *
 * @author helyho
 *         <p>
 *         Java Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class RequestAnalysis {
    private String path;
    private long count;
    private long avgTime;
    private long maxTime;
    private long minTime;

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
