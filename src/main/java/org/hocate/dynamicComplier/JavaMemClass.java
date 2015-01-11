package org.hocate.dynamicComplier;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.net.URI;
 
public class JavaMemClass extends SimpleJavaFileObject {
 
    protected final ByteArrayOutputStream classByteArrayOutputStream =
        new ByteArrayOutputStream();
 
 
    public JavaMemClass(String name, JavaFileObject.Kind kind) {
        super(URI.create("string:///" + name.replace('.', '/')
            + kind.extension), kind);
    }
 
 
    public byte[] getBytes() {
        return classByteArrayOutputStream.toByteArray();
    }
 
    @Override
    public OutputStream openOutputStream() throws IOException {
        return classByteArrayOutputStream;
    }
    
    public void loadThisClass(){
    	Class<?> classLoaderClass =  this.getClass().getClassLoader().getClass();
    	while (!classLoaderClass.equals(ClassLoader.class) && !classLoaderClass.equals(Object.class)) {
    		classLoaderClass = classLoaderClass.getSuperclass();
		}
    	try {
			Method[] methods = classLoaderClass.getDeclaredMethods();
			for(Method method : methods){
				if(method.getName().equals("defineClass") && method.getParameterTypes().length==4){
					method.setAccessible(true);
					byte[] classBytes = this.getBytes();
					method.invoke(this.getClass().getClassLoader(), new Object[]{null,classBytes,0,classBytes.length});
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
