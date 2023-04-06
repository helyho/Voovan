package org.voovan.tools.ioc;

import org.voovan.tools.TString;
import org.voovan.tools.exception.IOCException;
import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.ioc.annotation.Primary;
import org.voovan.tools.ioc.annotation.Value;
import org.voovan.tools.ioc.entity.BeanDefinition;
import org.voovan.tools.ioc.entity.MethodDefinition;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import static org.voovan.tools.TObject.cast;
import static org.voovan.tools.ioc.Utils.*;

/**
 * 定义管理类
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class Definitions {
    private final Map<String, BeanDefinition>         beanDefinitions = new ConcurrentHashMap<>();
    private final Map<Class, BeanDefinition>          beanDefinitionsByClass = new ConcurrentHashMap<>();
    private final Map<String, MethodDefinition>       methodDefinitions = new ConcurrentHashMap<>();
    private final Map<Class, List<MethodDefinition>>  methodDefinitionsByClass = new ConcurrentHashMap<>();

    Container container;

    public Definitions(Container container) {
        this.container = container;
    }

    public Map<String, BeanDefinition> getBeanDefinitions() {
        return beanDefinitions;
    }

    public Map<String, MethodDefinition> getMethodDefinitions() {
        return methodDefinitions;
    }

    /**
     * 创建 Bean
     * @param name  组件名称
     * @param <T> 泛型
     * @return 创建的对象
     */
    public <T> T createBean(String name) {
        BeanDefinition beanDefinition = beanDefinitions.get(name);
        Class clazz = beanDefinition.getClazz();
        Constructor[] constructors = TReflect.getConstructors(clazz);
        Constructor constructor = null;
        Constructor noArgsConstructor = null;

        //查找 Primary 构造方法, 如果没有则使用无参数构造方法
        for(Constructor item : constructors) {
            if(item.getAnnotation(Primary.class)!=null){
                constructor = item;
                break;
            }

            if(item.getParameterCount()==0){
                noArgsConstructor = item;
            }
        }

        T value;
        constructor = constructor == null ? noArgsConstructor : constructor;

        try {
            //无构造方法用 unsafe 构造对象
            if(constructor == null) {
                value = (T)TReflect.allocateInstance(clazz);
            }
            //使用构造方法构造
            else {
                Object[] params = prepareParam(container, constructor);
                value = TReflect.newInstance(constructor, params);
            }

            return value;
        } catch (Throwable e) {
            throw new IOCException("Try to create bean " + clazz.getName() + "@" + constructor.getName() + " failed", e);
        }
    }

    /**
     * 初始对象的属性
     * @param obj 对象
     * @param initAllField 是否初始化所有的属性, 如果为false则在属性为 null 时不初始化该属性
     */
    public void initField(Object obj, boolean initAllField) {
        Class clazz = obj.getClass();


        Field exField = null;
        try {
            Field[] fields = TReflect.getFields(clazz);
            for (Field field : fields) {
                exField = field;

                //是否初始化 非 null 值 field
                Object fieldVal = field.get(obj);
                if(!initAllField && fieldVal!=null){
                    continue;
                }

                Value value = field.getAnnotation(Value.class);
                if (value == null) {
                    continue;
                }
                String anchor = TReflect.getAnnotationValue(value, "anchor");

                Object data = null;
                if(!TString.isNullOrEmpty(anchor)) {
                    data = container.getByAnchor(anchor, null);
                } else {
                    data = container.getByType(field.getType(), null);
                }

                if(value.required() && data == null) {
                    Logger.warnf("Bean '{}' not found -> {Field: {}@{}} ", anchor, field.getDeclaringClass(), field.getName());
                }

                TReflect.setFieldValue(obj, field, data);
            }

        } catch (Throwable e) {
            throw new IOCException("Try to fill " + clazz.getName() + "@" + exField.getName() + " failed", e);
        }
    }

    /**
     * 执行方法创建 bean
     * @param methodDefinition 方法定义对象
     * @return 方法 bean 的对象
     * @param <T> 泛型
     */
    public <T> T createMethodBean(MethodDefinition methodDefinition) {
        Method method = methodDefinition.getMethod();


        try {
            Object[] params = prepareParam(container, method);

            Object obj = null;
            //获取方法的所属的对象, 静态方法无所属对象, 则使用方法所有的 class 作为对象
            if (methodDefinition.getOwner() != null) {
                obj = container.getByAnchor(methodDefinition.getOwner(), null);
            } else {
                obj = methodDefinition.getClazz();
            }

            if(obj == null) {
                throw new IOCException("Invoke method but the owner object is null");
            }

            T value = TReflect.invokeMethod(obj, method, params);
            return value;
        } catch (Throwable e) {
            throw new IOCException("Try to create method bean " +
                    methodDefinition.getClazz().getName() + "@" + methodDefinition.getMethod().getName() + " failed", e);
        }
    }


    /**
     * 增加 Bean 定义, 用于构建 Bean 对象
     * @param name Bean 名称
     * @param clazz Bean 类型
     * @param singletone 是否单例模式
     * @param lazy 是否延迟加载
     * @param primary 是否是主对象
     * @return bean 的定义对象
     */
    public BeanDefinition addBeanDefinition(String name, Class clazz, boolean singletone, boolean lazy, boolean primary) {
        BeanDefinition beanDefinition = new BeanDefinition(name, clazz, singletone, lazy, primary);
        beanDefinitions.put(name, beanDefinition);
        beanDefinitionsByClass.put(clazz, beanDefinition);
        return beanDefinition;
    }

    /**
     * 用 Class 增加 Bean 定义, 用于构建 Bean 对象
     * @param clazz class对象
     * @return bean 的定义对象
     */
    public BeanDefinition addBeanDefinition(Class clazz) {
        Bean bean = (Bean) clazz.getAnnotation(Bean.class);
        String beanName = Utils.getBeanName(clazz);
        String scope = TReflect.getAnnotationValue(bean, "scope");
        boolean singleton = TReflect.getAnnotationValue(bean, "singleton");
        boolean lazy = TReflect.getAnnotationValue(bean, "lazy");
        boolean primary = clazz.getAnnotation(Primary.class)!=null;

        BeanDefinition beanDefinition = addBeanDefinition(beanName, clazz, singleton, lazy, primary);

        return beanDefinition;
    }

    /**
     * 增加 method 定义, 用于构建方法对象
     * @param name 方法命名名称
     * @param owner 方法依赖对象
     * @param method 方法对象
     * @param singletone 是否单例模式
     * @param lazy 是否延迟加载
     * @param primary 是否是主对象
     * @return 方法 bean 的定义对象
     */
    public MethodDefinition addMethodDefinition(String name, String owner, Method method, boolean singletone, boolean lazy, boolean primary) {
        MethodDefinition methodDefinition = new MethodDefinition(name, owner, method, singletone, lazy, primary);
        methodDefinitions.put(name, methodDefinition);
        methodDefinitionsByClass.computeIfAbsent(method.getDeclaringClass(), key->new Vector<MethodDefinition>()).add(methodDefinition);
        return methodDefinition;
    }

    /**
     * 增加方法的定义
     * @param method 方法对象
     * @return 方法的定义对象
     */
    public MethodDefinition addMethodDefinition(Method method) {
        Bean bean = (Bean) method.getAnnotation(Bean.class);
        String beanName = getBeanName(method);
        String scope = TReflect.getAnnotationValue(bean, "scope");
        boolean singleton = TReflect.getAnnotationValue(bean, "singleton");
        boolean lazy = TReflect.getAnnotationValue(bean, "lazy");
        boolean primary = method.getAnnotation(Primary.class)!=null;

        String owner = Utils.getBeanName(method.getDeclaringClass());

        MethodDefinition methodDefinition = addMethodDefinition(beanName, owner, method, singleton, lazy, primary);

        return methodDefinition;
    }


    /**
     * 获取 bean 定义
     * @param clazz 指定类
     * @return bean的定义对象
     */
    public BeanDefinition getBeanDefinition(Class clazz){
        return beanDefinitionsByClass.get(clazz);
    }

    /**
     * 获取指定类的所有方法定义对象
     * @param clazz 指定类
     * @return 方法 bean 的定义对象集合
     */
    public List<MethodDefinition> getMethodDefinition(Class clazz){
        return methodDefinitionsByClass.get(clazz);
    }
}
