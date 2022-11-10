package org.voovan.tools.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 范型信息描述
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class GenericInfo {
    private Class clazz;
    private ParameterizedType type;
    private Class[] genericClasses;

    public GenericInfo(Type type) {
        if(type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            this.clazz = (Class) parameterizedType.getRawType();
            this.type = parameterizedType;
            this.genericClasses = TReflect.getGenericClass(this.type);
        } else {
            this.clazz = (Class) type;
            this.genericClasses = new Class[0];
        }
    }

    public Class getClazz() {
        return clazz;
    }

    public ParameterizedType getType() {
        return type;
    }

    public Class[] getGenericClass() {
        return genericClasses;
    }
}
