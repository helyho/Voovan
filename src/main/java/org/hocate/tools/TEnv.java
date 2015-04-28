package org.hocate.tools;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.hocate.tools.log.Logger;

/**
 * 系统环境相关
 * @author helyho
 *
 */
public class TEnv {

	/**
	 * 获得应用的工作根目录路径
	 * 
	 * @return
	 */
	public static String getContextPath() {
		return System.getProperty("user.dir");
	}

	/**
	 * 使用相对路径获得系统的完整路径
	 * 
	 * @return
	 */
	public static String getSystemPath(String absolutePath) {
		return getContextPath() + File.separator + absolutePath;
	}

	/**
	 * 休眠函数
	 * 
	 * @param sleepTime
	 */
	public static void sleep(int sleepTime) {
		try {
			Thread.currentThread();
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取当前栈信息
	 * 
	 * @return
	 */
	public static StackTraceElement[] getCurrentStackElements() {
		Throwable ex = new Throwable();
		return ex.getStackTrace();
	}
	
	/**
	 * 获取当前栈信息
	 * 
	 * @return
	 */
	public static String getCurrentStackMessage(){
		String stackInfo = "";
		Throwable ex = new Throwable();
		for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
			stackInfo += stackTraceElement.toString();
			stackInfo += "\r\n";
		}
		
		return stackInfo;
	}
	
	/**
	 * 获取当前栈信息
	 * 
	 * @return
	 */
	public static String getStackElementsMessage(StackTraceElement[] stackTraceElements){
		String stackInfo = "";
		for (StackTraceElement stackTraceElement : stackTraceElements) {
			stackInfo += stackTraceElement.toString();
			stackInfo += "\r\n";
		}
		
		return stackInfo;
	}
	
	/**
	 * 为JVM加载一个jar包 或者一个目录到 classpath
	 * @param file
	 */
	private static void loadBinary(File file) {
		try {
			if (file.isDirectory() || file.getPath().toLowerCase().endsWith(".jar")) {
				URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
				Method method = TReflect.findMethod(URLClassLoader.class, "addURL", URL.class);
				method.setAccessible(true);
				TReflect.invokeMethod(urlClassLoader, method, file.toURI().toURL());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 为JVM加载一个jar包 或者一个目录到 classpath
	 * @param file
	 * @throws Exception
	 */
	public static void loadBinary(String filePath) throws Exception {
		File file = new File(filePath);
		loadBinary(file);
	}

	/**
	 * 从目录读取所有 Jar 文件,递归并加载到JVM
	 * 
	 * @param directoryPath 传入一个目录
	 * @throws Exception
	 */
	public static void LoadJars(String directoryPath) throws Exception {
		File rootFile = new File(directoryPath);
		if(rootFile.isDirectory()){
			//文件过滤器取目录中的文件
			File[] files = rootFile.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					if(pathname.isDirectory() || pathname.getPath().toLowerCase().endsWith(".jar")){
						return true;
					}else{
						return false;
					}
				}
			});
			//遍历或者加载文件
			for(File file:files){
				if(file.isDirectory()){
					LoadJars(file.getPath());
				}else if(file.getPath().toLowerCase().endsWith(".jar")){
					loadBinary(file);
					Logger.simple("Load jar file : "+file.getPath());
				}
			}
		}
	}
	
	public static Thread[] getJVMThreads(){
		ThreadGroup group = Thread.currentThread().getThreadGroup().getParent();
		int estimatedSize = group.activeCount() * 2;
		Thread[] slackList = new Thread[estimatedSize];
		int actualSize = group.enumerate(slackList);
		Thread[] list = new Thread[actualSize];
		System.arraycopy(slackList, 0, list, 0, actualSize);
		return list;
	}
}
