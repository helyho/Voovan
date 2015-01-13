package org.hocate.test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import org.hocate.log.Logger;
import org.hocate.tools.TReflect;

public class Other {
	public static void main(String[] args) throws Exception {
		Integer s= 1025;
		Logger.simple(s.byteValue());
		ByteBuffer byteBuffer = ByteBuffer.allocate(0);
		Logger.simple(byteBuffer.hasRemaining());
		
		
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		byteOutputStream.write("bingo".getBytes());
		Logger.info(byteOutputStream.toByteArray().length);
		
		Object x = TReflect.newInstance(String.class);
		Logger.info(x.getClass().getName());
		Logger.simple("127.0.0.1".split(":")[0]);
	}
}
