package org.voovan.tools;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
	 * @param env 环境变量参数
	 * @param workDir 工作目录
	 * @return Process 对象
	 * @throws IOException IO 异常
	 */
	public static Process createSysProcess(String command, String[] env, File workDir) throws IOException {
		Runtime runTime  = Runtime.getRuntime();
		if(workDir==null || workDir.exists()) {
			return runTime.exec(command, env, workDir);
		}
		return null;
	}

	/**
	 * 构造一个系统进程
	 * @param command 命令行
	 * @param env 环境变量参数
	 * @param workDir 工作目录
	 * @return Process 对象
	 * @throws IOException IO 异常
	 */
	public static Process createSysProcess(String command, String[] env, String workDir) throws IOException {
		return createSysProcess(command, env, (workDir==null ? null :new File(workDir)) );
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

		return stackInfo.toString().trim();
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
	 * 判断主线程是否结束
	 * @return true: 主线程结束, false: 主线程未结束
	 */
	public static boolean isMainThreadShutDown(){
		Thread mainThread = getMainThread();
		//如果主线程结束,则线程池也关闭
		if(mainThread!=null && mainThread.getState() == Thread.State.TERMINATED) {
			return true;
		}

		//如果主线程没有的,则线程池也关闭
		if(mainThread==null){
			return true;
		}

		return false;
	}

	/**
	 * 获取指定的 Class 的 URLClassLoader
	 * 		如果指定的 Class 不是使用 URLClassLoader 加载的
	 * 		则使用 ClassLoader.getSystemClassLoader()获取 JVM 的 ClassLoader
	 * @param clazz Class 对象
	 * @return URLClassLoader对象
	 */
	public static URLClassLoader getURLClassLoader(Class clazz){
		if(clazz == null){
			clazz = TEnv.class;
		}

		URLClassLoader urlClassLoader = null;
		ClassLoader currentClassLoader = clazz.getClassLoader();
		if(currentClassLoader instanceof URLClassLoader){
			urlClassLoader = (URLClassLoader)currentClassLoader;
		} else {
			urlClassLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		}

		return urlClassLoader;
	}


	/**
	 * 为JVM加载一个jar包, 一个目录内的所有 jar 包 或者一个目录到 classpath
	 * @param file 文件路径
	 * @throws SecurityException  安全性异常
	 * @throws NoSuchMethodException  无方法异常
	 * @throws IOException IO异常
	 */
	public static void addClassPath(File file) throws NoSuchMethodException, SecurityException, IOException {
		if(!file.exists()){
			Logger.simple("[WARN] Method loadBinary, This ["+file.getCanonicalPath()+"] is not exists");
		}

		URLClassLoader urlClassLoader = getURLClassLoader(null);

		try {
			if (file.isDirectory()) {
				List<File> subfiles = TFile.scanFile(file, "\\.jar$");
				if(subfiles.size()>0){
					for(File subFile :subfiles) {
						addClassPath(subFile);
					}
				}else{
					TReflect.invokeMethod(urlClassLoader, "addURL", file.toURI().toURL());
				}
			}else if(file.getPath().toLowerCase().endsWith(".jar")){
				TReflect.invokeMethod(urlClassLoader, "addURL", file.toURI().toURL());
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
	public static void addClassPath(String filePath) throws NoSuchMethodException, IOException {
		File file = new File(filePath);
		addClassPath(file);
	}


	/**
	 * 从当前进程的ClassPath中寻找 Class
	 * @param pattern  确认匹配的正则表达式
	 * @param filters  过滤的 class, 满足这些条件的 class 才会被搜索到(注解,接口,继承的类)
	 * @return  匹配到的 class 集合
	 * @throws IOException IO 异常
	 */
	public static List<Class> searchClassInEnv(String pattern, Class[] filters) throws IOException {
		String userDir = System.getProperty("user.dir");
		List<String> classPaths = getClassPath();
		ArrayList<Class> clazzes = new ArrayList<Class>();
		for(String classPath : classPaths){
			if(classPath.startsWith(userDir)) {
				File classPathFile = new File(classPath);
				if(classPathFile.exists() && classPathFile.isDirectory()){
					clazzes.addAll( getDirectorClass(classPathFile, pattern, filters));
				} else if(classPathFile.exists() && classPathFile.isFile() && classPathFile.getName().endsWith(".jar")) {
					clazzes.addAll( getJarClass(classPathFile, pattern, filters) );
				}
			}
		}

		return clazzes;
	}

	/**
	 * 从指定 File 对象寻找 Class
	 * @param rootfile 文件目录 File 对象
	 * @param pattern  确认匹配的正则表达式
	 * @param filters  过滤的 class, 满足这些条件的 class 才会被搜索到(注解,接口,继承的类)
	 * @return  匹配到的 class 集合
	 * @throws IOException IO 异常
	 */
	public static List<Class> getDirectorClass(File rootfile, String pattern, Class[] filters) throws IOException {
		if(pattern!=null) {
			pattern = pattern.replace(".", File.separator);
		}
		ArrayList<Class> result = new ArrayList<Class>();
		List<File> files = TFile.scanFile(rootfile, pattern);
		for(File file : files){
			String fileName = file.getCanonicalPath();
			if("class".equals(TFile.getFileExtension(fileName))) {
				//如果是内部类则跳过
				if(TString.regexMatch(fileName,"\\$\\d\\.class")>0){
					continue;
				}
				fileName = fileName.replace(rootfile.getCanonicalPath() + "/", "");
				try {
					Class clazz = resourceToClass(fileName);
					if(TReflect.classChecker(clazz, filters)) {
						result.add(clazz);
					}
				} catch (ClassNotFoundException e) {
					Logger.warn("Try to load class["+fileName+"] failed",e);
				}
			}
		}
		return result;
	}

	/**
	 * 从指定jar 文件中寻找 Class
	 * @param jarFile  jar 文件 File 对象
	 * @param pattern  确认匹配的正则表达式
	 * @param filters  过滤的 class, 满足这些条件的 class 才会被搜索到(注解,接口,继承的类)
	 * @return  匹配到的 class
	 * @throws IOException IO 异常
	 */
	public static List<Class> getJarClass(File jarFile, String pattern, Class[] filters) throws IOException {
		if(pattern!=null) {
			pattern = pattern.replace(".", File.separator);
		}
		ArrayList<Class> result = new ArrayList<Class>();
		List<JarEntry> jarEntrys = TFile.scanJar(jarFile, pattern);
		for(JarEntry jarEntry : jarEntrys){
			String fileName = jarEntry.getName();
			if("class".equals(TFile.getFileExtension(fileName))) {
				//如果是内部类则跳过
				if (TString.regexMatch(fileName, "\\$\\d\\.class") > 0) {
					continue;
				}

				try {
					Class clazz = resourceToClass(fileName);
					if(TReflect.classChecker(clazz, filters)) {
						result.add(clazz);
					}
				} catch (Throwable e) {
					fileName = null;
				}
			}
		}
		return result;
	}

	/**
	 * 获取 Class 的修改时间
	 * @param clazz Class 对象
	 * @return 修改时间, 返回: -1 文件不存在 / 文件不是 Class 文件 / IO 异常
	 */
	public static long getClassModifyTime(Class clazz){
		String location = location = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
		String classNamePath = TEnv.classToResource(clazz);
		try {
			if(location.endsWith(".jar")) {
				try(JarFile jarFile = new JarFile(location)) {
					JarEntry jarEntry = jarFile.getJarEntry(classNamePath);
					if(jarEntry!=null) {
						return jarEntry.getTime();
					}else{
						return -1;
					}
				}
			} else if (location.endsWith(File.separator)) {
				File classFile = new File(location+classNamePath);
				if(classFile!=null && classFile.exists()) {
					return classFile.lastModified();
				}else{
					return -1;
				}
			} else {
				return -1;
			}
		}catch (IOException e){
			return -1;
		}
	}

	/**
	 * 获取类JVM 的 Class Path
	 *   因部分 ide 会自动增加全部的 jvm 的 classpath, 所以这里会自动剔除 classpath 中 jvm 的 classPath
	 * @return 获得用户的类加载路径
	 */
	public static List<String> getClassPath(){
		ArrayList<String> userClassPath = new ArrayList<String>();
		String javaHome = System.getProperty("java.home");

		//去除 javahome 的最后一个路径节点,扩大搜索范文
		javaHome = javaHome.replaceAll("\\/[a-zA-z0-9\\_\\$]*$","");

		for(URL url : getURLClassLoader(null).getURLs()){
			String classPath = url.getPath();
			if(!classPath.startsWith(javaHome)){
				userClassPath.add(classPath);
			}
		}
		return userClassPath;
	}

	/**
	 * 将class 转换成 资源资源文件路径
	 * @param clazz Class对象
	 * @return 资源文件路径
	 */
	public static String classToResource(Class clazz){
		String classNamePath = clazz.getName();
		if(clazz.isMemberClass()) {
			classNamePath = TString.fastReplaceAll(classNamePath, "\\$.*$", "");
		}
		return TString.fastReplaceAll(classNamePath, "\\.", File.separator)+".class";
	}

	/**
	 * 将资源文件路径 转换成 Class
	 * @param resourcePath 资源资源文件路径
	 * @return Class对象
	 * @throws ClassNotFoundException 类未找到异常
	 */
	public static Class resourceToClass(String resourcePath) throws ClassNotFoundException {
		String className = null;
		URLClassLoader urlClassLoader = getURLClassLoader(null);

		if(resourcePath.startsWith(File.separator)){
			resourcePath = TString.removePrefix(resourcePath);
		}

		className = TString.fastReplaceAll(resourcePath, "\\$.*\\.class$", ".class");
		className = TString.fastReplaceAll(className, ".class$", "");

		className = TString.fastReplaceAll(className, File.separator, ".");

		try {

			return Class.forName(className);
		}catch (Exception ex) {
			throw new ClassNotFoundException("load and define class "+className+" failed");
		}
	}

	/**
	 * 返回当前 jvm 的 JAVA_HOME 参数
	 * @return
	 */
	public static String getJavaHome(){
		return System.getProperty("sun.boot.library.path").replace("/jre/lib", "");
	}
}
