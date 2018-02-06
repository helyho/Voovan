package org.voovan.http.server;

/**
 * WebServer初始化类
 *
 * @author: helyho
 * Framework Framework.
 * WebSite: https://github.com/helyho/Framework
 * Licence: Apache v2 License
 */
public interface WebServerLifeCycle {
    public void init(WebServer webServer);
    
    public void destory(WebServer webServer);
}
