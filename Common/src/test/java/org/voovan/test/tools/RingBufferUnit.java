package org.voovan.test.tools;

import com.sun.org.apache.bcel.internal.generic.ARRAYLENGTH;
import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.DirectRingBuffer;
import org.voovan.tools.RingBuffer;
import org.voovan.tools.TEnv;
import sun.jvm.hotspot.memory.TenuredGeneration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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
			ringBuffer.push((byte) (70+i));
		}

		ringBuffer.pop(a, 0, a.length);
		TEnv.sleep(100000);
	}

	public static AtomicInteger m1 = new AtomicInteger(0);
	public static AtomicInteger m2 = new AtomicInteger(0);
	public static AtomicInteger md = new AtomicInteger(0);

	public void testConcurrent(){
		RingBuffer m = new RingBuffer(1024*1024);
		System.out.println(TEnv.measureTime(new Runnable() {
			@Override
			public void run() {

				ArrayList<Thread> mmm = new ArrayList<Thread>();

				String ms ="";

				Thread mt1 = new Thread(()->{
					while(md.get()<=12000000) {
						int data = md.incrementAndGet();
						m.push(data);
						m2.getAndIncrement();
					}
				});

				Thread mt2 = new Thread(()->{
					while(m1.get()<12000000) {
						Object o = m.pop();
						if (o != null) {
							m1.getAndIncrement();
						}
					}
				});

				mmm.add(mt1);
				mmm.add(mt2);

				mt1.start();
				mt2.start();

		//				for(int i=0;i<150;i++){
//					Thread mt = new Thread(()->{
//						while(true) {
//							if (Math.random() > 0.5 && m.remaining() > 0) {
//								Object o = m.pop();
//								if(o !=null) {
////							System.out.println("pop: " + o);
//									m1.getAndIncrement();
//								}
//							} else {
//								if(md.get()<10000000) {
//									int data = md.incrementAndGet();
//									m.push(data);
//									m2.getAndIncrement();
////							System.out.println("push: " + data);
//								} else {
//									break;
//								}
//							}
//						}
//					});
//					mmm.add(mt);
//					mt.start();
//				}

				for(Thread thread : mmm){
					try {
						thread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		}));

		System.out.println(m1.get() + " " + m2.get() + " " + m.remaining());

	}
}


