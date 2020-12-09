package org.voovan.tools.reflect;

import org.voovan.Global;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类的模型类
 *
 * @author: helyho
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
     * @param clazz
     * @return
     */
    public static boolean hasModel(Class clazz) {
        if(TReflect.isBasicType(clazz)){
            return false;
        } else if(TReflect.isImpByInterface(clazz, Map.class) || TReflect.isImpByInterface(clazz, Collection.class)){
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
        if(TReflect.isBasicType(clazz)){
            jsonStrBuilder.append(clazz.getName());
        } else if(TReflect.isImpByInterface(clazz, Map.class) || TReflect.isImpByInterface(clazz, Collection.class)){
            jsonStrBuilder.append(clazz.getName());
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
            jsonStrBuilder.append(clazz.getName());
            jsonStrBuilder.append("\"").append(",");

            for (Field field : TReflect.getFields(clazz)) {
                if(field.getName().startsWith("this$")){
                    break;
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
            }
            jsonStrBuilder.deleteCharAt(jsonStrBuilder.length()-1);
            jsonStrBuilder.append("}");
        }
        return jsonStrBuilder.toString();
    }

    /**
     * 根据模型构造 Class
     * @param classModel 模型
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
     */
    private static String buildClass(Map<String, Object> classModel) {
        StringBuilder classStrBuilder = new StringBuilder();
        String className = getClassName(classModel);
        String simpleClassName = getSimpleName(classModel);
        if(CLASS_CODE.containsKey(className)) {
            return null;
        }

        classStrBuilder.append("public static class " + simpleClassName + "{");

        StringBuilder fieldStrBuild = new StringBuilder();
        StringBuilder methodStrBuild = new StringBuilder();

        for(Map.Entry<String, Object> entry : classModel.entrySet()){
            if("$".equals(entry.getKey())){
                continue;
            }

            String fieldName = entry.getKey();
            String fieldType = entry.getValue().toString();

            if(entry.getValue() instanceof Map) {
                Map<String, Object> subClassModel = (Map)entry.getValue();
                fieldType = getSimpleName(subClassModel);
                buildClass(subClassModel);
            }

            fieldStrBuild.append("private " + fieldType + " " + fieldName + ";");

            methodStrBuild.append("public " + fieldType  + " get"+ TString.upperCaseHead(fieldName) + "(){");
            methodStrBuild.append("return " + fieldName + ";}");

            methodStrBuild.append("public void set"+ TString.upperCaseHead(fieldName) + "(" + fieldType + " " + fieldName + "){");
            methodStrBuild.append("this." + fieldName + "=" + fieldName + ";}");
        }

        String bodyCode = fieldStrBuild.toString() + "" + methodStrBuild.toString();
        classStrBuilder.append(bodyCode).append("}");


        CLASS_CODE.put(className, classStrBuilder.toString());
        return classStrBuilder.toString();
    }
}
