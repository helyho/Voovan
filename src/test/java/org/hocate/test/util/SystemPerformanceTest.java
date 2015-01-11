package org.hocate.test.util;

import org.hocate.tools.TPerformance;
import org.hocate.tools.TEnv;

public class SystemPerformanceTest {
	public static void main(String[] args) throws InterruptedException {
		
		for (int i = 0; i < 10000; i++) {
			System.out.println("=========================================");
			System.out.println(TPerformance.cpuPerCoreLoadAvg());
			System.out.println(TPerformance.jvmMemoryUsage());
			TEnv.sleep(1);
		}
		
	}
}
