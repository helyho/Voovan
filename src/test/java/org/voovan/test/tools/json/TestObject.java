package org.voovan.test.tools.json;

import java.util.HashMap;
import java.util.Vector;

public class TestObject{
	
	public String string;
	private int bint;
	private HashMap<String, String> map = new HashMap<String, String>();
	private Vector<String> list= new Vector<String>();
	private TestObject2 tb2 = new TestObject2();
	
	public String getString() {
		return string;
	}
	public void setString(String string) {
		this.string = string;
	}
	public int getBint() {
		return bint;
	}
	public void setBint(int bint) {
		this.bint = bint;
	}
	public HashMap<String, String> getMap() {
		return map;
	}
	public void setMap(HashMap<String, String> map) {
		this.map = map;
	}
	public Vector<String> getList() {
		return list;
	}
	public void setList(Vector<String> list) {
		this.list = list;
	}
	public TestObject2 getTb2() {
		return tb2;
	}
	public void setTb2(TestObject2 tb2) {
		this.tb2 = tb2;
	}
}
