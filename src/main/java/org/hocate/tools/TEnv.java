package org.hocate.tools;

import java.io.File;

public class TEnv {

	/**
	 * 获得应用的工作根目录路径
	 * 
	 * @return
	 */
	public static String getAppContextPath() {
		return System.getProperty("user.dir");
	}

	/**
	 * 使用相对路径获得系统的完整路径
	 * 
	 * @return
	 */
	public static String getSysPathFromContext(String absolutePath) {
		return getAppContextPath()+File.separator+absolutePath;
	}

	
	/**
	 * 休眠函数
	 * @param sleepTime
	 */
	public static void sleep(int sleepTime){
		try {
			Thread.currentThread();
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取当前栈信息
	 * @return
	 */
	public static StackTraceElement[] getCurrentStackInfo(){
		Throwable ex = new Throwable();
		return ex.getStackTrace();
	}
}
