package org.voovan.http.server.module.monitor;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;
import org.voovan.http.server.context.WebContext;
import org.voovan.tools.*;
import org.voovan.tools.json.JSON;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控业务处理类
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class MonitorRouter implements HttpRouter {

    /**
     * 从尾部读取日志信息
     * @param type    日志类型
     * @param lineNumber  日志行数
     * @return 日志信息
     * @throws IOException IO 异常
     */
    public static String readLogs(String type , int lineNumber) throws IOException {
        String fileName;
        if("SYSOUT".equals(type)){
            fileName = "sysout."+ TDateTime.now("yyyyMMdd")+".log";
        }else if("ACCESS".equals(type)){
            fileName = "access.log";
        }else{
            return null;
        }

        String fullPath = TFile.getSystemPath("logs"+ File.separator+fileName);
        return new String(TFile.loadFileLastLines(new File(fullPath),lineNumber),"UTF-8");
    }

    /**
     * 返回请求分析信息
     * @return 请求分析信息集合
     */
    public static List<RequestAnalysis> requestInfo() {
        return (List<RequestAnalysis>) TObject.mapValueToList(MonitorGlobal.REQUEST_ANALYSIS);
    }

    /**
     * 返回请求IP分析信息
     * @return 请求分析信息集合
     */
    public static List<IPAnalysis> ipAddressInfo() {
        return (List<IPAnalysis>) TObject.mapValueToList(MonitorGlobal.IP_ANALYSIS);
    }

    @Override
    public void process(HttpRequest request, HttpResponse response) throws Exception {
        String authToken = request.header().get("AUTH-TOKEN");

        if(authToken!=null && authToken.equals(WebContext.AUTH_TOKEN) &&
                MonitorGlobal.ALLOW_IP_ADDRESS.contains(request.getRemoteAddres())) {
            String type = request.getParameter("Type");
            String responseStr = "";
            if ("JVM".equals(type)) {
                responseStr = JSON.toJSON(TPerformance.getJVMInfo());
            } else if ("CPU".equals(type)) {
                responseStr = JSON.toJSON(TPerformance.getProcessorInfo());
            } else if ("Memory".equals(type)) {
                responseStr = JSON.toJSON(TPerformance.getJVMMemoryInfo());
            } else if ("MemoryUsage".equals(type)) {
                responseStr = JSON.toJSON(TPerformance.getJVMMemoryUsage());
            } else if ("Objects".equals(type)) {
                String filterWord = request.getParameter("Param1");
                filterWord = filterWord == null ? ".*" : filterWord;
                String headCountStr = request.getParameter("Param2");
                int headCount = headCountStr==null ? 10 : Integer.valueOf(headCountStr);

                responseStr = JSON.toJSON(TPerformance.getJVMObjectInfo(filterWord, headCount));
            } else if ("GC".equals(type)) {
                responseStr = JSON.toJSON(TPerformance.getJVMGCInfo());
            } else if ("Threads".equals(type)) {
                String state = request.getParameter("Param1");
                boolean withStack  = Boolean.valueOf(request.getParameter("Param2"));

                responseStr = JSON.toJSON(TPerformance.getThreadDetail(state, withStack));
            } else if ("ThreadCount".equals(type)) {
                responseStr = Integer.toString(TEnv.getThreads().length);
            } else if ("ThreadPool".equals(type)) {
                responseStr = JSON.toJSON(TPerformance.getThreadPoolInfo());
            } else if ("RequestAnalysis".equals(type)) {
                responseStr = JSON.toJSON(requestInfo());
            } else if ("IPAddressAnalysis".equals(type)) {
                responseStr = JSON.toJSON(ipAddressInfo());
            } else if ("Log".equals(type)) {
                String logType = request.getParameter("Param1");
                logType = logType == null? "SYSOUT" : logType;
                String lineNumberStr = request.getParameter("Param2");
                int lineNumber = lineNumberStr==null ? 50 : Integer.valueOf(lineNumberStr);
                responseStr = readLogs(logType, lineNumber);
            } else if("Summary".equals(type)){
                Map summary = new LinkedHashMap();
                summary.put("CPU", TPerformance.getProcessorInfo());
                summary.put("Memory", TPerformance.getJVMMemoryInfo());
                summary.put("MemoryUsage", TPerformance.getJVMMemoryUsage());
                summary.put("ThreadPool", TPerformance.getThreadPoolInfo());
                summary.put("ThreadCount", TEnv.getThreads().length);

                if(!"fast".equals(request.getParameter("Param1"))) {
                    summary.put("Objects", TPerformance.getJVMObjectInfo("", 10));
                    summary.put("GC", TPerformance.getJVMGCInfo());
                    summary.put("RunningThreads", TPerformance.getThreadDetail("RUNNABLE", false));
                    summary.put("RequestAnalysis", requestInfo());
                    summary.put("IPAddressAnalysis", ipAddressInfo());
                }
                responseStr = JSON.toJSON(summary);
            }  else {
                request.getSession().close();
            }

            response.header().put("Content-Type", "application/json");
            response.write(responseStr);
        }else{
            request.getSession().close();
        }
    }
}
