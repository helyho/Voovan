package org.hocate.log;

public class StaticParam {
	private static final long startTimeMillis = System.currentTimeMillis();
	
	public static long getStartTimeMillis(){
		return startTimeMillis;
	}
}
