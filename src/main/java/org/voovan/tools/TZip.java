package org.voovan.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
	 * @param encodeBytes
	 * @return
	 * @throws IOException
	 */
	public static byte[] decodeGZip(byte[] encodeBytes) throws IOException{
		GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(encodeBytes));
		return TStream.readAll(gzipInputStream);
	}
	
	/**
	 * GZIP 压缩
	 * @return
	 * @throws IOException
	 */
	public static byte[] encodeGZip(byte[] sourceBytes) throws IOException{
		ByteArrayOutputStream zipedBodyOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(zipedBodyOutputStream);
		gzipOutputStream.write(sourceBytes);
		gzipOutputStream.finish();
		return zipedBodyOutputStream.toByteArray();
	}
	
	/**
	 * Zip 解压缩
	 * @param encodeBytes
	 * @return
	 * @throws IOException
	 */
	public static byte[] decodeZip(byte[] encodeBytes) throws IOException{
		ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(encodeBytes));
		return TStream.readAll(zipInputStream);
	}
	
	/**
	 * ZIP 压缩
	 * @return
	 * @throws IOException
	 */
	public static byte[] encodeZip(byte[] sourceBytes) throws IOException{
		ByteArrayOutputStream zipedBodyOutputStream = new ByteArrayOutputStream();
		ZipOutputStream zipOutputStream = new ZipOutputStream(zipedBodyOutputStream);
		zipOutputStream.write(sourceBytes);
		zipOutputStream.finish();
		return zipedBodyOutputStream.toByteArray();
	}
}
