package org.voovan.http.message.packet;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TFile;
import org.voovan.tools.log.Logger;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * HTTP的内容对象
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Body {
	private ByteBufferChannel byteBufferChannel;
	private BodyType type;
	private File bodyFile;

	/**
	 * Body 类型枚举
	 * @author helyho
	 *
	 */
	public enum BodyType{
		BYTES,FILE
	}

	/**
	 * 构造函数
	 */
	public Body(){
		type = BodyType.BYTES;
		try{
            chaneToBytes("".getBytes());
        } catch (IOException e){
            Logger.error("Construct class Body error. ",e);
        }
	}


	/**
	 * 构造函数
	 * @param content 字节内容
	 */
	public Body(byte[] content){
		type = BodyType.BYTES;
		try {
			chaneToBytes(content);
		} catch (IOException e){
			Logger.error("Construct class Body error. ",e);
		}
	}

	/**
	 * 构造函数
	 * @param filePath 文件路径
	 */
	public Body(String filePath){
		type = BodyType.FILE;
		try {
			changeToFile(filePath);
		} catch (IOException e){
			Logger.error("Construct class Body error. ",e);
		}
	}

	/**
	 * 获取类型
	 * @return BodyType枚举
	 */
	public BodyType getType() {
		return type;
	}

	/**
	 * 转换成文件形式
	 * @param filePath  文件路径
	 * @throws FileNotFoundException 文件未找到异常
	 */
	public void changeToFile(String filePath) throws FileNotFoundException{

        bodyFile = new File(filePath);
        if(!bodyFile.exists()){
            throw new FileNotFoundException(" bodyFile " + filePath + " not exists");
        }

		if(byteBufferChannel != null){
			byteBufferChannel = null;
		}

		this.type = BodyType.FILE;
	}

	/**
	 * 转换成字节形式
	 * @param content 字节内容
	 * @throws IOException 文件未找到异常
	 */
	public void chaneToBytes(byte[] content) throws IOException {
		if(byteBufferChannel == null){
			byteBufferChannel = new ByteBufferChannel();
		}

		if(bodyFile != null){
			bodyFile = null;
		}

		byteBufferChannel.writeEnd(ByteBuffer.wrap(content));
		type = BodyType.BYTES;
	}

	/**
	 * 获取长度
	 * @return 长度 小于0,则读取失败.
     */
	public long size(){
		if(type == BodyType.FILE){
			try {
				return TFile.getFileSize(bodyFile);
			}catch(IOException e){
				Logger.error(e);
				return -1;
			}
		}else {
			return byteBufferChannel.size();
		}
	}

	/**
	 * 获取内容字节数组
	 * @return body 字节数组
	 */
	public byte[] getBodyBytes(){
		if(type == BodyType.FILE){
			return TFile.loadFile(bodyFile);
		}else {
			return byteBufferChannel.array();
		}
	}
	
	/**
	 * 获取 body 字符串
	 * @return body 字符串
	 */
	public String getBodyString(){
		byte[] bodyBytes = getBodyBytes();
		try {
			return new String(bodyBytes,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			Logger.error("This charset is unsupported.",e);
			return null;
		}
	}
	
	
	/**
	 * 获取 body 字符串
	 * @param charset 字符集
	 * @return body 字符串
	 */
	public String getBodyString(String charset){
		byte[] bodyBytes = getBodyBytes();
		try {
			return new String(bodyBytes,charset);
		} catch (UnsupportedEncodingException e) {
			Logger.error("This charset is unsupported.",e);
			return null;
		}
	}
	
	/**
	 * 写入 body 
	 * @param body 字节数组
	 * @param offset  字节数组偏移量
	 * @param length  写入长度
	 */
	public void write(byte[] body,int offset,int length){
		try {
            if(type == BodyType.BYTES) {
				ByteBuffer bodyTmp = ByteBuffer.wrap(body);
				bodyTmp.position(offset);
				bodyTmp.limit(length);
				byteBufferChannel.writeEnd(bodyTmp);
            }else{
            	TFile.writeFile(bodyFile,true, body, offset, length);
            }
		} catch (IOException e) {
			Logger.error("Wirte byte array faild by OutputStream",e);
		}
	}

	/**
	 * 写入 body
	 * @param body 字节数组
	 */
	public void write(byte[] body){
		write(body, 0, body.length);
	}

	/**
	 * 使用特定的字符集写入 body 字符串
	 * @param content body 字符串
	 * @param charset 字符集
	 */
	public void write(String content,String charset){
		try{
			//使用字符集编码
			write(content.getBytes(charset));
		} catch (IOException e) {
			Logger.error("Wirte string faild by OutputStream",e);
		}
	}

	/**
	 * 写入 body 字符串,默认 UTF-8
	 * @param content body 字符串
	 */
	public void write(String content){
		write(content,"UTF-8");
	}

	/**
	 * 清空缓冲
	 */
	public void clear(){
		if(type == BodyType.BYTES) {
			byteBufferChannel.clear();
		}
	}

	public void saveAsFile(File destFile) throws IOException {
		if(type == BodyType.BYTES){
			TFile.writeFile(destFile, getBodyBytes());
		}

		if(type == BodyType.FILE) {
			TFile.moveFile(bodyFile, destFile);
		}
	}
	
	@Override
	public String toString(){
		byte[] bodyBytes = getBodyBytes();
		try {
			if(type == BodyType.FILE) {
				return bodyFile.getPath();
			}else{
				return new String(bodyBytes, "UTF-8");

			}
		} catch (UnsupportedEncodingException e) {
			Logger.error(e);
			return null;
		}
	}
}
