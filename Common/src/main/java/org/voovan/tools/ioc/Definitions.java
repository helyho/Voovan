package org.voovan.tools.ioc;

import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.exception.ParseException;
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

import static org.voovan.tools.ioc.utils.classKey;

/**
 * 定义管理类
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class Definitions {
    private Map<String, BeanDefinition>     beanDefinitions = new ConcurrentHashMap<>();
    private Map<Class, BeanDefinition>      beanDefinitionsByClass = new ConcurrentHashMap<>();
    private Map<String, MethodDefinition>   methodDefinitions = new ConcurrentHashMap<>();
    private Map<Class, List<MethodDefinition>>    methodDefinitionsByClass = new ConcurrentHashMap<>();

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
     * @param clazz 需要构造的类型
     * @param <T> 泛型
     */
    public <T> T craeteBean(String name) {
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
                Annotation[][] paramAnnotations = constructor.getParameterAnnotations();
                Class[] parameterTypes = constructor.getParameterTypes();

                Object[] params = new Object[parameterTypes.length];

                for (int i = 0; i < parameterTypes.length; i++) {
                    Annotation[] annotaions = paramAnnotations[i];
                    //尝试使用注解的名称选择参数
                    for (Annotation annotation : annotaions) {
                        if (annotation.annotationType().isAssignableFrom(Value.class)) {
                            Value valueAnnotation = TObject.cast(annotation);
                            String anchor = TReflect.getAnnotationValue(annotation, "anchor");
                            if (anchor == null) {
                                continue;
                            }
                            params[i] = container.get(anchor, parameterTypes[i], null);
                            if(valueAnnotation.required() && params[i] == null) {
                                Logger.warnf("Bean '{}' not found -> {method: {}@{}, type: {}, No: {}}, On invoke constructor ", anchor, constructor.getDeclaringClass(), constructor.getName(), parameterTypes[i], i);
                            }
                        }
                    }
                    if (params[i] == null) {
                        //使用类型选择参数
                        params[i] = container.get(null, parameterTypes[i], null);
                    }
                }

                value = TReflect.newInstance(constructor, params);
            }

            return value;
        } catch (Throwable e) {
            throw new ParseException("Try to create bean " + clazz.getName() + "@" + constructor.getName() + " failed");
        }
    }

    public void initField(Object obj) {
        Class clazz = obj.getClass();


        Field exField = null;
        try {
            Field[] fields = TReflect.getFields(clazz);
            for (Field field : fields) {
                exField = field;
                Value value = field.getAnnotation(Value.class);
                if (value == null) {
                    continue;
                }
                String anchor = TReflect.getAnnotationValue(value, "anchor");

                Object data = null;
                if(TString.isNullOrEmpty(anchor)) {
                    data = container.getByType(field.getType(), null);
                } else {
                    data = container.get(anchor, null);
                }

                if(value.required() && data == null) {
                    Logger.warnf("Bean '{}' not found -> {Field: {}@{}} ", anchor, field.getDeclaringClass(), field.getName());
                }

                TReflect.setFieldValue(obj, field, data);
            }

        } catch (Throwable e) {
            throw new ParseException("Try to fill " + clazz.getName() + "@" + exField.getName() + " failed");
        }
    }

    /**
     * 执行方法创建 bean
     * @param methodDefinition 方法定义对象
     * @return 方法的值
     * @param <T> 泛型
     */
    public <T> T createMethodBean(MethodDefinition methodDefinition) {
        Annotation[][] paramAnnotations     = methodDefinition.getMethod().getParameterAnnotations();
        Class[] parameterTypes              = methodDefinition.getMethod().getParameterTypes();
        Method method = methodDefinition.getMethod();

        Object[] params = new Object[parameterTypes.length];

        try {
            for (int i = 0; i < parameterTypes.length; i++) {
                Annotation[] annotaions = paramAnnotations[i];
                //尝试使用注解的名称选择参数
                for (Annotation annotation : annotaions) {
                    if (annotation.annotationType().isAssignableFrom(Value.class)) {
                        Value valueAnnotation = TObject.cast(annotation);
                        String anchor = TReflect.getAnnotationValue(annotation, "anchor");
                        if (anchor == null) {
                            continue;
                        }
                        params[i] = container.get(anchor, parameterTypes[i], null);

                        if (valueAnnotation.required() && params[i] == null) {
                            Logger.warnf("Bean '{}' not found -> {method: {}@{}(...), type: {}, No: {}}, On invoke method ", anchor, method.getDeclaringClass(), method.getName(), parameterTypes[i], i);
                        }
                    }
                }
                if (params[i] == null) {
                    //使用类型选择参数
                    params[i] = container.getByType(parameterTypes[i], null);
                }
            }

            Object obj = null;
            //获取方法的所属的对象, 静态方法无所属对象, 则使用方法所有的 class 作为对象
            if (methodDefinition.getOwner() != null) {
                obj = container.get(methodDefinition.getOwner(), null);
            } else {
                obj = methodDefinition.getClazz();
            }

            T value = TReflect.invokeMethod(obj, method, params);
            return value;
        } catch (Throwable e) {
            throw new ParseException("Try to create method bean " +
                    methodDefinition.getClazz().getName() + "@" + methodDefinition.getMethod().getName() + " failed");
        }
    }


    /**
     * 增加 Bean 定义, 用于构建 Bean 对象
     * @param name Bean 名称
     * @param clazz Bean 类型
     * @param singletone 是否单例模式
     * @param lazy 是否延迟加载
     */
    public BeanDefinition addBeanDefinition(String name, Class clazz, boolean singletone, boolean lazy) {
        BeanDefinition beanDefinition = new BeanDefinition(name, clazz, singletone, lazy);
        beanDefinitions.put(name, beanDefinition);
        beanDefinitionsByClass.put(clazz, beanDefinition);
        return beanDefinition;
    }

    public BeanDefinition addBeanDefinition(Class clazz) {
        Bean bean = (Bean) clazz.getAnnotation(Bean.class);
        String beanName = getBeanName(clazz);
        String scope = TReflect.getAnnotationValue(bean, "scope");
        boolean singleton = TReflect.getAnnotationValue(bean, "singleton");
        boolean lazy = TReflect.getAnnotationValue(bean, "lazy");

        return addBeanDefinition(beanName, clazz, singleton, lazy);
    }

    /**
     * 增加 method 定义, 用于构建方法对象
     * @param name 方法命名名称
     * @param owner 方法依赖对象
     * @param method 方法对象
     * @param singletone 是否单例模式
     * @param lazy 是否延迟加载
     */
    public MethodDefinition addMethodDefinition(String name, String owner, Method method, boolean singletone, boolean lazy) {
        MethodDefinition methodDefinition = new MethodDefinition(name, owner, method, singletone, lazy);
        methodDefinitions.put(name, methodDefinition);
        methodDefinitionsByClass.computeIfAbsent(method.getDeclaringClass(), key->new Vector<MethodDefinition>()).add(methodDefinition);
        return methodDefinition;
    }

    /**
     * 增加 method 定义, 用于构建方法对象
     * @param name 方法命名名称
     * @param owner 方法依赖对象
     * @param method 方法对象
     * @param singletone 是否单例模式
     * @param lazy 是否延迟加载
     */
    public MethodDefinition addMethodDefinition(Method method) {
        Bean bean = (Bean) method.getAnnotation(Bean.class);
        String beanName = getBeanName(method);
        String scope = TReflect.getAnnotationValue(bean, "scope");
        boolean singleton = TReflect.getAnnotationValue(bean, "singleton");
        boolean lazy = TReflect.getAnnotationValue(bean, "lazy");

        String owner = getBeanName(method.getDeclaringClass());
        return addMethodDefinition(beanName, owner, method, singleton, lazy);
    }

    public static String getBeanName(Class clazz) {
        Bean bean = (Bean) clazz.getAnnotation(Bean.class);
        String beanName = TReflect.getAnnotationValue(bean, "name");
        if (TString.isNullOrEmpty(beanName)) {
            beanName = classKey(clazz);
        }

        return beanName;
    }

    public static String getBeanName(Method method) {
        Bean bean = (Bean) method.getAnnotation(Bean.class);
        String beanName = TReflect.getAnnotationValue(bean, "name");
        if (TString.isNullOrEmpty(beanName)) {
            beanName = classKey(method.getReturnType());
        }

        return beanName;
    }

    public static String getScope(Class clazz){
        Bean bean = (Bean) clazz.getAnnotation(Bean.class);
        return TReflect.getAnnotationValue(bean, "scope");
    }

    public static String getScope(Method method){
        Bean bean = (Bean) method.getAnnotation(Bean.class);
        return TReflect.getAnnotationValue(bean, "scope");
    }

    public BeanDefinition getBeanDefinition(Class clazz){
        return beanDefinitionsByClass.get(clazz);
    }

    public List<MethodDefinition> getMethodDefinition(Class clazz){
        return methodDefinitionsByClass.get(clazz);
    }
}
