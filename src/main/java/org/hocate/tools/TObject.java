package org.hocate.tools;

public class TObject {
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj){
		return (T)obj;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj,Class<T> t){
		return (T)obj;
	}
	
	public static <T>T nullDefault(T source,T value){
		return source!=null?source:value;
	}
}
