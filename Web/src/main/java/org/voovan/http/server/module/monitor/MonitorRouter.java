package org.voovan.http.server.module.monitor;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;
import org.voovan.http.server.context.WebContext;
import org.voovan.tools.*;
import org.voovan.tools.json.JSONEncode;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
     * 对象转换成 JSON 字符串
     *      json 中的换行被处理成"\\r\\n"
     * @param obj 待转换的对象
     * @return JSON 字符串
     */
    public static String toJsonWithLF(Object obj){
        String jsonStr = null;
        try {
            jsonStr = JSONEncode.fromObject(obj);
            return jsonStr;
        } catch (ReflectiveOperationException e) {
            Logger.error(e);
        }
        return "";
    }

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
                responseStr = toJsonWithLF(TPerformance.getJVMInfo());
            } else if ("CPU".equals(type)) {
                responseStr = toJsonWithLF(TPerformance.getProcessorInfo());
            } else if ("Memory".equals(type)) {
                responseStr = toJsonWithLF(TPerformance.getJVMMemoryInfo());
            } else if ("MemoryUsage".equals(type)) {
                responseStr = toJsonWithLF(TPerformance.getJVMMemoryUsage());
            } else if ("Objects".equals(type)) {
                String filterWord = request.getParameter("Param1");
                responseStr = toJsonWithLF(TPerformance.getJVMObjectInfo(filterWord));
            } else if ("ObjectCount".equals(type)) {
                responseStr = Integer.toString(TPerformance.getJVMObjectInfo("").size());
            } else if ("Threads".equals(type)) {
                responseStr = toJsonWithLF(TPerformance.getThreadDetail());
            } else if ("ThreadCount".equals(type)) {
                responseStr = Integer.toString(TEnv.getThreads().length);
            } else if ("ThreadPool".equals(type)) {
                responseStr = toJsonWithLF(TPerformance.getThreadPoolInfo());
            } else if ("RequestAnalysis".equals(type)) {
                responseStr = toJsonWithLF(requestInfo());
            } else if ("IPAddressAnalysis".equals(type)) {
                responseStr = toJsonWithLF(ipAddressInfo());
            } else if ("Log".equals(type)) {
                String logType = request.getParameter("Param1");
                int lineNumber = Integer.parseInt(request.getParameter("Param2"));
                responseStr = readLogs(logType, lineNumber);
            } else {
                request.getSession().close();
            }
            response.write(responseStr);
        }else{
            request.getSession().close();
        }
    }
}
