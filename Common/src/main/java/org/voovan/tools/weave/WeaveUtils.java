package org.voovan.tools.weave;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.exception.WeaveException;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.regex.Matcher;

/**
 * 代码织入工具类
 *
 * @author helyho
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class WeaveUtils {

    public static ClassPool CLASSPOOL = ClassPool.getDefault();

    /**
     * 获取 CtClass 对象
     * @param className CtClass 对象完全现定名
     * @return CtClass 对象
     * @throws NotFoundException 无法找到对一个的 CtClass
     */
    public static CtClass getCtClass(String className) throws
            NotFoundException {
        CtClass ctClass = null;
        ctClass = WeaveUtils.CLASSPOOL.get(className);
        ctClass.detach();
        return ctClass;
    }

    public static List<CtClass> getAllSuperCtClass(CtClass type) throws NotFoundException {
        if(type == null){
            return null;
        }

        ArrayList<CtClass> classes = new ArrayList<CtClass>();

        CtClass superClass = type;
        for( superClass = superClass.getSuperclass();
             superClass!=null && !superClass.getName().equals("java.lang.Object");
             superClass = superClass.getSuperclass()) {
            classes.addAll(Arrays.asList(superClass.getInterfaces()));
            classes.add(superClass);
        }
        return classes;
    }

    /**
     * 从当前进程的Javaassist中寻找 Class
     * @param pattern  确认匹配的正则表达式
     * @return  匹配到的 class 集合
     * @throws IOException IO 异常
     */
    public static List<CtClass> searchCtClassInJavassist(String pattern) throws IOException {
        String userDir = System.getProperty("user.dir");
        List<String> classPaths = TEnv.getClassPath();
        ArrayList<CtClass> clazzes = new ArrayList<CtClass>();
        for(String classPath : classPaths){

            if(TString.isNullOrEmpty(classPath)){
                continue;
            }

            if(!classPath.startsWith(".") && !classPath.startsWith(userDir)) {
                continue;
            }

            File classPathFile = new File(classPath);
            if(classPathFile.exists() && classPathFile.isDirectory()){
                clazzes.addAll(getDirectorCtClass(classPathFile, pattern));
            } else if(classPathFile.exists() && classPathFile.isFile() && classPathFile.getName().endsWith(".jar")) {
                clazzes.addAll(getJarCtClass(classPathFile, pattern) );
            }
        }

        return clazzes;
    }

    /**
     * 从指定 File 对象寻找 CtClass
     * @param rootfile 文件目录 File 对象
     * @param pattern  确认匹配的正则表达式
     * @return  匹配到的 class 集合
     * @throws IOException IO 异常
     */
    public static List<CtClass> getDirectorCtClass(File rootfile, String pattern) throws IOException {
        if(pattern!=null) {
            pattern = pattern.replace(".", File.separator);
        }

        ArrayList<CtClass> result = new ArrayList<CtClass>();
        List<File> files = TFile.scanFile(rootfile, pattern);
        for(File file : files){
            String fileName = file.getCanonicalPath();
            if("class".equals(TFile.getFileExtension(fileName))) {
                //如果是内部类则跳过
                if(TString.regexMatch(fileName,"\\$\\d\\.class")>0){
                    continue;
                }
                fileName = fileName.replace(rootfile.getCanonicalPath() + File.separator, "");
                try {
                    CtClass clazz = resourceToCtClass(fileName);
                    result.add(clazz);
                } catch (ClassNotFoundException e) {
                    Logger.warn("Try to load class["+fileName+"] failed",e);
                }
            }
        }
        return result;
    }

    /**
     * 从指定jar 文件中寻找 CtClass
     * @param jarFile  jar 文件 File 对象
     * @param pattern  确认匹配的正则表达式
     * @return  匹配到的 class
     * @throws IOException IO 异常
     */
    public static List<CtClass> getJarCtClass(File jarFile, String pattern) throws IOException {
        if(pattern!=null) {
            pattern = pattern.replace(".", File.separator);
        }
        ArrayList<CtClass> result = new ArrayList<CtClass>();
        List<JarEntry> jarEntrys = TFile.scanJar(jarFile, pattern);
        for(JarEntry jarEntry : jarEntrys){
            String fileName = jarEntry.getName();
            if("class".equals(TFile.getFileExtension(fileName))) {
                //如果是内部类则跳过
                if (TString.regexMatch(fileName, "\\$\\d\\.class") > 0) {
                    continue;
                }

                try {
                    CtClass clazz = resourceToCtClass(fileName);
                    result.add(clazz);
                } catch (Throwable e) {
                    fileName = null;
                }
            }
        }
        return result;
    }

    /**
     * 将资源文件路径 转换成 CtClass
     * @param resourcePath 资源资源文件路径
     * @return Class对象
     * @throws ClassNotFoundException 类未找到异常
     */
    public static CtClass resourceToCtClass(String resourcePath) throws ClassNotFoundException {
        String className = null;

        if(resourcePath.startsWith(File.separator)){
            resourcePath = TString.removePrefix(resourcePath);
        }

        className = TString.fastReplaceAll(resourcePath, "\\$.*\\.class$", ".class");
        className = TString.fastReplaceAll(className, ".class$", "");

        className = TString.fastReplaceAll(className, Matcher.quoteReplacement(File.separator), ".");

        try {
            return CLASSPOOL.get(className);
        }catch (java.lang.Exception ex) {
            throw new WeaveException("WeaveUtils.resourceToCtClass load and define class " + className + " failed");
        }
    }


}
