package org.voovan.tools.ioc;

import org.voovan.tools.ioc.entity.BeanDefinition;
import org.voovan.tools.ioc.entity.MethodDefinition;
import org.voovan.tools.json.BeanVisitor;

import java.util.List;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.voovan.tools.ioc.Utils.*;

/**
 * 容器类
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class Container {
    private Map<String, Object> configValues = new ConcurrentHashMap<>();
    private Map<String, Object> beanValues = new ConcurrentHashMap<>();
    private Map<String, Object> methodValues = new ConcurrentHashMap<>();

    private String scope;
    private Definitions definitions;

    private BeanVisitor configVisitor;
    private BeanVisitor beanVisitor;
    private BeanVisitor methodVisitor;

    public Container(String scope) {
        this.scope = scope;
        this.definitions = new Definitions(this);

        //Config 初始化
        Config config = new Config();
        configValues.putAll(config.getConfig());

        configVisitor = new BeanVisitor(configValues);
        configVisitor.setPathSplitor(BeanVisitor.SplitChar.POINT);

        //Bean 初始化
        //.................
        beanVisitor = new BeanVisitor(beanValues);
        beanVisitor.setPathSplitor(BeanVisitor.SplitChar.POINT);

        //Method 初始化
        //.................
        methodVisitor = new BeanVisitor(methodValues);
        methodVisitor.setPathSplitor(BeanVisitor.SplitChar.POINT);
    }


    public String getScope() {
        return scope;
    }

    public Definitions getDefinitions() {
        return definitions;
    }

    /**
     * 使用表达式获取数据
     *
     * @param path       表达式路径
     * @param defaultVal 默认值
     * @param clazz      值类型
     * @param <T>        泛型类型
     * @return 数据的值
     */
    private <T> T getByPath(String path, Class<T> clazz, T defaultVal) {
        T ret = configVisitor.value(path, clazz, defaultVal);
        if (ret != null) {
            return ret;
        }

        String beanName = getBeanNameInPath(path);
        //如果依赖的 Bean 不存在则创建
        initBean(beanName);
        ret = beanVisitor.value(path, clazz, defaultVal);
        if (ret != null) {
            return ret;
        }

        invokeMethodBean(beanName);
        ret = methodVisitor.value(path,clazz, defaultVal);
        if (ret != null) {
            return ret;
        }

        return defaultVal;
    }

    /**
     * 使用表达式获取数据, 保持解析后的类型, 无类型自动转移
     * 性能高, 但复杂类型转换可能不正常
     *
     * @param path       表达式路径
     * @param defaultVal 默认值
     * @param <T>        泛型类型
     * @return 数据的值
     */
    private <T> T getByPath(String path, T defaultVal) {
        T ret = configVisitor.value(path, defaultVal);
        if (ret != null) {
            return ret;
        }

        String beanName = getBeanNameInPath(path);;

        initBean(beanName);
        ret = beanVisitor.value(path, defaultVal);
        if (ret != null) {
            return ret;
        }

        invokeMethodBean(beanName);
        ret = methodVisitor.value(path, defaultVal);
        if (ret != null) {
            return ret;
        }

        return defaultVal;
    }

    /**
     * 不实用路径获取数据, 而是使用 key
     *
     * @param beanName       名称
     * @param defaultVal 默认值
     * @param <T>        泛型类型
     * @return 返回值
     */
    private <T> T getByName(String beanName, T defaultVal) {
        T ret = (T) configValues.getOrDefault(beanName, defaultVal);
        if (ret != null) {
            return ret;
        }

        //如果依赖的 Bean 不存在则创建

        initBean(beanName);
        ret = (T) beanValues.getOrDefault(beanName, defaultVal);
        if (ret != null) {
            return ret;
        }

        invokeMethodBean(beanName);
        ret = (T) methodValues.get(beanName);
        if (ret != null) {
            return ret;
        }

        return defaultVal;
    }

    /**
     * @param pathOrName
     * @param clazz
     * @param defaultVal
     * @param <T>
     * @return
     */
    public <T> T get(String pathOrName, Class clazz, T defaultVal) {
        if (isPath(pathOrName)) {
            return (T) getByPath(pathOrName, clazz, defaultVal);
        } else {
            return getByName(pathOrName, defaultVal);
        }
    }

    /**
     * @param pathOrName
     * @param clazz
     * @param defaultVal
     * @param <T>
     * @return
     */
    public <T> T get(String anchor, T defaultVal) {
        if (isPath(anchor)) {
            return (T) getByPath(anchor, defaultVal);
        } else {
            return getByName(anchor, defaultVal);
        }
    }

    public <T> T getByType(Class clazz, T defaultVal) {
        return getByName(classKey(clazz), defaultVal);
    }


    public boolean exists(String anchor) {
        return configValues.containsKey(anchor) || beanValues.containsKey(anchor) || methodValues.containsKey(anchor);
    }

    public boolean existsByType(Class clazz) {
        return exists(classKey(clazz));
    }

    /**
     * 增加组件
     *
     * @param name  组件名称
     * @param value 组件值
     */
    public void addBeanValue(String name, Object value) {
        nameChecker(name);
        beanValues.put(name, value);
        beanValues.put(classKey(value.getClass()), value);
    }

    /**
     * 增加组件
     *
     * @param name  组件名称
     * @param value 组件值
     */
    public void addMethodValue(String name, Object value) {
        nameChecker(name);
        methodValues.put(name, value);
        methodValues.put(classKey(value.getClass()), value);
    }



    public <T> T initBean(String beanName) {
        BeanDefinition beanDefinition = definitions.getBeanDefinitions().get(beanName);
        if(beanDefinition!=null && (!beanValues.containsKey(beanName) ||
                beanDefinition.isPrimary() ||
                !beanDefinition.isSingleton())

        ) {
            //延迟加载处理
            if(Context.isIsInited() || !beanDefinition.isLazy()) {
                T value = definitions.craeteBean(beanName);
                if (value != null) {
                    definitions.initField(value);
                    addBeanValue(beanName, value);
                    initMethodBean(beanDefinition.getClazz());
                    return value;
                }
            }
        }

        return null;
    }

    public void initMethodBean(Class clazz) {
        List<MethodDefinition> methodDefinitionList = definitions.getMethodDefinition(clazz);
        for(MethodDefinition methodDefinition : methodDefinitionList) {
            invokeMethodBean(methodDefinition);
        }
    }


    public <T> T invokeMethodBean(MethodDefinition methodDefinition) {
        String beanName = methodDefinition.getName();
        BeanDefinition beanDefinition = definitions.getBeanDefinition(methodDefinition.getClazz());
        if (methodDefinition != null && (!methodValues.containsKey(beanName) || methodDefinition.isPrimary() || !methodDefinition.isSingleton())) {
            //延迟加载处理
            if (Context.isIsInited() || !beanDefinition.isLazy() && !methodDefinition.isPrimary()) {
                T value = definitions.createMethodBean(methodDefinition);
                if (value != null) {
                    addMethodValue(beanName, value);
                    return value;
                }
            }
        }

        return null;
    }

    public <T> T invokeMethodBean(String beanName) {
        MethodDefinition methodDefinition = definitions.getMethodDefinitions().get(beanName);
        return invokeMethodBean(methodDefinition);
    }
}