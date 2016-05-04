package org.voovan.http.message.packet;

import org.voovan.tools.TZip;
import org.voovan.tools.log.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * HTTP的内容对象
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
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
	 * 获取长度
	 * @return 长度
     */
	public int size(){
		return outputStream.size();
	}

	/**
	 * 获取内容字节数组
	 * @return body 字节数组
	 */
	public byte[] getBodyBytes(){
		return outputStream.toByteArray();
	}
	
	/**
	 * 获取 body 字符串
	 * @return body 字符串
	 */
	public String getBodyString(){
		byte[] bodyBytes = getBodyBytes();
		return bodyBytes!=null?new String(bodyBytes):null;
	}
	
	
	/**
	 * 获取 body 字符串
	 * @param charset 字符集
	 * @return body 字符串
	 */
	public String getBodyString(String charset){
		byte[] bodyBytes = getBodyBytes();
		try {
			return bodyBytes!=null?new String(bodyBytes,charset):null;
		} catch (UnsupportedEncodingException e) {
			Logger.error("this charset is unsupported.",e);
			return new String(bodyBytes);
		}
	}
	
	/**
	 * 写入 body 
	 * @param body 字节数组
	 */
	public void write(byte[] body){
		try {
			outputStream.write(body);
		} catch (IOException e) {
			Logger.error("Wirte byte array faild by OutputStream",e);
		}
	}
	
	/**
	 * 写入 body 
	 * @param body 字节数组
	 * @param offset  字节数组偏移量
	 * @param length  写入长度
	 */
	public void write(byte[] body,int offset,int length){
		outputStream.write(body,offset,length);
	}
	
	/**
	 * 写入 body 字符串,默认 UTF-8
	 * @param content body 字符串
	 */
	public void write(String content){
		write(content,"UTF-8");
	}
	
	/**
	 * 使用特定的字符集写入 body 字符串
	 * @param content body 字符串
	 * @param charset 字符集
	 */
	public void write(String content,String charset){
		try{
			//使用字符集编码
			outputStream.write(content.getBytes(charset));
		} catch (IOException e) {
			Logger.error("Wirte string faild by OutputStream",e);
		}
	}
	
	/**
	 * 获取使用 GZIP 压缩后的 body 字节
	 * @return 压缩后的字节数组
	 * @throws IOException IO 异常
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
