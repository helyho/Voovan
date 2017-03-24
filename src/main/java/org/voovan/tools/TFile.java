package org.voovan.tools;

import org.voovan.tools.log.Logger;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 文件操作工具类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TFile {

	/**
	 * 判断文件是否存在
	 * @param fullPath 文件完整路径
	 * @return 文件是否存在
     */
	public static boolean fileExists(String fullPath){
		return new File(fullPath).exists();
	}
	
	/**
	 * 路径拼装
	 * @param pathParts 每个由路劲分割符分割的路径字符串
	 * @return 拼装后的路径
	 */
	public static String assemblyPath(String ...pathParts ){
		StringBuilder result = new StringBuilder();
		for(String pathPart : pathParts){
			result.append(pathPart);
			result.append(File.separator);
		}
		
		return TString.removeSuffix(result.toString());
	}
	
	/**
	 * 获取文件大小
	 * @param filePath 文件路径
	 * @throws IOException IO操作异常
	 * @return 文件大小
	 */
	public static long getFileSize(File file) throws IOException {
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "w");
		try {
			long fileSize = randomAccessFile.length();
			return fileSize;
		} catch (Exception e) {
			Logger.error("File not found: "+file.getCanonicalPath()+".",e);
			return -1;
		}finally {
			randomAccessFile.close();
		}
		
	}
	
	/**
	 * 从系统路径读取文件内容
	 * 
	 * @param filePath 文件路径
	 * @return 文件内容
	 */
	public static byte[] loadFileFromSysPath(String filePath) {
		byte[] fileContent = null;
		fileContent = loadFile(new File(filePath));
		return fileContent;
	}

	/**
	 * 从系统路径读取文件内容
	 * 
	 * @param filePath 文件路径
	 * @param beginPos 起始位置
	 * @param endPos   结束位置
	 * @return 文件内容
	 */
	public static byte[] loadFileFromSysPath(String filePath, long beginPos, long endPos) {
		byte[] fileContent = null;
		fileContent = loadFile(new File(filePath), beginPos, endPos);
		return fileContent;
	}

	/**
	 * 从应用的工作根目录为根的相对路径读取文件内容
	 * 
	 * @param filePath 文件路径
	 * @param beginPos
	 *            起始位置
	 * @param endPos
	 *            结束位置,如果值小于0则读取全部,如果大于文件的大小,则自动调整为文件的大小
	 * @return 文件内容
	 */
	public static byte[] loadFileFromContextPath(String filePath, long beginPos, long endPos) {
		String spliter = filePath.startsWith(File.separator) == true ? "" : File.separator;
		String fullFilePath = TEnv.getContextPath() + spliter + filePath;
		return loadFileFromSysPath(fullFilePath, beginPos, endPos);
	}

	/**
	 * 获取应用的工作根目录为根的相对路径
	 * 
	 * @param filePath 文件路径
	 * @return 文件内容
	 */
	public static byte[] loadFileFromContextPath(String filePath) {
		String spliter = filePath.startsWith(File.separator) == true ? "" : File.separator;
		String fullFilePath = TEnv.getContextPath() + spliter + filePath;
		return loadFileFromSysPath(fullFilePath);
	}

	/**
	 * 读取在Context的资源文件 完整路径
	 *
	 * @param resourcePath
	 *            路径起始不带"/"
	 * @return File 对象
	 */
	public static File getResourceFile(String resourcePath) {
		try {
			resourcePath = URLDecoder.decode(resourcePath,"utf-8");
			URL url = TEnv.class.getClassLoader().getResource(resourcePath);
			if(url!=null){
				File file = new File(url.getFile());
				return file;
			}
			return null;
		} catch (UnsupportedEncodingException e) {
			Logger.error("Load resource URLDecoder.decode failed",e);
			return null;
		}

	}

	/**
	 * 读取在Context的资源文件 (完整路径)
	 *
	 * @param resourcePath
	 *            路径起始不带"/"
	 * @return 文件内容
	 */
	public static byte[] loadResource(String resourcePath) {
		try {
			resourcePath = URLDecoder.decode(resourcePath,"utf-8");
			InputStream inputStream = TEnv.class.getClassLoader().getResourceAsStream(resourcePath);
			return TStream.readAll(inputStream);
		} catch (IOException e) {
			Logger.error("Load resource URLDecoder.decode failed",e);
			return null;
		}
	}

	/**
	 * 读取 File 对象所代表的文件的内容
	 * 
	 * @param file 文件对象
	 * @return 文件内容
	 */
	public static byte[] loadFile(File file) {
		return loadFile(file, 0, -1);
	}

	/**
	 * 读取 File 对象所代表的文件的内容
	 * 
	 * @param file
	 *            文件对象
	 * @param beginPos
	 *            起始位置
	 * @param endPos
	 *            结束位置,如果值小于0则读取全部,如果大于文件的大小,则自动调整为文件的大小
	 * @return 文件内容
	 */
	public static byte[] loadFile(File file, long beginPos, long endPos) {

		try {
			if(!file.exists()){
				return null;
			}

			long fileSize = file.length();

			if (beginPos < 0) {
				return null;
			}

			if (endPos > fileSize) {
				endPos = (int) fileSize;
			}
			
			// 计算需要读取的差高难度
			long loadLength = 0;
			if (endPos < 0) {
				loadLength = (int) fileSize - beginPos + 1;
			} else {
				loadLength = endPos - beginPos + 1;
			}
			RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
			byte[] fileBytes = new byte[(int) loadLength - 1];
			randomAccessFile.seek(beginPos);
			randomAccessFile.read(fileBytes);
			randomAccessFile.close();
			return fileBytes;
		} catch (IOException e) {
			Logger.error("Load file error: "+file.getAbsolutePath()+".",e);
		}
		return null;
	}

	/**
	 * 读取文件最后几行记录
	 * @param file  文件对象
	 * @param lastLineNum 最后几行的行数
     * @return 文件内容
	 * @throws IOException IO 异常
     */
	public static byte[] loadFileLastLines(File file, int lastLineNum) throws IOException {

		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(file, "r");
			long fileLength = randomAccessFile.length() - 1 ;
			randomAccessFile.seek(fileLength);
			int rowCount = 0;
			while (randomAccessFile.getFilePointer() != 0) {
				randomAccessFile.seek(fileLength);
                byte readByte = randomAccessFile.readByte();
				if (readByte == '\n') {
					rowCount++;
				}

				if (fileLength==0 || lastLineNum == rowCount) {
					if(fileLength==0){
						randomAccessFile.seek(0);
					}

					int byteCount = (int) (randomAccessFile.length() - fileLength);
					byte[] byteContent = new byte[byteCount];
					int readSize = randomAccessFile.read(byteContent);
					if (readSize > 0) {
						return byteContent;
					} else {
						return new byte[0];
					}
				}
				--fileLength;
			}
		} catch(IOException e){
			throw e;

		} finally {
			if(randomAccessFile!=null){
				randomAccessFile.close();
			}
		}

		return new byte[0];
	}

	/**
	 * 向文件写入内容
	 * @param file	文件对象
	 * @param append    是否以追加形式写入
	 * @param contents	文件内容
	 * @param offset	偏移值(起始位置)
	 * @param length	写入长度
	 * @throws IOException IO操作异常
	 * @return 成功返回 true,失败返回 false
	 */
	public static boolean writeFile(File file, boolean append, byte[] contents, int offset, int length) throws IOException {
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rwd");
		try {

			if(append){
				randomAccessFile.seek(randomAccessFile.length());
			}

			randomAccessFile.write(contents, offset, length);
			return true;
		} catch (IOException e) {
			Logger.error("TFile.writeFile Error!", e);
			return false;
		}finally {
			randomAccessFile.close();
		}
	}
	
	/**
	 * 向文件写入内容
	 * @param file	文件对象
	 * @param append    是否以追加形式写入
	 * @param contents	文件内容
	 * @throws IOException IO操作异常
	 * @return 成功返回 true,失败返回 false
	 */
	public static boolean writeFile(File file, boolean append, byte[] contents) throws IOException {
		return writeFile(file, append, contents, 0, contents.length);
	}

	/**
	 * 以追加的形式,向文件写入内容
	 * @param file	文件路径
	 * @param contents	文件内容
	 * @param offset	偏移值(起始位置)
	 * @param length	写入长度
	 * @throws IOException IO操作异常
	 * @return 成功返回 true,失败返回 false
	 */
	public static boolean writeFile(File file, byte[] contents, int offset, int length) throws IOException {
		return writeFile(file, true, contents, 0, contents.length);
	}

	/**
	 * 以追加的形式,向文件写入内容
	 * @param file	文件路径
	 * @param contents	文件内容
	 * @throws IOException IO操作异常
	 * @return 成功返回 true,失败返回 false
	 */
	public static boolean writeFile(File file, byte[] contents) throws IOException {
		return writeFile(file, true, contents, 0, contents.length);
	}

	/**
	 * 从当前进程的ClassPath中寻找 Class
	 * @param pattern  确认匹配的正则表达式
	 * @return  匹配到的 class 集合
	 * @throws IOException IO 异常
	 */
	public static List<Class> searchClassInEnv(String pattern) throws IOException {
		String userDir = System.getProperty("user.dir");
		String[] classPaths = System.getProperty("java.class.path").split(File.pathSeparator);
		ArrayList<Class> clazzes = new ArrayList<Class>();
		for(String classPath : classPaths){
			if(classPath.startsWith(userDir)) {
				File classPathFile = new File(classPath);
				if(classPathFile.exists() && classPathFile.isDirectory()){
					clazzes.addAll(getDirectorClass(classPathFile,pattern));
				} else if(classPathFile.exists() && classPathFile.isFile() && classPathFile.getName().endsWith(".jar")) {
					clazzes.addAll( getJarClass(classPathFile,"org.voovan.tools.*"));
				}
			}
		}

		return clazzes;
	}

	/**
	 * 从指定 File 对象寻找 Class
	 * @param rootfile 文件目录 File 对象
	 * @param pattern  确认匹配的正则表达式
	 * @return  匹配到的 class 集合
	 * @throws IOException IO 异常
	 */
	public static List<Class> getDirectorClass(File rootfile, String pattern) throws IOException {
		pattern = TObject.nullDefault(pattern,".*");
		pattern = pattern.replace("\\S\\.\\S","/");
		ArrayList<Class> result = new ArrayList<Class>();
		List<File> files = scanFile(rootfile,pattern);
		for(File file : files){
			String fileName = file.getCanonicalPath();
			if(fileName.endsWith("class")) {
				if(TString.regexMatch(fileName,"\\$\\d\\.class")>0){
					continue;
				}
				fileName = fileName.replace(rootfile.getCanonicalPath() + "/", "").replaceAll("/", "\\.").replaceAll("\\.class$","");
				try {
					result.add(Class.forName(fileName));
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
	 * @return  匹配到的 class
	 * @throws IOException IO 异常
	 */
	public static List<Class> getJarClass(File jarFile, String pattern) throws IOException {
		pattern = TObject.nullDefault(pattern,".*");
		pattern = pattern.replace("\\S\\.\\S","/");
		ArrayList<Class> result = new ArrayList<Class>();
		List<JarEntry> jarEntrys = scanJar(jarFile,pattern);
		for(JarEntry jarEntry : jarEntrys){
			String fileName = jarEntry.getName();
			if(fileName.endsWith("class")) {
				if (TString.regexMatch(fileName, "\\$\\d\\.class") > 0) {
					continue;
				}
				fileName = fileName.replaceAll("/", "\\.").replaceAll("\\.class$", "");
				try {
					result.add(Class.forName(fileName));
				} catch (Throwable e) {
					fileName = null;
				}
			}
		}
		return result;
	}

	/**
	 * 遍历指定文件对象
	 * @param file    特定的文件或文件目录
	 * @param pattern  确认匹配的正则表达式
	 * @return 匹配到的文件对象集合
	 * @throws IOException IO 异常
	 */
	public static List<File> scanFile(File file, String pattern) throws IOException {
		pattern = TObject.nullDefault(pattern,".*");
		ArrayList<File> result = new ArrayList<File>();
		if(file.isDirectory()){
			for(File subFile : file.listFiles()){
				result.addAll(scanFile(subFile,pattern));
			}
		} else if(TString.regexMatch(file.getCanonicalPath(),pattern) > 0) {
			result.add(file);
		}
		return result;
	}


	/**
	 * 遍历指定jar文件对象
	 * @param file    jar文件对象
	 * @param pattern  确认匹配的正则表达式
	 * @return 匹配到的文件对象集合
	 * @throws IOException IO 异常
	 */
	public static List<JarEntry> scanJar(File file, String pattern) throws IOException {
		pattern = TObject.nullDefault(pattern,".*");

		ArrayList<JarEntry> result = new ArrayList<JarEntry>();
		JarFile jarFile = new JarFile(file);
		Enumeration<JarEntry > jarEntrys = jarFile.entries();
		while(jarEntrys.hasMoreElements()){
			JarEntry jarEntry = jarEntrys.nextElement();
			String fileName = jarEntry.getName();
			if(TString.regexMatch(fileName,pattern) > 0) {
				result.add(jarEntry);
			}
		}
		jarFile.close();
		return result;
	}

}
