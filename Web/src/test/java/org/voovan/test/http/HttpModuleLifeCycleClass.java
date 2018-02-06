package org.voovan.test.http;

import org.voovan.http.server.HttpModule;
import org.voovan.http.server.HttpModuleLifeCycle;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Framework Framework.
 * WebSite: https://github.com/helyho/Framework
 * Licence: Apache v2 License
 */
public class HttpModuleLifeCycleClass implements HttpModuleLifeCycle {
    @Override
    public void init(HttpModule httpModule) {
        Logger.info("HttpModule Init messsage");
    }

    @Override
    public void destory(HttpModule httpModule) {
        Logger.info("HttpModule destory messsage");
    }
}
