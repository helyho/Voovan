package org.voovan.tools;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 流操作类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TStream {
	
	/**
	 * 从 InputStream 读取定长字符串
	 * @param inputStream 输入流
	 * @param length  产度
	 * @return 字节数组
	 * @throws IOException IO 异常
	 */
	public static byte[] read(InputStream inputStream,int length) throws IOException{
		byte[] resultBytes = new byte[length];
		BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
		int readLength = bufferedInputStream.read(resultBytes);
		resultBytes = Arrays.copyOfRange(resultBytes, 0, readLength);
		bufferedInputStream.close();
		return resultBytes.length==0?null:resultBytes;
	}

	/**
	 * 从 InputStream 读取一行
	 * @param inputStream 输入流
	 * @return 字符串
	 * @throws IOException IO 异常
	 */
	public static String readLine(InputStream inputStream) throws IOException{
		String lineStr="";
		while(true){
			int singleChar = inputStream.read();		
			if(singleChar==-1 || singleChar==65535) {
				break;
			}
			else{
				lineStr += String.valueOf((char)singleChar);
				if(lineStr.endsWith("\n")){
					break;
				}
			}
		}		
		return lineStr.equals("")?null:lineStr.substring(0,lineStr.length()-2);
	}
	
	/**
	 * 从 InputStream 读取一段,使用 byte数组 分割
	 * 		返回的 byte数组中不包含分割 byte 数组的内容
	 * @param inputStream  输入流
	 * @param splitByte 分割字节数组
	 * @return 字节数组
	 * @throws IOException IO 异常
	 */
	public static byte[] readWithSplit(InputStream inputStream,byte[] splitByte) throws IOException{
		byte[] resultBytes = new byte[0];
		while(true){
			byte[] tempbyte = new byte[1];
			int readSize = inputStream.read(tempbyte);		
			if(readSize==-1 || readSize==65535) {
				break;
			}
			else{
				resultBytes = TStream.byteArrayConcat(resultBytes, resultBytes.length, tempbyte, tempbyte.length);
			}
			
			if(resultBytes.length>=splitByte.length){
				byte[] resultLastedBytes = Arrays.copyOfRange(resultBytes, resultBytes.length-splitByte.length, resultBytes.length);
				if(Arrays.equals(resultLastedBytes, splitByte)){
					resultBytes = Arrays.copyOfRange(resultBytes, 0, resultBytes.length-splitByte.length);
					break;
				}
			}
		}		
		return resultBytes.length==0?null:resultBytes;
	}
	
	/**
	 * 从 InputStream 读取全部字节
	 * @param inputStrem	输入流
	 * @return 字节数组
	 * @throws IOException IO 异常
	 */
	public static byte[] readAll(InputStream inputStrem) throws IOException{
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStrem);
		while(true)
		{
			byte[] tempBytes = new byte[1024];
			int readSize = bufferedInputStream.read(tempBytes);
			if(readSize>0){
				byteOutputStream.write(tempBytes,0,readSize);
			}
			else if(readSize==-1 || readSize==65535){
				break;
			}
		}
		bufferedInputStream.close();
		return byteOutputStream.size()==0?null:byteOutputStream.toByteArray();
	}
	
	/**
	 * 字节数组拼接
	 * @param firstBytes		   首个字节数组		
	 * @param firstBytesLength     首个字节数组长度
	 * @param lastBytes			   拼接在后的字节数组
	 * @param lastBytesLength      拼接在后的字节数组长度
	 * @return 字节数组
	 */
	public static byte[] byteArrayConcat(byte[] firstBytes,int firstBytesLength, byte[] lastBytes,int lastBytesLength) {
		if (lastBytes.length == 0)
			return firstBytes;
		byte[] target = new byte[firstBytesLength + lastBytesLength];
		System.arraycopy(firstBytes, 0, target, 0, firstBytesLength);
		System.arraycopy(lastBytes, 0, target, firstBytes.length,lastBytesLength);
		return target;
	}
}
