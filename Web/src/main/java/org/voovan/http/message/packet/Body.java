package org.voovan.http.message.packet;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.TZip;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
	private long position;

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
			changeToBytes("".getBytes());
			bodyFile = null;
		} catch (IOException e){
			Logger.error("Construct Body error",e);
		}
	}


	/**
	 * 构造函数
	 * @param content 字节内容
	 */
	public Body(byte[] content){
		type = BodyType.BYTES;
		try {
			changeToBytes(content);
			bodyFile = null;
		} catch (IOException e){
			Logger.error("Construct Body error",e);
		}
	}

	/**
	 * 构造函数
	 * @param filePath 文件路径
	 */
	public Body(String filePath){
		type = BodyType.FILE;
		try {
			changeToFile(new File(filePath));
		} catch (IOException e){
			Logger.error("Construct Body error",e);
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
	 * 内容是否是由存储中的文件提供
	 * @return true: 由存储中的文件提供, false: 由字节提供
	 */
	public boolean isFile(){
		return bodyFile==null? false: true;
	}

	/**
	 * 转换成文件形式
	 * @param bodyFile  文件对象
	 * @throws FileNotFoundException 文件未找到异常
	 */
	public void changeToFile(File bodyFile) throws FileNotFoundException{

		if(!bodyFile.exists()){
			throw new FileNotFoundException("Upload file " + bodyFile.getPath() + " not exists");
		}

		this.bodyFile = bodyFile;

		if(byteBufferChannel != null){
			byteBufferChannel = null;
		}

		position = 0;
		this.type = BodyType.FILE;
	}

	/**
	 * 转换成文件形式
	 * @param file  文件路径
	 * @throws FileNotFoundException 文件未找到异常
	 */
	public void changeToFile(String file) throws FileNotFoundException{
		changeToFile(new File(file));
	}

	/**
	 * 转换成字节形式
	 * @param content 字节内容
	 * @throws IOException 文件未找到异常
	 */
	public void changeToBytes(byte[] content) throws IOException {
		if(byteBufferChannel == null || byteBufferChannel.isReleased()){
			byteBufferChannel = new ByteBufferChannel();
		}

		if(bodyFile != null){
			bodyFile = null;
		}

		if(content.length!=0) {
			byteBufferChannel.writeEnd(ByteBuffer.wrap(content));
		}
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
		} else {
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
			Logger.error("This charset is unsupported",e);
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
			Logger.error("This charset is unsupported",e);
			return null;
		}
	}

	/**
	 * 读取 Body 中的内容
	 * @param byteBuffer ByteBuffer 对象
	 * @return  读出的字节长度
	 */
	public int read(ByteBuffer byteBuffer){
		int readSize = -1;
		if(type == BodyType.BYTES) {
			readSize = byteBufferChannel.readHead(byteBuffer);
			readSize = readSize==0? -1: readSize;
		}else {
			byte[] fileContent = TFile.loadFile(bodyFile, position, position + byteBuffer.remaining());
			if (fileContent != null){
				readSize = fileContent.length;
				position = position + readSize;
				byteBuffer.put(fileContent);
				byteBuffer.flip();
				return readSize;
			}else{
				readSize = -1;
			}
		}
		return readSize;
	}

	/**
	 * 读取 Body 中的内容
	 * @param buffer byte 数组对象
	 * @return 读出的字节长度
	 */
	public int read(byte[] buffer){
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
		return read(byteBuffer);
	}

	/**
	 * 写入 body
	 * @param body 字节数组
	 * @param offset  在Body 对象中的偏移量,即在这个位置开始写入数据
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
		} else if(type == BodyType.FILE){
			if(bodyFile.getPath().startsWith(TFile.getTemporaryPath())) {
				bodyFile.delete();
			}
			bodyFile = null;
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

	/**
	 * 压缩
	 * @return true: 压缩成功, false: 压缩失败
	 * @throws IOException IO异常
	 */
	public boolean compress() throws IOException {

		if(size()!=0) {
			if (isFile()) {
				String fileName = TFile.getFileName(bodyFile.getCanonicalPath());
				fileName = fileName.equals("") ? ".tmp" : fileName;

				//拼文件名
				String localFileName = TFile.assemblyPath(TFile.getTemporaryPath(),
						"voovan",
						"webserver",
						"body",
						TString.assembly("VOOVAN_", TString.generateId(this), ".", fileName));

				new File(TFile.getFileDirectory(localFileName)).mkdirs();
				File gzipedFile = new File(localFileName);

				TZip.encodeGZip(bodyFile, gzipedFile);

				bodyFile = gzipedFile;

				return true;
			} else {
				byte[] bodyBytes = TZip.encodeGZip(getBodyBytes());
				byteBufferChannel.clear();
				byteBufferChannel.writeEnd(ByteBuffer.wrap(bodyBytes));
				return true;
			}
		}else {
			return false;
		}
	}

	public void release(){
		clear();
		if(byteBufferChannel!=null) {
			byteBufferChannel.release();
		}
	}
}
