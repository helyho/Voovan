package org.hocate.tools;

import java.text.SimpleDateFormat;
import java.util.Date;


public class TDateTime {
	public static String currentTime(){
		return dateFormat(new Date(),"YYYY-MM-DD HH:mm:ss");
	}
	
	public static String currentTime(String format){
		return dateFormat(new Date(),format);
	}
	
	private static String dateFormat(Date date,String format){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.format(date);
	}
}
