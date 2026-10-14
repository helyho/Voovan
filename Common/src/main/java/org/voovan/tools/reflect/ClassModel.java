package org.voovan.tools.reflect;

import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.annotation.NotSerialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类的模型类
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovand
 * Licence: Apache v2 License
 */
public class ClassModel {
    /**
     * Class 的模型缓存
     */
    public final static Map<Class, String> CLASS_MODEL = new ConcurrentHashMap<>();

    /**
     * 通过模型构造的类缓存
     */
    public final static Map<String, String> CLASS_CODE = new ConcurrentHashMap<>();

    /**
     * 通过模型获取当前类的简单名称
     * @param classModel class 模型
     * @return 当前类的简单名称
     */
    public static String getSimpleName(String classModel) {
        Object obj =  JSON.parse(classModel);
        if(obj instanceof Map) {
            return getSimpleName((Map) obj);
        } else {
            throw new IllegalArgumentException("Illegal class model format: " + JSON.formatJson(classModel));
        }
    }

    /**
     * 通过模型获取当前类的名称
     * @param classModel class 模型
     * @return 当前类的名称
     */
    public static String getClassName(String classModel) {
        Object obj =  JSON.parse(classModel);
        if(obj instanceof Map) {
            return getClassName((Map) obj);
        } else {
            throw new IllegalArgumentException("Illegal class model format: " + JSON.formatJson(classModel));
        }
    }



    private static String getSimpleName(Map<String, Object> classModel) {
        String[] $Array = getClassName(classModel).split("\\.");
        return $Array[$Array.length-1].replace("$", "_");
    }

    private static String getClassName(Map<String, Object> classModel) {
        return classModel.get("$").toString();
    }


    /**
     * 是否模型
     * @param clazz 类对象
     * @return true: 有模型, false: 基本类型 或 java.util 类型
     */
    public static boolean hasModel(Class clazz) {
        if(TReflect.isSystemType(clazz)){
            return false;
        } else if(TReflect.isImp(clazz, Map.class) || TReflect.isImp(clazz, Collection.class)){
            return false;
        } else if(clazz.isArray()){
            return false;
        } else {
            return true;
        }
    }

