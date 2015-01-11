package org.hocate.dynamicComplier;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * 内存源码保存对象
 * @author helyho
 *
 */
public class JavaMemSource  extends SimpleJavaFileObject{

	private String code;
	
	
	public JavaMemSource(String name,String code) {
		super(URI.create("string:///" + name.replace('.', '/')+ Kind.SOURCE.extension),Kind.SOURCE);
		this.code = code;
	}
	
	@Override 
	public CharSequence getCharContent(boolean ignoreEncodingErrors) { 
		return code; 
	} 

}
