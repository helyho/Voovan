package org.voovan.http.server.module.annontationRouter.router;

import org.voovan.http.server.HttpRequest;

/**
 * 异步路由的线程选择器
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public interface AsyncRunnerSelector {
    public Integer select(HttpRequest request);
}
