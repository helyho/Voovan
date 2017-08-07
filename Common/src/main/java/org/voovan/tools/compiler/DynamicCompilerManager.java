package org.voovan.tools.compiler;

import org.voovan.tools.compiler.clazz.DynamicClass;
import org.voovan.tools.compiler.function.DynamicFunction;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DynamicCompilerManager {

    private static  Map<String, DynamicFunction> functions = new ConcurrentHashMap<String, DynamicFunction>();
    private static  Map<String, DynamicClass> classes = new ConcurrentHashMap<String, DynamicClass>();

    /**
     * 增加一个动态函数
     * @param file  文件对象
     * @param charSet 字符集
     * @throws UnsupportedEncodingException 字符集异常
     * @throws ReflectiveOperationException  反射异常
     * @return DynamicFunction对象
     */
    public static DynamicFunction addFunction(File file, String charSet) throws UnsupportedEncodingException, ReflectiveOperationException {
        DynamicFunction dynamicFunction = new DynamicFunction(file, charSet);
        dynamicFunction.compileCode();;
        functions.put(dynamicFunction.getName(), dynamicFunction);
        return dynamicFunction;
    }

    /**
     * 增加一个动态函数
     * @param file 文件对象
     * @throws UnsupportedEncodingException 字符集异常
     * @throws ReflectiveOperationException 反射异常
     * @return DynamicFunction对象
     */
    public static DynamicFunction addFunction(File file) throws UnsupportedEncodingException, ReflectiveOperationException {
        DynamicFunction dynamicFunction = new DynamicFunction(file, "UTF-8");
        dynamicFunction.compileCode();
        functions.put(dynamicFunction.getName(), dynamicFunction);
        return dynamicFunction;
    }

    /**
     * 增加一个动态函数
     * @param name 动态函数名称
     * @param code 动态函数代码
     * @throws ReflectiveOperationException 反射异常
     * @return DynamicFunction对象
     */
    public static DynamicFunction addFunction(String name, String code) throws ReflectiveOperationException {
        DynamicFunction dynamicFunction = new DynamicFunction(name, code);
        dynamicFunction.compileCode();
        functions.put(dynamicFunction.getName(), dynamicFunction);
        return dynamicFunction;
    }

    /**
     * 增加一个动态函数
     * @param dynamicFunction 动态函数对象
     * @return DynamicFunction对象
     */
    public static DynamicFunction addFunction(DynamicFunction dynamicFunction) {
        functions.put(dynamicFunction.getName(), dynamicFunction);
        return dynamicFunction;
    }

    /**
     * 获取动态函数
     * @param name 动态函数名称
     * @return 动态函数对象
     */
    public static DynamicFunction getFunctions(String name){
        return functions.get(name);
    }

    /**
     * 移除一个动态函数
     * @param name 动态函数名称
     */
    public static void removeFunctions(String name){
        functions.remove(name);
    }

    /**
     * 获取动态函数集合
     * @return 键值对[key:动态函数名称, value:动态函数对象]
     */
    public static Map<String, DynamicFunction> getFunctions(){
        return functions;
    }

    /**
     * 调用动态函数
     * @param name 动态函数名称
     * @param args 动态函数调用的参数
     * @return 函数返回值
     * @throws ReflectiveOperationException 反射异常
     */
    public static Object callFunction(String name, Object ... args) throws ReflectiveOperationException {
        return functions.get(name).call(args);
    }

//====================================================================================================================

    /**
     * 增加一个动态类
     * @param file  文件对象
     * @param charSet 字符集
     * @throws UnsupportedEncodingException 字符集异常
     * @throws ReflectiveOperationException  反射异常
     * @return DynamicClass对象
     */
    public static DynamicClass addClazz(File file, String charSet) throws UnsupportedEncodingException, ReflectiveOperationException {
        DynamicClass dynamicClass = new DynamicClass(file, charSet);
        dynamicClass.compileCode();
        classes.put(dynamicClass.getName(), dynamicClass);
        return dynamicClass;
    }

    /**
     * 增加一个动态类
     * @param file  文件对象
     * @throws UnsupportedEncodingException 字符集异常
     * @throws ReflectiveOperationException  反射异常
     * @return DynamicClass对象
     */
    public static DynamicClass addClazz(File file) throws UnsupportedEncodingException, ReflectiveOperationException {
        DynamicClass dynamicClass = new DynamicClass(file, "UTF-8");
        dynamicClass.compileCode();
        classes.put(dynamicClass.getName(), dynamicClass);
        return dynamicClass;
    }

    /**
     * 增加一个动态类
     * @param name 动态类命名
     * @param code 动态类代码
     * @throws ReflectiveOperationException 反射异常
     * @return DynamicClass对象
     */
    public static DynamicClass addClazz(String name, String code) throws ReflectiveOperationException {
        DynamicClass dynamicClass = new DynamicClass(name, code);
        dynamicClass.compileCode();
        classes.put(dynamicClass.getName(), dynamicClass);
        return dynamicClass;
    }

    /**
     * 增加一个动态类
     * @param dynamicClass 动态类对象
     * @return DynamicClass对象
     */
    public static DynamicClass addClazz(DynamicClass dynamicClass) {
        classes.put(dynamicClass.getName(), dynamicClass);
        return dynamicClass;
    }

    /**
     * 获取一个动态类
     * @param name 动态类名称
     * @return  动态类对象
     */
    public static DynamicClass getClazz(String name){
        return classes.get(name);
    }

    /**
     * 移除一个动态类
     * @param name 动态类名称
     */
    public static void removeClazz(String name){
        classes.remove(name);
    }

    /**
     * 获取动态类集合
     * @return 键值对[key:动态类名称, value:动态类对象]
     */
    public static Map<String, DynamicClass> getClazzs(){
        return classes;
    }

    /**
     * 获取一个动态类的实例
     * @param name 动态类名称
     * @param args 动态类构造方法的参数
     * @param <T> 范型
     * @return 动态类的实例化对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static <T> T newInstance(String name, Object ... args) throws ReflectiveOperationException {
        return (T)TReflect.newInstance(classes.get(name).getClazz(), args);
    }

}
