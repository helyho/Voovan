package org.hocate.http.client;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hocate.http.message.HttpRequest;
import org.hocate.http.message.HttpRequest.RequestType;
import org.hocate.http.message.HttpResponse;
import org.hocate.http.message.packet.Header;
import org.hocate.http.message.packet.Part;
import org.hocate.network.aio.AioSocket;
import org.hocate.network.messagePartition.HttpMessageParter;
import org.hocate.tools.TEnv;
import org.hocate.tools.TString;


public class HttpClient {
	
	private AioSocket socket;
	private HttpClientHandler clientHandler; 
	private HttpRequest request;
	private Map<String, Object> parameters;
	
	public  HttpClient(String urlString) {
		try {
			URL url = new URL(urlString);
			String hostString = url.getHost();
			int port = url.getPort();
			
			request = new HttpRequest();
			//初始化
			request.protocol().setPath(url.getPath().isEmpty()?"/":url.getPath());
			request.header().put("Host", hostString);
			request.header().put("Pragma", "no-cache");
			request.header().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			request.header().put("User-Agent", "Http Client");
			request.header().put("Accept-Encoding","gzip,deflate,sdch");
			
			socket = new AioSocket(hostString, port==-1?80:port, 5000);
			parameters = new HashMap<String, Object>();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setMethod(String method){
		request.protocol().setMethod(method);
	}
	
	public Header getHeader(){
		return request.header();
	}
	
	public Map<String,Object> getParameters(){
		return parameters;
	}
	
	public HttpClient putParameters(String name,Object value){
		parameters.put(name, value);
		return this;
	}
	
	private String getQueryString(){
		String queryString = "";
		try {
			for (Entry<String, Object> parameter : parameters.entrySet()) {
				queryString += parameter.getKey()
						+ "="
						+ URLEncoder.encode(parameter.getValue().toString(), "UTF-8")
						+ "&";
			}
			queryString = queryString.length()>0?TString.removeLastChar(queryString):queryString;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return queryString;
	}
	
	private void buildRequest(){

		if (request.getType() == RequestType.GET) {
			String queryString = getQueryString().isEmpty() ? "" :"?"+getQueryString(); 
			request.protocol().setPath(request.protocol().getPath() + queryString);
		}
		else if(request.getType() == RequestType.POST && request.parts().size()!=0){
			
			try{
				for (Entry<String, Object> parameter : parameters.entrySet()) {
					Part part = new Part();
					part.header().put("name", parameter.getKey());
					part.body().writeBytes(URLEncoder.encode(parameter.getValue().toString(),"UTF-8").getBytes());
					request.parts().add(part);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		else if(request.getType() == RequestType.POST && request.body()!=null){
			request.body().writeBytes(getQueryString().getBytes());
		}
	}
	
	public HttpResponse Connect() throws Exception{
		buildRequest();
		
		clientHandler = new HttpClientHandler(request);
		socket.handler(clientHandler);
		socket.filterChain().add(new HttpClientFilter());
		socket.messageParter(new HttpMessageParter());
		System.out.println(request);
		socket.start();
		
		//等待获取 response并返回
		while(clientHandler.getResponse()==null){
			TEnv.sleep(1);
		}
		
		return clientHandler.getResponse();
	}
	
	public static void main(String[] args) throws Exception {
		HttpClient client = new HttpClient("http://www.sohu.com/");
		client.setMethod("GET");
		long t = System.currentTimeMillis();
		System.out.println("start :"+System.currentTimeMillis());
		HttpResponse response = client.Connect();
		System.out.print("====");
		System.out.println(System.currentTimeMillis()-t);
		
		String bodyString = response.body().toString();
		//substring(0,300)+
		//"\r\n<..............................................................................>\r\n"+
		System.out.println("body length:"+bodyString.length());
	}
}
