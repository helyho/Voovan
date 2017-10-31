package org.voovan.test.tools.json;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;

public class TestObject implements Serializable {
	
	public String string;
	private int bint;
	private HashMap<String, Object> map = new HashMap<String, Object>();
	private Vector<Object> list= new Vector<Object>();
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
	public HashMap<String, Object> getMap() {
		return map;
	}
	public void setMap(HashMap<String, Object> map) {
		this.map = map;
	}
	public Vector<Object> getList() {
		return list;
	}
	public void setList(Vector<Object> list) {
		this.list = list;
	}
	public TestObject2 getTb2() {
		return tb2;
	}
	public void setTb2(TestObject2 tb2) {
		this.tb2 = tb2;
	}
}
