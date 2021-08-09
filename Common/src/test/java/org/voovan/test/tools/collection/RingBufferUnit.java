package org.voovan.test.tools.collection;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.collection.RingBuffer;
import org.voovan.tools.TEnv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RingBufferUnit extends TestCase {
	public void test() throws IOException {
		RingBuffer ringBuffer = new RingBuffer(10);

		int i=0;
		while(ringBuffer.avaliable() > 0) {
			i++;
			ringBuffer.push((byte) (60+i));
		}
		System.out.println(i);
		Object[] a = new Object[ringBuffer.getCapacity()];
		ringBuffer.pop(a, 5, ringBuffer.getCapacity() - 5);

		ringBuffer.push((byte)80);
		ringBuffer.push((byte)80);
		ringBuffer.push((byte)80);
		ringBuffer.push((byte)80);

		i=0;
		while(ringBuffer.avaliable() > 0) {
			i++;
			ringBuffer.push((byte) (90+i));
		}

		ringBuffer.pop(a, 0, a.length);
		TEnv.sleep(100000);
	}

	public void testMulitThread() throws IOException {
		RingBuffer ringBuffer = new RingBuffer(1000);
		for(int i=0;i<500;i++) {
			int finalI = i;
			Global.getThreadPool().execute(()->{
				ringBuffer.push(finalI);
			});
		}

		AtomicInteger n = new AtomicInteger();
		Vector k = new Vector();
		for(int i=0;i<500;i++) {
			int finalI = i;
			Global.getThreadPool().execute(()->{
				Integer m = (Integer) ringBuffer.pop();
				if(m==null) {
					n.getAndIncrement();
					k.add(9999);
				} else {
					k.add(m);
				}
			});
		}

		TEnv.sleep(2000);

		System.out.println(ringBuffer + " " + n.get());

		Collections.sort(k);

		TEnv.sleep(100000);
	}
}


