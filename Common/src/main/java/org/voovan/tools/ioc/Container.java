package org.voovan.tools.ioc;

import org.voovan.tools.TString;
import org.voovan.tools.exception.IOCException;
import org.voovan.tools.ioc.entity.BeanDefinition;
import org.voovan.tools.ioc.entity.MethodDefinition;
import org.voovan.tools.json.BeanVisitor;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Method;
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
@SuppressWarnings("ALL")
public class Container {
    private final Map<String, Object> beanValues = new ConcurrentHashMap<>();

    private final String scope;
    private final Definitions definitions;
    private final BeanVisitor beanVisitor;


    public Container(String scope, Config config) {
        this.scope = scope;
        this.definitions = new Definitions(this);

        //Config 初始化
        for(String key : config.getConfig().keySet()) {
            beanValues.put(key, config.getConfig().get(key));
        }

        //BeanVistor 初始化
        //.................
        beanVisitor = new BeanVisitor(beanValues);
        beanVisitor.setPathSplitor(BeanVisitor.SplitChar.POINT);
    }

    public Container(String scope) {
       this(scope, new Config());
    }

    public String getScope() {
        return scope;
    }

    public Definitions getDefinitions() {
        return definitions;
    }

    /**
     * 使用表达式获取数据, 保持解析后的类型, 无类型自动转移
     * 性能高, 但复杂类型转换可能不正常
     *
     * @param expression       表达式路径
     * @param defaultVal 默认值
     * @param clazz      值的类型
     * @param <T>        泛型类型
     * @return 数据的值
     */
    private <T> T getByExpression(String expression, Class<T> clazz, T defaultVal) {
        String beanName = getBeanNameFromExpression(expression);

        //如果依赖的 Bean 不存在则创建, 忽略 lazy
        initBean(beanName, true);
        T ret = beanVisitor.value(expression, clazz, defaultVal);
        if (ret != null) {
            return ret;
        }

        //如果依赖的 Bean 不存在则创建, 忽略 lazy
        invokeMethodBean(beanName, true);
        ret = beanVisitor.value(expression,clazz, defaultVal);
        if (ret != null) {
            return ret;
        }

        return defaultVal;
    }

    /**
     * 使用表达式获取数据, 保持解析后的类型, 无类型自动转移
     * 性能高, 但复杂类型转换可能不正常
     *
     * @param expression       表达式路径
     * @param defaultVal 默认值
     * @param <T>        泛型类型
     * @return 数据的值
     */
    private <T> T getByExpression(String expression, T defaultVal) {
        String beanName = getBeanNameFromExpression(expression);

        //如果依赖的 Bean 不存在则创建, 忽略 lazy
        initBean(beanName, true);
        T ret = beanVisitor.value(expression, defaultVal);
        if (ret != null) {
            return ret;
        }

        //如果依赖的 Bean 不存在则创建, 忽略 lazy
        invokeMethodBean(beanName, true);
        ret = beanVisitor.value(expression, defaultVal);
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
        //如果依赖的 Bean 不存在则创建, 忽略 lazy
        initBean(beanName, true);
        T ret = (T) beanValues.getOrDefault(beanName, defaultVal);
        if (ret != null) {
            return ret;
        }

        //如果依赖的 Bean 不存在则创建, 忽略 lazy
        invokeMethodBean(beanName, true);
        ret = (T) beanValues.get(beanName);
        if (ret != null) {
            return ret;
        }

        return defaultVal;
    }

    /**
     * 按锚点(名称或者表达式)获取对象
     * @param anchor 表达式
     * @param clazz 表达式对象类型
     * @param defaultVal 默认值
     * @param <T> 泛型
     * @return 获取的对象
     */
    public <T> T getByAnchor(String anchor, Class<T> clazz, T defaultVal) {
        if (isPath(anchor)) {
            return (T) getByExpression(anchor, clazz, defaultVal);
        } else {
            return getByName(anchor, defaultVal);
        }
    }

    /**
     * 按锚点(名称或者表达式)获取对象
     * @param anchor 表达式
     * @param defaultVal 默认值
     * @param <T> 泛型
     * @return 获取的对象
     */
    public <T> T getByAnchor(String anchor, T defaultVal) {
        if (isPath(anchor)) {
            return (T) getByExpression(anchor, defaultVal);
        } else {
            return getByName(anchor, defaultVal);
        }
    }

    /**
     * 按类型获取对象
     * @param clazz 对象的类型
     * @param defaultVal 默认值
     * @param <T> 泛型
     * @return 获取的对象
     */
    public <T> T getByType(Class<T> clazz, T defaultVal) {
        return getByName(classKey(clazz), defaultVal);
    }

    /**
     * 获取对象
     * @param mark 按表达式、名称、类型获取对象, 可以传递 String 和 Class 类型
     * @param defaultVal 默认值
     * @param <T> 泛型
     * @return 获取的对象
     */
    public <T> T get(Object mark, T defaultVal) {
        if(mark instanceof String) {
            return getByAnchor((String)mark, defaultVal);
        } else if(mark instanceof Class) {
            return getByType((Class<T>)mark, defaultVal);
        } else {
            throw new IOCException("Contain.get only accept mark type by java.lang.[String,Class]");
        }
    }


    /**
     * 判断指定名称的对象是否存在
     * @param name 描
     * @return true: 存在, false: 不存在
     */
    public boolean exists(String name) {
        return beanValues.containsKey(name);
    }

    /**
     * 判断指定类型的对象是否存在
     * @param clazz 判断所检查的类型
     * @return true: 存在, false: 不存在
     */
    public boolean existsByType(Class<?> clazz) {
        return exists(classKey(clazz));
    }


