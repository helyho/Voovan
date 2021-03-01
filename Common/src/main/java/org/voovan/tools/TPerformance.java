package org.voovan.tools;

import org.voovan.Global;
import org.voovan.tools.log.Logger;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

	private static List<String> LOCAL_IP_ADDRESSES = new ArrayList<String>();
	private static ConcurrentHashMap<String, byte[]> LOCAL_IP_MAC = new ConcurrentHashMap<>();
	private static List<NetworkInterface> NETWROK_INTERFACE = new ArrayList<NetworkInterface>();


	private static long START_TIME_MILLIS	= System.currentTimeMillis();


	static {
		getLocalIpAddrs();
	}

	/**
	 * 内存信息类型枚举
	 */
	public enum MemType {
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
	 * @return 系统的负载情况
	 */
	public static double getSystemLoadAverage(){
		return osmxb.getSystemLoadAverage();
	}

	/**
	 * 获取当前系统 CPU 数
	 * @return 系统 CPU 数
	 */
	public static int getProcessorCount(){
		return osmxb.getAvailableProcessors();
	}

	/**
	 * 获取主机所有的网络接口
	 * @return 网络接口 List
	 */
	public static List<NetworkInterface> getNetworkInterfaces() {
		if (NETWROK_INTERFACE.size() == 0) {
			try {
				Enumeration netInterfaces = NetworkInterface.getNetworkInterfaces();
				InetAddress ip = null;
				while (netInterfaces.hasMoreElements()) {
					NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
					NETWROK_INTERFACE.add(ni);
				}
			} catch (SocketException e) {
				Logger.error(e);
			}
		}

		return NETWROK_INTERFACE;
	}

	/**
	 * 将 byte[]形式的 mac 地址, 转换为字符串形式的 mac 地址
	 * @param macBytes mac 地址字节数组
	 * @return mac 地址字符串形式
	 */
	public static String convertByteMac(byte[] macBytes){
		if(macBytes == null) return null;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < macBytes.length; i++) {
			sb.append(String.format("%02X%s", macBytes[i], (i < macBytes.length - 1) ? "-" : ""));
		}

		return sb.toString();
	}

	/**
	 * 获取所有的本机 ip 地址
	 * @return 所有的本机 ip 地址 List
	 */
	public static List<String> getLocalIpAddrs() {
		if (LOCAL_IP_ADDRESSES.size() == 0) {
			for(NetworkInterface networkInterface : getNetworkInterfaces()) {
				InetAddress ip = null;
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				while (inetAddresses.hasMoreElements()) {
					ip = (InetAddress) inetAddresses.nextElement();
					LOCAL_IP_ADDRESSES.add(ip.getHostAddress());
					try {
						byte[] macBytes = networkInterface.getHardwareAddress();
						if(macBytes!=null) {
							LOCAL_IP_MAC.put(ip.getHostAddress(), macBytes);
						}
					} catch (SocketException e) {
						Logger.error(e);
					}
				}
			}
		}

		return LOCAL_IP_ADDRESSES;
	}

	/**
	 * 获取某个ip 地址的 mac 地址
	 * @param address ip 地址
	 * @return mac 地址
	 */
	public static byte[] getMacByAddress(String address){
		return LOCAL_IP_MAC.get(address);
	}

	/**
	 * 获取系统单 CPU 核心的平均负载
	 * @return 单 CPU 核心的平均负载
	 */
	public static double cpuPerCoreLoadAvg(){
		double perCoreLoadAvg = osmxb.getSystemLoadAverage()/osmxb.getAvailableProcessors();
		BigDecimal bg = new BigDecimal(perCoreLoadAvg);
		perCoreLoadAvg = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		return perCoreLoadAvg;
	}

	private static long prevGetTime = System.nanoTime();
	private static long prevCpuUsedTime = 0;

	/**
	 * 获取当前进程 cpu 使用量
	 * @return cpu 使用量, 如使用2核, 返回200%
	 */
	public static double getProcessCpuUsage() {
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		long totalCpuUsedTime = 0;
		for (long id : threadBean.getAllThreadIds()) {
			totalCpuUsedTime += threadBean.getThreadCpuTime(id);
		}
		long curtime = System.nanoTime();
		long usedTime = totalCpuUsedTime - prevCpuUsedTime; //cpu 用时差
		long totalPassedTime = curtime - prevGetTime; //时间差
		prevGetTime = curtime;
		prevCpuUsedTime = totalCpuUsedTime;
		return (((double) usedTime) / totalPassedTime) * 100;
	}

	/**
	 * JVM 虚拟机的内存使用情况
	 * @return 内存使用情况
	 * */
	public static double getJVMMemoryUsage(){
		//maxMemory()这个方法返回的是java虚拟机（这个进程）能构从操作系统那里挖到的最大的内存，以字节为单位, -Xmx参数
		//totalMemory()这个方法返回的是java虚拟机现在已经从操作系统那里挖过来的内存大小
		//freeMemory() 当前申请到的内存有多少没有使用的空闲内存
		Runtime runtime = Runtime.getRuntime();
		double memoryUsage = 1-((double)runtime.freeMemory()+(runtime.maxMemory()-runtime.totalMemory()))/(double)runtime.maxMemory();
		BigDecimal bg = new BigDecimal(memoryUsage);
		memoryUsage = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		return memoryUsage;
	}

	/**
	 * 获取部分内存信息(栈,非栈)
	 * @param memType 获取的信息类型
	 * @return 当前内存数值
	 */
	public static long getJVMMemoryInfo(MemType memType){
		if(memType== MemType.NOHEAP_INIT){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getInit();
		} else if(memType== MemType.NOHEAP_MAX){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax();
		} else if(memType== MemType.NOHEAP_USAGE){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
		} else if(memType== MemType.NOHEAP_COMMIT){
			return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getCommitted();
		} else if(memType== MemType.HEAP_INIT){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getInit();
		} else if(memType== MemType.HEAP_MAX){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
		} else if(memType== MemType.HEAP_USAGE){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
		} else if(memType== MemType.HEAP_COMMIT){
			return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted();
		}else{
			throw new RuntimeException("getMemoryInfo function arg error!");
		}
	}

	/**
	 * 获取当前内存信息
	 * @return  内存信息描述对象
	 */
	public static MemoryInfo getJVMMemoryInfo(){
		MemoryInfo memoryInfo = new MemoryInfo();
		memoryInfo.setHeapInit(getJVMMemoryInfo(MemType.HEAP_INIT));
		memoryInfo.setHeapUsage(getJVMMemoryInfo(MemType.HEAP_USAGE));
		memoryInfo.setHeapCommit(getJVMMemoryInfo(MemType.HEAP_COMMIT));
		memoryInfo.setHeapMax(getJVMMemoryInfo(MemType.HEAP_MAX));

		memoryInfo.setNoHeapInit(getJVMMemoryInfo(MemType.NOHEAP_INIT));
		memoryInfo.setNoHeapUsage(getJVMMemoryInfo(MemType.NOHEAP_USAGE));
		memoryInfo.setNoHeapCommit(getJVMMemoryInfo(MemType.NOHEAP_COMMIT));
		memoryInfo.setNoHeapMax(getJVMMemoryInfo(MemType.NOHEAP_MAX));
		memoryInfo.setFree(Runtime.getRuntime().freeMemory());
		memoryInfo.setTotal(Runtime.getRuntime().totalMemory());
		memoryInfo.setMax(Runtime.getRuntime().maxMemory());
		return memoryInfo;
	}

	/**
	 * 获取指定进程的 GC 信息
	 * @param pid 进程 Id
	 * @return GC信息
	 * @throws IOException IO 异常
	 */
	public static Map<String, String> getJVMGCInfo(long pid) throws IOException {
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		Process process = TEnv.createSysProcess("jstat -gcutil "+pid, null, (File)null);
		InputStream processInputStream = process.getInputStream();
		String console = new String(TStream.readAll(processInputStream));
		String[] consoleLines = console.split(System.lineSeparator());
		String[] titleLine = consoleLines[0].trim().split("\\s+");
		String[] dataLine = consoleLines[1].trim().split("\\s+");
		for(int i=0; i<titleLine.length; i++){
			result.put(titleLine[i], dataLine[i]);
		}
		return result;
	}

	/**
	 * 获取当前进程的 GC 信息
	 * @return GC信息
	 * @throws IOException IO 异常
	 */
	public static Map<String, String> getJVMGCInfo() throws IOException {
		return getJVMGCInfo(TEnv.getCurrentPID());
	}

	/**
	 * 获取指定进程的对象信息
	 * @param pid		进程 Id
	 * @param regex     对象匹配字符串
	 * @param headCount 返回的对象数
	 * @return 虚拟机中的对象信息
	 * @throws IOException IO 异常
	 */
	public static Map<String,ObjectInfo> getJVMObjectInfo(long pid, String regex, Integer headCount) throws IOException, InterruptedException {

		regex = regex == null ? ".*" : regex;

		LinkedHashMap<String,ObjectInfo> result = new LinkedHashMap<String,ObjectInfo>();
		Process process = TEnv.createSysProcess("jmap -histo "+pid, null, (File)null);
		process.waitFor();
		InputStream processInputStream = process.getInputStream();
		String console = new String(TStream.readAll(processInputStream));


		String[] consoleLines = console.split(System.lineSeparator());

		if(headCount==null || headCount<0){
			headCount = consoleLines.length;
		}else{
			headCount=headCount+3;
		}

		for(int lineCount = 3;lineCount<headCount;lineCount++){
			String lineContent = consoleLines[lineCount];
			long count = Long.parseLong(lineContent.substring(5,19).trim());
			long size = Long.parseLong(lineContent.substring(19,34).trim());
			String name = lineContent.substring(34,lineContent.length()).trim();

			if(name.isEmpty()){
				continue;
			}

			if(TString.regexMatch(name,regex) > 0) {
				ObjectInfo objectInfo = new ObjectInfo(name, size, count);
				result.put(name,objectInfo);
			}
		}
		return result;
	}

	/**
	 * 获取指定进程的对象信息
	 * @param regex     对象匹配字符串
	 * @return 虚拟机中的对象信息
	 * @throws IOException IO 异常
	 */
	public static Map<String,ObjectInfo> getJVMObjectInfo(String regex) throws IOException, InterruptedException {
		return TPerformance.getJVMObjectInfo(TEnv.getCurrentPID(), regex, null);
	}

	/**
	 * 获取当前JVM加载的对象信息(数量,所占内存大小)
	 * @param regex 正则表达式
	 * @param headCount 头部记录数
	 * @return 系统对象信息的Map
	 */
	public static Map<String,TPerformance.ObjectInfo> getJVMObjectInfo(String regex, Integer headCount) {
		if(regex==null){
			regex = ".*";
		}

		Map<String,TPerformance.ObjectInfo> result;
		try {
			result = TPerformance.getJVMObjectInfo(TEnv.getCurrentPID(), regex, headCount);
		} catch (IOException | InterruptedException e) {
			result = new Hashtable<String,TPerformance.ObjectInfo>();
		}

		return result;

	}

	/**
	 * 获取当前 JVM 线程信息描述
	 * @return 线程信息信息集合
	 */
	public static Map<String,Object> getThreadPoolInfo(){
		Map<String,Object> threadPoolInfo = new HashMap<String,Object>();
		ThreadPoolExecutor threadPoolInstance = Global.getThreadPool();
		threadPoolInfo.put("ActiveCount",threadPoolInstance.getActiveCount());
		threadPoolInfo.put("CorePoolSize",threadPoolInstance.getCorePoolSize());
		threadPoolInfo.put("FinishedTaskCount",threadPoolInstance.getCompletedTaskCount());
		threadPoolInfo.put("TaskCount",threadPoolInstance.getTaskCount());
		threadPoolInfo.put("QueueSize",threadPoolInstance.getQueue().size());
		return threadPoolInfo;
	}

	/**
	 * 获取当前 JVM 线程信息描述
	 * @param state 线程状态, nul,返回所有状态的线程
	 * @param withStack 是否包含堆栈信息
	 * @return 线程信息集合
	 */
	public static List<Map<String,Object>> getThreadDetail(String state, boolean withStack){
		ArrayList<Map<String,Object>> threadDetailList = new ArrayList<Map<String,Object>>();
		for(Thread thread : TEnv.getThreads()){
			if(state==null || thread.getState().name().equals(state)) {
				Map<String, Object> threadDetail = new Hashtable<String, Object>();
				threadDetail.put("Name", thread.getName());
				threadDetail.put("Id", thread.getId());
				threadDetail.put("Priority", thread.getPriority());
				threadDetail.put("ThreadGroup", thread.getThreadGroup().getName());
				if (withStack) {
					threadDetail.put("StackTrace", TEnv.getStackElementsMessage(thread.getStackTrace()));
				}

				threadDetail.put("State", thread.getState().name());
				threadDetailList.add(threadDetail);
			}
		}
		return threadDetailList;
	}

	public static Map<String,Object> getCpuInfo(){
		Map<String,Object> processInfo = new Hashtable<String,Object>();
		processInfo.put("ProcessorCount",TPerformance.getProcessorCount());
		processInfo.put("CpuUsage",TPerformance.getProcessCpuUsage());
		processInfo.put("LoadAverage",TPerformance.getSystemLoadAverage());
		return processInfo;
	}

	/**
	 * 获取进程信息
	 * @return 进程信息 Map
	 */
	public static Map<String,Object> getProcessInfo(boolean isAll) throws IOException {
		Map<String,Object> processInfo = new Hashtable<String,Object>();
		processInfo.put("ProcessorCount",TPerformance.getProcessorCount());
		processInfo.put("CpuUsage",TPerformance.getProcessCpuUsage());
		processInfo.put("LoadAverage",TPerformance.getSystemLoadAverage());
		processInfo.put("Memory",TPerformance.getJVMMemoryInfo());

		if(isAll) {
			processInfo.put("ThreadCount", TEnv.getThreads().length);
			processInfo.put("RunningThreads", TPerformance.getThreadDetail("RUNNABLE", false).size());
			processInfo.put("GC", TPerformance.getJVMGCInfo());
		}
		return processInfo;
	}

	/**
	 * 获取JVM信息
	 * @return JVM 信息的 Map
	 */
	public static Map<String,Object> getJVMInfo(){
		Map<String, Object> jvmInfo = new Hashtable<String, Object>();
		for(Map.Entry<Object,Object> entry : System.getProperties().entrySet()){
			jvmInfo.put(entry.getKey().toString(),entry.getValue().toString());
		}
		return jvmInfo;
	}

	/**
	 * 获取当前进程的运行时间
	 * @param timeUnit 时间单位
	 * @return 响应的时间
	 */
	public static Long getRuningTime(TimeUnit timeUnit){
		return timeUnit.convert(getRuningTime(), TimeUnit.MILLISECONDS);
	}

	/**
	 * 获取当前进程的运行时间 (毫秒)
	 * @return 当前进程的运行时间
	 */
	public static Long getRuningTime(){
		return System.currentTimeMillis() - START_TIME_MILLIS;
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
