package org.voovan.tools.ioc;

import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.exception.IOCException;
import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

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

    public static String getBeanNameInPath(Class clazz) {
        Bean bean = (Bean) clazz.getAnnotation(Bean.class);
        String beanName = TReflect.getAnnotationValue(bean, "name");
        if (TString.isNullOrEmpty(beanName)) {

            beanName = classKey(clazz);
        }

        return beanName;
    }

    public static String getBeanNameInPath(Method method) {
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

}
