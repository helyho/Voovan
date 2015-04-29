package org.voovan.test.tools;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.voovan.tools.ByteBufferChannel;

import junit.framework.TestCase;

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
		byteBufferChannel.write(ByteBuffer.wrap(" -=======!".getBytes()));
		assertEquals(byteBufferChannel.size(),24);
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
}
