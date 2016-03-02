package org.voovan.complier;

import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

import javax.tools.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 * 编译器
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Complier {
	
	private JavaCompiler compiler = null ;
	private JavaFileManager fileManager = null ;
	private Iterable<String> options = null ;
	private DiagnosticCollector<JavaFileObject> diagnostics;
	
	/**
	 * 编译器
	 */
	public Complier() {
		this.compiler = this.getComplier();
		diagnostics = new DiagnosticCollector<JavaFileObject>();
	}

	/**
	 * 获取 JAVA编译器
	 * @return
	 */
	private JavaCompiler getComplier(){
		return ToolProvider.getSystemJavaCompiler(); 
	}
	
	/**
	 * 编译多个系统中的java源文件为class文件
	 * @param javaFileNameList
	 * @param classDir
	 * @return
	 */
	public Boolean compileCode(List<String> javaFileNameList,String classDir){
		fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = TObject.cast(fileManager,StandardJavaFileManager.class).getJavaFileObjectsFromStrings(javaFileNameList); 
		options = Arrays.asList("-d", classDir); 
		return basicCompileCode(compilationUnits) ;
	}
	
	/**
	 * 编译 内存中的java源码为class文件
	 * @param javaSourceContent 需要的java源码字符串
	 * @return
	 */
	public Boolean compileCode(String javaSourceCode){
		String className = getClassNameFromCode(javaSourceCode);
		fileManager = new MemFileManager(compiler.getStandardFileManager(diagnostics, null, null));
		JavaFileObject file = new JavaMemSource(className, javaSourceCode); 
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file) ;
		return basicCompileCode(compilationUnits);
	}
	
	/**
	 * 编译 内存中的java源码为class文件
	 * @param classDir 生成的class文件所在的目录
	 * @param javaSourceContent 需要的java源码字符串
	 * @return
	 */
	public Boolean compileCode(String classDir,String javaSourceCode){
		options = Arrays.asList("-d", classDir); 
		fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		return compileCode(javaSourceCode) ;
	}
	
	/**
	 * 编译 内存中的java源码为class文件
	 * @param className 生成的java类的名字
	 * @param classPath 需要引入的classpath字符串
	 * @param classDir 生成的class文件所在的目录
	 * @param javaSourceContent 需要的java源码字符串
	 * @return
	 */
	public Boolean compileCode(String classPath,String classDir,String javaSourceCode){
		options = Arrays.asList("-classpath",classPath,"-d", classDir); 
		fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		return compileCode(javaSourceCode) ;
	}

	/**
	 * 编译java源文件，底层函数描述
	 * @param compilationUnits
	 * @return
	 */
	private Boolean basicCompileCode(Iterable<? extends JavaFileObject> compilationUnits){
		
		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
		Boolean success = task.call(); 
		
		//对在内存中编译的进行特殊处理
		if(success && fileManager instanceof MemFileManager){
			MemFileManager memFileManager = TObject.cast(fileManager);
			JavaMemClass javaMemClass = memFileManager.getJavaMemClass();
			javaMemClass.loadThisClass();
		}
		
		if(fileManager != null){
			try {
				fileManager.close() ;
			} catch (IOException e) {
				Logger.error(e.getMessage(),e);
			}
		}
		return success ;
	}
	
	/**
	 * 从源代码中获取类名称
	 * @param javaSourceCode
	 * @return
	 */
	public static String getClassNameFromCode(String javaSourceCode){
		String className = javaSourceCode.substring(javaSourceCode.indexOf("class")+5,javaSourceCode.indexOf("{")).trim();
		className = className.trim();
		return className;
	}
}
