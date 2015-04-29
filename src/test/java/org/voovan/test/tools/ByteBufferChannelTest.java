package org.voovan.test.tools;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.log.Logger;

public class ByteBufferChannelTest {
	public static void main(String[] args) throws IOException {
		long t = System.currentTimeMillis();
		ByteBuffer buffer = ByteBuffer.wrap("helyho is a hero!!!".getBytes());
		ByteBufferChannel bChannel = new ByteBufferChannel();
		bChannel.write(buffer);
		
		ByteBuffer buffer1 = ByteBuffer.allocate(5);
		int size = bChannel.read(buffer1);
		Logger.simple("size:"+size+" :"+new  String(buffer1.array()));
		Logger.simple(new String(bChannel.getBuffer().array()));
		
		bChannel.write(ByteBuffer.wrap(" -=======!".getBytes()));
		Logger.simple((System.currentTimeMillis() - t));
		
		bChannel.close();
	}
}
