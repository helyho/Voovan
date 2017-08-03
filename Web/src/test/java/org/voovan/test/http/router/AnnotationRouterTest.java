package org.voovan.test.http.router;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpSession;
import org.voovan.http.server.router.annotation.Cookie;
import org.voovan.http.server.router.annotation.Head;
import org.voovan.http.server.router.annotation.Param;
import org.voovan.http.server.router.annotation.Router;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Router("/annon")
public class AnnotationRouterTest {

    @Router
    public String index(){
        return "index";
    }

    @Router
    public String params(@Param("bb") String aa, @Param("aa") int bb){
        return "params: aa=" + aa + ", bb=" + bb;
    }

    @Router
    public String cookie(@Cookie("_ga") String aa, HttpRequest request, HttpResponse response, HttpSession session){
        return "cookie: " + aa + " " +request +" " +response +" " +session ;
    }

    @Router
    public String head(@Head("Connection") String aa){
        return "head: " + aa;
    }

    @Router("/sp")
    public String seqparams(String aa, int bb){
        return "params: param1=" + aa + ", param2=" + bb;
    }
}
