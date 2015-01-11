package org.hocate.http.server;

import java.util.Map;

import org.hocate.tools.TString;

/**
 * MIME 相关处理
 * @author helyho
 *
 */
public class MimeTools {
	
	private static String mimeTypeRegex = MimeTools.buildMimeMatchRegex(Config.mimeDefine());
	
	public static  String getMimeTypeRegex(){
		return mimeTypeRegex;
	}
	
	/**
	 * 根据 MIME 的配置拼装匹配类型的正则表达式
	 * @param mimeTypes
	 * @return
	 */
	private static String buildMimeMatchRegex(Map<String, Object> mimeTypes){
		String mimeTypeRegex = "";
		for(String fileExtension : mimeTypes.keySet()){
			mimeTypeRegex += "\\."+fileExtension+"|";
		}
		mimeTypeRegex = TString.removeLastChar(mimeTypeRegex)+"$";
		System.out.println("load.........");
		return mimeTypeRegex;
	} 
	
	/**
	 * 根据文件扩展名获取 MIME 类型
	 * @param fileExtension
	 * @return
	 */
	public static String getMimeByFileExtension(String fileExtension){
		return Config.mimeDefine().get(fileExtension.toLowerCase()).toString();
	}
	
	/**
	 * 判断是否是 Mime 类型文件
	 * @param path
	 * @return
	 */
	public static boolean isMimeFile(String path){
		return TString.searchByRegex(path, mimeTypeRegex).length>0?true:false;
	}
}
