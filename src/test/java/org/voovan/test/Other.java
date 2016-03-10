package org.voovan.test;

import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;


public class Other {
	public static void main(String[] args) throws Exception {
		for(int i=0;i<100;i++){
			Logger.simple("Current count: "+i);
			System.out.println("-------------------------------");
		}
	}
	
}
