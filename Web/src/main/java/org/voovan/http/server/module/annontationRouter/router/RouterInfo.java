package org.voovan.http.server.module.annontationRouter.router;

import org.voovan.http.server.module.annontationRouter.annotation.Router;

import java.lang.reflect.Method;

/**
 * Class name
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RouterInfo {
    private String url;
    private String routeMethod;
    private Router classAnnotation;
    private Class clazz;
    private Router methodAnnotation;
    private Method method;

    public RouterInfo(String url, String routeMethod, Router classAnnotation, Class clazz, Router methodAnnotation, Method method) {
        this.url = url;
        this.routeMethod = routeMethod;
        this.classAnnotation = classAnnotation;
        this.clazz = clazz;
        this.methodAnnotation = methodAnnotation;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((routeMethod == null) ? 0 : routeMethod.hashCode());
        result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RouterInfo other = (RouterInfo) obj;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (routeMethod == null) {
            if (other.routeMethod != null)
                return false;
        } else if (!routeMethod.equals(other.routeMethod))
            return false;
        if (clazz == null) {
            if (other.clazz != null)
                return false;
        } else if (!clazz.equals(other.clazz))
            return false;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        return true;
    }

    

}
