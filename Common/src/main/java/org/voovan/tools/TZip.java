package org.voovan.tools;

import java.io.*;
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
	 * GZip 解压文件
	 *
	 * @param inputFile 源文件
	 * @param outputFile 目标文件
	 * @throws IOException IO异常
	 */
	public static void decodeGZip(File inputFile, File outputFile)
			throws IOException {
		FileInputStream fin = null;
		GZIPInputStream gzin = null;
		FileOutputStream fout = null;
		try {
			fin = new FileInputStream(inputFile);
			fout = new FileOutputStream(outputFile);
			gzin = new GZIPInputStream(fin);
			byte[] buf = new byte[1024];
			int num;
			while ((num = gzin.read(buf, 0, buf.length)) != -1) {
				fout.write(buf, 0, num);
			}
		} finally {
			if (fout != null)
				fout.close();
			if (gzin != null)
				gzin.close();
			if (fin != null)
				fin.close();
		}
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
	 * GZip 文件压缩处理
	 *
	 * @param inputFile 源文件
	 * @param outputFile 目标文件
	 * @throws IOException IO异常
	 */
	public static void encodeGZip(File inputFile, File outputFile) throws IOException{
		FileInputStream fin = null;
		FileOutputStream fout = null;
		GZIPOutputStream gzout = null;
		try {
			fin = new FileInputStream(inputFile);
			fout = new FileOutputStream(outputFile);
			gzout = new GZIPOutputStream(fout);
			byte[] buf = new byte[1024];
			int num;
			while ((num = fin.read(buf)) != -1) {
				gzout.write(buf, 0, num);
			}
		} finally {
			if (gzout != null)
				gzout.close();
			if (fout != null)
				fout.close();
			if (fin != null)
				fin.close();
		}
	}

	/**
	 * Zip 解压缩
	 * @param encodeBytes 待解压字节
	 * @return 解压后的字节
	 * @throws IOException IO 异常
	 */
	public static byte[] decodeZip(byte[] encodeBytes) throws IOException{
		ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(encodeBytes));
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


	/**
	 * 从 Zip 文件中读取指定的被压缩文件
	 * @param zipFilePath zip 文件路径
	 * @param filePath zip 文件中压缩文件路径
	 * @return 文件内容
	 * @throws IOException IO 异常
	 */
	public static byte[] loadFileFromZip(String zipFilePath, String filePath) throws IOException {
		try(ZipFile zipFile = new ZipFile(zipFilePath)) {
			ZipEntry zipEntry = zipFile.getEntry(filePath);
			try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
				return TStream.readAll(inputStream);
			}
		}
	}
}
