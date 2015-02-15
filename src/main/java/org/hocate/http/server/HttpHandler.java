package org.hocate.http.server;


/**
 * 路由业务处理接口,方法类型的接口
 * @author helyho
 *
 */
public interface HttpHandler {
	public void Process(HttpRequest request,HttpResponse response) throws Exception ;	
}
