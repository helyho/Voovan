package org.voovan.http.server.module.annontationRouter.router;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.context.HttpFilterConfig;

import java.lang.reflect.Method;

/**
 *  注解路由拦截器
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public interface AnnotationRouterFilter {

    /**
     * 注解路由前置拦截方法
     * @param request http 请求对象
     * @param response http 响应对象
     * @param router 注解路由对象
     * @return null: 执行请求路由方法, 非 null: 返回值作为 http 请求的响应直接返回
     */
    public Object beforeInvoke(HttpRequest request, HttpResponse response, AnnotationRouter router);

    /**
     * 注解路由后置拦截方法
     * @param request http 请求对象
     * @param response http 响应对象
     * @param router 注解路由对象
     * @param result 执行路由方法返回的结果
     * @return null: 执行请求路由方法的结果作为响应, 非 null: 返回值作为 http 请求的响应直接返回
     */
    public Object afterInvoke(HttpRequest request, HttpResponse response, AnnotationRouter router, Object result);

    /**
     * 注解路由异常拦截方法
     *
     * @param request http 请求对象
     * @param response http 响应对象
     * @param router 注解路由对象
     * @param e 异常对象
     * @return  null: 执行默认异常处理, 非 null: 返回值作为 http 请求的响应直接返回
     */
    public Object exception(HttpRequest request, HttpResponse response, AnnotationRouter router, Exception e);
    /**
     * 响应发送到 Socket 之前的拦截 
     *
     * @param request http 请求对象
     * @param response http 响应对象
     * @param router 注解路由对象
     * @param respRet 响应发送前的
     * @return  null: 执行默认异常处理, 非 null: 返回值作为 http 请求的响应直接返回
     */
    public Object beforeSend(HttpRequest request, HttpResponse response, AnnotationRouter router,  Object respRet);

}
