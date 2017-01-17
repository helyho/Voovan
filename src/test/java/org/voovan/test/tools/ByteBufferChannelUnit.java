package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
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
		byteBufferChannel.writeEnd(buffer);
		assertEquals(byteBufferChannel.size(),19);
	}
	
	public void testWrite() throws IOException{
		int size = byteBufferChannel.size();
		byteBufferChannel.writeEnd(ByteBuffer.wrap(" -=======!".getBytes()));
		assertEquals(byteBufferChannel.size(),size+10);
	}
	
	public void testRead() throws IOException{
		ByteBuffer buffer1 = ByteBuffer.allocate(5);
		int size = byteBufferChannel.readHead(buffer1);
		assertEquals(size, 5);
		assertEquals(new String(buffer1.array()), "helyh");
	}


	public void test() throws IOException {
		ByteBufferChannel byteBufferChannel1;
		byteBufferChannel1 = new ByteBufferChannel();
		byteBufferChannel1.writeEnd(ByteBuffer.wrap("kkkkk".getBytes()));
		byteBufferChannel1.writeEnd(ByteBuffer.wrap("fffff".getBytes()));
		byteBufferChannel1.writeHead(ByteBuffer.wrap("bbbbb".getBytes()));
		byteBufferChannel1.writeEnd(ByteBuffer.wrap("eeeee".getBytes()));
		Logger.simple(new String(TByteBuffer.toArray(byteBufferChannel1.getByteBuffer())));


		ByteBuffer xxx = ByteBuffer.allocate(5);
		byteBufferChannel1.readHead(xxx);
		Logger.simple(new String(xxx.array()));
		xxx = ByteBuffer.allocate(5);
		byteBufferChannel1.readEnd(xxx);
		Logger.simple(new String(xxx.array()));
		xxx = ByteBuffer.allocate(5);
		byteBufferChannel1.readHead(xxx);
		Logger.simple(new String(xxx.array()));
		xxx = ByteBuffer.allocate(5);
		byteBufferChannel1.readHead(xxx);
		Logger.simple(new String(xxx.array()));
	}
}
