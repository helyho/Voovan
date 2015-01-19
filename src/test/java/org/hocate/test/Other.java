package org.hocate.test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import org.hocate.log.Logger;

public class Other {
	public static void main(String[] args) throws Exception {
		Integer s= 1025;
		Logger.simple(s.byteValue());
		ByteBuffer byteBuffer = ByteBuffer.allocate(0);
		Logger.simple(byteBuffer.hasRemaining());
		
		
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		byteOutputStream.write("bingo".getBytes());
		Logger.info(byteOutputStream.toByteArray().length);
		
		Logger.simple(ClassLoader.getSystemClassLoader().getClass().getName());
		
		Logger.simple(System.getProperty("user.dir"));
	}
}
