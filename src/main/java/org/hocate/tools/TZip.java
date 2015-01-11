package org.hocate.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
	 * 获取使用 GZIP 压缩后的 body 字节
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
}
