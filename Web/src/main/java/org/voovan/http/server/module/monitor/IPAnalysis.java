package org.voovan.http.server.module.monitor;

import org.voovan.tools.TDateTime;
import org.voovan.tools.json.annotation.NotJSON;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Some description
 *
 * @author: helyho
 * Project: Framework
 * Create: 2017/9/28 12:13
 */
public class IPAnalysis {

    private String address;
    private AtomicLong totalCount;
    private int lastMinuteCount;
    @NotJSON
    private int minute;
    private Map<String, Integer> data;


    public IPAnalysis(String address){
        this.address = address;
        this.totalCount = new AtomicLong(0);
        this.lastMinuteCount = 0;
        this.minute = TDateTime.getDateAtom(null, Calendar.MINUTE);
        this.data = new ConcurrentHashMap<String, Integer>();
    }

    public String getAddress() {
        return address;
    }


    public long getTotalCount() {
        return totalCount.get();
    }

    public int getLastMinuteCount() {
        return lastMinuteCount;
    }

    public Map<String, Integer> getData() {
        return data;
    }

    public void addRequest(String requestPath){
        if(data.containsKey(requestPath)){
            synchronized (data) {
                data.put(requestPath, data.get(requestPath) + 1);
            }
        }else{
            data.put(requestPath, 1);
        }

        int currentMinute = TDateTime.getDateAtom(null, Calendar.MINUTE);
        if(currentMinute == minute){
            lastMinuteCount ++;
        }else{
            minute = currentMinute;
            lastMinuteCount = 1;
        }

        totalCount.addAndGet(1);

    }
}
