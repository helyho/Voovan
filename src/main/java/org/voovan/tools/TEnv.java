package org.voovan.tools;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 系统环境相关
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TEnv {

	/**
	 * 获取当前进程 PID
	 * @return 当前进程 ID
     */
	public static long getCurrentPID(){
		return Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	}

	/**
	 * 构造一个系统进程
	 * @param command 命令行
	 * @return 控制台输出
	 * @throws IOException IO 异常
     */
	public static Process createSysProcess(String command) throws IOException {
		Runtime runTime  = Runtime.getRuntime();
		return runTime.exec(command);
	}

	/**
	 * 休眠函数
	 * 
	 * @param sleepTime 休眠时间
	 */
	public static void sleep(int sleepTime) {
		try {
			Thread.currentThread();
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			Logger.error("TEnv.sleep interrupted",e);
		}
	}

	/**
	 * 获取当前栈信息
	 * 
	 * @return 当前栈信息
	 */
	public static StackTraceElement[] getStackElements() {
		Throwable ex = new Throwable();
		return ex.getStackTrace();
	}
	
	/**
	 * 获取当前栈信息
	 * 		会自动过滤掉栈里的第一行,即当前类的信息
	 *
	 * @return 当前栈信息
	 */
	public static String getStackMessage(){
		StringBuilder stackInfo = new StringBuilder();
		Throwable ex = new Throwable();
		int row = 0;
		for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
			if(row!=0){
				stackInfo.append(stackTraceElement.toString());
				stackInfo.append("\r\n");
			}
			row++;
		}
		return stackInfo.toString();
	}
	
	/**
	 * 获取当前栈信息
	 * @param stackTraceElements 栈信息对象数组
	 * @return 当前栈信息
	 */
	public static String getStackElementsMessage(StackTraceElement[] stackTraceElements){
		StringBuilder stackInfo = new StringBuilder();
		for (StackTraceElement stackTraceElement : stackTraceElements) {
			stackInfo.append(stackTraceElement.toString());
			stackInfo.append("\r\n");
		}
		
		return stackInfo.toString();
	}

	/**
	 * 获取JVM中的所有线程
	 * @return 线程对象数组
	 */
	public static Thread[] getThreads(){
		ThreadGroup group = Thread.currentThread().getThreadGroup().getParent();
		int estimatedSize = group.activeCount() * 2;
		Thread[] slackList = new Thread[estimatedSize];
		int actualSize = group.enumerate(slackList);
		Thread[] list = new Thread[actualSize];
		System.arraycopy(slackList, 0, list, 0, actualSize);
		return list;
	}

	/**
	 * 获取进程的主线程
	 * @return 进程的主线程
	 */
	public static Thread getMainThread(){
		for(Thread thread: TEnv.getThreads()){
			if(thread.getId()==1){
				return thread;
			}
		}
		return null;
	}
}
