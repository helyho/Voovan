package org.voovan.http.server;

/**
 * 模块初始化类
 *
 * @author: helyho
 * Project: Framework
 * Create: 2017/10/12 12:02
 */
public interface HttpModuleLifeCycle {
    public void init(HttpModule httpModule);
    public void destory(HttpModule httpModule);
}
