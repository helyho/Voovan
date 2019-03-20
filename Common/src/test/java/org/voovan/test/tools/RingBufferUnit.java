package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.RingBuffer;
import org.voovan.tools.TEnv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
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
//				=======================
//
				for(int i=0;i<20;i++){
					if(i%2==0){
						Thread pushThread = new Thread(()->{
							while(md.get()<=20000000) {
								m.push(md.incrementAndGet());
								m2.getAndIncrement();
							}
						});
						mmm.add(pushThread);
						pushThread.start();
					} else {

						Thread popThread = new Thread(()->{
							while(m1.get()<20000000) {
								Object o = m.pop();
								if (o != null) {
//									System.out.println(o);
									m1.getAndIncrement();
								}
							}
						});

						mmm.add(popThread);
						popThread.start();
					}
				}

				for(Thread thread : mmm){
					try {
						thread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		})/1000000000f);

		System.out.println(m1.get() + " " + m2.get() + " " + m.remaining());
		m1.set(0);
		m2.set(0);
		md.set(0);

		ArrayBlockingQueue a = new ArrayBlockingQueue(1024*1024);
		System.out.println(TEnv.measureTime(new Runnable() {
			@Override
			public void run() {

				ArrayList<Thread> mmm = new ArrayList<Thread>();
//				=======================
//
				for(int i=0;i<20;i++){
					if(i%2==0){
						Thread pushThread = new Thread(()->{
							while(md.get()<=20000000) {
								a.offer(md.incrementAndGet());
								m2.getAndIncrement();
							}
						});
						mmm.add(pushThread);
						pushThread.start();
					} else {

						Thread popThread = new Thread(()->{
							while(m1.get()<20000000) {
								Object o = a.poll();
								if (o != null) {
//									System.out.println(o);
									m1.getAndIncrement();
								}
							}
						});

						mmm.add(popThread);
						popThread.start();
					}
				}

				for(Thread thread : mmm){
					try {
						thread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		})/1000000000f);
		System.out.println(m1.get() + " " + m2.get() + " " + a.size());
	}
}


