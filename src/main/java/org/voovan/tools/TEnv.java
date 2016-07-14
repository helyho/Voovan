package org.voovan.tools;

import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
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
	public static byte[] createSysProcess(String command) throws IOException {
		Runtime runTime  = Runtime.getRuntime();
		Process process = runTime.exec(command);
		InputStream is = process.getInputStream();
		return TStream.readAll(is);
	}

	/**
	 * 获得应用的工作根目录路径
	 * 
	 * @return 工作根目录路径
	 */
	public static String getContextPath() {
		return System.getProperty("user.dir");
	}

	/**
	 * 使用相对路径获得系统的完整路径
	 * @param absolutePath 相对路径
	 * @return 系统的完整路径
	 */
	public static String getSystemPath(String absolutePath) {
		return getContextPath() + File.separator + absolutePath;
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
	 * 为JVM加载一个jar包 或者一个目录到 classpath
	 * @param file 文件路径
	 * @throws SecurityException  安全性异常
	 * @throws NoSuchMethodException  无方法异常
	 * @throws IOException IO异常
	 */
	public static void loadBinary(File file) throws NoSuchMethodException, SecurityException, IOException {
		if(!file.exists()){
			Logger.error("Method loadBinary, This director["+file.getCanonicalPath()+"] is not exists");
		}

		try {
			if (file.isDirectory() || file.getPath().toLowerCase().endsWith(".jar")) {
				URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
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
	 * @throws Exception 异常信息
	 */
	public static void loadBinary(String filePath) throws Exception {
		File file = new File(filePath);
		loadBinary(file);
	}

	/**
	 * 从目录读取所有 Jar 文件,递归并加载到JVM
	 *
	 * @param rootFile 传入一个File 对象
	 * @throws IOException IO异常
	 */
	public static void loadJars(File rootFile) throws IOException, NoSuchMethodException {
		if(!rootFile.exists()){
			Logger.error("Method loadJars, This director["+rootFile.getCanonicalPath()+"] is not exists");
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
				for(File file:files){
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
	 * @throws Exception 异常信息
	 */
	public static void loadJars(String directoryPath) throws IOException, NoSuchMethodException {
		File file = new File(directoryPath);
		loadJars(file);
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
}
