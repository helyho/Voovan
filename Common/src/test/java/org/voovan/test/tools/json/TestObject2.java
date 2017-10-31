package org.voovan.test.tools.json;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;

public class TestObject2 implements Serializable{
	private String string;
	private int bint;
	private HashMap<String, String> map = new HashMap<String, String>();
	private Vector<String> list= new Vector<String>();
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
}
