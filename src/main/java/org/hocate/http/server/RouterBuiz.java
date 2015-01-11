package org.hocate.http.server;

import org.hocate.http.message.HttpRequest;
import org.hocate.http.message.HttpResponse;

/**
 * 路由业务处理接口,方法类型的接口
 * @author helyho
 *
 */
public interface RouterBuiz {
	public void Process(HttpRequest request,HttpResponse response) throws Exception ;	
}
