package org.voovan.tools.aop;

import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.aop.annotation.Abnormal;
import org.voovan.tools.aop.annotation.After;
import org.voovan.tools.aop.annotation.Around;
import org.voovan.tools.aop.annotation.Before;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;

/**
 * 切面工具类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class AopUtils {
    public static ClassPool CLASSPOOL = ClassPool.getDefault();
    public static List<CutPointInfo> CUT_POINTINFO_LIST = new ArrayList<CutPointInfo>();

    /**
     * 从当前进程的Javaassist中寻找 Class
     * @param pattern  确认匹配的正则表达式
     * @param filters  过滤的 class, 满足这些条件的 class 才会被搜索到(注解,接口,继承的类)
     * @return  匹配到的 class 集合
     * @throws IOException IO 异常
     */
    public static List<CtClass> searchClassInJavassist(String pattern, Class[] filters) throws IOException {
        String userDir = System.getProperty("user.dir");
        List<String> classPaths = TEnv.getClassPath();
        ArrayList<CtClass> clazzes = new ArrayList<CtClass>();
        for(String classPath : classPaths){
            File classPathFile = new File(classPath);
            if(classPathFile.exists() && classPathFile.isDirectory()){
                clazzes.addAll( getDirectorClass(classPathFile, pattern, filters));
            } else if(classPathFile.exists() && classPathFile.isFile() && classPathFile.getName().endsWith(".jar")) {
                clazzes.addAll( getJarClass(classPathFile, pattern, filters) );
            }
        }

        return clazzes;
    }

    /**
     * 从指定 File 对象寻找 CtClass
     * @param rootfile 文件目录 File 对象
     * @param pattern  确认匹配的正则表达式
     * @param filters  过滤的 class, 满足这些条件的 class 才会被搜索到(注解,接口,继承的类)
     * @return  匹配到的 class 集合
     * @throws IOException IO 异常
     */
    public static List<CtClass> getDirectorClass(File rootfile, String pattern, Class[] filters) throws IOException {
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
                fileName = fileName.replace(rootfile.getCanonicalPath() + "/", "");
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
     * @param filters  过滤的 class, 满足这些条件的 class 才会被搜索到(注解,接口,继承的类)
     * @return  匹配到的 class
     * @throws IOException IO 异常
     */
    public static List<CtClass> getJarClass(File jarFile, String pattern, Class[] filters) throws IOException {
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

        className = TString.fastReplaceAll(className, File.separator, ".");

        try {
            return CLASSPOOL.get(className);
        }catch (Exception ex) {
            throw new ClassNotFoundException("load and define class " + className + " failed");
        }
    }

    /**
     * Javassist 扫描所有的切面注入点
     * @param scanPackage 扫描的包路径
     * @throws IOException IO 异常
     * @throws ClassNotFoundException 类未找到异常
     */
    public static void scanAopClass(String scanPackage) throws IOException, ClassNotFoundException {
        System.out.println("[AOP] Scan from package: " + scanPackage);
        List<CtClass> aopClasses = AopUtils.searchClassInJavassist(scanPackage, new Class[]{org.voovan.tools.aop.annotation.Aop.class});

        for(CtClass clazz : aopClasses){
            CtMethod[] methods = clazz.getDeclaredMethods();
            for(CtMethod cutPointMethod : methods){
                Before onBefore = (Before) cutPointMethod.getAnnotation(Before.class);
                After onAfter = (After)cutPointMethod.getAnnotation(After.class);
                Abnormal onAbnormal = (Abnormal)cutPointMethod.getAnnotation(Abnormal.class);
                Around onAround = (Around)cutPointMethod.getAnnotation(Around.class);

                if(onBefore!=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onBefore.value());
                    cutPointInfo.setType(-1);
                    cutPointInfo.setCutPointMethod(cutPointMethod);
                    cutPointInfo.setInterceptLambda(onBefore.lambda());
                    AopUtils.CUT_POINTINFO_LIST.add(cutPointInfo);
                    System.out.println("[AOP] Add cutpoint: " + JSON.toJSON(cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName()));
                }

                if(onAfter!=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onAfter.value());
                    cutPointInfo.setType(1);
                    cutPointInfo.setCutPointMethod(cutPointMethod);
                    cutPointInfo.setInterceptLambda(onAfter.lambda());
                    AopUtils.CUT_POINTINFO_LIST.add(cutPointInfo);
                    System.out.println("[AOP] Add cutpoint: " + JSON.toJSON(cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName()));
                }

                if(onAbnormal !=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onAbnormal.value());
                    cutPointInfo.setType(2);
                    cutPointInfo.setCutPointMethod(cutPointMethod);
                    cutPointInfo.setInterceptLambda(onAbnormal.lambda());
                    AopUtils.CUT_POINTINFO_LIST.add(cutPointInfo);
                    System.out.println("[AOP] Add cutpoint: " + JSON.toJSON(cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName()));
                }

                if(onAround !=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onAround.value());
                    cutPointInfo.setType(3);
                    cutPointInfo.setCutPointMethod(cutPointMethod);
                    cutPointInfo.setInterceptLambda(onAround.lambda());
                    AopUtils.CUT_POINTINFO_LIST.add(cutPointInfo);
                    System.out.println("[AOP] Add cutpoint: " + JSON.toJSON(cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName()));
                }
            }
        }
    }
}
