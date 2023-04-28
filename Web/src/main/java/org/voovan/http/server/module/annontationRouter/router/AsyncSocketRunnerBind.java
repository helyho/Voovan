package org.voovan.http.server.module.annontationRouter.router;

import org.voovan.http.server.HttpRequest;

/**
 * 异步路由的 Socket 绑定
 *  来自相同 ip 的请求会被绑定到同一个线程
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class AsyncSocketRunnerBind implements AsyncRunnerSelector {
    @Override
    public Integer select(HttpRequest httpRequest) {
        return httpRequest.getRemoteAddress().hashCode();
    }
}
