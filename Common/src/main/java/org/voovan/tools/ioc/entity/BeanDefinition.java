package org.voovan.tools.ioc.entity;

/**
 * Bean 定义信息
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class BeanDefinition<T> {
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


    public BeanDefinition(String objName, Class clazz, boolean singleton, boolean lazy) {
        this.name = objName;
        this.clazz = clazz;
        this.singleton = singleton;
        this.lazy = lazy;
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
}
