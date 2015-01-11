package org.hocate.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;


public class ByteBufferChannel implements ByteChannel {

	private ByteBuffer buffer; 
	
	public ByteBufferChannel() {
		buffer = ByteBuffer.allocate(0);
	}
	
	/**
	 * 重置
	 */
	public void reset() {
		buffer = ByteBuffer.allocate(0);
	}
	
	/**
	 * 当前数据大小
	 * @return
	 */
	public int size(){
		return buffer.limit();
	} 
	
	public ByteBuffer getBuffer(){
		return buffer;
	}
	
	@Override
	public synchronized int read(ByteBuffer dst) throws IOException {
			int readSize = 0;
			
			//确定读取大小
			if( dst.limit() > buffer.limit() ){
				readSize = buffer.limit();
			}else{
				readSize = dst.limit();
			}
			
			if(readSize!=0)
			{
				dst.put(buffer.array(),0,readSize);
				buffer.position(readSize);
				byte[] tempBytes = new byte[buffer.remaining()];
				buffer.get(tempBytes,0,buffer.remaining());
				buffer = ByteBuffer.wrap(tempBytes);
			}
			dst.flip();
			return readSize;
	}

	/**
	 * 没有作用永远返回 true
	 */
	@Override
	public boolean isOpen() {
		return true;
	}

	/**
	 * 没有作用
	 */
	@Override
	public void close() throws IOException {
		
	}

	@Override
	public synchronized int write(ByteBuffer src) throws IOException {
		if(src.limit()!=0){
			int newSize = buffer.limit()+src.limit();
			ByteBuffer tempBuffer = ByteBuffer.allocate(newSize);
			tempBuffer.put(buffer);
			tempBuffer.put(src);
			buffer = tempBuffer;
			buffer.flip();
		}
		return src.limit();
	}

	
	
	public static void main(String[] args) throws IOException {
		long t = System.currentTimeMillis();
		ByteBuffer buffer = ByteBuffer.wrap("helyho is a hero!!!".getBytes());
		ByteBufferChannel bChannel = new ByteBufferChannel();
		bChannel.write(buffer);
		
		ByteBuffer buffer1 = ByteBuffer.allocate(5);
		int size = bChannel.read(buffer1);
		System.out.println("size:"+size+" :"+new  String(buffer1.array()));
		System.out.println(new String(bChannel.getBuffer().array()));
		
		bChannel.write(ByteBuffer.wrap(" -=======!".getBytes()));
		System.out.println((System.currentTimeMillis() - t));
		
		bChannel.close();
	}
	
	@Override
	public String toString(){
		return buffer.toString();
	}
}
