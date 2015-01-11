package org.hocate.http.server.router;

import java.io.File;
import java.io.FileNotFoundException;

import org.hocate.http.message.HttpRequest;
import org.hocate.http.message.HttpResponse;
import org.hocate.http.server.MimeTools;
import org.hocate.http.server.RouterBuiz;
import org.hocate.tools.TFile;

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
			String fileExtension = urlPath.substring(urlPath.lastIndexOf(".")+1, urlPath.length());
			response.header().put("Content-Type", MimeTools.getMimeByFileExtension(fileExtension));
			urlPath = urlPath.replaceAll("//", File.separator);
			byte[] fileByte = TFile.loadFileFromSysPath(rootPath+urlPath);
			if(fileByte==null){
				throw new FileNotFoundException(rootPath+urlPath);
			}
			else{
				response.body().writeBytes(fileByte);
			}
		}
	}

}
