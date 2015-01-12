package org.hocate.test.util;

import org.hocate.log.Logger;
import org.hocate.tools.TPerformance;
import org.hocate.tools.TEnv;

public class SystemPerformanceTest {
	public static void main(String[] args) throws InterruptedException {
		
		for (int i = 0; i < 10000; i++) {
			Logger.simple("=========================================");
			Logger.simple(TPerformance.cpuPerCoreLoadAvg());
			Logger.simple(TPerformance.jvmMemoryUsage());
			TEnv.sleep(1);
		}
		
	}
}