    /**
     * 获取类的模型的描述
     * @param clazz  Class 类型对象
     * @return 类的模型, 如果是 jdk 的基础类则直接返回类名称
     */
    public static String getClazzModel(Class clazz){
        String code = CLASS_MODEL.get(clazz);
        if(code!=null) {
            return code;
        }

        StringBuilder jsonStrBuilder = new StringBuilder();
        if(TReflect.isSystemType(clazz)){
            jsonStrBuilder.append(clazz.getCanonicalName());
        } else if(TReflect.isImp(clazz, Map.class) || TReflect.isImp(clazz, Collection.class)){
            jsonStrBuilder.append(clazz.getCanonicalName());
        } else if(clazz.isArray()){
            String clazzName = TReflect.getClassName(clazz);
            Class componentType = clazz.getComponentType();
            if(hasModel(componentType)) {
                String componentModel = getClazzModel(componentType);
                componentModel = componentModel.replace(componentType.getTypeName(), clazzName);
                jsonStrBuilder.append(componentModel);
            } else {
                jsonStrBuilder.append(clazzName);
            }
        } else {
            jsonStrBuilder.append("{");

            jsonStrBuilder.append("\"");
            jsonStrBuilder.append("$");
            jsonStrBuilder.append("\"").append(":");
            jsonStrBuilder.append("\"");
            jsonStrBuilder.append(clazz.getCanonicalName());
            jsonStrBuilder.append("\"").append(",");

            for (Field field : TReflect.getFields(clazz)) {
                if(field.getName().startsWith("this$")){
                    break;
                }

                if(Modifier.isStatic(field.getModifiers())){
                    continue;
                }

                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }

                if(field.isAnnotationPresent(NotSerialization.class)) {
                    continue;
                }

                jsonStrBuilder.append("\"");
                jsonStrBuilder.append(field.getName());
                jsonStrBuilder.append("\"").append(":");
                String filedValueModel = getClazzModel(field.getType());
                if(filedValueModel.startsWith("{") && filedValueModel.endsWith("}")) {
                    jsonStrBuilder.append(filedValueModel);
                } else if(filedValueModel.startsWith("[") && filedValueModel.endsWith("]")) {
                    jsonStrBuilder.append(filedValueModel);
                } else {
                    jsonStrBuilder.append("\"");
                    jsonStrBuilder.append(filedValueModel);
                    jsonStrBuilder.append("\"");
                }
                jsonStrBuilder.append(",");

                //将字段的所有注解及其参数描述追加到模型中, 使用 "@字段名" 作为键, 值为注解描述的数组
                if(field.getAnnotations().length > 0) {
                    jsonStrBuilder.append("\"@").append(field.getName()).append("\"").append(":").append("[");
                    boolean isFirstAnno = true;
                    for (Annotation annotation : field.getAnnotations()) {
                        if (!isFirstAnno) {
                            jsonStrBuilder.append(",");
                        }

                        jsonStrBuilder.append("{");
                        jsonStrBuilder.append("\"@\":").append("\"").append(annotation.annotationType().getCanonicalName()).append("\"");

                        //追加注解的所有参数, 按参数名排序保证模型确定性
                        java.lang.reflect.Method[] annoMethods = annotation.annotationType().getDeclaredMethods();
                        Arrays.sort(annoMethods, Comparator.comparing(java.lang.reflect.Method::getName));
                        for (java.lang.reflect.Method method : annoMethods) {
                            Object value;
                            try {
                                value = method.invoke(annotation);
                            } catch (Exception e) {
                                continue;
                            }
                            jsonStrBuilder.append(",").append("\"").append(method.getName()).append("\"").append(":");
                            appendAnnotationValue(jsonStrBuilder, value);
                        }
                        jsonStrBuilder.append("}");

                        isFirstAnno = false;
                    }
                    jsonStrBuilder.append("]").append(",");
                }
            }
            jsonStrBuilder.deleteCharAt(jsonStrBuilder.length()-1);
            jsonStrBuilder.append("}");
        }
        return jsonStrBuilder.toString();
    }

    /**
     * 根据模型构造 Class
     * @param classModel 模型
     * @return 模型
     */
    public static String buildClass(String classModel){
        Object obj =  JSON.parse(classModel);
        if(obj instanceof Map) {
            return buildClass((Map) obj);
        } else {
            throw new IllegalArgumentException("Illegal class model format: " + JSON.formatJson(classModel));
        }
    }

    /**
     * 根据模型构造 class
     * @param classModel class 模型 Map
     * @return 模型
     */
    private static String buildClass(Map<String, Object> classModel) {
        StringBuilder classStrBuilder = new StringBuilder();
        String className = getClassName(classModel);
        String simpleClassName = getSimpleName(classModel);
        if(CLASS_CODE.containsKey(className)) {
            return null;
        }

        classStrBuilder.append("import java.util.*;");
        classStrBuilder.append("import java.math.*;");

        classStrBuilder.append("public class " + simpleClassName + "{");

        StringBuilder fieldStrBuild = new StringBuilder();
        StringBuilder methodStrBuild = new StringBuilder();

        //字段的注解代码, 键为字段名, 先收集全部注解代码以保证注解在字段之前
        Map<String, String> fieldAnnoCode = new LinkedHashMap<String, String>();
        for(Map.Entry<String, Object> entry : classModel.entrySet()){
            //"$" 键描述类名, 跳过
            if("$".equals(entry.getKey())){
                continue;
            }

            //"@字段名" 条目描述了字段的所有注解, 转换为注解声明代码
            if(entry.getKey().startsWith("@") && entry.getValue() instanceof List){
                StringBuilder annoStrBuilder = new StringBuilder();
                for(Object annoItem : (List) entry.getValue()){
                    if(!(annoItem instanceof Map)){
                        continue;
                    }
                    Map<String, Object> annoModel = (Map<String, Object>) annoItem;
                    Object annoType = annoModel.get("@");
                    if(annoType == null){
                        continue;
                    }
                    annoStrBuilder.append("@").append(annoType);

                    //追加注解参数
                    boolean hasParam = false;
                    StringBuilder paramStrBuilder = new StringBuilder();
                    for(Map.Entry<String, Object> annoEntry : annoModel.entrySet()){
                        if("@".equals(annoEntry.getKey())){
                            continue;
                        }
                        String paramCode = annotationParamToCode(annoEntry.getValue());
                        //注解参数不允许 null 值, 跳过该参数以使用注解默认值
                        if(paramCode == null){
                            continue;
                        }
                        if(hasParam){
                            paramStrBuilder.append(",");
                        }
                        paramStrBuilder.append(annoEntry.getKey()).append("=").append(paramCode);
                        hasParam = true;
                    }
                    if(hasParam){
                        annoStrBuilder.append("(").append(paramStrBuilder).append(")");
                    }
                    annoStrBuilder.append(" ");
                }
                fieldAnnoCode.put(entry.getKey().substring(1), annoStrBuilder.toString());
            }
        }

        for(Map.Entry<String, Object> entry : classModel.entrySet()){
            //"$" 键和注解描述键已在上一循环处理, 跳过
            if("$".equals(entry.getKey()) || entry.getKey().startsWith("@")){
                continue;
            }

            String fieldName = entry.getKey();
            String fieldType = entry.getValue().toString().replace("java.lang.", "").replace("java.util.", "").replace("java.math.", "");;

            if(entry.getValue() instanceof Map) {
                Map<String, Object> subClassModel = (Map)entry.getValue();
                fieldType = getSimpleName(subClassModel);
                buildClass(subClassModel);
            }

            //拼接字段注解代码
            if(fieldAnnoCode.containsKey(fieldName)){
                fieldStrBuild.append(fieldAnnoCode.get(fieldName));
            }

            fieldStrBuild.append("private " + fieldType + " " + fieldName + "; ");

            methodStrBuild.append("public " + fieldType  + " get"+ TString.upperCaseHead(fieldName) + "(){");
            methodStrBuild.append("return " + fieldName + ";} ");

            methodStrBuild.append("public void set"+ TString.upperCaseHead(fieldName) + "(" + fieldType + " " + fieldName + "){");
            methodStrBuild.append("this." + fieldName + "=" + fieldName + ";} ");
        }

        String bodyCode = fieldStrBuild.toString() + "" + methodStrBuild.toString();
        classStrBuilder.append(bodyCode).append("}");


        CLASS_CODE.put(className, classStrBuilder.toString());
        return classStrBuilder.toString();
    }

    /**
     * 将注解参数值转换为 JSON 描述并追加, 支持字符串/Class/枚举/嵌套注解/字符/数组/数值/布尔
     * @param jsonStrBuilder 模型 JSON 构建器
     * @param value 注解参数值
     */
    private static void appendAnnotationValue(StringBuilder jsonStrBuilder, Object value) {
        if (value instanceof String) {
            String strValue = ((String) value).replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
            jsonStrBuilder.append("\"").append(strValue).append("\"");
        } else if (value instanceof Class) {
            jsonStrBuilder.append("\"").append(((Class) value).getCanonicalName()).append("\"");
        } else if (value instanceof Enum) {
            jsonStrBuilder.append("\"").append(((Enum) value).name()).append("\"");
        } else if (value instanceof Annotation) {
            jsonStrBuilder.append("\"").append(((Annotation) value).annotationType().getCanonicalName()).append("\"");
        } else if (value instanceof Character) {
            jsonStrBuilder.append("\"").append(value).append("\"");
        } else if (value.getClass().isArray()) {
            //数组参数逐元素转换, 避免直接 toString 产生非法 JSON
            jsonStrBuilder.append("[");
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    jsonStrBuilder.append(",");
                }
                appendAnnotationValue(jsonStrBuilder, Array.get(value, i));
            }
            jsonStrBuilder.append("]");
        } else {
            jsonStrBuilder.append(value);
        }
    }

    /**
     * 将注解参数值还原为 Java 注解声明中的参数代码
     * @param paramValue 模型中的注解参数值
     * @return 参数代码, 值为 null 时返回 null 表示跳过该参数
     */
    private static String annotationParamToCode(Object paramValue) {
        if (paramValue == null) {
            return null;
        }
        if (paramValue instanceof Number || paramValue instanceof Boolean) {
            return paramValue.toString();
        }
        if (paramValue instanceof List) {
            //数组参数在 Java 注解中使用 {...} 语法
            StringBuilder arrayStrBuilder = new StringBuilder("{");
            boolean isFirst = true;
            for (Object item : (List) paramValue) {
                if (!isFirst) {
                    arrayStrBuilder.append(",");
                }
                String itemCode = annotationParamToCode(item);
                if (itemCode != null) {
                    arrayStrBuilder.append(itemCode);
                }
                isFirst = false;
            }
            arrayStrBuilder.append("}");
            return arrayStrBuilder.toString();
        }
        //字符串类型参数按 Java 字符串字面量转义
        String strValue = paramValue.toString().replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return "\"" + strValue + "\"";
    }
}
