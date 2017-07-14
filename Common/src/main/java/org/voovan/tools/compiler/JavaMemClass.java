package org.voovan.tools.compiler;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * java 内存 Class
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JavaMemClass extends SimpleJavaFileObject {
    private ClassLoader classLoader;


    protected final ByteArrayOutputStream classByteArrayOutputStream =
        new ByteArrayOutputStream();
 
 
    public JavaMemClass(String name, Kind kind, ClassLoader classLoader) {
        super(URI.create("string:///" + name.replace('.', '/')
            + kind.extension), kind);
        this.classLoader = classLoader;
    }
 
 
    public byte[] getBytes() {
        return classByteArrayOutputStream.toByteArray();
    }
 
    @Override
    public OutputStream openOutputStream() throws IOException {
        return classByteArrayOutputStream;
    }
    
    public Class loadThisClass(){
    	try {
            byte[] classBytes = this.getBytes();
            return (Class)TReflect.invokeMethod(classLoader, "defineClass", new Object[]{null, classBytes, 0, classBytes.length});
        } catch (ReflectiveOperationException e) {
			Logger.error(e);
		}

		return null;
    }
}
