package org.voovan.tools.reflect;


import org.voovan.Global;
import org.voovan.tools.*;
import org.voovan.tools.compiler.function.DynamicFunction;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.annotation.NotSerialization;
import org.voovan.tools.security.THash;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 反射工具类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TReflect {
    private class EmptyClass {
        private Object emptyField;

        private EmptyClass() {
        }

        private void emptyMethod(){
        }
    }

    public static Constructor EMPTY_CONSTRUCTOR;
    public static Field EMPTY_FIELD;
    public static Method EMPTY_METHOD;

    static {
        try {
            EMPTY_CONSTRUCTOR = EmptyClass.class.getDeclaredConstructor(TReflect.class);
            EMPTY_FIELD = EmptyClass.class.getDeclaredField("emptyField");
            EMPTY_METHOD = EmptyClass.class.getDeclaredMethod("emptyMethod");
        } catch (Exception e) {
            Logger.error("Create empty reflect object failed", e);
        }
    }

    private static Map<Integer, Field>           FIELDS               = new ConcurrentHashMap<Integer ,Field>();
    private static Map<Integer, Method>          METHODS              = new ConcurrentHashMap<Integer ,Method>();
    private static Map<Integer, Constructor>     CONSTRUCTORS         = new ConcurrentHashMap<Integer ,Constructor>();
    private static Map<Class, Field[]>           FIELD_ARRAYS         = new ConcurrentHashMap<Class ,Field[]>();
    private static Map<Integer, Method[]>        METHOD_ARRAYS        = new ConcurrentHashMap<Integer ,Method[]>();
    private static Map<Integer, Constructor[]>   CONSTRUCTOR_ARRAYS   = new ConcurrentHashMap<Integer ,Constructor[]>();
    private static Map<Class, String>            NAME_CLASS           = new ConcurrentHashMap<Class ,String>();
    private static Map<String, Class>            CLASS_NAME           = new ConcurrentHashMap<String, Class>();
    private static Map<Class, Boolean>           CLASS_BASIC_TYPE     = new ConcurrentHashMap<Class ,Boolean>();
    private  static Map<Class, DynamicFunction>  FIELD_READER         = new ConcurrentHashMap<Class, DynamicFunction>();
    private  static Map<Class, DynamicFunction>  FIELD_WRITER         = new ConcurrentHashMap<Class, DynamicFunction>();
    private  static Map<Class, DynamicFunction>  CONSTRUCTOR_INVOKE   = new ConcurrentHashMap<Class, DynamicFunction>();


    /**
     * 注册一个类, 尝试采用 native 方式进行反射调用
     * @param clazz 类对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static void register(Class clazz) throws ReflectiveOperationException {
        TReflect.genFieldReader(clazz);
        TReflect.genFieldWriter(clazz);
        TReflect.genConstructorInvoker(clazz);
    }

    /**
     * 清理所有的已经注册的 native 调用类
     */
    public static void clearRegister() {
        TReflect.FIELD_READER.clear();
        TReflect.FIELD_WRITER.clear();
        TReflect.CONSTRUCTOR_INVOKE.clear();
    }

    /**
     * 生成对象的读取的动态编译方法
     * @param clazz 目标对象类
     */
    private static void genFieldReader(Class clazz) throws ReflectiveOperationException {
        Field[] fields = getFields(clazz);

        //arg1 obj, arg2 fieldName
        String code = "switch(fieldName) {";
        for(Field field : fields) {
            code = code + "case \"" + field.getName() + "\" : ";

            String getMethodName = "get"+TString.upperCaseHead(field.getName())+"();";
            code = code + "return obj."+getMethodName + " \r\n";
        }
        code = code + "default : return null;\r\n }";

        DynamicFunction dynamicFunction = new DynamicFunction(clazz.getSimpleName()+"_READER", code);
        dynamicFunction.addImport(clazz);
        dynamicFunction.addPrepareArg(0, clazz, "obj");     //目标对象
        dynamicFunction.addPrepareArg(1, String.class, "fieldName"); //取值字段
        dynamicFunction.compileCode();

        FIELD_READER.put(clazz, dynamicFunction);
    }

    /**
     * 生成对象的写入的动态编译方法
     * @param clazz 目标对象类
     */
    private static void genFieldWriter(Class clazz) throws ReflectiveOperationException {
        Field[] fields = getFields(clazz);

        //arg1 obj, arg2 fieldName, arg3 value
        String code = "switch(fieldName) {";
        for(Field field : fields) {
            code = code + "case \"" + field.getName() + "\" : ";

            String setMethodName = "set"+TString.upperCaseHead(field.getName())+"(("+field.getType().getName()+")value); return true;";
            code = code + "obj."+setMethodName + " \r\n";
        }

        code = code + "default : return null;\r\n }";

        DynamicFunction dynamicFunction = new DynamicFunction(clazz.getSimpleName()+"_WRITER", code);
        dynamicFunction.addImport(clazz);
        dynamicFunction.addPrepareArg(0, clazz, "obj");     //目标对象
        dynamicFunction.addPrepareArg(1, String.class, "fieldName"); //写入字段
        dynamicFunction.addPrepareArg(2, Object.class, "value");     //写入数据
        dynamicFunction.compileCode();

        FIELD_WRITER.put(clazz, dynamicFunction);
    }

    /**
     * 生成构造方法的原生调用代码
     * @param clazz 根绝这个对象的元信息生成静态调用代码
     */
    private static void genConstructorInvoker(Class clazz) {
        Constructor[] constructors = getConstructors(clazz);
        String paramtypeCode = "int paramTypeLength = params==null ? 0 : params.length;\r\n\r\n";

        StringBuilder code = new StringBuilder();
        for(Constructor constructor : constructors) {
            Class[] paramTypes = constructor.getParameterTypes();
            code.append(code.length() == 0 ? "if" : "else if");
            code.append("(paramTypeLength == " + paramTypes.length );

            //参数类型匹配
            for(int i=0;i<paramTypes.length;i++) {
                Class paramClazz = paramTypes[i];
                code.append(" && params["+i+"] instanceof " + TReflect.getClassName(paramClazz));
            }

            code.append(" ) {");

            code.append("return new " + TReflect.getClassName(clazz)+"(");

            //拼装方法参数代码, 类似: (java.lang.String) params[i],
            if(paramTypes.length > 0) {
                for (int i = 0; i < paramTypes.length; i++) {
                    code.append("(" + paramTypes[i].getCanonicalName() + ") params[" + i + "],");
                }
                code.setLength(code.length() - 1);
            }

            code.append(");}\r\n");
        }

        code.insert(0, paramtypeCode);

        code.append("else { return null; }");

        DynamicFunction dynamicFunction = new DynamicFunction(clazz.getSimpleName()+"_CONSTRUCTOR", code.toString());
        dynamicFunction.addImport(TReflect.class);
        dynamicFunction.addImport(clazz);
        dynamicFunction.addPrepareArg(0, Class.class, "clazz");     //写入数据
        dynamicFunction.addPrepareArg(1, Object[].class, "params");     //写入数据

        try {
            //编译代码
            dynamicFunction.compileCode();
        } catch (ReflectiveOperationException e) {
            Logger.error("TReflect.genMethodInvoker error", e);
        }

        CONSTRUCTOR_INVOKE.put(clazz, dynamicFunction);
        if(Global.IS_DEBUG_MODE) {
            Logger.debug(code);
        }
    }

    /**
     * 通过原生调用的方式获取 Field 的值
     * @param obj  对象
     * @param fieldName field 名称
     * @param <T> field 的类型
     * @return  Field 的值
     * @throws ReflectiveOperationException 调用异常
     */
    public static <T> T getFieldValueNatvie(Object obj, String fieldName) throws ReflectiveOperationException {
        DynamicFunction dynamicFunction = FIELD_READER.get(obj.getClass());

        if(dynamicFunction == null) {
            return null;
        }

        try {
            return dynamicFunction.call(obj, fieldName);
        } catch (Exception e) {
            if (!(e instanceof ReflectiveOperationException)) {
                throw new ReflectiveOperationException(e.getMessage(), e);
            } else {
                throw  (ReflectiveOperationException) e;
            }
        }
    }

    /**
     * 通过原生调用的方式设置 Field 的值
     * @param obj 对象
     * @param fieldName field 名称
     * @param value Field 的值
     * @throws Exception 调用异常
     * @return
     */
    public static Boolean setFieldValueNatvie(Object obj, String fieldName, Object value) throws ReflectiveOperationException {
        DynamicFunction dynamicFunction = FIELD_WRITER.get(obj.getClass());

        if(dynamicFunction == null) {
            return false;
        }

        try {
            return dynamicFunction.call(obj, fieldName, value);
        } catch (Exception e) {
            if (!(e instanceof ReflectiveOperationException)) {
                throw new ReflectiveOperationException(e.getMessage(), e);
            } else {
                throw  (ReflectiveOperationException) e;
            }
        }
    }

    /**
     * 通过原生调用一个方法
     * @param clazz 对象
     * @param params 方法参数
     * @param <T> 方法返回值的范型
     * @return 方法返回值
     * @throws Exception 调用异常
     */
    public static <T> T newInstanceNative(Class clazz, Object[] params) throws Exception {

        DynamicFunction dynamicFunction = CONSTRUCTOR_INVOKE.get(clazz);

        if(dynamicFunction == null) {
            return null;
        }

        try {
            return dynamicFunction.call(clazz, params);
        } catch (Exception e) {
            if (!(e instanceof ReflectiveOperationException)) {
                throw new ReflectiveOperationException(e.getMessage(), e);
            } else {
                throw  (ReflectiveOperationException) e;
            }
        }
    }

    /**
     * 根据 Class 对象获取类的完全现定名
     * @param clazz Class 对象
     * @return 类的完全现定名
     */
    public static String getClassName(Class clazz){
        String canonicalName = NAME_CLASS.get(clazz);
        if(canonicalName == null){
            canonicalName = clazz.getCanonicalName();
            NAME_CLASS.put(clazz, canonicalName);
        }

        return canonicalName;
    }

    /**
     * 根据类的完全现定名, 获取 Class
     * @param className 类的完全现定名
     * @return Class 对象
     * @throws ClassNotFoundException 类没有找到的异常
     */
    public static Class getClassByName(String className) throws ClassNotFoundException {
        Class clazz = CLASS_NAME.get(className);
        if(clazz == null){
            clazz = Class.forName(className);
            CLASS_NAME.put(className, clazz);
        }

        return clazz;
    }

    /**
     * 获得类所有的Field
     *
     * @param clazz 类对象
     * @return Field数组
     */
    public static Field[] getFields(Class<?> clazz) {
        Field[] fields = FIELD_ARRAYS.get(clazz);

        Class loopClazz = clazz;
        if(fields==null){
            LinkedHashSet<Field> fieldArray = new LinkedHashSet<Field>();
            for (; loopClazz!=null && loopClazz != Object.class; loopClazz = loopClazz.getSuperclass()) {
                Field[] tmpFields = loopClazz.getDeclaredFields();
                for (Field field : tmpFields){
                    field.setAccessible(true);
                }
                fieldArray.addAll(Arrays.asList(tmpFields));
            }

            fields = fieldArray.toArray(new Field[]{});

            if(clazz!=null) {
                FIELD_ARRAYS.put(clazz, fields);
                fieldArray.clear();
            }
        }

        return fields;
    }

    private static Integer getFieldMark(Class<?> clazz, String fieldName) {
        return THash.HashFNV1(clazz.getName()) ^ THash.HashFNV1(fieldName);
    }

    /**
     * 查找类特定的Field
     *
     * @param clazz   类对象
     * @param fieldName field 名称
     * @return field 对象
     */
    public static Field findField(Class<?> clazz, String fieldName) {

        Integer mark = getFieldMark(clazz, fieldName);

        Field field = FIELDS.get(mark);

        Class loopClazz = clazz;

        if(field==null){

            for (; loopClazz!=null && loopClazz != Object.class; loopClazz = loopClazz.getSuperclass()) {
                try {
                    field = loopClazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    break;
                } catch(ReflectiveOperationException e){
                    field = null;
                }
            }

            if(mark!=null) {
                field = field == null ? EMPTY_FIELD : field;
                FIELDS.put(mark, field);
            }

        }

        return field == EMPTY_FIELD ? null : field;
    }

    /**
     * 查找类特定的Field
     * 			不区分大小写,并且替换掉特殊字符
     * @param clazz   类对象
     * @param fieldName Field 名称
     * @return Field 对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static Field findFieldIgnoreCase(Class<?> clazz, String fieldName) {

        Integer marker = getFieldMark(clazz, fieldName);

        Field field = FIELDS.get(marker);
        if (field==null){
            for (Field fieldItem : getFields(clazz)) {
                if (fieldItem.getName().equalsIgnoreCase(fieldName) || fieldItem.getName().equalsIgnoreCase(TString.underlineToCamel(fieldName))) {
                    if(marker!=null) {
                        fieldItem.setAccessible(true);
                        field = fieldItem;
                        break;
                    }

                }
            }

            field = field == null ? EMPTY_FIELD : field;
            FIELDS.put(marker, field);
        }
        return field == EMPTY_FIELD ? null : field;
    }

    /**
     * 获取类型的范型类型
     * @param type 类型对象
     * @return Class[] 对象
     */
    public static Class[] getGenericClass(Type type) {
        ParameterizedType parameterizedType = null;
        if(type instanceof ParameterizedType) {
            parameterizedType = (ParameterizedType) type;
        }

        if(parameterizedType==null){
            return null;
        }

        Class[] result = null;
        Type[] actualType = parameterizedType.getActualTypeArguments();
        result = new Class[actualType.length];

        for(int i=0;i<actualType.length;i++){
            if(actualType[i] instanceof Class){
                result[i] = (Class)actualType[i];
            } else if(actualType[i] instanceof Type){
                String classStr = actualType[i].toString();
                classStr = TString.fastReplaceAll(classStr, "<.*>", "");
                try {
                    result[i] = Class.forName(classStr);
                } catch(Exception e){
                    result[i] = Object.class;
                }
            } else{
                result[i] = Object.class;
            }
        }
        return result;
    }
    /**
     * 获取对象的范型类型
     * @param object 对象
     * @return Class[] 对象
     */
    public static Class[] getGenericClass(Object object) {
        Class[] genericClazzs = TReflect.getGenericClass(object.getClass());

        if (genericClazzs == null) {
            if (object instanceof Map) {
                if (((Map) object).size() > 0) {
                    Map.Entry entry = (Map.Entry) ((Map) object).entrySet().iterator().next();
                    genericClazzs = new Class[]{entry.getKey().getClass(), entry.getValue().getClass()};
                }
            } else if (object instanceof Collection) {
                if (((Collection) object).size() > 0) {
                    Object obj = ((Collection) object).iterator().next();
                    genericClazzs = new Class[]{obj.getClass()};
                }
            }
        }

        return genericClazzs;
    }

    /**
     * 获取 Field 的范型类型
     * @param field  field 对象
     * @return 返回范型类型数组
     */
    public static Class[] getFieldGenericType(Field field) {
        Type fieldType = field.getGenericType();
        return getGenericClass((ParameterizedType)fieldType);
    }

    /**
     * 获取类中指定Field的值
     * @param <T> 范型
     * @param obj  对象
     * @param fieldName Field 名称
     * @return Field 的值
     * @throws ReflectiveOperationException 反射异常
     */
    @SuppressWarnings("unchecked")
    static public <T> T getFieldValue(Object obj, String fieldName)
            throws ReflectiveOperationException {
        //尝试 native 方法
        T t = getFieldValueNatvie(obj, fieldName);
        if(t==null) {
            Field field = findField(obj.getClass(), fieldName);
            t = (T) field.get(obj);
        }

        return t;
    }


    /**
     * 更新对象中指定的Field的值
     * 		注意:对 private 等字段有效
     *
     * @param obj  对象
     * @param field field 对象
     * @param fieldValue field 值
     * @throws ReflectiveOperationException 反射异常
     */
    public static void setFieldValue(Object obj, Field field, Object fieldValue)
            throws ReflectiveOperationException {

        field.set(obj, fieldValue);
    }

    /**
     * 更新对象中指定的Field的值
     * 		注意:对 private 等字段有效
     *
     * @param obj  对象
     * @param fieldName field 名称
     * @param fieldValue field 值
     * @throws ReflectiveOperationException 反射异常
     */
    public static void setFieldValue(Object obj, String fieldName, Object fieldValue)
            throws ReflectiveOperationException {
        //尝试 native 方法
        boolean isSucc = setFieldValueNatvie(obj, fieldName, fieldValue);
        if(!isSucc) {
            Field field = findField(obj.getClass(), fieldName);
            setFieldValue(obj, field, fieldValue);
        }
    }

    /**
     * 将对象中的field和其值组装成Map 静态字段(static修饰的)不包括
     *
     * @param obj 对象
     * @return 所有 field 名称-值拼装的 Map
     * @throws ReflectiveOperationException 反射异常
     */
    public static Map<Field, Object> getFieldValues(Object obj)
            throws ReflectiveOperationException {
        HashMap<Field, Object> result = new HashMap<Field, Object>();
        Field[] fields = getFields(obj.getClass());
        for (Field field : fields) {

            //静态属性不序列化
            if((field.getModifiers() & 0x00000008) != 0){
                continue;
            }

            Object value = field.get(obj);
            result.put(field, value);
        }
        return result;
    }

    private static int getMethodParamTypeMark(Class<?> clazz, String name, Class<?>... paramTypes) {
        int hashCode = THash.HashFNV1(clazz.getName()) ^ THash.HashFNV1(name);
        for(Class<?> paramType : paramTypes){
            hashCode = hashCode ^ THash.HashFNV1(paramType.getName());
        }
        return hashCode;
    }

    /**
     * 查找类中的方法
     * @param clazz        类对象
     * @param name		   方法名
     * @param paramTypes   参数类型
     * @return			   方法对象
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Integer marker = getMethodParamTypeMark(clazz, name, paramTypes);

        Method method = METHODS.get(marker);

        if (method==null){

            for (; clazz!=null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                try {
                    method = clazz.getDeclaredMethod(name, paramTypes);
                    method.setAccessible(true);
                    break;
                } catch(ReflectiveOperationException e){
                    method = null;
                }
            }

            if(marker!=null) {
                method = method == null ? EMPTY_METHOD : method;
                METHODS.put(marker, method);
            }
        }

        return method == EMPTY_METHOD ? null : method;
    }

    private static int getMethodParamCountMark(Class<?> clazz, String name, int paramCount) {
        int hashCode = THash.HashFNV1(clazz.getName()) ^ THash.HashFNV1(name) ^ paramCount;
        return hashCode;
    }

    /**
     * 查找类中的方法(使用参数数量)
     * @param clazz        类对象
     * @param name		   方法名
     * @param paramCount   参数数量
     * @return			   方法对象
     */
    public static Method[] findMethod(Class<?> clazz, String name, int paramCount) {
        Integer marker = getMethodParamCountMark(clazz, name, paramCount);

        Method[] methods = METHOD_ARRAYS.get(marker);

        if (methods==null){
            LinkedHashSet<Method> methodList = new LinkedHashSet<Method>();
            Method[] allMethods = getMethods(clazz, name);
            for (Method method : allMethods) {
                if (method.getParameterTypes().length == paramCount) {
                    method.setAccessible(true);
                    methodList.add(method);
                }
            }

            methods = methodList.toArray(new Method[]{});

            if(marker!=null) {
                METHOD_ARRAYS.put(marker, methods);
                methodList.clear();
            }
        }

        return methods;
    }

    /**
     * 获取类的方法集合
     * @param clazz		类对象
     * @return Method 对象数组
     */
    public static Method[] getMethods(Class<?> clazz) {

        Method[] methods = null;

        Integer marker = clazz.hashCode();

        methods = METHOD_ARRAYS.get(marker);

        if(methods==null){
            LinkedHashSet<Method> methodList = new LinkedHashSet<Method>();
            for (; clazz!=null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                Method[] tmpMethods = clazz.getDeclaredMethods();
                for(Method method : tmpMethods){
                    method.setAccessible(true);
                }
                methodList.addAll(Arrays.asList(tmpMethods));
            }

            methods = methodList.toArray(new Method[]{});

            if(marker!=null) {
                METHOD_ARRAYS.put(marker, methods);
                methodList.clear();
            }

        }

        return methods;
    }

    private static int getMethodMark(Class<?> clazz, String name) {
        int hashCode = THash.HashFNV1(clazz.getName()) ^ THash.HashFNV1(name);
        return hashCode;
    }

    /**
     * 获取类的特定方法的集合
     * 		类中可能存在同名方法
     * @param clazz		类对象
     * @param name		方法名
     * @return Method 对象数组
     */
    public static Method[] getMethods(Class<?> clazz, String name) {

        Method[] methods = null;

        Integer marker = getMethodMark(clazz, name);
        methods = METHOD_ARRAYS.get(marker);
        if(methods==null){

            LinkedHashSet<Method> methodList = new LinkedHashSet<Method>();
            Method[] allMethods = getMethods(clazz);
            for (Method method : allMethods) {
                if (method.getName().equals(name))
                    methodList.add(method);
            }

            methods = methodList.toArray(new Method[0]);

            if(marker!=null) {
                METHOD_ARRAYS.put(marker, methods);
                methodList.clear();
            }
        }

        return methods;
    }

    /**
     * 获取方法的参数返回值的范型类型
     * @param method  method 对象
     * @param parameterIndex 参数索引(大于0)参数索引位置[第一个参数为0,以此类推], (-1) 返回值
     * @return 返回范型类型数组
     */
    public static Class[] getMethodParameterGenericType(Method method, int parameterIndex) {
        Class[] result = null;
        Type parameterType;

        if(parameterIndex == -1){
            parameterType = method.getGenericReturnType();
        }else{
            parameterType = method.getGenericParameterTypes()[parameterIndex];
        }

        return getGenericClass(parameterType);
    }

    /**
     * 使用对象执行它的一个方法
     * 		对对象执行一个指定Method对象的方法
     * @param obj				执行方法的对象
     * @param method			方法对象
     * @param parameters        多个参数
     * @param <T> 范型
     * @return					方法返回结果
     * @throws ReflectiveOperationException 反射异常
     */
    public static <T> T invokeMethod(Object obj, Method method, Object... parameters)
            throws ReflectiveOperationException {
        return (T)method.invoke(obj, parameters);
    }

    /**
     * 使用对象执行方法
     * 推荐使用的方法,这个方法会自动寻找参数匹配度最好的方法并执行
     * 对对象执行一个通过 方法名和参数列表选择的方法
     * @param obj				执行方法的对象,如果调用静态方法直接传 Class 类型的对象
     * @param methodName		执行方法名
     * @param args		        方法参数
     * @param <T>               范型
     * @return					方法返回结果
     * @throws ReflectiveOperationException		反射异常
     */
    public static <T> T invokeMethod(Object obj, String methodName, Object... args)
            throws ReflectiveOperationException {
        if(args==null){
            args = new Object[0];
        }

        Exception exception = null;
        Class<?>[] paramTypes = getArrayClasses(args);

        Class objClass = (obj instanceof Class) ? (Class)obj : obj.getClass();
        Method method = findMethod(obj.getClass(), methodName, paramTypes);

        if(method!=null) {
            return (T) method.invoke(obj, args);
        } else {
            Method[] methods = null;
            methods = findMethod(objClass, methodName, args.length);

            //如果失败循环尝试各种相同类型的方法
            for(Method methodItem : methods) {
                try {
                    T result = (T) methodItem.invoke(obj, args);
                    //匹配到合适则加入缓存
                    METHODS.put(getMethodParamTypeMark(objClass, methodName, paramTypes), methodItem);
                    return result;
                } catch (Exception e) {
                    Logger.warn("Method: " + getClassName(objClass) + "#" +methodName + "("+args.length+") reflect invoke, can be use strict parameter type to improve performance");

                    if(Global.IS_DEBUG_MODE) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (!(exception instanceof ReflectiveOperationException)) {
            exception = new ReflectiveOperationException(exception.getMessage(), exception);
        } else {
            exception = (ReflectiveOperationException) exception;
        }

        throw (ReflectiveOperationException)exception;
    }

    public static int getConstructorParamTypeMark(Class<?> clazz, Class<?>... paramTypes) {
        int hashCode = THash.HashFNV1(clazz.getName());
        for(Class<?> paramType : paramTypes){
            hashCode = hashCode ^ THash.HashFNV1(paramType.getName());
        }
        return hashCode;
    }

    /**
     * 查找类中的构造方法
     * @param clazz        类对象
     * @param paramTypes   参数类型
     * @return			   方法对象
     */
    public static Constructor findConstructor(Class<?> clazz, Class<?>... paramTypes) {
        Integer marker = getConstructorParamTypeMark(clazz, paramTypes);

        Constructor constructor = CONSTRUCTORS.get(marker);

        if (constructor==null){

            try {
                constructor = clazz.getDeclaredConstructor(paramTypes);
                if(constructor!=null) {
                    constructor.setAccessible(true);
                }
            } catch (NoSuchMethodException e) {
                constructor = null;
            }
        }

        if(marker!=null) {
            constructor = constructor == null ? EMPTY_CONSTRUCTOR : constructor;
            CONSTRUCTORS.put(marker, constructor);
        }

        return constructor == EMPTY_CONSTRUCTOR ? null : constructor;
    }

    private static int getConstructorParamCountMark(Class<?> clazz, int paramCount) {
        int hashCode = THash.HashFNV1(clazz.getName()) ^ paramCount;
        return hashCode;
    }

    /**
     * 查找类中的构造方法(使用参数数量)
     * @param clazz        类对象
     * @param paramCount   参数数量
     * @return			   方法对象
     */
    public static Constructor[] findConstructor(Class<?> clazz, int paramCount) {
        Integer marker = getConstructorParamCountMark(clazz, paramCount);

        Constructor[] constructors = CONSTRUCTOR_ARRAYS.get(marker);

        if (constructors==null){
            LinkedHashSet<Constructor> constructorList = new LinkedHashSet<Constructor>();
            Constructor[] allConstructor = getConstructors(clazz);
            for (Constructor constructor : allConstructor) {
                if (constructor.getParameterTypes().length == paramCount) {
                    constructor.setAccessible(true);
                    constructorList.add(constructor);
                }
            }

            constructors = constructorList.toArray(new Constructor[]{});

            if(marker!=null) {
                CONSTRUCTOR_ARRAYS.put(marker, constructors);
                constructorList.clear();
            }
        }

        return constructors;
    }

    /**
     * 获取类的构造方法集合
     * @param clazz		类对象
     * @return Method 对象数组
     */
    public static Constructor[] getConstructors(Class<?> clazz) {

        Constructor[] constructors = null;

        Integer marker = clazz.hashCode();

        constructors = CONSTRUCTOR_ARRAYS.get(marker);

        if(constructors==null){
            LinkedHashSet<Constructor> constructorList = new LinkedHashSet<Constructor>();
            Constructor[] allConstructors = clazz.getDeclaredConstructors();
            for(Constructor constructor : allConstructors){
                constructor.setAccessible(true);
            }
            constructorList.addAll(Arrays.asList(allConstructors));

            constructors = constructorList.toArray(new Constructor[]{});

            if(marker!=null) {
                CONSTRUCTOR_ARRAYS.put(marker, constructors);
                CONSTRUCTOR_ARRAYS.clear();
            }
        }

        return constructors;
    }

    /**
     * 构造新的对象
     * 	通过参数中的构造参数对象parameters,选择特定的构造方法构造
     * @param <T>           范型
     * @param clazz			类对象
     * @param args	构造方法参数
     * @return 新的对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static <T> T newInstance(Class<T> clazz, Object ...args)
            throws ReflectiveOperationException {

        Class targetClazz = clazz;

        //不可构造的类型使用最常用的类型
        if(isImpByInterface(clazz, List.class) && (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers()))){
            targetClazz = ArrayList.class;
        }

        //不可构造的类型使用最常用的类型
        if(isImpByInterface(clazz, Set.class) && (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers()))){
            targetClazz = LinkedHashSet.class;
        }

        //不可构造的类型使用最常用的类型
        if(isImpByInterface(clazz, Map.class) && (Modifier.isAbstract(clazz.getModifiers()) && Modifier.isInterface(clazz.getModifiers()))){
            targetClazz = LinkedHashMap.class;
        }

        if(args==null){
            args = new Object[0];
        }

        T result = null;
        Exception exception = null;

        //尝试 native 方法
        try {
            result = (T)newInstanceNative(targetClazz, args);
        } catch (Exception e) {
            Logger.warn("Constructor: " + getClassName(targetClazz) + "("+args.length+") native invoke, can be use strict parameter type to improve performance");

            if(Global.IS_DEBUG_MODE) {
                e.printStackTrace();
            }
        }

        if(result != null) {
            return result;
        }

        Class<?>[] paramTeypes = getArrayClasses(args);
        Constructor constructor = findConstructor(targetClazz, paramTeypes);

        if(constructor!=null) {
            return (T) constructor.newInstance(args);
        } else {
            //如果失败循环尝试各种构造函数构造
            Constructor[] constructors = null;
            constructors = findConstructor(targetClazz, args.length);
            for(Constructor constructorItem : constructors) {
                try {
                     result = (T) constructorItem.newInstance(args);
                    //匹配到合适的则加入缓存
                    CONSTRUCTORS.put(getConstructorParamTypeMark(clazz, paramTeypes), constructorItem);

                    return result;
                } catch (Exception e) {
                    Logger.warn("Constructor: " + getClassName(targetClazz) + "("+args.length+") reflect invoke, can be use strict parameter type to improve performance");
                    if(Global.IS_DEBUG_MODE) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //全都失败用 unsafe 构造对象
        try {
            return (T) TUnsafe.getUnsafe().allocateInstance(targetClazz);
        } catch (Exception e) {
            if (!(e instanceof ReflectiveOperationException)) {
                exception = new ReflectiveOperationException(e.getMessage(), e);
            } else {
                exception = (ReflectiveOperationException) e;
            }

            throw (ReflectiveOperationException)exception;
        }
    }

    /**
     * 构造新的对象
     * @param <T> 范型
     * @param className		类名称
     * @param parameters	构造方法参数
     * @return 新的对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static <T> T newInstance(String className, Object ...parameters) throws ReflectiveOperationException {
        Class<T> clazz = getClassByName(className);
        return newInstance(clazz, parameters);
    }

    /**
     * 采用 Unsafe 构造一个对象,无须参数
     * @param clazz 对象类型
     * @param <T> 范型
     * @return 新的对象
     * @throws InstantiationException 实例化异常
     */
    public static <T> T allocateInstance(Class<T> clazz) throws InstantiationException {
        return (T) TUnsafe.getUnsafe().allocateInstance(clazz);
    }

    /**
     * 将对象数组转换成,对象类型的数组
     * @param objs	对象类型数组
     * @return 类数组
     */
    public static Class<?>[] getArrayClasses(Object[] objs){
        if(objs == null){
            return new Class<?>[0];
        }

        Class<?>[] parameterTypes= new Class<?>[objs.length];
        for(int i=0;i<objs.length;i++){
            if(objs[i]==null){
                parameterTypes[i] = Object.class;
            }else {
                parameterTypes[i] = objs[i].getClass();
            }
        }
        return parameterTypes;
    }

    /**
     * 将Map转换成指定的对象
     *
     * @param type			类对象
     * @param mapArg		Map 对象
     * @param ignoreCase    匹配属性名是否不区分大小写
     * @return 转换后的对象
     * @param <T> 范型
     * @throws ReflectiveOperationException 反射异常
     * @throws ParseException 解析异常
     */
    public static <T>T getObjectFromMap(Type type, Map<String, ?> mapArg,  boolean ignoreCase) throws ParseException, ReflectiveOperationException {
        Class[] genericType = null;

        if(type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            genericType = getGenericClass(parameterizedType);
        }

        return getObjectFromMap(type, mapArg, genericType, ignoreCase);
    }

    //只有值的 Map 的键数据
    public final static Object SINGLE_VALUE_KEY = new Object();

    /**
     * 将Map转换成指定的对象
     *
     * @param type			类对象
     * @param mapArg		Map 对象
     * @param genericType   范型类型描述
     * @param ignoreCase    匹配属性名是否不区分大小写
     * @return 转换后的对象
     * @param <T> 范型
     * @throws ReflectiveOperationException 反射异常
     * @throws ParseException 解析异常
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T>T getObjectFromMap(Type type, Map<String, ?> mapArg, Class[] genericType,  boolean ignoreCase)
            throws ReflectiveOperationException, ParseException {
        T obj = null;
        Class<?> clazz = null;
        if(type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            clazz = (Class)parameterizedType.getRawType();
        }else if(type instanceof Class){
            clazz = (Class)type;
        }

        if(mapArg==null){
            return null;
        }

        Object singleValue = mapArg;

        if(mapArg.containsKey(SINGLE_VALUE_KEY)){
            singleValue = mapArg.get(SINGLE_VALUE_KEY);
        } else if(mapArg.size() == 1) {
            singleValue = mapArg.values().iterator().next();
        }

        //对象类型
        if(clazz == Object.class){
            if(mapArg.containsKey(SINGLE_VALUE_KEY)) {
                obj = (T) singleValue;
            } else {
                obj = (T) mapArg;
            }
        }

        //java标准对象
        else if (clazz.isPrimitive()){
            if(singleValue!=null && singleValue.getClass() !=  clazz) {
                obj = TString.toObject(singleValue.toString(), clazz);
            } else {
                obj = (T)singleValue;
            }
        }
        //java基本对象
        else if (TReflect.isBasicType(clazz)) {
            //取 Map.Values 里的递第一个值
            obj = (T)(singleValue==null ? null : newInstance(clazz,  singleValue.toString()));
        }
        //java BigDecimal对象
        else if (clazz == BigDecimal.class) {
            //取 Map.Values 里的递第一个值
            String value = singleValue==null ? null:singleValue.toString();
            obj = (T)(singleValue==null ? null : new BigDecimal(value));
        }
        //对 Atom 类型的处理
        else if (clazz == AtomicLong.class || clazz == AtomicInteger.class || clazz == AtomicBoolean.class) {
            if(singleValue==null){
                obj = null;
            } else {
                obj = (T) TReflect.newInstance(clazz, singleValue);
            }
        }
        //java 日期对象
        else if(isExtendsByClass(clazz, Date.class)){
            //取 Map.Values 里的递第一个值
            String value = singleValue == null ? null : singleValue.toString();
            SimpleDateFormat dateFormat = new SimpleDateFormat(TDateTime.STANDER_DATETIME_TEMPLATE);
            Date dateObj = singleValue != null ? dateFormat.parse(value.toString()) : null;
            obj = (T)TReflect.newInstance(clazz,dateObj.getTime());
        }
        //Map 类型
        else if(isImpByInterface(clazz, Map.class)){
            Map mapObject = (Map)newInstance(clazz);

            if(genericType!=null) {
                Iterator iterator = mapArg.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry entry = (Entry) iterator.next();
                    Map keyOfMap = null;
                    Map valueOfMap = null;

                    if (entry.getKey() instanceof Map) {
                        keyOfMap = (Map) entry.getKey();
                    } else {
                        keyOfMap = TObject.asMap(SINGLE_VALUE_KEY, entry.getKey());
                    }

                    if (entry.getValue() instanceof Map) {
                        valueOfMap = (Map) entry.getValue();
                    } else {
                        valueOfMap = TObject.asMap(SINGLE_VALUE_KEY, entry.getValue());
                    }

                    Object keyObj = getObjectFromMap(genericType[0], keyOfMap, ignoreCase);
                    Object valueObj = getObjectFromMap(genericType[1], valueOfMap, ignoreCase);
                    mapObject.put(keyObj, valueObj);
                }
            }else{
                mapObject.putAll(mapArg);
            }
            obj = (T)mapObject;
        }
        //Collection 类型
        else if(isImpByInterface(clazz, Collection.class)){
            Collection collectionObject = (Collection)newInstance(clazz);

            if(singleValue!=null){
                if(genericType!=null){
                    for (Object listItem : (Collection)singleValue) {
                        Map valueOfMap = null;
                        if (listItem instanceof Map) {
                            valueOfMap = (Map) listItem;
                        } else {
                            valueOfMap = TObject.asMap(SINGLE_VALUE_KEY, listItem);
                        }

                        Object item = getObjectFromMap(genericType[0], valueOfMap, ignoreCase);
                        collectionObject.add(item);
                    }
                }else{
                    collectionObject.addAll((Collection)singleValue);
                }
            }
            obj = (T)collectionObject;
        }
        //Array 类型
        else if(clazz.isArray()){
            Class arrayClass = clazz.getComponentType();
            Object tempArrayObj = Array.newInstance(arrayClass, 0);
            return (T)((Collection)singleValue).toArray((Object[])tempArrayObj);
        }
        // 复杂对象
        else {
            try {
                obj = (T) newInstance(clazz);
            } catch (InstantiationException e){
                return null;
            }
            for(Entry<String,?> argEntry : mapArg.entrySet()){
                String key = argEntry.getKey();
                Object value = argEntry.getValue();

                Field field = null;
                if(ignoreCase) {
                    //忽略大小写匹配
                    field = findFieldIgnoreCase(clazz, key);
                }else{
                    //精确匹配属性
                    field = findField(clazz, key);
                }

                if(field!=null && !Modifier.isFinal(field.getModifiers())) {
                    String fieldName = field.getName();
                    Class fieldType = field.getType();
                    Type fieldGenericType = field.getGenericType();
                    try {

                        //value 和 fieldType class类型不同时，且　value 不为空时处理
                        if(value != null && fieldType != value.getClass()) {
                            //通过 JSON 将,String类型的 value转换,将 String 转换成 Collection, Map 或者 复杂类型 对象作为参数
                            if( value instanceof String &&
                                    (
                                            isImpByInterface(fieldType, Map.class) ||
                                                    isImpByInterface(fieldType, Collection.class) ||
                                                    !TReflect.isBasicType(fieldType)
                                    )
                            ){
                                value = TString.toObject(value.toString(), fieldType);
                            }

                            //对于 目标对象类型为 Map 的属性进行处理,查找范型,并转换为范型定义的类型
                            else if (isImpByInterface(fieldType, Map.class) && value instanceof Map) {
                                value = getObjectFromMap(fieldGenericType, (Map<String,?>)value, ignoreCase);
                            }
                            //对于 目标对象类型为 Collection 的属性进行处理,查找范型,并转换为范型定义的类型
                            else if (isImpByInterface(fieldType, Collection.class) && value instanceof Collection) {
                                value = getObjectFromMap(fieldGenericType, TObject.asMap(SINGLE_VALUE_KEY, value), ignoreCase);
                            }
                            //对于 目标对象类型不是 Map,则认定为复杂类型
                            else if (!isImpByInterface(fieldType, Map.class)) {
                                if (value instanceof Map) {
                                    value = getObjectFromMap(fieldType, (Map<String, ?>) value, ignoreCase);
                                } else {
                                    value = getObjectFromMap(fieldType, TObject.asMap(SINGLE_VALUE_KEY, value), ignoreCase);
                                }
                            }else{
                                throw new ReflectiveOperationException("Conver field object error! Exception type: " +
                                        fieldType.getName() +
                                        ", Object type: "+
                                        value.getClass().getName());
                            }
                        }
                        setFieldValue(obj, field, value);
                    }catch(Exception e){
                        throw new ReflectiveOperationException("Fill object " + getClassName(obj.getClass()) +
                                Global.CHAR_SHAPE+fieldName+" failed", e);
                    }
                }
            }
        }
        return obj;
    }

    /**
     * 将对象转换成 Map
     * 			key 对象属性名称
     * 			value 对象属性值
     * @param obj      待转换的对象
     * @return 转后的 Map
     * @throws ReflectiveOperationException 反射异常
     */
    public static Map<String, Object> getMapfromObject(Object obj) throws ReflectiveOperationException{
        return getMapfromObject(obj, false);
    }

    /**
     * 将对象转换成 Map
     * 			key 对象属性名称
     * 			value 对象属性值
     * @param obj      待转换的对象
     * @param allField 是否序列化所有属性
     * @return 转后的 Map
     * @throws ReflectiveOperationException 反射异常
     */
    public static Map<String, Object> getMapfromObject(Object obj, boolean allField) throws ReflectiveOperationException {
        LinkedHashMap<String, Object> mapResult = new LinkedHashMap<String, Object>();

        if(obj==null || TReflect.isBasicType(obj.getClass())){
            mapResult.put(null, obj);

            return mapResult;
        }

        if(obj.getClass().isAnnotationPresent(NotSerialization.class)){
            return null;
        }

        //如果是 java 标准类型
        if(TReflect.isBasicType(obj.getClass())){
            mapResult.put(null, obj);
        }
        //java 日期对象
        else if(isExtendsByClass(obj.getClass(),Date.class)){
            mapResult.put(null,TDateTime.format((Date) obj, TDateTime.STANDER_DATETIME_TEMPLATE));
        }
        //对 Collection 类型的处理
        else if(obj instanceof Collection){
            Collection collection = new ArrayList();
            for (Object collectionItem : (Collection)obj) {
                Map<String, Object> item = getMapfromObject(collectionItem, allField);
                collection.add((item.size() == 1 && item.containsKey(null)) ? item.get(null) : item);
            }
            mapResult.put(null, collection);
        }
        //对 Array 类型的处理
        else if(obj.getClass().isArray()){
            Class arrayClass = obj.getClass().getComponentType();
            Object targetArray = Array.newInstance(arrayClass, Array.getLength(obj));

            for(int i=0;i<Array.getLength(obj);i++) {
                Object arrayItem = Array.get(obj, i);
                Map<String, Object> item = getMapfromObject(arrayItem, allField);
                Array.set(targetArray, i, (item.size()==1 && item.containsKey(null)) ? item.get(null) : item);
            }
            mapResult.put(null, targetArray);
        }
        //对 Atom 类型的处理
        else if (obj instanceof AtomicLong || obj instanceof AtomicInteger || obj instanceof AtomicBoolean) {
            mapResult.put(null, TReflect.invokeMethod(obj, "get"));
        }
        //对 BigDecimal 类型的处理
        else if (obj instanceof BigDecimal) {
            if(BigDecimal.ZERO.compareTo((BigDecimal)obj)==0){
                obj = BigDecimal.ZERO;
            }
            mapResult.put(null, ((BigDecimal) obj).toPlainString());
        }
        //对 Map 类型的处理
        else if(obj instanceof Map){
            Map mapObject = (Map)obj;

            Map map = new HashMap();
            Iterator iterator = mapObject.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<?, ?> entry = (Entry<?, ?>) iterator.next();
                Map<String, Object> keyItem = getMapfromObject(entry.getKey(), allField);
                Map<String, Object> valueItem = getMapfromObject(entry.getValue(), allField);
                Object key = (keyItem.size() == 1 && keyItem.containsKey(null)) ? keyItem.get(null) : keyItem;
                Object value = (valueItem.size() == 1 && valueItem.containsKey(null)) ? valueItem.get(null) : valueItem;
                map.put(key, value);
            }
            mapResult.put(null, map);
        }
        //复杂对象类型
        else {
            Field[] fields =  TReflect.getFields(obj.getClass());
            for(Field field : fields){

                //过滤不可序列化的字段
                if (!allField) {
                    if(field.isAnnotationPresent(NotSerialization.class)) {
                        continue;
                    }
                }

                String key = field.getName();
                Object value = null;
                try {
                    value = getFieldValue(obj, key);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if(value == null){
                    //由于属性是按子类->父类顺序处理的, 所以如果子类和父类有重复属性, 则只在子类为空时用父类的属性覆盖
                    if(mapResult.get(key) == null) {
                        mapResult.put(key, value);
                    }
                }else if(!key.contains("$")){
                    Class valueClass = value.getClass();
                    if(TReflect.isBasicType(valueClass)){
                        //由于属性是按子类->父类顺序处理的, 所以如果子类和父类有重复属性, 则只在子类为空时用父类的属性覆盖
                        if(mapResult.get(key) == null) {
                            mapResult.put(key, value);
                        }
                    }else {
                        //如果是复杂类型则递归调用
                        Map resultMap = getMapfromObject(value, allField);
                        if(resultMap.size()==1 && resultMap.containsKey(null)){
                            mapResult.put(key, resultMap.get(null));
                        }else{
                            mapResult.put(key, resultMap);
                        }
                    }
                }
            }
        }
        return mapResult;
    }

    /**
     * 判断某个类型是否实现了某个接口
     * 		包括判断其父接口
     * @param type               被判断的类型
     * @param interfaceClass     检查是否实现了次类的接口
     * @return 是否实现某个接口
     */
    public static boolean isImpByInterface(Class<?> type,Class<?> interfaceClass){
        if(type==interfaceClass && interfaceClass.isInterface()){
            return true;
        }

        return interfaceClass.isAssignableFrom(type);
    }


    /**
     * 判断某个类型是否继承于某个类
     * 		包括判断其父类
     * @param type			判断的类型
     * @param extendsClass	用于判断的父类类型
     * @return 是否继承于某个类
     */
    public static boolean isExtendsByClass(Class<?> type,Class<?> extendsClass){
        if(type==extendsClass && !extendsClass.isInterface()){
            return true;
        }

        return extendsClass.isAssignableFrom(type);
    }

    /**
     * 类检查器
     * 		是否符合 filters 中的约束条件, 注解/类/接口等
     * @param clazz    Class 对象
     * @param filters  过滤器
     * @return true: 符合约束, false: 不符合约束
     */
    public static boolean classChecker(Class clazz, Class[] filters){
        int matchCount = 0;
        List<Annotation> annotations = TObject.asList(clazz.getAnnotations());

        if(clazz.isAnonymousClass()) {
            return false;
        }

        for(Class filterClazz : filters){
            if(clazz == filterClazz){
                break;
            }

            if(filterClazz.isAnnotation() && clazz.isAnnotationPresent(filterClazz)){
                matchCount++;
            }else if(filterClazz.isInterface() && TReflect.isImpByInterface(clazz, filterClazz)){
                matchCount++;
            }else if(TReflect.isExtendsByClass(clazz, filterClazz)){
                matchCount++;
            }
        }

        if(matchCount < filters.length){
            return false;
        }else{
            return true;
        }
    }

    /**
     * 获取类的继承树上的所有父类
     * @param type 类型 Class
     * @return 所有父类
     */
    public static List<Class> getAllSuperClass(Class<?> type){
        if(type == null){
            return null;
        }

        ArrayList<Class> classes = new ArrayList<Class>();

        Class<?> superClass = type;
        do{
            superClass = superClass.getSuperclass();
            classes.addAll(Arrays.asList(superClass.getInterfaces()));
            classes.add(superClass);
        }while(superClass!=null && Object.class != superClass);
        return classes;
    }

    /**
     * 获取类的 json 形式的描述
     * @param clazz  Class 类型对象
     * @return 类的 json 形式的描述
     */
    public static String getClazzJSONModel(Class clazz){
        StringBuilder jsonStrBuilder = new StringBuilder();
        if(TReflect.isBasicType(clazz)){
            jsonStrBuilder.append(clazz.getName());
        } else if(clazz.isArray()){
            String clazzName = getClassName(clazz);
            clazzName = clazzName.substring(clazzName.lastIndexOf(Global.STR_POINT)+1,clazzName.length()-2)+"[]";
            jsonStrBuilder.append(clazzName);
        } else {
            jsonStrBuilder.append(Global.STR_LC_BRACES);
            for (Field field : TReflect.getFields(clazz)) {
                jsonStrBuilder.append(Global.STR_QUOTE);
                jsonStrBuilder.append(field.getName());
                jsonStrBuilder.append(Global.STR_QUOTE).append(Global.STR_COLON);
                String filedValueModel = getClazzJSONModel(field.getType());
                if(filedValueModel.startsWith(Global.STR_LC_BRACES) && filedValueModel.endsWith(Global.STR_RC_BRACES)) {
                    jsonStrBuilder.append(filedValueModel);
                    jsonStrBuilder.append(Global.STR_COMMA);
                } else if(filedValueModel.startsWith(Global.STR_LS_BRACES) && filedValueModel.endsWith(Global.STR_RS_BRACES)) {
                    jsonStrBuilder.append(filedValueModel);
                    jsonStrBuilder.append(Global.STR_COMMA);
                } else {
                    jsonStrBuilder.append(Global.STR_QUOTE);
                    jsonStrBuilder.append(filedValueModel);
                    jsonStrBuilder.append(Global.STR_QUOTE).append(Global.STR_COMMA);
                }
            }
            jsonStrBuilder.deleteCharAt(jsonStrBuilder.length()-1);
            jsonStrBuilder.append("}");
        }
        return jsonStrBuilder.toString();
    }

    /**
     * 过滤对象的属性, 产生一个 Map
     *      未包含的属性的值将会以 null 返回
     * @param obj 对象
     * @param fields 保留的属性
     * @return 最后产生的 Map
     */
    public static Map<String, Object> fieldFilter(Object obj, String ... fields) {
        Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
        for(String fieldFilter : fields){
            int firstIndex = fieldFilter.indexOf(Global.STR_LS_BRACES);
            String field = firstIndex == -1? fieldFilter : fieldFilter.substring(0, firstIndex);
            ;
            Object value = null;

            //Map
            if(obj instanceof Map){
                Map paramMap = (Map)obj;
                value = paramMap.get(field);
            }
            //List/Array
            else if(obj.getClass().isArray() || obj instanceof List) {
                if(obj.getClass().isArray()){
                    obj = TObject.asList((Object[])obj);
                }
                for(Object subObj : (List)obj){
                    fieldFilter(subObj, fields);
                }
            }
            //complex object
            else {
                try {
                    value = TReflect.getFieldValue(obj, field);
                } catch (ReflectiveOperationException e) {
                    value = null;
                }
            }

            if(firstIndex>1) {
                Map<String, Object> subResultMap = new LinkedHashMap<String, Object>();
                String subFieldStr= fieldFilter.substring(firstIndex);
                subFieldStr = TString.removeSuffix(subFieldStr);
                subFieldStr = TString.removePrefix(subFieldStr);
                String[] subFieldArray = subFieldStr.split(",");
                for(String subField : subFieldArray) {
                    Map<String, Object> data = fieldFilter(value, subField);
                    subResultMap.putAll(data);
                }
                value = subResultMap;
            }

            resultMap.put(field, value);
        }
        return resultMap;
    }

    /**
     * 判读是否是基本类型(null, boolean, byte, char, double, float, int, long, short, string)
     * @param clazz Class 对象
     * @return true: 是基本类型, false:非基本类型
     */
    public static boolean isBasicType(Class clazz) {
        Boolean isBasicType = CLASS_BASIC_TYPE.get(clazz);
        if(isBasicType==null) {
            if (clazz == null ||
                    clazz.isPrimitive() ||
                    clazz.getName().startsWith("java.lang")) {
                CLASS_BASIC_TYPE.put(clazz, true);
                isBasicType = true;
            } else {
                CLASS_BASIC_TYPE.put(clazz, false);
                isBasicType =  false;
            }
        }

        return isBasicType;
    }

    /**
     * 判断对象是否是指定类型的数组
     * @param object 对象
     * @param type 对象类型
     * @return true: 是指定类型的数据, false: 不是指定类型的数组
     */
    public static boolean isTypeOfArray(Object object, Type type){
        return object.getClass().isArray() && object.getClass().getComponentType().equals(type);
    }

    private static List<String> systemPackages = TObject.asList("java.","jdk.","sun.","javax.","com.sun","com.oracle","javassist");

    /**
     * 判读是否是 JDK 中定义的类(java包下的所有类)
     * @param clazz Class 对象
     * @return true: 是JDK 中定义的类, false:非JDK 中定义的类
     */
    public static boolean isSystemType(Class clazz){

        if( clazz.isPrimitive()){
            return true;
        }

        //排除的包中的 class 不加载
        for(String systemPackage : systemPackages){
            if(getClassName(clazz).startsWith(systemPackage)){
                return true;
            }
        }

        return false;
    }

    /**
     * 判读是否是 JDK 中定义的类(java包下的所有类)
     * @param className Class 对象完全限定名
     * @return true: 是JDK 中定义的类, false:非JDK 中定义的类
     */
    public static boolean isSystemType(String className) {
        if(className.indexOf(Global.STR_POINT)==-1){
            return true;
        }

        //排除的包中的 class 不加载
        for(String systemPackage : systemPackages){
            if(className.startsWith(systemPackage)){
                return true;
            }
        }

        return false;
    }

    /**
     * 获得装箱类型
     * @param primitiveType 原始类型
     * @return 装箱类型
     */
    public static String getPackageType(String primitiveType){
        switch (primitiveType){
            case "int": return "java.lang.Integer";
            case "byte": return "java.lang.Byte";
            case "short": return "java.lang.Short";
            case "long": return "java.lang.Long";
            case "float": return "java.lang.Float";
            case "double": return "java.lang.Double";
            case "char": return "java.lang.Character";
            case "boolean": return "java.lang.Boolean";
            default : return null;
        }
    }
}

