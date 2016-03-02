package org.voovan.complier;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * 内存源码保存对象
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
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
