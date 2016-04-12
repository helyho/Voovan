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

	/**
	 * 内存信息类型枚举
	 */
	public enum MEMTYPE{
		NOHEAP_INIT,
		HEAP_INIT,

		NOHEAP_MAX,
		HEAP_MAX,

		NOHEAP_USAGE,
		HEAP_USAGE,

		NOHEAP_COMMIT,
		HEAP_COMMIT
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
	public static double getProcessorCount(){
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
	 * @return	 */
	public static double getJVMMemoryUsage(){
		Runtime runtime = Runtime.getRuntime();
		double memoryUsage = 1-((double)runtime.freeMemory()+(runtime.maxMemory()-runtime.totalMemory()))/(double)runtime.maxMemory();
		BigDecimal bg = new BigDecimal(memoryUsage);
		memoryUsage = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		return memoryUsage;
	}

	/**
	 * 获取部分内存信息(栈,非栈)
	 * @param memType 获取的信息类型
     * @return
     */
	public static long getHeapMemoryInfo(MEMTYPE memType){
		if(memType==MEMTYPE.NOHEAP_INIT){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getInit();
		} else if(memType==MEMTYPE.NOHEAP_MAX){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax();
		} else if(memType==MEMTYPE.NOHEAP_USAGE){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
		} else if(memType== MEMTYPE.NOHEAP_COMMIT){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getCommitted();
		} else if(memType==MEMTYPE.HEAP_INIT){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getInit();
		} else if(memType==MEMTYPE.HEAP_MAX){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
		} else if(memType==MEMTYPE.HEAP_USAGE){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
		} else if(memType==MEMTYPE.HEAP_COMMIT){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted();
		}else{
			throw new RuntimeException("getMemoryInfo function arg error!");
		}
	}

	/**
	 * 获取当前内存信息
	 * @return  内存信息描述对象
     */
	public static MemoryInfo getMemoryInfo(){
		MemoryInfo memoryInfo = new MemoryInfo();
		memoryInfo.setHeapInit(getHeapMemoryInfo(MEMTYPE.HEAP_INIT));
		memoryInfo.setHeapUsage(getHeapMemoryInfo(MEMTYPE.HEAP_USAGE));
		memoryInfo.setHeapCommit(getHeapMemoryInfo(MEMTYPE.HEAP_COMMIT));
		memoryInfo.setHeapMax(getHeapMemoryInfo(MEMTYPE.HEAP_MAX));

		memoryInfo.setNoHeapInit(getHeapMemoryInfo(MEMTYPE.NOHEAP_INIT));
		memoryInfo.setNoHeapUsage(getHeapMemoryInfo(MEMTYPE.NOHEAP_USAGE));
		memoryInfo.setNoHeapCommit(getHeapMemoryInfo(MEMTYPE.NOHEAP_COMMIT));
		memoryInfo.setNoHeapMax(getHeapMemoryInfo(MEMTYPE.NOHEAP_MAX));
		memoryInfo.setFree(Runtime.getRuntime().freeMemory());
		memoryInfo.setTotal(Runtime.getRuntime().totalMemory());
		memoryInfo.setMax(Runtime.getRuntime().maxMemory());
		return memoryInfo;
	}

	/**
	 * 获取虚拟机中的对象信息
	 * @param pid		进程 Id
	 * @param regex     对象匹配字符串
	 * @return
	 * @throws IOException
     */
	public static Map<String,ObjectInfo> getSysObjectInfo(long pid,String regex) throws IOException {
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

	/**
	 * 内存信息对象
	 */
	public static class MemoryInfo{
		private long heapInit;
		private long heapUsage;
		private long heapMax;
		private long heapCommit;
		private long noHeapInit;
		private long noHeapUsage;
		private long noHeapMax;
		private long noHeapCommit;
		private long free;
		private long max;
		private long total;

		public long getFree() {
			return free;
		}

		public void setFree(long free) {
			this.free = free;
		}

		public long getMax() {
			return max;
		}

		public void setMax(long max) {
			this.max = max;
		}

		public long getTotal() {
			return total;
		}

		public void setTotal(long total) {
			this.total = total;
		}

		public long getHeapInit() {
			return heapInit;
		}

		public void setHeapInit(long heapInit) {
			this.heapInit = heapInit;
		}

		public long getHeapUsage() {
			return heapUsage;
		}

		public void setHeapUsage(long heapUsage) {
			this.heapUsage = heapUsage;
		}

		public long getHeapMax() {
			return heapMax;
		}

		public void setHeapMax(long heapMax) {
			this.heapMax = heapMax;
		}

		public long getHeapCommit() {
			return heapCommit;
		}

		public void setHeapCommit(long heapCommit) {
			this.heapCommit = heapCommit;
		}

		public long getNoHeapInit() {
			return noHeapInit;
		}

		public void setNoHeapInit(long noHeapInit) {
			this.noHeapInit = noHeapInit;
		}

		public long getNoHeapUsage() {
			return noHeapUsage;
		}

		public void setNoHeapUsage(long noHeapUsage) {
			this.noHeapUsage = noHeapUsage;
		}

		public long getNoHeapMax() {
			return noHeapMax;
		}

		public void setNoHeapMax(long noHeapMax) {
			this.noHeapMax = noHeapMax;
		}

		public long getNoHeapCommit() {
			return noHeapCommit;
		}

		public void setNoHeapCommit(long noHeapCommit) {
			this.noHeapCommit = noHeapCommit;
		}
	}
}
