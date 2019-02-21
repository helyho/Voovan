package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.DirectRingBuffer;
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
public class DirectRingBufferUnit extends TestCase {
	public void test() throws IOException {
		DirectRingBuffer directRingBuffer = new DirectRingBuffer(10);

		int i=0;
		while(directRingBuffer.avaliable() > 0) {
			i++;
			directRingBuffer.write((byte) (60+i));
		}
		System.out.println(i);
		byte[] a = new byte[directRingBuffer.getCapacity()];
		directRingBuffer.read(a, 5, directRingBuffer.getCapacity() - 5);

		directRingBuffer.write((byte)80);
		directRingBuffer.write((byte)80);
		directRingBuffer.write((byte)80);
		directRingBuffer.write((byte)80);

		ByteBuffer byteBuffer = directRingBuffer.getByteBuffer();

		i=0;
		while(directRingBuffer.avaliable() > 0) {
			directRingBuffer.write((byte) (70+i));
		}

		ByteBuffer b = ByteBuffer.wrap(a);
		directRingBuffer.read(b);
		TEnv.sleep(100000);
	}
}
