package org.voovan.http.server;

import org.voovan.http.server.context.WebContext;
import org.voovan.tools.TObject;
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

//	private static String mimeTypeRegex = MimeTools.buildMimeMatchRegex(WebContext.getMimeDefine());

	private MimeTools(){

	}

//	/**
//	 * 获取 MIME 正则表达式
//	 * @return MIME 正则表达式
//     */
//	public static String getMimeTypeRegex(){
//		return mimeTypeRegex;
//	}
//
//	/**
//	 * 根据 MIME 的配置拼装匹配类型的正则表达式
//	 * @param mimeTypes MIME 类型
//	 * @return 匹配MIME类型的正则表达式
//	 */
//	private static String buildMimeMatchRegex(Map<String, Object> mimeTypes){
//		String mimeTypeRegex = "";
//		StringBuilder mimeTypeRegexSB = new StringBuilder();
//		for(String fileExtension : mimeTypes.keySet()){
//			mimeTypeRegexSB.append("\\.");
//			mimeTypeRegexSB.append(fileExtension);
//			mimeTypeRegexSB.append("$|");
//		}
//
//		mimeTypeRegex = mimeTypeRegexSB.toString();
//		mimeTypeRegex = TString.removeSuffix(mimeTypeRegex)+"$";
//		return mimeTypeRegex;
//	}

	/**
	 * 根据文件扩展名获取 MIME 类型
	 * @param fileExtension  文件扩展名
	 * @return MIME 类型
	 */
	public static String getMimeByFileExtension(String fileExtension){
		Object mimeTypeObj = WebContext.getMimeDefine().get(fileExtension.toLowerCase());
		return TObject.nullDefault(mimeTypeObj,"application/octet-stream").toString();
	}

//	/**
//	 * 判断是否是 Mime 类型文件
//	 * @param path  请求路径
//	 * @return  是否是Mime 类型文件
//	 */
//	public static boolean isMimeFile(String path){
//		return TString.regexMatch(path, mimeTypeRegex)>0?true:false;
//	}
}
