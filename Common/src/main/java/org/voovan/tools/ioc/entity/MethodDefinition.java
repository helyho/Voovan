package org.voovan.tools.ioc.entity;

import java.lang.reflect.Method;

/**
 * 方法描述信息
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class MethodDefinition extends BeanDefinition {

    private Class returnType;
    private String owner;
    /**
     * 方法对象
     */
    private Method method;

    public MethodDefinition(String name, String owner, Method method, boolean singleton, boolean lazy, boolean primary) {
        super(name, method.getDeclaringClass(), singleton, lazy, primary);
        this.returnType = method.getReturnType();
        this.owner = owner;
        this.method = method;
    }

    public String getOwner() {
        return owner;
    }

    public Method getMethod() {
        return method;
    }

    public Class getReturnType() {
        return returnType;
    }

    public void setReturnType(Class returnType) {
        this.returnType = returnType;
    }

    @Override
    public String toString() {
        return "MethodDefinition{" +
                "owner='" + owner + '\'' +
                ", method=" + method +
                "} " + super.toString();
    }
}
