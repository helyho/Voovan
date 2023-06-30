package org.voovan.tools.classloader;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.voovan.tools.TEnv;
import org.voovan.tools.TStream;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

/**
 *  类环境隔离用的 ClassLoader
 */
public class IsolatedClassLoader extends URLClassLoader {
    private Map<String, Object> objectPool = new ConcurrentHashMap<>();
    private List<String> isolatedClass = new ArrayList<>();

    public IsolatedClassLoader() {
        super(new URL[0], getSystemClassLoader());

        List<String> classPaths = TEnv.getClassPath();
        URL[] urls = TEnv.getClassPath().stream().map(s-> {
            try {
                return new URL("file://"+s);
            } catch (MalformedURLException e) {
                Logger.error(e);
                return null;
            }
        }).filter(o->o!=null).collect(Collectors.toList()).toArray(new URL[0]);

        for(URL url : urls) {
            super.addURL(url);
        }
    }

    public IsolatedClassLoader(Class ... isolatedClazzes) {
        this();
        for(Class clazz : isolatedClazzes) {
            isolatedClass.add(clazz.getCanonicalName());
        }
    }

    // public Resource getResource

     protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
            Class clazz = this.findLoadedClass("name");
            if(clazz!=null) {
                return clazz;
            }

            if(isolatedClass.contains(name)) {
                return getSystemClassLoader().loadClass(name); 
            }

            String path = TString.fastReplaceAll(name, "\\.", File.separator) + ".class";

            byte[] classByte = null;
            String usedClassPath = null;
            try {
                  URL classURL = this.getClass().getClassLoader().getResource(path);

                  InputStream classInputStream = (InputStream)classURL.getContent();
                  classByte = TStream.readAll(classInputStream);

                if(classURL.getProtocol().equals("jrt")) {
                     return getSystemClassLoader().loadClass(name);
                }
                CodeSource codeSource =  new CodeSource(new URL("file://" + usedClassPath), new CodeSigner[0]);
                return defineClass(name, classByte, 0, classByte.length, codeSource);
            } catch(Exception e) {
                throw new ClassNotFoundException(name, e);
            }
    }

    public void isolatedRun(Class<? extends Runnable> clazz) throws Exception {
        clazz = (Class<? extends Runnable>) loadClass(clazz.getCanonicalName());
        ((Runnable)TReflect.newInstance(clazz)).run();
    }

    public <T> T isolatedCall(Class<? extends Callable<T>> clazz) throws Exception {
        clazz = (Class<? extends Callable<T>>) loadClass(clazz.getCanonicalName());
        return ((Callable<T>)TReflect.newInstance(clazz)).call();
    }


    public <T> T isolatedInvoke(String name, Class clazz, String method, Object ... args) throws Exception {
        clazz = loadClass(clazz.getCanonicalName());
        Object obj = TReflect.newInstance(clazz);
        T ret = (T)TReflect.invokeMethod(obj, method, args);
        if(name !=null) {
            objectPool.put(name, ret);
        }
        return ret;
    }

    public <T> T isolatedInvoke(Class clazz, String method, Object ... args) throws Exception {
        return isolatedInvoke(null, clazz, method, args);
    }

    public Object createObject(String name, Class clazz, Object ... args) throws Exception {
        clazz = loadClass(clazz.getCanonicalName());
        Object object = TReflect.newInstance(clazz, args);
        objectPool.put(name, object);
        return object;
    }

     public <T> T invoke(String name, String method, Object ... args) throws Exception {
        Object obj = objectPool.get(name) ;
        if(obj == null) {
            throw new NullPointerException("Object " + name + " not found in this IsolatedClassLoader");
        }
        return (T)TReflect.invokeMethod(obj, method, args);
    }
    
}
