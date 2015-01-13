package org.hocate.http.server;

import org.hocate.http.message.Request;
import org.hocate.http.message.Response;

/**
 * 路由业务处理接口,方法类型的接口
 * @author helyho
 *
 */
public interface RouterBuiz {
	public void Process(Request request,Response response) throws Exception ;	
}
