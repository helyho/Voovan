package org.voovan.http.server;


/**
 * HTTP 路由业务处理器
 * 
 * 路由业务处理接口,方法类型的接口
 * 用于用户实现
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface HttpRouter {
	public void process(HttpRequest request,HttpResponse response) throws Exception ;	
}
