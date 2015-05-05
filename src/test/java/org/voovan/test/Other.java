package org.voovan.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

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
		String regex = ":[^/]+";
		Logger.simple("/test/:username_a/:id".replaceAll(regex, "[^/?]+"));
		
		
		File fileClasses = new File("/Users/helyho/Work/Java/try/bin/");
		
		TEnv.LoadJars("/Users/helyho/Work/Java/Ozologo/WEBAPP/WEB-INF/lib");
		TEnv.loadBinary(fileClasses.getPath());
		Class.forName("trySomething");
	}
	
}
