package org.voovan.http.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.voovan.http.message.packet.Cookie;
import org.voovan.http.message.packet.Part;
import org.voovan.tools.TObject;
import org.voovan.tools.TStream;
import org.voovan.tools.TString;
import org.voovan.tools.TZip;


/**
 * Http 报文解析类
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpParser {
	
	private static final String HTTP_PROTOCOL = "HTTP/";
	private static final String HTTP_COOKIE = "Cookie";
	
	private static final String FL_METHOD = "FL_Method";
	private static final String FL_PATH="FL_Path";
	private static final String FL_PROTOCOL="FL_Protocol";
	private static final String FL_VERSION="FL_Version";
	private static final String FL_STATUS="FL_Status";
	private static final String FL_STATUSCODE="FL_StatusCode";
	
	private static final String HEAD_CONTENT_ENCODING="Content-Encoding";
	private static final String HEAD_CONTENT_TYPE = "Content-Type";
	private static final String HEAD_TRANSFER_ENCODING = "Transfer-Encoding";
	private static final String HEAD_CONTENT_LENGTH = "Content-Length";
	
	private static final String STATIC_VALUE = "value";
	private static final String STATIC_PARTS = "Parts";
	
	/**
	 * 私有构造函数
	 * 该类无法被实例化
	 */
	private HttpParser(){
		
	}
	
	/**
	 * 解析协议信息
	 * 		http 头的第一行
	 * @param protocolLine
	 * @throws UnsupportedEncodingException 
	 */
	private static Map<String, Object> parseProtocol(String protocolLine) throws UnsupportedEncodingException{
		Map<String, Object> protocol = new HashMap<String, Object>();
		//请求方法
		String[] lineSplit = protocolLine.split(" ");
		if(protocolLine.indexOf(HTTP_PROTOCOL)!=0){
			protocol.put(FL_METHOD, lineSplit[0]);
			//请求路径和请求串
			String[] pathSplit = lineSplit[1].split("\\?");
			protocol.put(FL_PATH, pathSplit[0]);
			if(pathSplit.length==2){
				protocol.put(STATIC_VALUE, pathSplit[1].getBytes());
			}
			//协议和协议版本
			String[] protocolSplit= lineSplit[2].split("/");
			protocol.put(FL_PROTOCOL, protocolSplit[0]);
			protocol.put(FL_VERSION, protocolSplit[1]);
		}else if(protocolLine.indexOf(HTTP_PROTOCOL)==0){
			String[] protocolSplit= lineSplit[0].split("/");
			protocol.put(FL_PROTOCOL, protocolSplit[0]);
			protocol.put(FL_VERSION, protocolSplit[1]);
			protocol.put(FL_STATUS, lineSplit[1]);
			protocol.put(FL_STATUSCODE, lineSplit[2]);
		}
		return protocol;
	}
	
	/**
	 * 解析 HTTP Header属性行
	 * @param propertyLine
	 * @return
	 */
	private static Map<String,String> parsePropertyLine(String propertyLine){
		Map<String,String> property = new HashMap<String, String>();
		String[] propertySplit = propertyLine.split(": ");
		if(propertySplit.length==2){
			String propertyName = propertySplit[0];
			String properyValue = propertySplit[1];
			property.put(propertyName, properyValue);
		}
		return property;
	}
	
	/**
	 * 解析字符串中的所有等号表达式成 Map
	 * @param str
	 * @return
	 */
	public static Map<String, String> getEqualMap(String str){
		Map<String, String> equalMap = new HashMap<String, String>();
		String[] searchedStrings = TString.searchByRegex(str,"([^ ;,]+=[^ ;,]+)");
		for(String groupString : searchedStrings){
			//这里不用 split 的原因是有可能等号后的值字符串中出现等号
			String[] equalStrings = new String[2];
			int equalCharIndex= groupString.indexOf("=");
			equalStrings[0] = groupString.substring(0,equalCharIndex);
			equalStrings[1] = groupString.substring(equalCharIndex+1,groupString.length());
			if(equalStrings.length==2){
				String key = equalStrings[0];
				String value = equalStrings[1];
				if(value.startsWith("\"") && value.endsWith("\"")){
					value = value.substring(1,value.length()-1);
				}
				equalMap.put(key, value);
			}
		}
		return equalMap;
	}
	
	/**
	 * 获取HTTP 头属性里等式的值
	 * 		可以从字符串 Content-Type: multipart/form-data; boundary=ujjLiiJBznFt70fG1F4EUCkIupn7H4tzm
	 * 		直接解析出boundary的值.
	 * 		使用方法:getPerprotyEqualValue(packetMap,"Content-Type","boundary")获得ujjLiiJBznFt70fG1F4EUCkIupn7H4tzm
	 * @param propertyName
	 * @param valueName
	 * @return
	 */
	private static String getPerprotyEqualValue(Map<String,Object> packetMap,String propertyName,String valueName){
		String propertyValue = packetMap.get(propertyName).toString();//"Content-Type"
		Map<String, String> equalMap = getEqualMap(propertyValue);
		return equalMap.get(valueName); 
	}
	
	/**
	 * 处理消息的Cookie
	 * @param packetMap
	 * @param cookieLine
	 */
	@SuppressWarnings("unchecked")
	private static void parseCookie(Map<String, Object> packetMap,String cookieLine){
		if(!packetMap.containsKey(HTTP_COOKIE)){
			packetMap.put(HTTP_COOKIE, new Vector<Map<String, String>>());
		}
		List<Map<String, String>> cookies = (List<Map<String, String>>) packetMap.get(HTTP_COOKIE);
		
		//解析 Cookie 行
		Map<String, String>cookieMap = getEqualMap(cookieLine);
		
		
		//响应 response 的 cookie 形式 一个cookie 一行
		if(cookieLine.contains("Set-Cookie")){
			//处理非键值的 cookie 属性
			if(cookieLine.toLowerCase().contains("httponly")){
				cookieMap.put("httponly", "");
			}
			if(cookieLine.toLowerCase().contains("secure")){
				cookieMap.put("secure", "");
			}
			cookies.add(cookieMap);
		}
		//请求 request 的 cookie 形式 多个cookie 一行
		else if(cookieLine.contains(HTTP_COOKIE)){
			for(Entry<String,String> cookieMapEntry: cookieMap.entrySet()){
				HashMap<String, String> cookieOneMap = new HashMap<String, String>();
				cookieOneMap.put(cookieMapEntry.getKey(), cookieMapEntry.getValue());
				cookies.add(cookieOneMap);
			}
		}
		
	}
	
	/**
	 * 处理 body 段
	 * 		判断是否使用 GZIP 压缩,如果使用则解压缩后返回,如果没有压缩则直接返回
	 * @param packetMap
	 * @param contentBytes
	 * @return
	 * @throws IOException
	 */
	private static byte[] dealBodyContent(Map<String, Object> packetMap,byte[] contentBytes) throws IOException{
		byte[] bytesValue = new byte[0];
		
		//是否支持 GZip
		boolean isGZip = packetMap.get(HEAD_CONTENT_ENCODING)==null?false:packetMap.get(HEAD_CONTENT_ENCODING).toString().contains("gzip");
		
		//如果是 GZip 则解压缩
		if(isGZip && contentBytes.length>0){
			bytesValue =TZip.decodeGZip(contentBytes);
		} else {
			bytesValue = TObject.nullDefault(contentBytes,new byte[0]);
		}
		return bytesValue;
	}
	
	/**
	 * 解析 HTTP 报文
	 * 		解析称 Map 形式,其中:
	 * 			1.protocol 解析成 key/value 形式
	 * 			2.header   解析成 key/value 形式
	 * 			3.cookie   解析成 List<Map<String,String>> 形式
	 * 			3.part     解析成 List<Map<Stirng,Object>>(因为是递归,参考 HTTP 解析形式) 形式
	 * 			5.body     解析成 key="value" 的Map 元素
	 * @param source
	 * @throws IOException 
	 */
	public static Map<String, Object> parser(InputStream sourceInputStream) throws IOException{
		Map<String, Object> packetMap = new HashMap<String, Object>();

		int headerLength = 0;
		boolean isBodyConent = false;
		//按行遍历HTTP报文
		for(String currentLine = TStream.readLine(sourceInputStream);
			currentLine!=null;
			currentLine = TStream.readLine(sourceInputStream)){
			
			//空行分隔处理,遇到空行标识下面有可能到内容段
			if(currentLine.equals("")){
				//1. Method 是 null 的请求,代表是在解析 chunked 内容,则 isBodyConent = true
				//2. Method 不是 Get 方法的请求,代表有 body 内容段,则 isBodyConent = true
				if(packetMap.get(FL_METHOD)==null || !packetMap.get(FL_METHOD).equals("GET")){
					isBodyConent = true;
				}
			}
			
			//解析 HTTP 协议行
			if(!isBodyConent && currentLine.contains("HTTP")){
				packetMap.putAll(parseProtocol(currentLine));
			}
			
			//处理 cookie 和 header
			if(!isBodyConent){
				if(currentLine.contains(HTTP_COOKIE)){
					parseCookie(packetMap,currentLine);
				}else{
					packetMap.putAll(parsePropertyLine(currentLine));
				}
			}
			
			
			//解析 HTTP 请求 body
			if(isBodyConent){
				String contentType =packetMap.get(HEAD_CONTENT_TYPE)!=null?packetMap.get(HEAD_CONTENT_TYPE).toString():"";
				String transferEncoding = packetMap.get(HEAD_TRANSFER_ENCODING)==null ? "" : packetMap.get(HEAD_TRANSFER_ENCODING).toString();
				
				//1. 解析 HTTP 的 POST 请求 body part
				 if(contentType.contains("multipart/form-data")){
					//用来保存 Part 的 list
					List<Map<String, Object>> bodyPartList = new ArrayList<Map<String, Object>>();
					
					//取boundary 用于 part 内容分段
					String boundary = HttpParser.getPerprotyEqualValue(packetMap,HEAD_CONTENT_TYPE,"boundary");
					
					for(byte[] spliteBytes = TStream.readWithSplit(sourceInputStream, ("--"+boundary).getBytes());
							sourceInputStream.available()>0;
							spliteBytes = TStream.readWithSplit(sourceInputStream, ("--"+boundary).getBytes())){
						
						if(spliteBytes!=null){
							spliteBytes = Arrays.copyOfRange(spliteBytes, 2, spliteBytes.length-2);
							//递归调用 pareser 方法解析
							Map<String, Object> partMap = parser(new ByteArrayInputStream(spliteBytes));
							//加入bodyPartList中
							bodyPartList.add(partMap);
						}
						
					}
					//将存有多个 part 的 list 放入packetMap
					packetMap.put(STATIC_PARTS, bodyPartList);
				}
				
				//2. 解析 HTTP 响应 body 内容段的 chunked 
				else if(transferEncoding.equals("chunked")){
					
					byte[] chunkedBytes = new byte[0];
					for(String chunkedLengthLine = TStream.readLine(sourceInputStream);
							chunkedLengthLine!=null && !chunkedLengthLine.equals("0");
							chunkedLengthLine = TStream.readLine(sourceInputStream)){
						
						//读取chunked长度
						int chunkedLength = Integer.parseInt(chunkedLengthLine,16);
						
						//按长度读取chunked内容
						byte[] chunkedPartBytes  = TStream.read(sourceInputStream,chunkedLength);
						
						//如果多次读取则拼接
						chunkedBytes = TStream.byteArrayConcat(chunkedBytes, chunkedBytes.length, chunkedPartBytes, chunkedPartBytes.length);
						
						//跳过换行符号
						sourceInputStream.read();
						sourceInputStream.read();
					}
					byte[] value = dealBodyContent(packetMap, chunkedBytes);
					packetMap.put(STATIC_VALUE, value);
				}
				//3. HTTP(请求和响应) 报文的内容段中Content-Length 提供长度,按长度读取 body 内容段
				else if(packetMap.containsKey(HEAD_CONTENT_LENGTH)){
					byte[] contentBytes = new byte[0];
					int contentLength = Integer.parseInt(packetMap.get(HEAD_CONTENT_LENGTH).toString());
					contentBytes = TStream.read(sourceInputStream,contentLength);
					byte[] value = dealBodyContent(packetMap, contentBytes);
					packetMap.put(STATIC_VALUE, value);
				}
				//4. 容错,没有标识长度则默认读取全部内容段
				else if(packetMap.get(STATIC_VALUE)==null || packetMap.get(STATIC_VALUE).equals("")){
					byte[] contentBytes = TStream.readAll(sourceInputStream);
					if(contentBytes!=null && contentBytes.length>0){
						contentBytes = Arrays.copyOf(contentBytes, contentBytes.length);
					}
					byte[] value = dealBodyContent(packetMap, contentBytes);
					packetMap.put(STATIC_VALUE, value);
				}

				break;
			}
			if(!isBodyConent){
				headerLength = headerLength+currentLine.length()+2;
			}
		}
		
		sourceInputStream.close();
		return packetMap;
	}
	
	/**
	 * 解析报文成 HttpRequest 对象
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static Request parseRequest(InputStream inputStream) throws IOException{
		Map<String, Object> parsedPacket = parser(inputStream);
		
		//如果解析的Map为空,则直接返回空
		if(parsedPacket==null || parsedPacket.isEmpty()){
			return null;
		}
		
		Request request = new Request();
		//填充报文到请求对象
		Set<Entry<String, Object>> parsedItems= parsedPacket.entrySet();
		for(Entry<String, Object> parsedPacketEntry: parsedItems){
			String key = parsedPacketEntry.getKey();
			switch (key) {
			case FL_METHOD:
				request.protocol().setMethod(parsedPacketEntry.getValue().toString());
				break;
			case FL_PROTOCOL:
				request.protocol().setProtocol(parsedPacketEntry.getValue().toString());
				break;
			case FL_VERSION:	
				request.protocol().setVersion(Float.valueOf(parsedPacketEntry.getValue().toString()));
				break;
			case FL_PATH:	
				request.protocol().setPath(parsedPacketEntry.getValue().toString());
				break;
			case HTTP_COOKIE:
				List<Map<String, String>> cookieMap = (List<Map<String, String>>)parsedPacket.get(HTTP_COOKIE);
				//遍历 Cookie,并构建 Cookie 对象
				for(Map<String,String> cookieMapItem : cookieMap){
					Cookie cookie = Cookie.buildCookie(cookieMapItem);
					request.cookies().add(cookie);
				}
				break;
			case STATIC_VALUE:
				byte[] value = (byte[])(parsedPacketEntry.getValue());
				//如果是 GET 请求,则分析出来的内容(parsedPacket)中的 value 是 QueryString
				if(parsedPacket.get(FL_METHOD).equals("GET")){
					request.protocol().setQueryString(new String(value));
				} else {
					request.body().write(value);
				}
				break;
			case STATIC_PARTS:
				List<Map<String, Object>> parsedParts = (List<Map<String, Object>>)(parsedPacketEntry.getValue());
				//遍历 part List,并构建 Part 对象
				for(Map<String, Object> parsedPartMap : parsedParts){
					Part part = new Part();
					//将 part Map中的值,并填充到新构建的 Part 对象中
					for(Entry<String, Object> parsedPartMapItem : parsedPartMap.entrySet()){
						//填充 Value 中的值到 body 中
						if(parsedPartMapItem.getKey().equals(STATIC_VALUE)){
							part.body().write(TObject.cast(parsedPartMapItem.getValue()));
						}else{
							//填充 header
							String partedHeaderKey = parsedPartMapItem.getKey();
							String partedHeaderValue = parsedPartMapItem.getValue().toString();
							part.header().put(partedHeaderKey, partedHeaderValue);
							if(partedHeaderKey.equals("Content-Disposition")){
								//对Content-Disposition中的"name=xxx"进行处理,方便直接使用
								Map<String, String> contentDispositionValue = HttpParser.getEqualMap(partedHeaderValue);
								part.header().putAll(contentDispositionValue);
							}
						}
					}
					request.parts().add(part);
				}
				break;
			default:
				request.header().put(parsedPacketEntry.getKey(), parsedPacketEntry.getValue().toString());
				break;
			}
		}
		
		return request;
	}
	
	/**
	 * 解析报文成 HttpResponse 对象
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static Response parseResponse(InputStream inputStream) throws IOException{
		Response response = new Response();
		
		Map<String, Object> parsedPacket = parser(inputStream);
		
		//填充报文到响应对象
		Set<Entry<String, Object>> parsedItems= parsedPacket.entrySet();
		for(Entry<String, Object> parsedPacketEntry: parsedItems){
			String key = parsedPacketEntry.getKey();
			switch (key) {
			case FL_PROTOCOL:
				response.protocol().setProtocol(parsedPacketEntry.getValue().toString());
				break;
			case FL_VERSION:	
				response.protocol().setVersion(Float.valueOf(parsedPacketEntry.getValue().toString()));
				break;
			case FL_STATUS:	
				response.protocol().setStatus(Integer.valueOf(parsedPacketEntry.getValue().toString()));
				break;
			case FL_STATUSCODE:	
				response.protocol().setStatusCode(parsedPacketEntry.getValue().toString());
				break;
			case HTTP_COOKIE:
				List<Map<String, String>> cookieMap = (List<Map<String, String>>)parsedPacketEntry.getValue();
				//遍历 Cookie,并构建 Cookie 对象
				for(Map<String,String> cookieMapItem : cookieMap){
					Cookie cookie = Cookie.buildCookie(cookieMapItem);
					response.cookies().add(cookie);
				}
				break;
			case STATIC_VALUE:
				response.body().write(TObject.cast(parsedPacketEntry.getValue()));
				break;
			default:
				response.header().put(parsedPacketEntry.getKey(), parsedPacketEntry.getValue().toString());
				break;
			}
		}
		return response;
	}
}
