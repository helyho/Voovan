package org.voovan.tools.classloader;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.voovan.tools.TEnv;
import org.voovan.tools.TStream;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

/**
 *  类环境隔离用的 ClassLoader
 */
public class IsolatedClassLoader extends URLClassLoader{
    private static List<String> classPaths;
    private static URL[] urls;
    static {
        classPaths = TEnv.getClassPath();
        urls = TEnv.getClassPath().stream().map(s-> {
            try {
                return new URL("file://"+s);
            } catch (MalformedURLException e) {
                Logger.error(e);
                return null;
            }
        }).filter(o->o!=null).collect(Collectors.toList()).toArray(new URL[0]);
    }

    public IsolatedClassLoader() {
        super(urls, getSystemClassLoader());
    }

    // public Resource getResource

     protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
            Class clazz = this.findLoadedClass("name");
            if(clazz!=null) {
                return clazz;
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

    public static void isolatedRun(Class<? extends Runnable> clazz) throws Exception {
        IsolatedClassLoader isolatedClassLoader = new IsolatedClassLoader();
        clazz = (Class<? extends Runnable>) isolatedClassLoader.loadClass(clazz.getCanonicalName());
        ((Runnable)TReflect.newInstance(clazz)).run();
    }

    public static <T> T isolatedCall(Class<? extends Callable<T>> clazz) throws Exception {
        IsolatedClassLoader isolatedClassLoader = new IsolatedClassLoader();
        clazz = (Class<? extends Callable<T>>) isolatedClassLoader.loadClass(clazz.getCanonicalName());
        return ((Callable<T>)TReflect.newInstance(clazz)).call();
    }


    public static <T> T isolatedInvoke(Class clazz, String method, Object args) throws Exception {
        IsolatedClassLoader isolatedClassLoader = new IsolatedClassLoader();
         clazz = isolatedClassLoader.loadClass(clazz.getCanonicalName());
        Object obj = TReflect.newInstance(clazz);
        return (T)TReflect.invokeMethod(obj, method, args);
    }
    
}
