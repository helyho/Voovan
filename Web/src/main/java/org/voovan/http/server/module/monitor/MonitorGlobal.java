package org.voovan.http.server.module.monitor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Some description
 *
 * @author: helyho
 * Project: Framework
 * Create: 2017/9/28 12:07
 */
public class MonitorGlobal {
    public static List<String> ALLOW_IP_ADDRESS = null;

    public static Map<String,RequestAnalysis> REQUEST_ANALYSIS= new ConcurrentHashMap<String,RequestAnalysis>();
    public static Map<String,IPAnalysis> IP_ANALYSIS = new ConcurrentHashMap<String,IPAnalysis>();

}
