package org.voovan.test.tools.json;

import org.voovan.tools.reflect.annotation.Serialization;
import org.voovan.tools.reflect.exclude.Null;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.Vector;

public class TestObject2 implements Serializable{
	@Serialization(exclude = Null.class)
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TestObject2)) return false;
		TestObject2 that = (TestObject2) o;
		return bint == that.bint &&
				Objects.equals(string, that.string) &&
				Objects.equals(map, that.map) &&
				Objects.equals(list, that.list);
	}

	@Override
	public int hashCode() {
		return Objects.hash(string, bint, map, list);
	}
}
