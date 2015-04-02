package org.hocate.tools;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;

/**
 * 文件操作工具类
 * 
 * @author helyho
 *
 */
public class TFile {
	
	/**
	 * 路劲拼装
	 * @param pathParts 每个由路劲分割符分割的路劲字符串
	 * @return
	 */
	public static String assemblyPath(String ...pathParts ){
		String result = "";
		for(String pathPart : pathParts){
			result = result+pathPart+File.separator;
		}
		
		return TString.removeLastChar(result);
	}
	
	/**
	 * 从系统路径读取文件内容
	 * 
	 * @param FilePath
	 * @return
	 * @throws IOException
	 */
	public static byte[] loadFileFromSysPath(String filePath) {
		byte[] fileContent = null;
		try {
			fileContent = loadFile(new File(filePath));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fileContent;
	}

	/**
	 * 从系统路径读取文件内容
	 * 
	 * @param FilePath
	 * @return
	 * @throws IOException
	 */
	public static byte[] loadFileFromSysPath(String filePath, int beginPos, int endPos) {
		byte[] fileContent = null;
		try {
			fileContent = loadFile(new File(filePath), beginPos, endPos);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fileContent;
	}

	/**
	 * 从应用的工作根目录为根的相对路径读取文件内容
	 * 
	 * @param FilePath
	 * @return
	 * @throws IOException
	 */
	public static byte[] loadFileFromContextPath(String filePath, int beginPos, int endPos) {
		String spliter = filePath.startsWith(File.separator) == true ? "" : File.separator;
		String fullFilePath = TEnv.getAppContextPath() + spliter + filePath;
		return loadFileFromSysPath(fullFilePath, beginPos, endPos);
	}

	/**
	 * 获取应用的工作根目录为根的相对路径
	 * 
	 * @param FilePath
	 * @return
	 * @throws IOException
	 */
	public static byte[] loadFileFromContextPath(String filePath) {
		String spliter = filePath.startsWith(File.separator) == true ? "" : File.separator;
		String fullFilePath = TEnv.getAppContextPath() + spliter + filePath;
		return loadFileFromSysPath(fullFilePath);
	}

	/**
	 * 读取在Context的资源文件 完整路径
	 * 
	 * @param resourcePath
	 *            路径起始不带"/"
	 * @return
	 * @throws IOException
	 */
	public static File getResourceFile(String resourcePath) {
		try {
			URL url = TEnv.class.getClassLoader().getResource(resourcePath);
			File file = new File(url.getFile());
			return file;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 读取在Context的资源文件 完整路径
	 * 
	 * @param resourcePath
	 *            路径起始不带"/"
	 * @return
	 * @throws IOException
	 */
	public static byte[] loadResource(String resourcePath) {

		byte[] fileContent = null;
		try {
			fileContent = loadFile(getResourceFile(resourcePath));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fileContent;
	}

	/**
	 * 读取 File 对象所代表的文件的内容
	 * 
	 * @param file
	 * @return
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
	 * @return
	 */
	public static byte[] loadFile(File file, int beginPos, int endPos) {

		try {
			long fileSize = file.length();

			if (beginPos < 0) {
				return null;
			}

			if (endPos > fileSize) {
				endPos = (int) fileSize;
			}
			// 计算需要读取的差高难度
			int loadLength = 0;
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
