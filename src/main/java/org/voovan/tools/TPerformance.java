package org.voovan.tools;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Map;

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

	private static OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

	public static final int MEM_NOHEAP=-1;
	public static final int MEM_HEAP=-2;

	public static final int MEM_INIT=1;
	public static final int MEM_MAX=2;
	public static final int MEM_USAGE=3;
	public static final int MEM_COMMIT=3;
	/**
	 * 获取 CPU 数量
	* @return
			*/
	public static int getCPUCount(){
		return osmxb.getAvailableProcessors();
	}

	/**
	 * 获取当前系统的负载情况
	 * @return
     */
	public static double getSystemLoadAverage(){
		return osmxb.getSystemLoadAverage();
	}

	/**
	 * 获取当前系统 CPU 数
	 * @return
     */
	public static double getAvailableProcessors(){
		return osmxb.getAvailableProcessors();
	}

	/**
	 * 获取系统单 CPU 核心的平均负载
	 * @return
	 */
	public static double cpuPerCoreLoadAvg(){
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

	/**
	 * 获取内存信息(栈,非栈)
	 * @param type
	 * @param kind
     * @return
     */
	public static long getMemoryInfo(int type,int kind){
		if(type==MEM_NOHEAP && kind == MEM_INIT){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getInit();
		} else if(type==MEM_NOHEAP && kind == MEM_MAX){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax();
		} else if(type==MEM_NOHEAP && kind == MEM_USAGE){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
		} else if(type==MEM_NOHEAP && kind == MEM_COMMIT){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getCommitted();
		} else if(type==MEM_HEAP && kind == MEM_INIT){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getInit();
		} else if(type==MEM_HEAP && kind == MEM_MAX){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
		} else if(type==MEM_HEAP && kind == MEM_USAGE){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
		} else if(type==MEM_HEAP && kind == MEM_COMMIT){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted();
		}else{
			throw new RuntimeException("getMemoryInfo function arg error!");
		}
	}

	/**
	 * 获取虚拟机中的对象信息
	 * @param pid		进程 Id
	 * @param regex     对象匹配字符串
	 * @return
	 * @throws IOException
     */
	public static Map<String,ObjectInfo> getSysObjectInfo(int pid,String regex) throws IOException {
		Hashtable<String,ObjectInfo> result = new Hashtable<String,ObjectInfo>();
		String console = new String(TEnv.createSysProcess("jmap -histo "+pid));
		String[] consoleLines = console.split(System.getProperty("line.separator"));
		for(int lineCount = 3;lineCount<consoleLines.length;lineCount++){
			String lineContent = consoleLines[lineCount];
			long count = new Long(lineContent.substring(5,19).trim());
			long size = new Long(lineContent.substring(19,34).trim());
			String name = lineContent.substring(34,lineContent.length()).trim();
			if(TString.searchByRegex(name,regex).length>0) {
				ObjectInfo objectInfo = new ObjectInfo(name, size, count);
				result.put(name,objectInfo);
			}
		}
		return result;
	}

	/**
	 * JVM 中对象信息
	 */
	public static class ObjectInfo{
		private String name;
		private long size;
		private long count;

		public ObjectInfo(String name,long size,long count){
			this.name = name;
			this.size = size;
			this.count = count;
		}

		public long getCount() {
			return count;
		}

		public void setCount(long count) {
			this.count = count;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public long getSize() {
			return size;
		}

		public void setSize(long size) {
			this.size = size;
		}
	}
}
