package org.voovan.http.server.module.annontationRouter.router;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.context.HttpFilterConfig;

import java.lang.reflect.Method;

/**
 *  注解路由拦截器
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public interface AnnotationRouterFilter {

    public final static AnnotationRouterFilter EMPYT = new AnnotationRouterFilter() {
        @Override
        public Object beforeInvoke(HttpRequest request, HttpResponse response, Method method) {
            return null;
        }

        @Override
        public Object afterInvoke(HttpRequest request, HttpResponse response, Method method, Object result) {
            return null;
        }

        @Override
        public Object exception(HttpRequest request, HttpResponse response, Method method, Exception e) {
            return null;
        }
    };

    /**
     * 注解路由前置拦截方法
     * @param request http 请求对象
     * @param response http 响应对象
     * @param method 请求执行的路由方法
     * @return null: 执行请求路由方法, 非 null: 返回值作为 http 请求的响应直接返回
     */
    public Object beforeInvoke(HttpRequest request, HttpResponse response, Method method);

    /**
     * 注解路由后置拦截方法
     * @param request http 请求对象
     * @param response http 响应对象
     * @param method 请求执行的路由方法
     * @param result 执行路由方法返回的结果
     * @return null: 执行请求路由方法的结果作为响应, 非 null: 返回值作为 http 请求的响应直接返回
     */
    public Object afterInvoke(HttpRequest request, HttpResponse response, Method method, Object result);

    /**
     * 注解路由异常拦截方法
     *
     * @param request http 请求对象
     * @param response http 响应对象
     * @param method 请求执行的路由方法
     * @param e 异常对象
     * @return  null: 执行默认异常处理, 非 null: 返回值作为 http 请求的响应直接返回
     */
    public Object exception(HttpRequest request, HttpResponse response, Method method, Exception e);
}
