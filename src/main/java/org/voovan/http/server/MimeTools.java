package org.voovan.http.server;

import org.voovan.tools.TString;

import java.util.Map;

/**
 * MIME 相关处理
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class MimeTools {
	
	private static String mimeTypeRegex = MimeTools.buildMimeMatchRegex(WebContext.getMimeDefine());
	
	private MimeTools(){
		
	}

	/**
	 * 获取 MIME 正则表达式
	 * @return MIME 正则表达式
     */
	public static  String getMimeTypeRegex(){
		return mimeTypeRegex;
	}
	
	/**
	 * 根据 MIME 的配置拼装匹配类型的正则表达式
	 * @param mimeTypes MIME 类型
	 * @return 匹配MIME类型的正则表达式
	 */
	private static String buildMimeMatchRegex(Map<String, Object> mimeTypes){
		String mimeTypeRegex = "";
		for(String fileExtension : mimeTypes.keySet()){
			mimeTypeRegex += "\\."+fileExtension+"|";
		}
		mimeTypeRegex = TString.removeSuffix(mimeTypeRegex)+"$";
		return mimeTypeRegex;
	} 
	
	/**
	 * 根据文件扩展名获取 MIME 类型
	 * @param fileExtension  文件扩展名
	 * @return MIME 类型
	 */
	public static String getMimeByFileExtension(String fileExtension){
		Object mimeType = WebContext.getMimeDefine().get(fileExtension.toLowerCase());
		return mimeType==null?"text/plain":mimeType.toString();
	}
	
	/**
	 * 判断是否是 Mime 类型文件
	 * @param path  请求路径
	 * @return  是否是Mime 类型文件
	 */
	public static boolean isMimeFile(String path){
		return TString.searchByRegex(path, mimeTypeRegex).length>0?true:false;
	}
}
