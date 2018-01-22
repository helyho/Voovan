package org.voovan.test.http.router;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpSession;
import org.voovan.http.server.module.annontationRouter.annotation.*;

//将当前类注解为一个请求路由处理类, 采用默认的请求方法 GET
//为当前类指定一个请求路径为:/annon，如果不指定则默认的路径为/AnnotationRouterTest
@Router(value = "/annon", singleton = true)
public class AnnotationRouterTest {

    private String lastPath = "";

    //将当前方法注解为一个请求路由
    //当前方法的请求路由为:/annon/index,采用方法名作为路由的路径
    @Router
    public String index(){
        String oldPath = lastPath;
        lastPath = "/annon/index, time:" + System.currentTimeMillis();
        return "index, lastPath="+oldPath;
    }

    //将当前方法注解为一个请求路由
    //当前方法的请求路由为:/annon/params,采用方法名作为路由的路径
    //将请求中名为 aa 的 参数在调用时注入成方法的 aa 参数
    //将请求中名为 bb 的 参数在调用时注入成方法的 bb 参数
    @Router
//    @Check(name = "bb", value = "null", valueMethod = "org.voovan.test.http.router.AnnotationRouterTest#checkMethod", response = "bb is error")
    @Check(name = "bb", value = "null", responseMethod = "org.voovan.test.http.router.AnnotationRouterTest#checkMethod")
    @Check(name = "aa", value = "0")
    public String params(@Param("bb") String aa, @Param("aa") int bb){
        String oldPath = lastPath;
        lastPath = "/annon/parms, time:" + System.currentTimeMillis();
        return "params: aa=" + aa + ", bb=" + bb+ ", lastPath="+oldPath;
    }

    public static String checkMethod(Object obj){
        return null;
    }

    //将当前方法注解为一个请求路由
    //当前方法的请求路由为:/annon/cookie,采用方法名作为路由的路径
    //将Cookie中名为 _ga 的 参数在调用时注入成方法的 aa 参数
    //同时将请求对象,响应对象和会话对象在调用时注入到方法的参数
    @Router
    public String cookie(@Cookie("_ga") String aa, HttpRequest request, HttpResponse response, HttpSession session){
        String oldPath = lastPath;
        lastPath = "/annon/cookie, time:" + System.currentTimeMillis();
        return "cookie: " + aa + " " +request +" " +response +" " +session + ", lastPath="+oldPath;
    }

    //将当前方法注解为一个请求路由
    //当前方法的请求路由为:/annon/head,采用方法名作为路由的路径
    //将head中名为 Connection 的属性在调用时注入成方法的 aa 参数
    @Router
    public String head(@Head("Connection") String aa){
        String oldPath = lastPath;
        lastPath = "/annon/head, time:" + System.currentTimeMillis();
        return "head: " + aa+ ", lastPath="+oldPath;
    }

    //将当前方法注解为一个请求路由, 并指定请求的方法为 POST,在这里 POST 会覆盖类注解的请求方法 GET
    //当前方法的请求路由为:/annon/body,采用方法名作为路由的路径
    //将请求中报文在调用时注入成方法的 aa 参数,在 resetful 中经常被使用到
    @Router(method="POST")
    public String body(@Body String aa){
        String oldPath = lastPath;
        lastPath = "/annon/body, time:" + System.currentTimeMillis();
        return "body: " + aa + ", lastPath="+oldPath;
    }

    //将当前方法注解为一个请求路由, 并指定请求的访问路径为 sp
    //当前方法的请求路由为:/annon/sp
    //将请求中报文在调用时的参数按照顺序在调用方法时注入成方法的参数
    @Router("/sp")
    public String seqparams(String aa, int bb){
        String oldPath = lastPath;
        lastPath = "/annon/sp, time:" + System.currentTimeMillis();
        return "seqparams: param1=" + aa + ", param2=" + bb + ", lastPath="+oldPath;
    }

    @Router()
    public String error(String aa, int bb){
       throw new RuntimeException("my exception.");
    }
}
