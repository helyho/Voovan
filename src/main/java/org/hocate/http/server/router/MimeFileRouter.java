package org.hocate.http.server.router;

import java.io.File;
import java.io.FileNotFoundException;

import org.hocate.http.message.HttpRequest;
import org.hocate.http.message.HttpResponse;
import org.hocate.http.server.MimeTools;
import org.hocate.http.server.RouterBuiz;
import org.hocate.tools.TFile;
import org.hocate.tools.TString;

/**
 * MIME 文件路由处理类
 * @author helyho
 *
 */
public class MimeFileRouter implements RouterBuiz{

	private String rootPath;
	
	public MimeFileRouter(String rootPath){
		this.rootPath = rootPath;	
	}
	
	@Override
	public void Process(HttpRequest request, HttpResponse response) throws Exception {
		String urlPath = request.protocol().getPath();
		if(MimeTools.isMimeFile(urlPath)){
			//获取扩展名
			String fileExtension = urlPath.substring(urlPath.lastIndexOf(".")+1, urlPath.length());
			//根据扩展名,设置 MIME 类型
			response.header().put("Content-Type", MimeTools.getMimeByFileExtension(fileExtension));
			//转换请求Path 里的文件路劲分割符为系统默认分割符
			urlPath = urlPath.replaceAll("//", File.separator);
			//拼装文件路径
			String filePath = rootPath+urlPath;
			
			byte[] fileByte = null;
			//如果包含取一个范围内的文件内容进行处理,形似:Range: 0-800
			if(request.header().get("Range")!=null && request.header().get("Range").contains("-")){
				
				String rangeStr = request.header().get("Range");
				String[] ranges= rangeStr.split("-");
				if(ranges[1].equals("")){
					ranges[1]="-1";
				}
				//是否是整形数字进行判断,如果不是整形数字,则全部使用-1
				int beginPos = TString.isInteger(ranges[0]) ? Integer.parseInt(ranges[0]) : -1;
				int endPos 	 = TString.isInteger(ranges[1]) ? Integer.parseInt(ranges[1]) : -1;
				fileByte = TFile.loadFileFromSysPath(filePath,beginPos,endPos);
				response.header().put("Content-Range","bytes "+rangeStr+"/"+fileByte.length);
			}else{
				fileByte = TFile.loadFileFromSysPath(filePath);
			}
			if(fileByte==null){
				throw new FileNotFoundException(filePath);
			}
			else{
				response.header().put("Content-Length",""+fileByte.length);
				response.body().writeBytes(fileByte);
			}
		}
	}

}
