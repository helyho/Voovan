package org.voovan.http.message.packet;

import org.voovan.tools.TFile;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

/**
 * HTTP 的 part 对象
 * 	改对象仅在 POST 请求,并且Content-Type = multipart/form-data时使用
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Part {
	private Header header;
	private Body body;

	/**
	 * Part 类型枚举
	 * @author helyho
	 *
	 */
	public enum PartType{
		BINARY,TEXT
	} 
	
	/**
	 * 构造函数
	 */
	public Part(){
		header = new  Header();
		body = new Body();
	}
	
	/**
	 * 构造函数
	 * @param name 参数名
	 * @param value 参数值
	 * @param charset 字符集
	 */
	public Part(String name,String value, String charset){
		header = new  Header();
		body = new Body();
		header.put("name", name);
		body.write(value,charset);
	}
	
	/**
	 * 构造函数
	 * @param name 参数名
	 * @param value 参数值
	 */
	public Part(String name,String value){
		header = new  Header();
		body = new Body();
		header.put("name", name);
		try {
			body.write(value.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Logger.error("This charset is unsupported.",e);
		}
	}
	
	/**
	 * Part 的 Header 对象
	 * @return HTTP-Header 对象
	 */
	public Header header(){
		return header;
	}
	
	/**
	 * Part 的 body 对象
	 * @return Body 对象
	 */
	public Body body(){
		return body;
	}
	
	/**
	 * 获取 Part 的名称
	 * @return Part 的名称
	 */
	public String getName() {
		return  header.get("name");
	}

	/**
	 * 获取 part 的文件名称
	 * @return part 的文件名称
	 */
	public String getFileName() {
		return header.get("filename");
	}

	/**
	 * 获取 Part 内容的类型
	 * @return Part 内容的类型
	 */
	public PartType getType(){
		if("binary".equals(header.get("Content-Transfer-Encoding"))){
			return PartType.BINARY;
		}else{
			return PartType.TEXT;
		}
	}

	/**
	 * 将 Part 的内容保存为文件
	 * @param file art 的内容保存为文件
	 * @throws IOException IO 异常
	 */
	public void saveAsFile(File file) throws IOException {
		if(getType() == PartType.TEXT){
			TFile.writeFile(file, body.getBodyBytes());
		}else{
			TFile.moveFile(new File(body.toString()), file);
		}
	}
	
	/**
	 * 将 Part 的内容保存为文件
	 * @param fileName    Part 的内容保存为文件
	 * @throws IOException IO 异常
	 */
	public void saveAsFile(String fileName) throws IOException{
		if(getType() == PartType.TEXT){
			TFile.writeFile(new File(fileName), body.getBodyBytes());
		}else{
			TFile.moveFile(new File(body.toString()), new File(fileName));
		}

	}

	/**
	 * 清理
	 */
	public void clear(){
		this.header.clear();
		this.body().clear();
	}
	
	@Override
	public String toString(){
		header.put("Content-Disposition", "form-data");
		if(header.get("name")!=null){
			header.put("Content-Disposition", header.get("Content-Disposition")+"; name=\""+header.get("name")+"\"");
		}
		if(header.get("filename")!=null){
			header.put("Content-Disposition", header.get("Content-Disposition")+"; filename=\""+header.get("filename")+"\"");
		}
		return header.toString()+"\r\n";
	}
}
