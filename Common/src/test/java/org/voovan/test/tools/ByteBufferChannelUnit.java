package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

public class ByteBufferChannelUnit extends TestCase {

	private ByteBufferChannel byteBufferChannel;
	private String tmp1 = "helyho is a hero!!!";
	private String tmp2 = " -=======!";

	public ByteBufferChannelUnit(String name) {
		super(name);
	}

	public void init() {
		ByteBuffer buffer = ByteBuffer.wrap(tmp1.getBytes());
		byteBufferChannel = new ByteBufferChannel();
		byteBufferChannel.writeEnd(buffer);
		assertEquals(byteBufferChannel.size(),19);
	}

	public void testWrite() throws IOException{
		init();
		int size = byteBufferChannel.size();
		byteBufferChannel.write(1, ByteBuffer.wrap(tmp2.getBytes()));
		assertEquals(TByteBuffer.toString(byteBufferChannel.getByteBuffer()), "h -=======!elyho is a hero!!!");
		byteBufferChannel.release();
	}

	public void testWriteEnd() throws IOException{
		init();
		int size = byteBufferChannel.size();
		byteBufferChannel.writeEnd(ByteBuffer.wrap(tmp2.getBytes()));
		assertEquals(TByteBuffer.toString(byteBufferChannel.getByteBuffer()),tmp1+tmp2);
		byteBufferChannel.release();
	}

	public void testWriteHead() throws IOException{
		init();
		byteBufferChannel.writeHead(ByteBuffer.wrap(tmp2.getBytes()));
		assertEquals(TByteBuffer.toString(byteBufferChannel.getByteBuffer()),tmp2+tmp1);
		byteBufferChannel.release();
	}

	public void testRead() throws IOException{
		init();
		ByteBuffer buffer1 = ByteBuffer.allocate(3);
		int size = byteBufferChannel.read(6, buffer1);
		assertEquals(size, 3);
		assertEquals(TByteBuffer.toString(buffer1), " is");
		assertEquals(TByteBuffer.toString(byteBufferChannel.getByteBuffer()),"helyho a hero!!!");
		byteBufferChannel.release();
	}

	public void testReadHead() throws IOException{
		init();
		ByteBuffer buffer1 = ByteBuffer.allocate(5);
		int size = byteBufferChannel.readHead(buffer1);
		assertEquals(size, 5);
		assertEquals(TByteBuffer.toString(buffer1), "helyh");
		assertEquals(TByteBuffer.toString(byteBufferChannel.getByteBuffer()),"o is a hero!!!");
		byteBufferChannel.release();
	}

	public void testReadEnd() throws IOException{
		init();
		ByteBuffer buffer1 = ByteBuffer.allocate(5);
		int size = byteBufferChannel.readEnd(buffer1);
		assertEquals(size, 5);
		assertEquals(TByteBuffer.toString(buffer1), "ro!!!");
		assertEquals(TByteBuffer.toString(byteBufferChannel.getByteBuffer()),"helyho is a he");
		byteBufferChannel.release();
	}

	public void testArray(){
		init();
		assertEquals(new String(byteBufferChannel.array()), tmp1);
		byteBufferChannel.release();
	}

	public void testGetByte(){
		init();
		assertEquals('h', byteBufferChannel.get(0));
		assertEquals('e', byteBufferChannel.get(1));
		assertEquals('l', byteBufferChannel.get(2));
		assertEquals('y', byteBufferChannel.get(3));
		byteBufferChannel.release();
	}

	public void testIndex(){
		init();
		byteBufferChannel.clear();
		byteBufferChannel.writeEnd(ByteBuffer.wrap("PINGq==== test message =====".getBytes()));
		int index = byteBufferChannel.indexOf("PINGq".getBytes());
		assertEquals(index,0);
		byteBufferChannel.release();
	}

	public void testShrinkCommon(){
		//向前
		init();
		byteBufferChannel.shrink(3, 3); //3 向右缩减2个
		assertEquals("hel is a hero!!!",TByteBuffer.toString(byteBufferChannel.getByteBuffer()));
		byteBufferChannel.release();

		init();
		byteBufferChannel.shrink(3, -3); //2 向左缩减2个
		assertEquals("yho is a hero!!!",TByteBuffer.toString(byteBufferChannel.getByteBuffer()));
		byteBufferChannel.release();

	}

	public void testShrink(){
		init();
		byteBufferChannel.shrink(3);
		assertEquals("yho is a hero!!!",TByteBuffer.toString(byteBufferChannel.getByteBuffer()));
		byteBufferChannel.release();

		init();
		byteBufferChannel.shrink(-3);
		assertEquals("helyho is a hero",TByteBuffer.toString(byteBufferChannel.getByteBuffer()));
		byteBufferChannel.release();
	}

