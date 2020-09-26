package org.voovan.test.http.router;

import org.voovan.Global;
import org.voovan.http.HttpContentType;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpSession;
import org.voovan.http.server.module.annontationRouter.annotation.*;
import org.voovan.http.server.module.annontationRouter.swagger.annotation.ApiModel;
import org.voovan.http.server.module.annontationRouter.swagger.annotation.ApiProperty;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;

import java.io.IOException;
import java.util.List;

//将当前类注解为一个请求路由处理类, 采用默认的请求方法 GET
//为当前类指定一个请求路径为:/annon，如果不指定则默认的路径为/AnnotationRouterTest
@Router(value = "/annon", singleton = true)
public class AnnotationRouterTest {

    private String lastPath = "";

    //将当前方法注解为一个请求路由
    //当前方法的请求路由为:/annon/index,采用方法名作为路由的路径
    @Router(contentType = HttpContentType.IMAGE_GIF)
    public Object index(){
        String oldPath = lastPath;
        lastPath = "/annon/index, time:" + System.currentTimeMillis();
        return TObject.asMap("index", 1, "seq", 133832);//"index, lastPath="+oldPath;
    }

    //将当前方法注解为一个请求路由
    //当前方法的请求路由为:/annon/params,采用方法名作为路由的路径
    //将请求中名为 aa 的 参数在调用时注入成方法的 aa 参数
    //将请求中名为 bb 的 参数在调用时注入成方法的 bb 参数
    @Router(method = "GET")
    //支持同一方法多个路由
    //同时支持 GET 和 POST 方法
//    @Router(path = "/params/r1", method = {"GET", "POST"})
    public String params(@Param(value = "aa", example = "aa example") String aa,
                         @Param(value = "bb", example = "111222") int bb){
        String oldPath = lastPath;
        lastPath = "/annon/parms, time:" + System.currentTimeMillis();
        return "params: aa=" + aa + ", bb=" + bb+ ", lastPath="+oldPath;
    }

    //如果 Request 的 body 是 json 形式则直接解释出 json 中的key 作为参数注入路由方法
    //下面例子会将{"data": "testdata", "number": 1}中的 testdata 注入到方法参数 data
    //下面例子会将{"data": "testdata", "number": 1}中的 1 注入到方法参数 number
    @Router(value = "bodyParmas", method = "POST")
    public String bodyParmas(@BodyParam(value="data", isRequire=false, example = "data example") List<String> data,
                             @BodyParam(value = "number") A number){
        return data + " " + number;
    }


    @Router(method = "POST", hide = true)
    public void asyncBodyParmas(@BodyParam(value="data", isRequire=false, defaultVal = "123123") String data,
                                @BodyParam("number") int number,
                                @BodyParam(value = "array", isRequire = false) List<String> array,
                                HttpResponse response){
        HttpResponse asyncResponse = response.getAsyncResponse();
        Global.getThreadPool().execute(()->{
            TEnv.sleep(50);
            asyncResponse.write(System.currentTimeMillis() + " " + data + " " + number);
            try {
                asyncResponse.send();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
    public String head(@Header("Connection") String aa){
        String oldPath = lastPath;
        lastPath = "/annon/head, time:" + System.currentTimeMillis();
        return "head: " + aa+ ", lastPath="+oldPath;
    }

    //将当前方法注解为一个请求路由, 并指定请求的方法为 POST,在这里 POST 会覆盖类注解的请求方法 GET
    //当前方法的请求路由为:/annon/body,采用方法名作为路由的路径
    //将请求中报文在调用时注入成方法的 aa 参数,在 resetful 中经常被使用到
    @Router(method="POST")
    public String body(@Body(description = "123123", defaultVal = "{'a_name': '123'}", example = "{'a_name': '123'}") B aa){
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
    public B error(String aa, int bb){
       throw new RuntimeException("my exception.");
    }

    @ApiModel("A classes")
    public class A {
        @ApiProperty(value = "a_name", isRequire = false, example = "11111")
        String a_name;

        @ApiProperty(hidden = true)
        String hiddes;

        @Override
        public String toString() {
            return "A{" +
                    "a_name='" + a_name + '\'' +
                    '}';
        }
    }

    @ApiModel("B classes")
    public class B {
        String b_name;
        A a;
    }
}
