package org.voovan.tools.compiler;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;

/**
 * 内存文件管理实现
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class MemFileManager extends ForwardingJavaFileManager<JavaFileManager> {

	private JavaMemClass javaMemClass;
	private ClassLoader classLoader;

	protected MemFileManager(JavaFileManager fileManager, ClassLoader classLoader) {
		super(fileManager);
		this.classLoader = classLoader;
	}
	
	public JavaMemClass getJavaMemClass() {
		return javaMemClass;
	}
	
	@Override
	public JavaFileObject getJavaFileForOutput(Location location,
            String className,
            JavaFileObject.Kind kind,
            FileObject sibling)
     throws IOException{
		javaMemClass = new JavaMemClass(className, kind, classLoader);
		return javaMemClass;
		
	}
}
