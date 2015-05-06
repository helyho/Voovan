package org.voovan.tools;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;

/**
 * 系统性能相关
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TPerformance {
	
	/**
	 * 获取系统单 CPU 核心的平均负载
	 * @return
	 */
	public static double cpuPerCoreLoadAvg(){
		OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory   
                .getOperatingSystemMXBean();   
		double perCoreLoadAvg = osmxb.getSystemLoadAverage()/osmxb.getAvailableProcessors();
		BigDecimal bg = new BigDecimal(perCoreLoadAvg);
		perCoreLoadAvg = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		return perCoreLoadAvg;
	}
	
	/**
	 * JVM 虚拟机的内存使用情况
	 * @return
	 */
	public static double jvmMemoryUsage(){
		Runtime runtime = Runtime.getRuntime();
		double memoryUsage = 1-((double)runtime.freeMemory()+(runtime.maxMemory()-runtime.totalMemory()))/(double)runtime.maxMemory();
		BigDecimal bg = new BigDecimal(memoryUsage);
		memoryUsage = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		return memoryUsage;
	}
	
	
}
