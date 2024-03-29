package org.voovan.tools.ioc;

import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.exception.IOCException;
import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.ioc.annotation.Value;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.GenericInfo;
import org.voovan.tools.reflect.TReflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.voovan.tools.TObject.cast;
import static org.voovan.tools.reflect.TReflect.SINGLE_VALUE_KEY;

/**
 * Class name
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class IOCUtils {
    public static final String DEFAULT_SCOPE = "defalut";

    public static final Object EMPTY = new Object();

    public static void nameChecker(String name) {
        if(name.indexOf('.') >= 0) {
            throw new IOCException("Bean name errro cause it has '.'");
        }
    }

    public static String classKey(Class clazz) {
        //归总到接口类型
        if(TReflect.isImp(clazz, Map.class)) {
            clazz = Map.class;
        } else if(TReflect.isImp(clazz, List.class)){
            clazz = List.class;
        } else if(TReflect.isImp(clazz, Set.class)){
            clazz = Set.class;
        }
        return TEnv.shortClassName(clazz.getName(), "");
    }

    public static void checkBean(Container container, String beanName, Class type) {
        Object bean = container.getAll().get(beanName);
        if(bean!=null && !TReflect.isSuper(bean.getClass(), type)){
            throw new IOCException("Bean name duplicate, beanName: ["+beanName+"], current: [" + bean.getClass().getCanonicalName() + "], new: [" + type.getCanonicalName() + "]");
        }
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
            Class clazz = method.getReturnType();
            beanName = classKey(clazz);
        }

        return beanName;
    }

    public static String getBeanNameFromExpression(String path) {
        return path.split("\\.")[0];
    }

    public static boolean isPath(String path) {
        return path.indexOf('.') >= 0;
    }

    public static String getScope(Class clazz){
        Bean bean = (Bean) clazz.getAnnotation(Bean.class);
        return bean == null ? DEFAULT_SCOPE : TReflect.getAnnotationValue(bean, "scope");
    }

    public static String getScope(Method method){
        Bean bean = (Bean) method.getAnnotation(Bean.class);
        return bean == null ? DEFAULT_SCOPE : TReflect.getAnnotationValue(bean, "scope");
    }


    public static Object[] prepareParam(Container container, Executable executable) {
        Annotation[][] paramAnnotations = executable.getParameterAnnotations();
        Class[] parameterTypes          = executable.getParameterTypes();
        Type[] genericParameterTypes    = executable.getGenericParameterTypes();

        Object[] params = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Annotation[] annotaions = paramAnnotations[i];
            //尝试使用注解的名称选择参数
            for (Annotation annotation : annotaions) {
                Class parameterType = parameterTypes[i];
                try {
                    if (annotation.annotationType().isAssignableFrom(Value.class)) {
                        Value valueAnnotation = cast(annotation);
                        String anchor = TReflect.getAnnotationValue(annotation, "anchor");


                        params[i] = getAnnotationValueData(anchor, parameterType, TReflect.getGenericClass(genericParameterTypes[i]), container);
                        if(valueAnnotation.required() && params[i] == null) {
                            Logger.warnf("Bean '{}' not found -> {method: {}@{}, type: {}, No: {}}, On invoke constructor ", anchor, executable.getDeclaringClass(), executable.getName(), parameterTypes[i], i);
                        }
                    }
                } catch (Throwable e) {
                    throw new IOCException("Try to fill " + executable.getName() + " parameter " + i + " failed", e);
                }
            }

        }

        return params;
    }

    public static Object getAnnotationValueData(String anchor, Type type, Class[] genericType, Container container) throws ReflectiveOperationException, ParseException {
        Object ret = null;
        if(!TString.isNullOrEmpty(anchor)) {
            ret = container.getByAnchor(anchor, (Class)type, null);
        } else {
            ret = container.getByType((Class)type, null);
        }

        if(ret instanceof Collection) {
            ret = TReflect.getObjectFromMap(type, TObject.asMap(SINGLE_VALUE_KEY, ret), genericType, false);
        } else if(ret instanceof Map) {
            ret = TReflect.getObjectFromMap(type, (Map)ret, genericType, false);
        }
        return ret;
    }

}
