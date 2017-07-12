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
import java.util.ArrayList;
import java.util.List;

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


	/**
	 * 为JVM加载一个jar包 或者一个目录到 classpath
	 * @param file 文件路径
	 * @throws SecurityException  安全性异常
	 * @throws NoSuchMethodException  无方法异常
	 * @throws IOException IO异常
	 */
	public static void loadBinary(File file) throws NoSuchMethodException, SecurityException, IOException {
		if(!file.exists()){
			Logger.warn("Method loadBinary, This ["+file.getCanonicalPath()+"] is not exists");
		}

		try {
			if (file.isDirectory() || file.getPath().toLowerCase().endsWith(".jar")) {
				URLClassLoader urlClassLoader = null;

				ClassLoader currentClassLoader = TEnv.class.getClassLoader();
				if(currentClassLoader instanceof URLClassLoader){
					urlClassLoader = (URLClassLoader)currentClassLoader;
				} else {
					urlClassLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
				}

				Method method = TReflect.findMethod(URLClassLoader.class, "addURL", URL.class);
				method.setAccessible(true);
				TReflect.invokeMethod(urlClassLoader, method, file.toURI().toURL());
			}
		} catch (IOException | ReflectiveOperationException e) {
			Logger.error("Load jar or class failed",e);
		}
	}

	/**
	 * 为JVM加载一个jar包 或者一个目录到 classpath
	 * @param filePath  文件路径
	 * @throws NoSuchMethodException 异常信息
	 * @throws IOException 异常信息
	 */
	public static void loadBinary(String filePath) throws NoSuchMethodException, IOException {
		File file = new File(filePath);
		loadBinary(file);
	}

	/**
	 * 从目录读取所有 Jar 文件,递归并加载到JVM
	 *
	 * @param rootFile 传入一个File 对象
	 * @throws IOException IO异常
	 * @throws NoSuchMethodException 异常信息
	 */
	public static void loadJars(File rootFile) throws IOException, NoSuchMethodException {
		if(!rootFile.exists()){
			Logger.warn("Method loadJars, This ["+rootFile.getCanonicalPath()+"] is not exists");
		}
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
			if(files!=null){
				for(File file : files){
					if(file.isDirectory()){
						loadJars(file.getPath());
					}else if(file.getPath().toLowerCase().endsWith(".jar")){
						loadBinary(file);
					}
				}
			}
		}
	}

	/**
	 * 从目录读取所有 Jar 文件,递归并加载到JVM
	 *
	 * @param directoryPath 传入一个目录
	 * @throws IOException 异常信息
	 * @throws NoSuchMethodException 异常信息
	 */
	public static void loadJars(String directoryPath) throws IOException, NoSuchMethodException {
		File file = new File(directoryPath);
		loadJars(file);
	}

	/**
	 * 获取类JVM 的 Class Path
	 *   因部分 ide 会自动增加全部的 jvm 的 classpath, 所以这里会自动剔除 classpath 中 jvm 的 classPath
	 * @return 获得用户的类加载路径
	 */
	public static List<String> getClassPath(){
		ArrayList<String> userClassPath = new ArrayList<String>();
		String javaHome = System.getProperty("java.home");
		javaHome = javaHome.replaceAll("\\/[a-zA-z0-9\\_\\$]*$","");
		String [] classPaths = System.getProperty("java.class.path").split(File.pathSeparator);
		for(String classPath : classPaths){
			if(!classPath.startsWith(javaHome)){
				userClassPath.add(classPath);
			}
		}
		return userClassPath;
	}
}