	public void testGetByteArray(){
		init();
		byte[] tmp = new byte[6];
		byteBufferChannel.get(tmp, 0, 6);
		assertEquals("helyho", new String(tmp));
		byteBufferChannel.release();
	}

	public void testCompact(){
		init();
		ByteBufferChannel byteBufferChannel1;
		byteBufferChannel1 = new ByteBufferChannel(8);
		byteBufferChannel1.writeEnd(ByteBuffer.wrap("bbccdd".getBytes()));
		byteBufferChannel1.getByteBuffer().position(6);
		byteBufferChannel1.getByteBuffer().limit(8);
		byteBufferChannel1.getByteBuffer().put("ee".getBytes());
		byteBufferChannel1.getByteBuffer().position(2);
		byteBufferChannel1.compact();
		assertEquals("ccddee",TByteBuffer.toString(byteBufferChannel1.getByteBuffer()));
		byteBufferChannel.release();
	}

	public void testReadLine(){
		init();
		ByteBufferChannel byteBufferChannel1 = new ByteBufferChannel();
		byteBufferChannel1.writeHead(ByteBuffer.wrap("aaaaa\r\nbbbbb\r\nccccc\r\n".getBytes()));
		while(true){
			String tmp = byteBufferChannel1.readLine();
			if(tmp==null){
				break;
			}
			Logger.simple("lineCount: "+tmp);
		}
		byteBufferChannel.release();
	}

	public void testReadWithSplit(){
		init();
		ByteBufferChannel byteBufferChannel1 = new ByteBufferChannel();
		byteBufferChannel1.writeEnd(ByteBuffer.wrap("aaaaa\r\nbbbbb\r\nccccc\r\n".getBytes()));
		while(true){
			ByteBuffer byteBuffer = byteBufferChannel1.readWithSplit("bbbbb\r\n".getBytes());
			if(byteBuffer.limit() == 0){
				break;
			}
			Logger.simple("splitedContent: "+ TByteBuffer.toString(byteBuffer));
		}

		Logger.simple("========================");

		byteBufferChannel1.writeEnd(ByteBuffer.wrap("bbbbb\r\nccccc\r\nbbbbb\r\nccccc\r\n".getBytes()));
		while(true){
			ByteBuffer byteBuffer = byteBufferChannel1.readWithSplit("bbbbb\r\n".getBytes());
			if(byteBuffer.limit()==0){
				break;
			}
			Logger.simple("splitedContent: "+ TByteBuffer.toString(byteBuffer));
		}
		byteBufferChannel.release();
	}

	public void testSaveToFile() throws IOException {
		init();
		byteBufferChannel.shrink(-3);
		byteBufferChannel.saveToFile("/Users/helyho/Downloads/test.txt",byteBufferChannel.size()-3);
		byteBufferChannel.release();
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
		Logger.simple("bytbyteBufferChannel content: "+TByteBuffer.toString(bytebuffer));
		Logger.simple("bytebuffer get: '"+(char)bytebuffer.get()+"'");

		Logger.simple("bytebuffer put: 'c'");
		bytebuffer.put(new byte[]{99});

		byteBufferChannel1.getByteBuffer().rewind();

		Logger.simple("bytbyteBufferChannel content:"+new String(byteBufferChannel1.array()));

		ByteBuffer xxx = ByteBuffer.allocate(7);
		xxx.put((byte) '-');
		xxx.put((byte) '=');
		byteBufferChannel1.readHead(xxx);
		Logger.simple("read head 5: "+new String(xxx.array()));

		xxx.clear();
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

		byteBufferChannel1.clear();

		byteBufferChannel1.release();
		byteBufferChannel1.release();
	}

	public void testMulitThread(){
		ByteBufferChannel byteBufferChannel  = new ByteBufferChannel();
		for(int i=0; i < 200; i++){
			Global.getThreadPool().execute(() -> {
				Random random = new Random();
				int r = random.nextInt();
				r = r < 0 ? (r*-1) : r;
				if(random.nextInt()%2==0){
					System.out.println("Write: "+r+"_");
					byteBufferChannel.writeEnd(ByteBuffer.wrap((String.valueOf(r)+"_").getBytes()));
				}  else if(r%18 == 0){
					Logger.simple("release");
					byteBufferChannel.release();
				} else {
					ByteBuffer byteBuffer = ByteBuffer.allocate(10);
					byteBufferChannel.readHead(byteBuffer);
					System.out.println("Load: "+TByteBuffer.toString(byteBuffer));
				}
			});
		}

		TEnv.sleep(60*1000);

	}
}
