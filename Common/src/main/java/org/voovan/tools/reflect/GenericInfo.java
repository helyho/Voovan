package org.voovan.tools.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 范型信息描述
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class GenericInfo {
    private Class clazz;
    private Type type;

    public GenericInfo(Class clazz) {
        this.clazz = clazz;
        this.type = (Type)clazz;
        this.type = type instanceof ParameterizedType ? (ParameterizedType)type : type;
    }

    public GenericInfo(Class clazz, Type type) {
        this.clazz = clazz;
        this.type = type;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Class[] getGenericClass() {
        return TReflect.getGenericClass(this.type);
    }

    public static Class[] toClassArray(GenericInfo[] genericInfos) {
        if(genericInfos==null || genericInfos.length == 0) {
            return null;
        }

        Class[] genericClazzs = new Class[genericInfos.length];
        for(int i=0;i<genericInfos.length - 1; i++) {
            genericClazzs[i] = genericInfos[i].getClazz();
        }

        return genericClazzs;
    }
}
