package org.voovan.http.monitor;

import org.voovan.http.server.HttpBizHandler;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.tools.*;
import org.voovan.tools.json.JSONEncode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 监控业务处理类
 *
 * @author helyho
 *         <p>
 *         Java Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class MonitorHandler implements HttpBizHandler {

    /**
     * 获取当前 JVM 线程信息描述
     * @return
     */
    public static List<Map<String,Object>> getThreadDetail(){
        ArrayList<Map<String,Object>> threadDetailList = new ArrayList<Map<String,Object>>();
        for(Thread thread : TEnv.getThreads()){
            Map<String,Object> threadDetail = new Hashtable<String,Object>();
            threadDetail.put("Name",thread.getName());
            threadDetail.put("Id",thread.getId());
            threadDetail.put("Priority",thread.getPriority());
            threadDetail.put("ThreadGroup",thread.getThreadGroup().getName());
            threadDetail.put("StackTrace",TEnv.getStackElementsMessage(thread.getStackTrace()));
            threadDetail.put("State",thread.getState().name());
            threadDetailList.add(threadDetail);
        }
        return threadDetailList;
    }

    /**
     * 获取处理器信息
     * @return
     */
    public static Map<String,Object>  getProcessorInfo(){
        Map<String,Object> processInfo = new Hashtable<String,Object>();
        processInfo.put("ProcessorCount",TPerformance.getProcessorCount());
        processInfo.put("SystemLoadAverage",TPerformance.getSystemLoadAverage());
        return processInfo;
    }

    /**
     * 获取当前JVM加载的对象信息(数量,所占内存大小)
     * @param regex
     * @return
     */
    public static Map<String,TPerformance.ObjectInfo> getSysObjectInfo(String regex) {
        Map<String,TPerformance.ObjectInfo> result;
        try {
            result = TPerformance.getSysObjectInfo(TEnv.getCurrentPID(),regex);
        } catch (IOException e) {
            result = new Hashtable<String,TPerformance.ObjectInfo>();
        }
        return result;

    }

    /**
     * 获取JVM信息
     * @return
     */
    public static Map<String,Object> getJVMInfo(){
        Map<String, Object> jvmInfo = new Hashtable<String, Object>();
        for(Entry<Object,Object> entry : System.getProperties().entrySet()){
            jvmInfo.put(entry.getKey().toString(),entry.getValue().toString());
        }
        return jvmInfo;
    }

    /**
     * 对象转换成 JSON 字符串
     *      json 中的换行被处理成"\\r\\n"
     * @param obj
     * @return
     */
    public static String toJsonWithLF(Object obj){
        String jsonStr = null;
        try {
            jsonStr = JSONEncode.fromObject(obj);
            jsonStr=jsonStr.replace("\r", "\\r");
            jsonStr=jsonStr.replace("\n", "\\n");
            return jsonStr;
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 读取日志信息
     * @param type
     * @param lineNumber
     * @return
     * @throws IOException
     */
    public static String readLogs(String type ,int lineNumber) throws IOException {
        String fileName;
        if(type.equals("SYSOUT")){
            fileName = "sysout."+ TDateTime.now("yyyyMMdd")+".log";
        }else if(type.equals("ACCESS")){
            fileName = "access.log";
        }else{
            return null;
        }

        String fullPath = TEnv.getSystemPath("logs"+ File.separator+fileName);
        return new String(TFile.loadFileLastLines(new File(fullPath),lineNumber));
    }

    public static List<RequestAnalysis> requestInfo() {
       return (List<RequestAnalysis>) TObject.mapValueToList(HttpMonitorFilter.getRequestInfos());
    }

    @Override
    public void process(HttpRequest request, HttpResponse response) throws Exception {
        String type = request.getParameter("Type");
        String responseStr = "";
        if(type.equals("JVM")){
            responseStr = toJsonWithLF(getJVMInfo());
        }else if(type.equals("CPU")){
            responseStr = toJsonWithLF(getProcessorInfo());
        }else if(type.equals("Memory")){
            responseStr = toJsonWithLF(TPerformance.getMemoryInfo());
        }else if(type.equals("Objects")){
            String filterWord = request.getParameter("Param1");
            responseStr = toJsonWithLF(getSysObjectInfo(filterWord));
        }else if(type.equals("ObjectCount")){
            responseStr = Integer.toString(getSysObjectInfo("").size());
        }else if(type.equals("Threads")){
            responseStr = toJsonWithLF(getThreadDetail());
        }else if(type.equals("ThreadCount")){
            responseStr = Integer.toString(TEnv.getThreads().length);
        }else if(type.equals("RequestInfo")){
            responseStr = toJsonWithLF(requestInfo());
        }else if(type.equals("Log")){
            String logType = request.getParameter("Param1");
            int lineNumber = new Integer(request.getParameter("Param2"));
            responseStr = readLogs(logType,lineNumber);
        }
        response.write(responseStr);
    }
}
