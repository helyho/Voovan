package org.hocate.http.message;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.hocate.http.message.packet.Body;
import org.hocate.http.message.packet.Cookie;
import org.hocate.http.message.packet.Header;
import org.hocate.http.message.packet.Part;
import org.hocate.http.message.packet.Part.PartType;
import org.hocate.http.message.packet.RequestProtocol;
import org.hocate.tools.THash;
import org.hocate.tools.TString;

/**
 * GET      请求获取Request-URI所标识的资源
 * POST     在Request-URI所标识的资源后附加新的数据
 * HEAD     请求获取由Request-URI所标识的资源的响应消息报头
 * PUT      请求服务器存储一个资源，并用Request-URI作为其标识
 * DELETE   请求服务器删除Request-URI所标识的资源
 * TRACE    请求服务器回送收到的请求信息，主要用于测试或诊断
 * CONNECT  保留将来使用
 * OPTIONS  请求查询服务器的性能，或者查询与资源相关的选项和需求
 * @author helyho
 *
 */

public class Request {
	private RequestProtocol protocol;
	private Header header;
	private List<Cookie> cookies;
	private Body body;
	private List<Part> parts;
	
	/**
	 * HTTP 请求的枚举对象
	 * @author helyho
	 *
	 */
	public enum RequestType{
		GET, POST, POST_URLENCODED, POST_MULTIPART, HEAD, PUT, DELETE, TRACE, CONNECT,OPTIONS,UNKNOWN
	}
	
	/**
	 * 构造函数
	 */
	public Request(){
		protocol = new RequestProtocol();
		header = new  Header();
		cookies = new Vector<Cookie>();
		body = new Body();
		parts = new Vector<Part>();
	}
	
	/**
	 * 获取协议对象
	 * @return
	 */
	public RequestProtocol protocol(){
		return protocol;
	}
	
	/**
	 * 获取 Header 对象
	 * @return
	 */
	public Header header(){
		return header;
	}
	
	/**
	 * 获取所有的Cookies对象,返回一个 List
	 * @return
	 */
	public List<Cookie> cookies(){
		return cookies;
	}
	
	/**
	 * 获取 Body 对象
	 * @return
	 */
	public Body body(){
		return body;
	}

	/**
	 * 获取所有的 Part 对象,返回一个 List
	 * @return
	 */
	public List<Part> parts(){
		return parts;
	}
	
	/**
	 * 获取请求类型
	 * @return RequestType枚举
	 */
	public RequestType getType(){
		switch(protocol.getMethod()){
		case "GET":
			return RequestType.GET;
		case "PUT":
			return RequestType.PUT;
		case "DELETE":
			return RequestType.DELETE;
		case "TRACE":
			return RequestType.TRACE;
		case "CONNECT":
			return RequestType.CONNECT;
		case "OPTIONS":
			return RequestType.OPTIONS;
		case "HEAD":
			return RequestType.HEAD;
		case "POST":
			if(header.get("Content-Type")!=null){
				if(header.get("Content-Type").contains("application/x-www-form-urlencoded")){
					return RequestType.POST_URLENCODED;
				}else if(header.get("Content-Type").contains("multipart/form-data")){
					return RequestType.POST_MULTIPART;
				}
			}else {
				return RequestType.POST;
			}
		default:
			return RequestType.UNKNOWN;
		}
	}
	
	/**
	 * 解码 QueryString 中通过 URLEncode 加密的内容
	 * @param queryString
	 * @return
	 */
	private static String decodeQueryString(String queryString){
		if(queryString!=null && !queryString.equals("")){
			String[] encodedValues = TString.searchByRegex(queryString,"=[^ \\&]+");
			for(String encodedValue : encodedValues){
				try {
					String decodeValueString = "="+URLDecoder.decode(encodedValue.substring(1, encodedValue.length()),"UTF-8")+"&";
					queryString = queryString.replace(encodedValue, decodeValueString);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
			}
			return TString.removeLastChar(queryString);
		}
		return null;
	}
	
	/**
	 * 获取QueryStirng
	 * 			将参数解密,或拼装成QueryString(用&符号分割的等号表达式)
	 * @return
	 */
	public String getQueryString(){
		//GET 请求类型的处理
		if(getType()==RequestType.GET){
			return decodeQueryString(protocol.getQueryString());
		}
		//POST_URLENCODED 请求类型的处理
		else if(getType()==RequestType.POST_URLENCODED){
			return decodeQueryString(body.getBodyString());
		}
		//POST_MULTIPART 请求类型的处理
		else if(getType().equals(RequestType.POST_MULTIPART)){
			String result = "";
			for(Part part : parts){
				if(part.getType()==PartType.TEXT){
					String name = part.header().get("name");
					String value = part.body().getBodyString();
					result+=name+"="+value+"&";
				}
			}
			return TString.removeLastChar(result);
		}
		return null;
	}
	
	/**
	 * 根据内容构造一些必要的 Header 属性
	 */
	private String initHeader(){
		
		if(body.getBodyBytes()!=null && body.getBodyBytes().length>0){
			header.put("Content-Length", Integer.toString(body.getBodyBytes().length));
		}
		//如果请求中包含 Part 的处理
		else if(parts.size()!=0){
			//产生新的boundary备用
			String boundary = THash.encryptBASE64(UUID.randomUUID().toString().getBytes());
			String contentType = "Content-Type: multipart/form-data; boundary="+boundary;
			header.put("Content-Type",contentType);
			return boundary;
		}
		return null;
	}
	
	/**
	 * 根据 Cookie 对象,生成  HTTP 请求中的 Cookie 字符串
	 * 		用于报文拼装
	 * @return
	 */
	private String genCookieString(){
		String cookieString = "";
		if(cookies.size()>0){
			cookieString +="Cookies: ";
		}
		for(Cookie cookie : cookies){
			cookieString += cookie.getName()+"="+cookie.getValue()+"; ";
		}
		if(cookies.size()>0){
			cookieString = cookieString.substring(0,cookieString.length()-2)+"\r\n";
		}
		return cookieString;
	}
	
	/**
	 * 根据对象的内容,构造 Http 请求报文
	 * @return
	 */
	public byte[] asBytes() {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		//Body 对应的 Header 预处理
		String boundary = initHeader();
		
		//报文组装
		try{
			//处理协议行
			outputStream.write(protocol.toString().getBytes()); 
			
			//处理 Header
			outputStream.write(header.toString().getBytes()); 
			
			//处理 Cookie
			outputStream.write(genCookieString().getBytes());
			
			//处理 Body
			//没有 parts 时直接写入包体
			if(body.getBodyBytes()!=null && body.getBodyBytes().length>0){
				String bodyString = "\r\n"+body.getBodyString();
				outputStream.write(bodyString.getBytes());
			}
			//有 parts 时按 parts 的格式写入 parts
			else {
				//Content-Type存在
				if(parts.size()!=0){
					outputStream.write("\r\n".getBytes());
					for(Part part : this.parts){
						outputStream.write( ("--"+boundary+"\r\n").getBytes());
						outputStream.write(part.toString().getBytes());
						outputStream.write(part.body().getBodyBytes());
						outputStream.write("\r\n".getBytes());
					}
					outputStream.write( ("--"+boundary+"--").getBytes() );
				}
			}
			
			outputStream.write("\r\n".getBytes());
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return outputStream.toByteArray();
	}
	
	@Override
	public String toString() { 
		return new String(asBytes());
	}
}
