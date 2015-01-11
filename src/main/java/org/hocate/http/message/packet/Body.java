package org.hocate.http.message.packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.hocate.tools.TZip;

/**
 * HTTP的内容对象
 * @author helyho
 *
 */
public class Body {
	private ByteArrayOutputStream outputStream;
	
	/**
	 * 构造函数
	 */
	public Body(){
		outputStream = new ByteArrayOutputStream();
	}
	
	/**
	 * 获取内容字节数组
	 */
	public byte[] getBodyBytes(){
		return outputStream.toByteArray();
	}
	
	/**
	 * 填写 body 体
	 * @param body
	 * @throws IOException 
	 */
	public void writeBytes(byte[] body){
		try {
			outputStream.write(body);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 获取 body 字符串
	 * @return
	 */
	public String getBodyString(){
		byte[] bodyBytes = getBodyBytes();
		return bodyBytes!=null?new String(bodyBytes):null;
	}
	
	/**
	 * 写入 body 字符串
	 * @param content
	 * @param charset
	 */
	public void writeString(String content){
		try{
			outputStream.write(content.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取使用 GZIP 压缩后的 body 字节
	 * @return
	 * @throws IOException
	 */
	public byte[] getGZipedBody() throws IOException{
		return TZip.encodeGZip(outputStream.toByteArray());
	}
	
	/**
	 * 清空缓冲
	 */
	public void clear(){
		outputStream.reset();
	}
	
	@Override
	public String toString(){
		byte[] bodyBytes = getBodyBytes();
		return new String(bodyBytes);
	}
}
