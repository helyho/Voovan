package org.hocate.tools;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;

/**
 * 文件操作工具类
 * @author helyho
 *
 */
public class TFile {
	/**
	 * 从系统路径读取文件内容
	 * 
	 * @param FilePath
	 * @return
	 * @throws IOException
	 */
	public static byte[] loadFileFromSysPath(String filePath) {
		byte[] fileContent = null;
		try{
			fileContent = loadFile(new File(filePath));
		}
		catch(Exception e){
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
	public static byte[] loadFileFromContextPath(String filePath){
		String spliter = filePath.startsWith(File.separator)==true?"":File.separator ;
		String fullFilePath = TEnv.getAppContextPath() + spliter + filePath;
		return loadFileFromSysPath(fullFilePath);
	}

	/**
	 * 读取在Context的资源文件
	 * 		完整路径
	 * @param resourcePath  路径起始不带"/"
	 * @return
	 * @throws IOException
	 */
	public static byte[] loadResource(String resourcePath) {
		
		byte[] fileContent = null;
		try{
			URL url = TEnv.class.getClassLoader().getResource(resourcePath);
			File file = new File(url.getFile());
			fileContent = loadFile(file);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return fileContent;
	}
	
	/**
	 * 读取 File 对象所代表的文件的内容
	 * @param file
	 * @return
	 */
	public  static byte[] loadFile(File file){
		try{
			RandomAccessFile randomAccessFile = new RandomAccessFile(file,"r");
			byte[] fileBytes = new byte[(int) file.length()];
			randomAccessFile.read(fileBytes);
			randomAccessFile.close();
			return fileBytes;
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
