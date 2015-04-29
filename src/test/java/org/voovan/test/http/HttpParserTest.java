package org.voovan.test.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.voovan.http.message.HttpParser;
import org.voovan.http.message.Request;
import org.voovan.tools.log.Logger;

public class HttpParserTest {
	public static void main(String[] args) throws IOException {
		String httpRequestString = 
		 
//				"POST /test/t HTTP/1.1\r\n"+
//				"Connection: keep-alive\r\n"+
//				"Content-Type: application/x-www-form-urlencoded\r\n"+
//				"Content-Length: 34\r\n"+
//				"User-Agent: Jakarta Commons-HttpClient/3.1\r\n"+
//				"Host: 127.0.0.1:1031\r\n"+
//				"\r\n"+
//				"name=helyho&age=32%3D&address=wlmq\r\n"+
//				"\r\n";
			
				"GET /test/t HTTP/1.1\r\n"+
				"Connection: keep-alive\r\n"+
				"UserAgent: Jakarta Commons-HttpClient/3.1\r\n"+
				"Host: 127.0.0.1:1031\r\n"+
				"\r\n";
	
//				"POST /test/t HTTP/1.1\r\n"+
//				"Connection: keep-alive\r\n"+
//				"Content-Type: multipart/form-data; boundary=ujjLiiJBznFt70fG1F4EUCkIupn7H4tzm\r\n"+
//				"Content-Length: 329\r\n"+
//				"User-Agent: Jakarta Commons-HttpClient/3.1\r\n"+
//				"Cookie: BAIDUID=57939E50D6B2A0B23D20CA330C89E290:FG=1; BAIDUPSID=57939E50D6B2A0B23D20CA330C89E290;\r\n"+
//				"Host: 127.0.0.1:1031\r\n"+
//				"\r\n"+
//				"--ujjLiiJBznFt70fG1F4EUCkIupn7H4tzm\r\n"+
//				"Content-Disposition: form-data; name=\"name\"; filename=\"hh.jpg\"\r\n"+
//				"\r\n"+
//				"helyho\r\n"+
//				"--ujjLiiJBznFt70fG1F4EUCkIupn7H4tzm\r\n"+
//				"Content-Disposition: form-data; name=\"age\"\r\n"+
//				"\r\n"+
//				"32\r\n"+
//				"--ujjLiiJBznFt70fG1F4EUCkIupn7H4tzm\r\n"+
//				"Content-Disposition: form-data; name=\"address\" filename=\"1.jpg\"\r\n"+
//				"\r\n"+
//		 		"wlmq\r\n"+
//				"--ujjLiiJBznFt70fG1F4EUCkIupn7H4tzm--\r\n\r\n";
		
		Request request = HttpParser.parseRequest(new ByteArrayInputStream(httpRequestString.getBytes()));
		Logger.simple(request.toString());
	}
}
