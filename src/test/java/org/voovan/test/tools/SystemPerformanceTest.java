package org.voovan.test.tools;

import org.voovan.tools.TEnv;
import org.voovan.tools.TPerformance;
import org.voovan.tools.log.Logger;

public class SystemPerformanceTest {
	public static void main(String[] args) throws InterruptedException {
		
		for (int i = 0; i < 10000; i++) {
			Logger.simple("=========================================");
			Logger.simple(TPerformance.cpuPerCoreLoadAvg());
			Logger.simple(TPerformance.jvmMemoryUsage());
			TEnv.sleep(100);
		}
		
	}
}
