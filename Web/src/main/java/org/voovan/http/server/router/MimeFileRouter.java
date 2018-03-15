package org.voovan.http.server.router;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;
import org.voovan.http.server.MimeTools;
import org.voovan.http.server.exception.ResourceNotFound;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TFile;
import org.voovan.tools.THash;
import org.voovan.tools.TString;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;


/**
 * MIME 文件路由处理类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class MimeFileRouter implements HttpRouter {

	private String	rootPath;

	/**
	 * 构造函数
	 * @param rootPath 根路径
	 */
	public MimeFileRouter(String rootPath) {
		this.rootPath = rootPath;
	}

	/**
	 * 获取请求对应的静态文件
	 * @param request request 请求对象
	 * @return 返回文件对象, 文件不一定存在
	 */
	public File getStaticFile(HttpRequest request){
		String urlPath = request.protocol().getPath();

		// 转换请求Path 里的文件路劲分割符为系统默认分割符
		urlPath = TString.fastReplaceAll(urlPath, "//", File.separator);
		// 拼装文件实际存储路径
		String filePath = rootPath + urlPath;
		return new File(filePath);
	}

	@Override
	public void process(HttpRequest request, HttpResponse response) throws Exception {
		String urlPath = request.protocol().getPath();
		File responseFile = getStaticFile(request);

		if (responseFile.exists()) {
			// 获取扩展名
			String fileExtension = urlPath.substring(urlPath.lastIndexOf(".") + 1, urlPath.length());
			response.header().put("Content-Type", MimeTools.getMimeByFileExtension(fileExtension));

			if(isNotModify(responseFile,request,response)){
				return ;
			}else{
				fillMimeFile(responseFile, request, response);
			}
		}else{
			throw new ResourceNotFound(urlPath);
		}
	}

	/**
	 * 判断是否是304 not modify
	 * @param responseFile   响应文件
	 * @param request   HTTP 请求对象
	 * @param response  HTTP 响应对象
	 * @return 是否是304 not modify
	 * @throws ParseException 解析异常
	 */
	public boolean isNotModify(File responseFile,HttpRequest request,HttpResponse response) throws ParseException{
		//文件的修改日期
		Date fileModifyDate = new Date(responseFile.lastModified());

		//请求中的修改时间
		Date requestModifyDate = null;
		if(request.header().contain("If-Modified-Since")){
			try {
				requestModifyDate = TDateTime.parseToGMT(request.header().get("If-Modified-Since"));
			}catch(Exception e){
				requestModifyDate = null;
			}
		}

		//文件的 ETag
		String eTag = TString.assembly("\"", THash.encryptMD5(Integer.toString(responseFile.hashCode()+fileModifyDate.hashCode())).toUpperCase(), "\"");

		//请求中的 ETag
		String requestETag = request.header().get("If-None-Match");

		//设置响应头 ETag
		response.header().put("ETag", eTag);
		//设置最后修改时间
		response.header().put("Last-Modified",TDateTime.formatToGMT(fileModifyDate));
		//设置缓存控制
		response.header().put("Cache-Control", "max-age=86400");
		//设置浏览器缓存超时控制
		response.header().put("Expires",TDateTime.formatToGMT(new Date(System.currentTimeMillis()+86400*1000)));

		//文件 hashcode 无变化,则返回304
		if(eTag.equals(requestETag)){
			setNotModifyResponse(response);
			return true;
		}
		//文件更新时间比请求时间大,则返回304
		if(fileModifyDate.equals(requestModifyDate)){
			setNotModifyResponse(response);
			return true;
		}
		return false;
	}

	/**
	 * 填充 mime 文件到 response
	 * @param responseFile   响应文件
	 * @param request   HTTP 请求对象
	 * @param response  HTTP 响应对象
	 * @throws IOException IO操作异常
	 */
	public void fillMimeFile(File responseFile,HttpRequest request,HttpResponse response) throws IOException {
		byte[] fileByte = null;
		long fileSize = TFile.getFileSize(responseFile);

		// 如果包含取一个范围内的文件内容进行处理,形似:Range: 0-800
		if (request.header().get("Range") != null && request.header().get("Range").contains("-")) {

			long beginPos=-1;
			long endPos=-1;

			String rangeStr = request.header().get("Range");
			rangeStr = rangeStr.replace("bytes=", "").trim();
			String[] ranges = rangeStr.split("-");

			//形似:Range: -800
			if(rangeStr.startsWith("-") && ranges.length==1){
				beginPos = fileSize - Long.parseLong(ranges[0]);
				endPos = fileSize;
			}
			//形似:Range: 800-
			else if(rangeStr.endsWith("-") && ranges.length==1){
				beginPos = Integer.parseInt(ranges[0]);
				endPos = fileSize;
			}
			//形似:Range: 0-800
			else if(ranges.length==2){
				beginPos = Long.parseLong(ranges[0]);
				endPos   = Long.parseLong(ranges[1]);
			}
			fileByte = TFile.loadFileFromSysPath(responseFile.getPath(), beginPos, endPos);
			response.header().put("Content-Range", TString.assembly("bytes ", rangeStr, "/", fileSize));
			response.body().write(fileByte);

		} else {
			response.body().changeToFile(responseFile.getCanonicalPath());
		}
	}

	/**
	 * 将响应报文设置称304
	 * @param response HTTP 响应对象
	 */
	public void setNotModifyResponse(HttpResponse response){
		response.protocol().setStatus(304);
		response.protocol().setStatusCode("Not Modified");
	}

}
