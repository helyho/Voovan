package org.voovan.tools.ioc.entity;

import java.lang.reflect.Method;

/**
 * Bean 定义信息
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class BeanDefinition {
    /**
     * 方法执行依赖的对象
     */
    private String name;

    private Class clazz;

    /**
     * 是否是单例模式
     */
    private boolean singleton = true;
    private boolean lazy = false;

    private boolean primary = false;

    private Method init;

    private Method destory;

    public BeanDefinition(String objName, Class clazz, boolean singleton, boolean lazy, boolean primary) {
        this.name = objName;
        this.clazz = clazz;
        this.singleton = singleton;
        this.lazy = lazy;
        this.primary = primary;
    }

    public String getName() {
        return name;
    }

    public Class getClazz() {
        return clazz;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public boolean isLazy() {
        return lazy;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Method getInit() {
        return init;
    }

    public void setInit(Method init) {
        this.init = init;
    }

    public Method getDestory() {
        return destory;
    }

    public void setDestory(Method destory) {
        this.destory = destory;
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "name='" + name + '\'' +
                ", clazz=" + clazz +
                ", singleton=" + singleton +
                ", lazy=" + lazy +
                ", primary=" + primary +
                '}';
    }
}
