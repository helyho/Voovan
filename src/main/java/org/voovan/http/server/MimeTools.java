package org.voovan.http.server;

import java.util.Map;

import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

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
		mimeTypeRegex = TString.removeSuffix(mimeTypeRegex)+"$";
		return mimeTypeRegex;
	} 
	
	/**
	 * 根据文件扩展名获取 MIME 类型
	 * @param fileExtension
	 * @return
	 */
	public static String getMimeByFileExtension(String fileExtension){
		Object mimeType = WebContext.getMimeDefine().get(fileExtension.toLowerCase());
		return mimeType==null?"text/plain":mimeType.toString();
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
