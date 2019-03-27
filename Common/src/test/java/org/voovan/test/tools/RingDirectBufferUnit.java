package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.RingDirectBuffer;
import org.voovan.tools.TEnv;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RingDirectBufferUnit extends TestCase {
	public void test() throws IOException {
		RingDirectBuffer ringDirectBuffer = new RingDirectBuffer(10);

		int i=0;
		while(ringDirectBuffer.avaliable() > 0) {
			i++;
			ringDirectBuffer.write((byte) (60+i));
		}
		System.out.println(i);
		byte[] a = new byte[ringDirectBuffer.getCapacity()];
		ringDirectBuffer.read(a, 5, ringDirectBuffer.getCapacity() - 5);

		ringDirectBuffer.write((byte)80);
		ringDirectBuffer.write((byte)80);
		ringDirectBuffer.write((byte)80);
		ringDirectBuffer.write((byte)80);

		ByteBuffer byteBuffer = ringDirectBuffer.getByteBuffer();

		i=0;
		while(ringDirectBuffer.avaliable() > 0) {
			ringDirectBuffer.write((byte) (70+i));
		}

		ByteBuffer b = ByteBuffer.wrap(a);
		ringDirectBuffer.read(b);
		TEnv.sleep(100000);
	}

	public void testReadWrite(){
		RingDirectBuffer ringDirectBuffer = new RingDirectBuffer(23);

		String m = "1234567890";

		byte[] xx = new byte[m.length()];

		ringDirectBuffer.write(m.getBytes(), 0, m.length());
		for( int i=0;i<100;i++) {
			ringDirectBuffer.write(m.getBytes(),0,m.length());
			ringDirectBuffer.read(xx, 0, m.length());
			System.out.println(new String(xx) + " " + ringDirectBuffer);
		}
	}

	public void testReadLine(){
		RingDirectBuffer ringDirectBuffer = new RingDirectBuffer(1024);

		String m = "12345\r\n";

		byte[] xx = new byte[m.length()];

		ringDirectBuffer.write(m.getBytes(), 0, m.length());
		for( int i=0;i<10;i++) {
			int length = i==9?m.length()-2:m.length();
			ringDirectBuffer.write(m.getBytes(), 0, length);
		}

		System.out.println(new String(ringDirectBuffer.toArray()) + "====\r\n");

		int t = 0;
		while(true){
			String line = ringDirectBuffer.readLine();
			if(line==null){
				break;
			}
			t++;
			System.out.println(t + " " + line + ringDirectBuffer);
		}
	}

	public void testSaveToFile() throws IOException {
		RingDirectBuffer ringDirectBuffer = new RingDirectBuffer(1024);

		String m = "12345\r\n";

		byte[] xx = new byte[m.length()];

		ringDirectBuffer.write(m.getBytes(), 0, m.length());
		for( int i=0;i<10;i++) {
			int length = i==9?m.length()-2:m.length();
			ringDirectBuffer.write(m.getBytes(), 0, length);
		}

		System.out.println(new String(ringDirectBuffer.toArray()) + "====");

		ringDirectBuffer.saveToFile("/Users/helyho/Downloads/test.txt", ringDirectBuffer.remaining());
	}
}