    /**
     * 增加外部类到容器中进行管理
     * @param beanName 名称
     * @param value 被增加到容器中对象
     * @param <T> 泛型对象
     */
    public <T> void addExtBean(String beanName, T value) {
        if(TString.isNullOrEmpty(beanName)) {
            beanName = classKey(value.getClass());
        }

        //创建类和方法的Definition
        Context.loadClass(value.getClass());
        Context.loadMethod(value.getClass());

        definitions.initField(value, false);  //外部 Bean 的属性不初始化值为null的属性
        addBeanValue(beanName, value);
        initMethodBean(value.getClass(), true); //外部 Bean 的方法不支持 lazy
    }

    /**
     * 增加组件
     *
     * @param name  组件名称
     * @param value 组件值
     */
    public void addBeanValue(String name, Object value) {
        nameChecker(name);
        BeanDefinition beanDefinition = definitions.getBeanDefinitions().get(name);
        if(beanDefinition == null) {
            beanDefinition = definitions.getMethodDefinitions().get(name);
        }

        try {
            //执行初始化
            if (beanDefinition != null) {
                Method initMethod = beanDefinition.getInit();
                if (initMethod != null) {
                    Object[] params = prepareParam(this,initMethod);
                    TReflect.invokeMethod(value, initMethod, params);
                }
            }

            Object preValue = beanValues.put(name, value);

            //执行销毁方法
            if (preValue != null && beanDefinition != null) {
                Method destoryMethod = beanDefinition.getDestory();
                if (destoryMethod != null) {
                    Object[] params = prepareParam(this,destoryMethod);
                    TReflect.invokeMethod(preValue, destoryMethod, params);
                }
            }
        } catch (Throwable e) {
            throw new IOCException("Invoke init or destory method failed", e);
        }

        beanValues.put(name, value);
        beanValues.put(classKey(value.getClass()), value);
    }

    /**
     * 初始化 Bean
     * @param beanDefinition bean定义
     * @param ingoreLazy true: 忽略 Lazy 标记, false: 检查 Lazy标记
     * @return 初始化的 bean 对象类型. null 表示无可用对象初始化
     * @param <T> 泛型类型
     */
    public <T> T initBean(BeanDefinition beanDefinition, boolean ingoreLazy) {
        if(beanDefinition==null){
            return null;
        }

        String beanName = beanDefinition.getName();
        //单例 及 Primary 支持
        if(!beanValues.containsKey(beanName) || beanDefinition.isPrimary() || !beanDefinition.isSingleton() ) {
            if (ingoreLazy || !beanDefinition.isLazy()) {
                //延迟加载处理
                T value = definitions.createBean(beanName);
                if (value != null) {
                    definitions.initField(value, true);
                    addBeanValue(beanName, value);
                    initMethodBean(beanDefinition.getClazz(), false); //bean 初始化时不忽略方法的 lazy
                    return value;
                }
            }
        }

        return null;
    }

    /**
     * 初始化 Bean
     * @param beanName bean名称
     * @param ingoreLazy true: 忽略 Lazy 标记, false: 检查 Lazy标记
     * @return 初始化的 bean 对象类型. null 表示无可用对象初始化
     * @param <T> 泛型类型
     */
    public <T> T initBean(String beanName, boolean ingoreLazy) {
        BeanDefinition beanDefinition = definitions.getBeanDefinitions().get(beanName);
        return initBean(beanDefinition, ingoreLazy);
    }

    /**
     * 初始化方法的Bean
     * @param methodDefinition 定义在方法上的 bean 定义
     * @param ingoreLazy true: 忽略 Lazy 标记, false: 检查 Lazy标记
     * @return 初始化的 bean 对象类型. null 表示无可用对象初始化
     * @param <T> 泛型类型
     */
    public <T> T invokeMethodBean(MethodDefinition methodDefinition, boolean ingoreLazy) {
        if(methodDefinition==null) {
            return null;
        }

        String beanName = methodDefinition.getName();
        BeanDefinition beanDefinition = definitions.getBeanDefinition(methodDefinition.getClazz());
        //单例 及 Primary 支持
        if (!beanValues.containsKey(beanName) || methodDefinition.isPrimary() || !methodDefinition.isSingleton()) {
            //延迟加载处理
            if (ingoreLazy || !methodDefinition.isLazy()) {
                T value = definitions.createMethodBean(methodDefinition);
                if (value != null) {
                    addBeanValue(beanName, value);
                    return value;
                }
            }
        }

        return null;
    }

    /**
     * 初始化方法的Bean
     * @param beanName bean名称
     * @param ingoreLazy true: 忽略 Lazy 标记, false: 检查 Lazy标记
     * @return 初始化的 bean 对象类型. null 表示无可用对象初始化
     * @param <T> 泛型类型
     */
    public <T> T invokeMethodBean(String beanName, boolean ingoreLazy) {
        MethodDefinition methodDefinition = definitions.getMethodDefinitions().get(beanName);
        return invokeMethodBean(methodDefinition, ingoreLazy);
    }

    /**
     * 初始化类中所有的方法Bean
     * @param clazz 指定的类型
     * @param ingoreLazy true: 忽略 Lazy 标记, false: 检查 Lazy标记
     */
    public void initMethodBean(Class<?> clazz, boolean ingoreLazy) {
        List<MethodDefinition> methodDefinitionList = definitions.getMethodDefinition(clazz);

        if(methodDefinitionList==null) {
            return;
        }

        for(MethodDefinition methodDefinition : methodDefinitionList) {
            invokeMethodBean(methodDefinition, ingoreLazy);
        }
    }
}