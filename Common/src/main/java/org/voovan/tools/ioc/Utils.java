package org.voovan.tools.ioc;

import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.exception.IOCException;
import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.ioc.annotation.Value;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.voovan.tools.TObject.cast;

/**
 * Class name
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class Utils {
    public static final String DEFAULT_SCOPE = "defalut";

    public static void nameChecker(String name) {
        if(name.indexOf('.') >= 0) {
            throw new IOCException("Bean name errro cause it has '.'");
        }
    }

    public static String classKey(Class clazz) {
        //TODO: 转换成配置文件
        //归总到接口类型
        if(TReflect.isImp(clazz, Map.class)) {
            clazz = Map.class;
        } else if(TReflect.isImp(clazz, List.class)){
            clazz = List.class;
        }
        return TEnv.shortClassName(clazz.getName(), "");
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
        return TReflect.getAnnotationValue(bean, "scope");
    }

    public static String getScope(Method method){
        Bean bean = (Bean) method.getAnnotation(Bean.class);
        return TReflect.getAnnotationValue(bean, "scope");
    }

    public static Object[] prepareParam(Container container, Executable executable) {
        Annotation[][] paramAnnotations = executable.getParameterAnnotations();
        Class[] parameterTypes          = executable.getParameterTypes();

        Object[] params = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Annotation[] annotaions = paramAnnotations[i];
            //尝试使用注解的名称选择参数
            for (Annotation annotation : annotaions) {
                if (annotation.annotationType().isAssignableFrom(Value.class)) {
                    Value valueAnnotation = cast(annotation);
                    String anchor = TReflect.getAnnotationValue(annotation, "anchor");
                    if (TString.isNullOrEmpty(anchor)) {
                        continue;
                    }
                    params[i] = container.getByAnchor(anchor, parameterTypes[i], null);
                    if(valueAnnotation.required() && params[i] == null) {
                        Logger.warnf("Bean '{}' not found -> {method: {}@{}, type: {}, No: {}}, On invoke constructor ", anchor, executable.getDeclaringClass(), executable.getName(), parameterTypes[i], i);
                    }
                }
            }
            if (params[i] == null) {
                //使用类型选择参数
                params[i] = container.getByType(parameterTypes[i], null);
            }
        }

        return params;
    }

}
