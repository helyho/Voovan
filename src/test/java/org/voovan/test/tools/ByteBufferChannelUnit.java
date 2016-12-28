package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteBufferChannelUnit extends TestCase {
	
	private ByteBufferChannel byteBufferChannel;
	
	public ByteBufferChannelUnit(String name) {
		super(name);
	}

	public void setUp() throws IOException{
		String initStr = "helyho is a hero!!!";
		ByteBuffer buffer = ByteBuffer.wrap(initStr.getBytes());
		byteBufferChannel = new ByteBufferChannel();
		byteBufferChannel.write(buffer);
		assertEquals(byteBufferChannel.size(),19);
	}
	
	public void testWrite() throws IOException{
		int size = byteBufferChannel.size();
		byteBufferChannel.write(ByteBuffer.wrap(" -=======!".getBytes()));
		assertEquals(byteBufferChannel.size(),size+10);
	}
	
	public void testRead() throws IOException{
		ByteBuffer buffer1 = ByteBuffer.allocate(5);
		int size = byteBufferChannel.read(buffer1);
		assertEquals(size, 5);
		assertEquals(new String(buffer1.array()), "helyh");
	}
	
	public void tearDown() throws IOException{
		byteBufferChannel.close();
	}

	public void test() throws IOException {
		ByteBufferChannel byteBufferChannel1;
		byteBufferChannel1 = new ByteBufferChannel();
		byteBufferChannel1.write(ByteBuffer.wrap("kkkkk".getBytes()));
		byteBufferChannel1.write(ByteBuffer.wrap("fffff".getBytes()));
		ByteBuffer xxx = ByteBuffer.allocate(5);
		byteBufferChannel1.read(xxx);
		Logger.simple(new String(xxx.array()));
		byteBufferChannel1.read(xxx);
		Logger.simple(new String(xxx.array()));
	}
}
