package org.voovan.test.tools;

import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import junit.framework.TestCase;

import java.util.Map;

public class TStringUnit extends TestCase {

	String simpleStr ="^ helyho is my name ^";
	public TStringUnit(String name) {
		super(name);
	}

	public void testRemovePrefix() {
		String resultStr = TString.removePrefix(simpleStr);
		assertEquals(resultStr, " helyho is my name ^");
	}

	public void testRemoveSuffix() {
		String resultStr = TString.removeSuffix(simpleStr);
		assertEquals(resultStr, "^ helyho is my name ");
	}

	public void testLeftPad() {
		String resultStr = TString.leftPad(simpleStr, 25, '-');
		assertEquals(resultStr, "----^ helyho is my name ^");
	}

	public void testRightPad() {
		String resultStr = TString.rightPad(simpleStr, 25, '-');
		assertEquals(resultStr, "^ helyho is my name ^----");
	}

	public void testIsNumber() {
		boolean test = TString.isNumber("10", 10);
		assertTrue(test);
		test = TString.isNumber("1A", 10);
		assertTrue(!test);
		test = TString.isNumber("1A", 16);
		assertTrue(test);
	}

	public void testIsInteger() {
		assertTrue(TString.isInteger("1"));
		assertTrue(!TString.isInteger("1.0"));
	}

	public void testIsFloat() {
		assertTrue(TString.isDecimal("1.0"));
		assertTrue(!TString.isDecimal("1"));
	}

	public void testSearchByRegex() {
		assertTrue(TString.regexMatch(simpleStr, "helyho")==1);
	}

	public void testIsNullOrEmpty() {
		assertTrue(TString.isNullOrEmpty(""));
		assertTrue(TString.isNullOrEmpty(null));
		assertTrue(!TString.isNullOrEmpty("str"));
	}

	@SuppressWarnings("unchecked")
	public void testTokenReplaceStringMap() {
		String simpleTokenStr ="^ {{helyho}} {{is}} my name ^";
		Map<String, String> tokens = TObject.asMap("helyho","HELY HO","is","IS'NT");
		String replacedStr = TString.tokenReplace(simpleTokenStr, tokens);
		assertEquals(replacedStr,"^ HELY HO IS'NT my name ^");
	}

	public void testTokenReplaceStringArray() {
		String simpleTokenStr ="^ {{}} is my {{}} name ^";
		String replacedStr = TString.tokenReplace(simpleTokenStr, "HELY HO", "full");
		assertEquals(replacedStr,"^ HELY HO is my full name ^");

		simpleTokenStr ="^ {{1}} is my {{2}} name ^";
		replacedStr = TString.tokenReplace(simpleTokenStr, "HELY HO", "full");
		assertEquals(replacedStr,"^ HELY HO is my full name ^");
	}

	public void testReplaceFirst() {
		String formatStr = "aaaa{}bbbb{}cccc{}";
		String formatedStr = TString.replaceFirst(formatStr,"{}", "1");
		assertEquals(formatedStr,"aaaa1bbbb{}cccc{}");
	}

	public void testReplaceLast() {
		String formatStr = "aaaa{}bbbb{}cccc{}";
		String formatedStr = TString.replaceLast(formatStr,"{}", "1");
		assertEquals(formatedStr,"aaaa{}bbbb{}cccc1");
	}

	public void testReverse() {
		String formatStr = "abcdefg";
		String formatedStr = TString.reverse(formatStr);
		assertEquals(formatedStr,"gfedcba");
	}

	public void testUppercaseHead(){
		String uppercaseHeadStr = "abcdefg";
		String uppercaseHeadedStr = TString.uppercaseHead(uppercaseHeadStr);
		assertEquals(uppercaseHeadedStr,"Abcdefg");
	}

	public void testToUnicode(){
		String str = "abcdefg";
		String uncodeStr = TString.toUnicode(str);
		assertEquals(uncodeStr,"\\u0061\\u0062\\u0063\\u0064\\u0065\\u0066\\u0067");
	}

	public void testFromUnicode(){
		String uncodeStr = "=\\u0061=\\u0062=\\u0063=";
		String str = TString.fromUnicode(uncodeStr);
		assertEquals(str, "=a=b=c=");
		System.out.println(TString.fromUnicode("\\u0063="));
	}
}
