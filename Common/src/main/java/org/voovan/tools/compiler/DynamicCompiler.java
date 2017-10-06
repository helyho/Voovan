package org.voovan.tools.compiler;

import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * 编译器
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DynamicCompiler {
	private static DynamicClassLoader classLoader =
			new DynamicClassLoader(DynamicCompiler.class.getClassLoader());

	private JavaCompiler compiler = null ;
	private JavaFileManager fileManager = null ;
	private List<String> options = new ArrayList<String>();
	private DiagnosticCollector<JavaFileObject> diagnosticCollector;
	private Class clazz = null;


	/**
	 * 编译器
	 */
	public DynamicCompiler() {
		this.compiler = this.getComplier();
		//保存编译的诊断信息
		diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
	}

	/**
	 * 编译器
	 */
	public DynamicCompiler(String classPath, String extDirs, String classDir) {
		if(classPath!=null) {
			options.addAll((List<String>)TObject.asList("-classpath", classPath));
		}

		if(classDir!=null) {
			options.addAll((List<String>)TObject.asList("-d", classDir));
		}

		if(extDirs!=null) {
			options.addAll((List<String>)TObject.asList("-extdirs", extDirs));
		}

		this.compiler = this.getComplier();
		//保存编译的诊断信息
		diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
	}

	/**
	 * 获得编译后的 Class 对象
	 *  	仅在内存中编译对象时生效
	 * @return Class 对象
	 */
	public Class getClazz() {
		return clazz;
	}

	/**
	 * 获取 JAVA编译器
	 * @return 获取 java 编译对象
	 */
	private JavaCompiler getComplier(){
		return ToolProvider.getSystemJavaCompiler();
	}

	/**
	 * 编译 内存中的java源码为内存中的class,并默认加载 进 JVM
	 * @param javaSourceCode 需要的java源码字符串
	 * @return 是否编译成功
	 */
	public Boolean compileCode(String javaSourceCode){
		String className = getClassNameFromCode(javaSourceCode);
		fileManager = new MemFileManager(compiler.getStandardFileManager(diagnosticCollector, null, null),
				classLoader);
		JavaFileObject file = new JavaMemSource(className, javaSourceCode);
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file) ;
		return basicCompileCode(compilationUnits);
	}

	/**
	 * 编译多个系统中的java源文件为class文件
	 * @param javaFileNameList java文件名列表
	 * @return  是否编译成功
	 */
	public Boolean compileCode(List<String> javaFileNameList){
		fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);

		StandardJavaFileManager standardJavaFileManager = (StandardJavaFileManager) fileManager;
		Iterable<? extends JavaFileObject> compilationUnits = standardJavaFileManager.getJavaFileObjectsFromStrings(javaFileNameList);
		return basicCompileCode(compilationUnits) ;
	}

	/**
	 * 编译多个系统中的java源文件为class文件
	 * @param javaFileArray java文件列表
	 * @return  是否编译成功
	 */
	public Boolean compileCode(File[] javaFileArray){
		fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);


		StandardJavaFileManager standardJavaFileManager = (StandardJavaFileManager) fileManager;
		Iterable<? extends JavaFileObject> compilationUnits = standardJavaFileManager.getJavaFileObjects(javaFileArray);
		return basicCompileCode(compilationUnits) ;
	}

	/**
	 * 获取编译时的诊断信息
	 * @return 诊断信息列表
	 */
	public List<Diagnostic<? extends JavaFileObject>> getDiagnostics(){
		return diagnosticCollector.getDiagnostics();
	}

	/**
	 * 编译java源文件，底层函数描述
	 * @param compilationUnits  编译对象
	 * @return 是否编译成功
	 */
	private Boolean basicCompileCode(Iterable<? extends JavaFileObject> compilationUnits){

		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector,
				options, null, compilationUnits);
		Boolean success = task.call();

		//对在内存中编译的进行特殊处理
		if(success && fileManager instanceof MemFileManager){
			MemFileManager memFileManager = (MemFileManager)fileManager;
			JavaMemClass javaMemClass = memFileManager.getJavaMemClass();
			clazz = javaMemClass.loadThisClass();
		}else{
			clazz = null;
		}

		if(fileManager != null){
			try {
				fileManager.close() ;
			} catch (IOException e) {
				Logger.error(e);
			}
		}
		return success ;
	}

	/**
	 * 获取一个动态编译器中的 Class
	 * @param className class 名称
	 * @return Class 对象
	 * @throws ClassNotFoundException 类未找到的异常
	 */
	public static Class getClassByName(String className) throws ClassNotFoundException {
		return DynamicCompiler.classLoader.loadClass(className);
	}

	/**
	 * 从源代码中获取类名称
	 * @param javaSourceCode java 源代码
	 * @return 类名称
	 */
	public static String getClassNameFromCode(String javaSourceCode){
		String className = javaSourceCode.substring(javaSourceCode.indexOf(" class ")+7,javaSourceCode.indexOf("{")).trim();
		int spaceIndex = className.indexOf(" ");
		if(spaceIndex != -1){
			className = className.substring(0, spaceIndex);
		}
		className = className.trim();
		return className;
	}
}
