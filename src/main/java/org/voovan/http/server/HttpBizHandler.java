package org.voovan.http.server;


/**
 * HTTP 服务业务处理句柄
 * 
 * 路由业务处理接口,方法类型的接口
 * 用于用户实现
 * @author helyho
 *
 */
public interface HttpBizHandler {
	public void Process(HttpRequest request,HttpResponse response) throws Exception ;	
}
