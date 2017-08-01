package org.voovan.test.http.router;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
@TestAnnotation
public class HttpTestRouter implements HttpRouter {
    @Override
    public void process(HttpRequest request, HttpResponse response) throws Exception {
        response.write("this router is config in /conf/web.json -> router node.");
    }
}
