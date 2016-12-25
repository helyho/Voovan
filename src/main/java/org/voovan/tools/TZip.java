package org.voovan.tools;

import org.voovan.tools.log.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.*;

/**
 * 压缩算法
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TZip {
	/**
	 * GZip 解压缩
	 * @param encodeBytes 待解压字节
	 * @return 解压后的字节
	 * @throws IOException IO 异常
	 */
	public static byte[] decodeGZip(byte[] encodeBytes) throws IOException{
		GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(encodeBytes));
		byte[] result = TStream.readAll(gzipInputStream);
		gzipInputStream.close();
		return result;
	}
	
	/**
	 * GZIP 压缩
	 * @param sourceBytes 待压缩字节
	 * @return 压缩后的字节
	 * @throws IOException IO 异常
	 */
	public static byte[] encodeGZip(byte[] sourceBytes) throws IOException{
		ByteArrayOutputStream zipedBodyOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(zipedBodyOutputStream);
		gzipOutputStream.write(sourceBytes);
		gzipOutputStream.finish();
		byte[] result = zipedBodyOutputStream.toByteArray();
		gzipOutputStream.close();
		zipedBodyOutputStream.close();
		return result;
	}
	
	/**
	 * Zip 解压缩
	 * @param encodeBytes 待解压字节
	 * @return 解压后的字节
	 * @throws IOException IO 异常
	 */
	public static byte[] decodeZip(byte[] encodeBytes) throws IOException{
		ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(encodeBytes));
		Logger.simple(zipInputStream.getNextEntry().getName());
		byte[] result = TStream.readAll(zipInputStream);
		zipInputStream.close();
		return result;
	}
	
	/**
	 * ZIP 压缩
	 * @param sourceBytes 待压缩字节
	 * @return 压缩后的字节
	 * @throws IOException IO 异常
	 */
	public static byte[] encodeZip(byte[] sourceBytes) throws IOException{
		ZipEntry zipEntry = new ZipEntry("VoovanZipEntry");

		ByteArrayOutputStream zipedBodyOutputStream = new ByteArrayOutputStream();
		ZipOutputStream zipOutputStream = new ZipOutputStream(zipedBodyOutputStream);
		zipOutputStream.putNextEntry(zipEntry);
		zipOutputStream.write(sourceBytes);
		zipOutputStream.finish();
		byte[] result = zipedBodyOutputStream.toByteArray();
		zipedBodyOutputStream.close();
		zipOutputStream.close();
		return result;

	}
}
