package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TString;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TDateTimeUnit extends TestCase {

	public TDateTimeUnit(String name) {
		super(name);
	}

	public void testNow() {
		String now = TDateTime.now();
		int findCount = TString.searchByRegex(now, "^\\d{4}-[0-1][0-9]-[0-3][0-9]\\s[0-2][0-9]:[0-6][0-9]:[0-6][0-9]$").length;
		assert (findCount == 1);
	}

	public void testNowString() {
		String now = TDateTime.now("yyyyMMddHHmmss");
		int findCount = TString.searchByRegex(now, "^\\d{4}[0-1][0-9][0-3][0-9][0-2][0-9][0-6][0-9][0-6][0-9]$").length;
		assert (findCount == 1);
	}

	public void testFormatDateString() {
		String now = TDateTime.format(new Date(), "yyyyMMddHHmmss");
		int findCount = TString.searchByRegex(now, "^\\d{4}[0-1][0-9][0-3][0-9][0-2][0-9][0-6][0-9][0-6][0-9]$").length;
		assert (findCount == 1);
	}

	public void testFormatDateStringStringLocale() {
		String now = TDateTime.format(new Date(), "yyyyMMddHHmmss", TimeZone.getDefault().getID(), Locale.getDefault());
		int findCount = TString.searchByRegex(now, "^\\d{4}[0-1][0-9][0-3][0-9][0-2][0-9][0-6][0-9][0-6][0-9]$").length;
		assert (findCount == 1);
	}

	public void testFormatToGMT() {
		String now = TDateTime.formatToGMT(new Date());
		int findCount = TString.searchByRegex(now, "GMT$").length;
		assert (findCount == 1);
	}

	public void testParseStringString() throws ParseException {
		Date date = new Date();
		Date parsedDate = TDateTime.parse(TDateTime.format(date, "yyyy-MM-dd HH:mm:ss"), "yyyy-MM-dd HH:mm:ss");
		assert (date.compareTo(parsedDate) == 0);
	}

	public void testParseStringStringStringLocale() throws ParseException {
		Date date = new Date();
		Date parsedDate = TDateTime.parse(TDateTime.format(date, "yyyy-MM-dd HH:mm:ss"), 
											"yyyy-MM-dd HH:mm:ss", TimeZone.getDefault()
											.getID(), Locale.getDefault());
		assert (date.compareTo(parsedDate) == 0);
	}
	
	 public void testParseToGMT() throws ParseException {
		Date date = new Date();
		Date parsedDate = TDateTime.parseToGMT(TDateTime.formatToGMT(date));
		assert (date.compareTo(parsedDate) == 0);
		
	 }
	
	 public void testAdd() {
		 Date date = new Date();
		 Date afterDate = TDateTime.add(date, 3600*1000);
		 assertTrue(afterDate.after(date));
	 }

}
