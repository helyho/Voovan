package org.hocate.test.network.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.hocate.http.HttpPacketParser;
import org.hocate.http.message.HttpRequest;
import org.hocate.http.message.HttpResponse;
import org.hocate.network.IoHandler;
import org.hocate.network.IoSession;


public class HttpRequestHandlerTest implements IoHandler {

	private String hostNameString;
	
	public HttpRequestHandlerTest(String hostNameString){
		this.hostNameString = hostNameString;
	}
	@Override
	public Object onConnect(IoSession session) {
		System.out.println("onConnect");
		HttpRequest request = new HttpRequest();
		request.header().put("Host", hostNameString);
		request.header().put("Connection", "keep-alive");
		
		return ByteBuffer.wrap(request.asBytes());
	}

	@Override
	public void onDisconnect(IoSession session) {
		System.out.println("onDisconnect");
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		System.out.println("onRecive string "+obj.toString()+"["+session.remoteAddress()+":"+session.remotePort()+"]" +" "+obj.getClass().getName());
		
		try {
			HttpResponse response = HttpPacketParser.parseResponse(new ByteArrayInputStream(((ByteBuffer)obj).array()));
			System.out.println(response);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		session.close();
		return obj;
	}

	@Override
	public void onException(IoSession session, Exception e) {
		System.out.println("Exception");
		e.printStackTrace();
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		System.out.println("onSent "+obj);
	}

}
