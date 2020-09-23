package org.voovan.http.server.module.annontationRouter.swagger;

import org.voovan.http.server.module.annontationRouter.annotation.Router;

import java.lang.reflect.Method;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SwaggerRouter {
    private String path;
    private String routeMethod;
    private Router classAnnotation;
    private Class clazz;
    private Router methodAnnotation;
    private Method method;

    public SwaggerRouter(String path, String routeMethod, Router classAnnotation, Class clazz, Router methodAnnotation, Method method) {
        this.path = path;
        this.routeMethod = routeMethod;
        this.classAnnotation = classAnnotation;
        this.clazz = clazz;
        this.methodAnnotation = methodAnnotation;
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRouteMethod() {
        return routeMethod;
    }

    public void setRouteMethod(String routeMethod) {
        this.routeMethod = routeMethod;
    }

    public Router getClassAnnotation() {
        return classAnnotation;
    }

    public void setClassAnnotation(Router classAnnotation) {
        this.classAnnotation = classAnnotation;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public Router getMethodAnnotation() {
        return methodAnnotation;
    }

    public void setMethodAnnotation(Router methodAnnotation) {
        this.methodAnnotation = methodAnnotation;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }


}
