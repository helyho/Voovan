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

	public void testReadLine(){
		ByteBufferChannel byteBufferChannel1 = new ByteBufferChannel();
		byteBufferChannel1.writeHead(ByteBuffer.wrap("aaaaa\r\nbbbbb\r\nccccc\r\n".getBytes()));
		while(true){
			String tmp = byteBufferChannel1.readLine();
			if(tmp==null){
				break;
			}
			Logger.simple("lineCount: "+tmp);
		}
	}

	public void testReadWithSplit(){
		ByteBufferChannel byteBufferChannel1 = new ByteBufferChannel();
		byteBufferChannel1.writeHead(ByteBuffer.wrap("aaaaa\r\nbbbbb\r\nccccc\r\n".getBytes()));
		while(true){
			ByteBuffer byteBuffer = byteBufferChannel1.readWithSplit("bbbbb\r\n".getBytes());
			if(byteBuffer==null){
				break;
			}
			Logger.simple("splitedContent: "+ TByteBuffer.toString(byteBuffer));
		}
	}

	public void testAll() throws IOException {
		ByteBufferChannel byteBufferChannel1;
		byteBufferChannel1 = new ByteBufferChannel(2);
		byteBufferChannel1.writeEnd(ByteBuffer.wrap("bbbbb".getBytes()));
		Logger.simple("bytbyteBufferChannel writeEnd: bbbbb");
		byteBufferChannel1.writeEnd(ByteBuffer.wrap("ccccc".getBytes()));
		Logger.simple("bytbyteBufferChannel writeEnd: ccccc");
		byteBufferChannel1.writeHead(ByteBuffer.wrap("aaaaa".getBytes()));
		Logger.simple("bytbyteBufferChannel writeHead: aaaaa");
		byteBufferChannel1.writeEnd(ByteBuffer.wrap("ddddd".getBytes()));
		Logger.simple("bytbyteBufferChannel writeEnd: ddddd");
		ByteBuffer bytebuffer = byteBufferChannel1.getByteBuffer();
		Logger.simple("bytbyteBufferChannel content: "+new String(bytebuffer.array()));
		Logger.simple("bytebuffer get: '"+(char)bytebuffer.get()+"'");

		Logger.simple("bytebuffer put: 'c'");
		bytebuffer.put(new byte[]{99});

		Logger.simple("bytbyteBufferChannel content:"+new String(byteBufferChannel1.array()));

		ByteBuffer xxx = ByteBuffer.allocate(7);
		xxx.put((byte) '-');
		xxx.put((byte) '=');
		byteBufferChannel1.readHead(xxx);
		Logger.simple("read head 5: "+new String(xxx.array()));

		xxx.rewind();
		xxx.put((byte) '-');
		xxx.put((byte) '=');
		byteBufferChannel1.readEnd(xxx);
		Logger.simple("read End 5: "+new String(xxx.array()));

		xxx = ByteBuffer.allocate(5);
		byteBufferChannel1.readHead(xxx);
		Logger.simple("read head 5: "+new String(xxx.array()));

		xxx.rewind();
		byteBufferChannel1.readHead(xxx);
		Logger.simple("read head 5: "+new String(xxx.array()));
	}
}
