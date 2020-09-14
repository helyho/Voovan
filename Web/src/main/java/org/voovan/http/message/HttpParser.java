package org.voovan.http.message;

import org.voovan.Global;
import org.voovan.http.message.packet.Cookie;
import org.voovan.http.message.packet.Part;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.message.exception.HttpParserException;
import org.voovan.http.server.exception.RequestTooLarge;
import org.voovan.network.IoSession;
import org.voovan.tools.*;
import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.buffer.TByteBuffer;
import org.voovan.tools.collection.LongKeyMap;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;
import org.voovan.tools.security.THash;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

/**
 * Http 报文解析类
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpParser {
	private final static int HEADER = 0;
	private final static int BODY_VALUE = 1;
	private final static int BODY_FILE  = 2;
	private final static int BODY_PARTS = 3;

	private final static int PL_METHOD = 4;
	private final static int PL_PATH = 5;
	private final static int PL_PROTOCOL = 6;
	private final static int PL_VERSION = 7;
	private final static int PL_STATUS = 8;
	private final static int PL_STATUS_CODE = 9;
	private final static int PL_QUERY_STRING = 10;
	private final static int HEADER_MARK = 11;

	private final static String UPLOAD_PATH 			= TFile.assemblyPath(TFile.getTemporaryPath(),"voovan", "webserver", "upload");

	private final static String PROPERTY_LINE_SPILITER 	= ": ";
	private final static String EQUAL_MAP_REGEX 		= "([^ ;,]+=[^;,]+)";

	private final static FastThreadLocal<Object[]> THREAD_PACKET_MAP = FastThreadLocal.withInitial(()->new Object[20]);
	private final static FastThreadLocal<Request>  THREAD_REQUEST    = FastThreadLocal.withInitial(()->new Request());
	private final static FastThreadLocal<Response> THREAD_RESPONSE   = FastThreadLocal.withInitial(()->new Response());
	private final static FastThreadLocal<byte[]>   THREAD_BYTE_ARRAY = FastThreadLocal.withInitial(()->new byte[1024]);

	private final static LongKeyMap<Long> 	  	   PROTOCOL_HASH_MAP = new LongKeyMap<Long>(64);
	private final static LongKeyMap<Object[]> 	   PARSED_PACKET_MAP = new LongKeyMap<Object[]>(64);

	public static final int PARSER_TYPE_REQUEST = 0;
	public static final int PARSER_TYPE_RESPONSE = 1;

	static {
		Global.getHashWheelTimer().addTask(new HashWheelTask() {
			@Override
			public void run() {
				PARSED_PACKET_MAP.clear();
				PROTOCOL_HASH_MAP.clear();
			}
		}, 60);
	}

	/**
	 * 私有构造函数
	 * 该类无法被实例化
	 */
	private HttpParser(){

	}

//	/**
//	 * 解析 HTTP Header属性行
//	 * @param propertyLine
//	 *              Http 报文头属性行字符串
//	 * @return
//	 */
//	private static Map<String,String> parsePropertyLine(String propertyLine){
//		Map<String,String> property = new HashMap<String, String>();
//
//		int index = propertyLine.indexOf(propertyLineRegex);
//		if(index > 0){
//			String propertyName = propertyLine.substring(0, index);
//			String properyValue = propertyLine.substring(index+2, propertyLine.length());
//
//			property.put(fixHeaderName(propertyName), properyValue.trim());
//		}
//
//		return property;
//	}
//
//	/**
//	 * 校正全小写形式的 Http 头
//	 * @param headerName http 头的行数据
//	 * @return 校正后的http 头的行数据
//	 */
//	public static String fixHeaderName(String headerName) {
//		if(headerName==null){
//			return null;
//		}
//		String[] headerNameSplits = headerName.split("-");
//		StringBuilder stringBuilder = new StringBuilder();
//		for(String headerNameSplit : headerNameSplits) {
//			if(Character.isLowerCase(headerNameSplit.codePointAt(0))){
//				stringBuilder.append((char)(headerNameSplit.codePointAt(0) - 32));
//				stringBuilder.append(TString.removePrefix(headerNameSplit));
//			} else {
//				stringBuilder.append(headerNameSplit);
//			}
//
//			stringBuilder.append("-");
//		}
//
//		return TString.removeSuffix(stringBuilder.toString());
//	}

	/**
	 * 解析字符串中的所有等号表达式成 Map
	 * @param str
	 *              等式表达式
	 * @return 等号表达式 Map
	 */
	public static Map<String, String> getEqualMap(String str){
		Map<String, String> equalMap = new HashMap<String, String>();
		String[] searchedStrings = TString.searchByRegex(str, EQUAL_MAP_REGEX);
		for(String groupString : searchedStrings){
			//这里不用 split 的原因是有可能等号后的值字符串中出现等号
			String[] equalStrings = new String[2];
			int equalCharIndex= groupString.indexOf(Global.STR_EQUAL);
			equalStrings[0] = groupString.substring(0,equalCharIndex);
			equalStrings[1] = groupString.substring(equalCharIndex+1,groupString.length());
			if(equalStrings.length==2){
				String key = equalStrings[0];
				String value = equalStrings[1];
				if(value.charAt(0) == Global.CHAR_QUOTE && value.charAt(value.length() - 1)== Global.CHAR_QUOTE){
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
	 * @param propertyName   属性名
	 * @param valueName      属性值
	 * @return
	 */
	private static String getPerprotyEqualValue(Map<String,Object> packetMap,String propertyName,String valueName){
		Object propertyValueObj = packetMap.get(propertyName);
		if(propertyValueObj == null){
			return null;
		}
		String propertyValue = propertyValueObj.toString();
		Map<String, String> equalMap = getEqualMap(propertyValue);
		return equalMap.get(valueName);
	}

	/**
	 * 处理消息的Cookie
	 * @param cookies           Cookie 保存集合
	 * @param cookieType        Cookie 类型 0: request, 1: response
	 * @param cookieValue       Cookie 数据
	 */
	@SuppressWarnings("unchecked")
	public static void parseCookie(List<Cookie> cookies, int cookieType, String cookieValue){
		if(cookieValue == null) {
			return;
		}

		List<Map<String, String>> cookieMapList = new ArrayList<Map<String, String>>();

		Map<String, String>cookieMap = getEqualMap(cookieValue);

		// Cookie
		//请求 request 的 cookie 形式 多个cookie 一行
		if(cookieType == 0){
			for(Entry<String,String> cookieMapEntry: cookieMap.entrySet()){
				HashMap<String, String> cookieOneMap = new HashMap<String, String>();
				cookieOneMap.put(cookieMapEntry.getKey(), cookieMapEntry.getValue());
				cookieMapList.add(cookieOneMap);
			}
		}

		//Set-Cookie
		//响应 response 的 cookie 形式 一个cookie 一行
		else if(cookieType == 1){
			//处理非键值的 cookie 属性
			if(cookieValue.toLowerCase().contains(HttpStatic.HTTPONLY_STRING)){
				cookieMap.put(HttpStatic.HTTPONLY_STRING, Global.EMPTY_STRING);
			}
			if(cookieValue.toLowerCase().contains(HttpStatic.SECURE_STRING)){
				cookieMap.put(HttpStatic.SECURE_STRING, Global.EMPTY_STRING);
			}
			cookieMapList.add(cookieMap);
		}

		//遍历 Cookie,并构建 Cookie 对象
		for (Map<String, String> cookieMapItem : cookieMapList) {
			Cookie cookie = Cookie.buildCookie(cookieMapItem);
			cookies.add(cookie);
		}
	}

	/**
	 * 处理 body 段
	 * 		判断是否使用 GZIP 压缩,如果使用则解压缩后返回,如果没有压缩则直接返回
	 * @param headerMap
	 * @param contentBytes
	 * @return
	 * @throws IOException
	 */
	private static byte[] dealBodyContent(Map<String, Object> headerMap, byte[] contentBytes) throws IOException{
		byte[] bytesValue;
		if(contentBytes.length == 0 ){
			return contentBytes;
		}

		//是否支持 GZip
		boolean isGZip = headerMap.get(HttpStatic.CONTENT_ENCODING_STRING)==null ? false : headerMap.get(HttpStatic.CONTENT_ENCODING_STRING).toString().contains(HttpStatic.GZIP_STRING);

		//如果是 GZip 则解压缩
		if(isGZip && contentBytes.length>0){
			bytesValue = TZip.decodeGZip(contentBytes);
		} else {
			bytesValue = contentBytes;
		}
		return TObject.nullDefault(bytesValue,new byte[0]);
	}

	/**
	 * 解析 HTTP 请求写一行
	 * @param packetMap 解析后数据的容器
	 * @param type 解析的报文类型
	 * @param byteBuffer ByteBuffer对象
	 * @param contiuneRead 当数据不足时的读取器
	 * @param timeout 读取超时时间参数
	 * @return 协议行的 hash
	 */
	public static int parseProtocol(Object[] packetMap, int type, ByteBuffer byteBuffer, Runnable contiuneRead, int timeout) {
		byte[] bytes = THREAD_BYTE_ARRAY.get();
		int position = 0;
		int hashCode = 0;

		//遍历 Protocol
		int segment = 0;
		HttpItem segment_1 = null;
		HttpItem segment_2 = null;
		HttpItem segment_3 = null;

		String queryString = null;

		int questPositiion = -1;
		byte prevByte = '\0';
		byte currentByte = '\0';

		long start = System.currentTimeMillis();
		while (true) {

			//如果数据不够则尝试读取
			while(!byteBuffer.hasRemaining()) {
				contiuneRead.run();
				if(System.currentTimeMillis() - start > timeout) {
					throw new HttpParserException("HttpParser read timeout");
				}
			}

			currentByte = byteBuffer.get();

			//兼容部分 Web 中间件,在尾部增加换行的问题
			if(segment==0 && (currentByte == Global.BYTE_CR || currentByte == Global.BYTE_LF)){
				continue;
			}

			if (currentByte == Global.BYTE_SPACE && segment < 2) { // " "
				if (segment == 0) {
					//常用 http method 快速判断
					if(position == 3 && bytes[0] == 'G' && bytes[1] == 'E' && bytes[2] == 'T') {
						segment_1 = HttpStatic.GET;
					} else if(position == 4 && bytes[0] == 'P' && bytes[1] == 'O' && bytes[2] == 'S' && bytes[3] == 'T') {
						segment_1 = HttpStatic.POST;
					} else {
						segment_1 = HttpItem.getHttpItem(bytes, 0, position);
					}
				} else if (segment == 1) {
					segment_2 = HttpItem.getHttpItem(bytes, 0, questPositiion > 0 ? questPositiion : position);

					if(questPositiion > 0) {
						queryString = new String(bytes, questPositiion + 1, position - questPositiion - 1);
					}
				}
				position = 0;
				segment++;
				continue;
			} else if (currentByte == Global.BYTE_QUESTION) { // "?"
				if (segment == 1) {
					questPositiion = position;
					bytes[position] = currentByte;
					position++;
					continue;
				}
			} else if (prevByte == Global.BYTE_CR && currentByte == Global.BYTE_LF && segment == 2) {
				segment_3 = HttpItem.getHttpItem(bytes, 0, position);
				position = 0;
				break;
			}

			prevByte = currentByte;

			if (currentByte == Global.BYTE_CR) {
				continue;
			}

			bytes[position] = currentByte;
			position++;
		}

		if (type == 0) {
			//1
			packetMap[PL_METHOD] = segment_1;

			//2
			packetMap[PL_PATH] = segment_2;
			if(questPositiion > 0 && queryString!=null) {
				packetMap[PL_QUERY_STRING] = queryString;
			}

			//3
			if(segment_3.getBytes()[0] =='H' && segment_3.getBytes()[1]=='T' && segment_3.getBytes()[2]=='T' && segment_3.getBytes()[3]=='P') {
				packetMap[PL_PROTOCOL] = HttpStatic.HTTP;
			} else {
				throw new HttpParserException("Not a http packet");
			}

			switch (segment_3.getBytes()[7]) {
				case '1':
					packetMap[PL_VERSION] = HttpStatic.HTTP_11_STRING;
					break;
				case '0':
					packetMap[PL_VERSION] = HttpStatic.HTTP_10_STRING;
					break;
				case '9':
					packetMap[PL_VERSION] = HttpStatic.HTTP_09_STRING;
					break;
				default:
					packetMap[PL_VERSION] = HttpStatic.HTTP_11_STRING;
			}
		}

		if (type == 1) {
			//1
			if(segment_1.getBytes()[0]=='H' && segment_1.getBytes()[1]=='T' && segment_1.getBytes()[2]=='T' && segment_1.getBytes()[3]=='P') {
				packetMap[PL_PROTOCOL] = HttpStatic.HTTP.getValue();
			} else {
				throw new HttpParserException("Not a http packet");
			}

			switch (segment_1.getBytes()[0]) {
				case '1':
					packetMap[PL_VERSION] = HttpStatic.HTTP_11_STRING;
					break;
				case '0':
					packetMap[PL_VERSION] = HttpStatic.HTTP_10_STRING;
					break;
				case '9':
					packetMap[PL_VERSION] = HttpStatic.HTTP_09_STRING;
					break;
				default:
					packetMap[PL_VERSION] = HttpStatic.HTTP_11_STRING;
			}

			//2
			packetMap[PL_STATUS] = segment_2;

			//3
			packetMap[PL_STATUS_CODE] = segment_3;
		}

		int queryStringHashCode = queryString==null ? 0 : queryString.hashCode();
		return segment_1.hashCode() + segment_2.hashCode() + queryStringHashCode + packetMap[PL_VERSION].hashCode();
	}

	/**
	 * 解析 HTTP 请求 Header 中的一行
	 * @param headerMap 解析后数据的容器
	 * @param byteBuffer ByteBuffer对象
	 * @param contiuneRead 当数据不足时的读取器
	 * @param timeout 读取超时时间参数
	 * @return true: Header解析未完成, false: Header解析完成
	 */
	public static boolean parseHeaderLine(Map<String, Object> headerMap, ByteBuffer byteBuffer, Runnable contiuneRead, int timeout) {
		byte[] bytes = THREAD_BYTE_ARRAY.get();
		int position = 0;
		boolean isCache = WebContext.isCache();

		//遍历 Protocol
		boolean onHeaderName = true;
		byte prevByte = '\0';
		byte currentByte = '\0';
		String headerName = null;
		String headerValue = null;

		long start = System.currentTimeMillis();
		while (true) {

			//如果数据不够则尝试读取
			while(!byteBuffer.hasRemaining()) {
				contiuneRead.run();
				if(System.currentTimeMillis() - start > timeout) {
					throw new HttpParserException("HttpParser read timeout");
				}
			}

			currentByte = byteBuffer.get();

			if (onHeaderName && prevByte == Global.BYTE_COLON && currentByte == Global.BYTE_SPACE) {
				if(isCache) {
					headerName = HttpItem.getHttpItem(bytes, 0, position).getValue();
				} else {
					headerName = new String(bytes, 0, position);
				}

				onHeaderName = false;
				position = 0;
				continue;
			} else if (!onHeaderName && prevByte == Global.BYTE_CR && currentByte == Global.BYTE_LF) {
				if(isCache) {
					headerValue = HttpItem.getHttpItem(bytes, 0, position).getValue();
				} else {
					headerValue = new String(bytes, 0, position);
				}
				break;
			}

			//http 头结束了
			if (onHeaderName && prevByte == Global.BYTE_CR && currentByte == Global.BYTE_LF) {
				return true;
			}

			prevByte = currentByte;

			if (onHeaderName && currentByte == Global.BYTE_COLON) {
				continue;
			} else if (!onHeaderName && currentByte == Global.BYTE_CR) {
				continue;
			}

			bytes[position] = currentByte;
			position++;

		}

		if(headerName!=null && headerValue!=null) {
			headerMap.put(headerName, headerValue);
		}
		return false;
	}

	/**
	 * 解析 HTTP 请求 Header 中的一行
	 * @param byteBuffer ByteBuffer对象
	 * @param contiuneRead 当数据不足时的读取器
	 * @param timeout 读取超时时间参数
	 * @return true: Header解析未完成, false: Header解析完成
	 */
	public static TreeMap<String, Object> parseHeader(ByteBuffer byteBuffer, Runnable contiuneRead, int timeout) {
		TreeMap<String, Object> headerMap = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
		while (!parseHeaderLine(headerMap, byteBuffer, contiuneRead, timeout)) {
			if (!byteBuffer.hasRemaining()) {
			 	throw new HttpParserException("HttpParser parse header failed, not enough data");
			}
		}

		return headerMap;
	}

	/**
	 * 获取可用缓存
	 * @param byteBufferChannel 字节缓冲通道
	 * @param protocolPosition 协议行结束位置
	 * @param protocolMark 协议行 hash
	 * @return null: 无可用缓存, not null: 获取到可用的缓存
	 */
	private static Object[] findCache(ByteBufferChannel byteBufferChannel, int protocolPosition, long protocolMark) {
		if(!WebContext.isCache()) {
			return null;
		}

		Long cachedMark = PROTOCOL_HASH_MAP.get(protocolMark);
		ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer();

		try {
			if (cachedMark != null) {
				Object[] cachedPacketMap = PARSED_PACKET_MAP.get(cachedMark);

				if (cachedPacketMap != null) {
					//高位清空, 获得整个头的长度
					long totalLengthInMark = cachedMark & 0x00000000FFFFFFFFL;

					if (totalLengthInMark >= byteBuffer.limit() ||
							(byteBufferChannel.size() >= totalLengthInMark &&
									byteBufferChannel.get((int) totalLengthInMark - 1) == 10 &&
									byteBufferChannel.get((int) totalLengthInMark - 2) == 13 &&
									byteBufferChannel.get((int) totalLengthInMark - 3) == 10 &&
									byteBufferChannel.get((int) totalLengthInMark - 4) == 13
							)
					) {

						int headerMark = THash.HashFNV1(byteBuffer, protocolPosition, (int) (totalLengthInMark - protocolPosition));
						if (protocolMark + headerMark == cachedMark >> 32) {
							byteBuffer.position((int) totalLengthInMark);

							return Arrays.copyOf(cachedPacketMap, cachedPacketMap.length);
						}
					} else {
						PROTOCOL_HASH_MAP.remove(protocolMark);
						PARSED_PACKET_MAP.remove(cachedMark);
					}
				}
			}
		} catch (Exception e) {
			PROTOCOL_HASH_MAP.remove(protocolMark);
			PARSED_PACKET_MAP.remove(cachedMark);
		}

		return null;
	}

	/**
	 * 创建缓存数序
	 * @param packetMap			中间解析对象
	 * @param byteBuffer		字节缓冲对象
	 * @param protocolPosition 协议行结束位置
	 * @param protocolMark 协议行 hash
	 * @return 当前
	 */
	private static void createCache(Object[] packetMap, ByteBuffer byteBuffer, int protocolPosition, long protocolMark) {
		int totalLength = byteBuffer.position();

		if(WebContext.isCache()) {
			int headerMark = THash.HashFNV1(byteBuffer, protocolPosition, (int) (totalLength - protocolPosition));
			//高位存头部 hash, 低位存整个头的长度
			long mark = ((protocolMark + headerMark) << 32) + totalLength;
			packetMap[HEADER_MARK] = mark;

			Object[] cachedPacketMap = Arrays.copyOf(packetMap, packetMap.length);
			PARSED_PACKET_MAP.put(mark, cachedPacketMap);
			PROTOCOL_HASH_MAP.put(protocolMark, mark);
		}
	}

	/**
	 * 解析 HTTP 报文
	 * 		解析称 Map 形式,其中:
	 * 			1.protocol 解析成 key/value 形式
	 * 			2.header   解析成 key/value 形式
	 * 			3.cookie   解析成 List[Map[String,String]] 形式
	 * 			3.part     解析成 List[Map[Stirng,Object]](因为是递归,参考 HTTP 解析形式) 形式
	 * 			5.body     解析成 key=BODY_VALUE 的Map 元素
	 * @param session socket 会话对象
	 * @param packetMap 用于填充的解析 map
	 * @param type 解析的报文类型, 0: Request, 1: Response
	 * @param byteBufferChannel 输入流
	 * @param timeout 读取超时时间参数
	 * @param requestMaxSize 上传文件的最大尺寸, 单位: kb
	 * @return 解析后的 Map
	 * @throws IOException IO 异常
	 */
	public static Object[] parser(IoSession session, Object[] packetMap, int type,
								  ByteBufferChannel byteBufferChannel, int timeout,
								  long requestMaxSize) throws IOException {
		int totalLength = 0;
		long protocolMark = 0;
		int headerMark = 0;
		int protocolPosition = 0;

		boolean isCache = WebContext.isCache();
		TreeMap<String, Object> headerMap = null;

		requestMaxSize = requestMaxSize < 0 ? Integer.MAX_VALUE : requestMaxSize;

		//继续从 Socket 中读取数据
		Runnable contiuneRead = ()->{
			if(session==null || !session.isConnected()) {
				throw new HttpParserException("Socket is disconnect", HttpParserException.SOCKET_DISCONNECT);
			}

			session.getSocketSelector().select();
			if(session.getReadByteBufferChannel().isReleased()) {
				throw new HttpParserException("Socket read buffer is released, may be Socket is disconnected", HttpParserException.BUFFER_RELEASED);
			}
		};

		//按行遍历HTTP报文
		while(byteBufferChannel.size() > 0) {
			ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer();

			try {
				//处理 Http [ protocol ]
				{
					protocolMark = parseProtocol(packetMap, type, byteBuffer, contiuneRead, timeout);
					protocolPosition = byteBuffer.position() - 1;
				}

				//检查缓存是否存在,并获取
				Object[] cachedPacketMap = findCache(byteBufferChannel, protocolPosition, protocolMark);
				if (cachedPacketMap != null) {
					packetMap = cachedPacketMap;
					headerMap = (TreeMap<String, Object>) packetMap[HEADER];
				} else {
					//处理 Http [ header ]
					{
						headerMap = parseHeader(byteBuffer, contiuneRead, timeout);
						packetMap[HEADER] = headerMap;
					}

					//处理更新或设置缓存
					createCache(packetMap, byteBuffer, protocolPosition, protocolMark);
				}

			} finally {
				totalLength = byteBuffer.position();
				byteBufferChannel.compact();
			}

			//无 body 报文完成解析
			if(HttpStatic.GET.equals(packetMap[PL_METHOD])) {
				break;
			}

			String contentType = (String)headerMap.get(HttpStatic.CONTENT_TYPE_STRING);
			if(contentType == null) {
				//无 body 报文完成解析
				break;
			}

			//解析 HTTP 请求 body
			String transferEncoding = (String)headerMap.get(HttpStatic.TRANSFER_ENCODING_STRING);
			String contentLength 	= (String)headerMap.get(HttpStatic.CONTENT_LENGTH_STRING);

			//1. 解析 HTTP 的 POST 请求 body part
			if(contentType!=null && contentType.contains(HttpStatic.MULTIPART_FORM_DATA_STRING)){
				//用来保存 Part 的 list
				List<Object[]> bodyPartList = new ArrayList<Object[]>();

				//取boundary 用于 part 内容分段
				String boundary = TString.assembly("--", getPerprotyEqualValue(headerMap, HttpStatic.CONTENT_TYPE_STRING, HttpStatic.BOUNDARY_STRING));

				ByteBuffer boundaryByteBuffer = ByteBuffer.allocate(2);
				while(true) {
					//等待数据
					if (!byteBufferChannel.waitData(boundary.getBytes(), timeout, contiuneRead)) {
						throw new HttpParserException("Http Parser readFromChannel data error");
					}

					int boundaryIndex = byteBufferChannel.indexOf(boundary.getBytes(Global.CS_UTF_8));

					//跳过 boundary
					byteBufferChannel.shrink((boundaryIndex + boundary.length()));

					//取 boundary 结尾字符
					boundaryByteBuffer.clear();
					int readSize = byteBufferChannel.readHead(boundaryByteBuffer);

					//累计请求大小
					totalLength = totalLength + readSize;
					//请求过大的处理
					if(totalLength > requestMaxSize * 1024){
						throw new RequestTooLarge("Request is too large: {max size: " + requestMaxSize*1024 + ", expect size: " + totalLength + "}");
					}

					//确认 boundary 结尾字符, 如果是"--" 则标识报文结束
					if (Arrays.equals(boundaryByteBuffer.array(), "--".getBytes())) {
						//收缩掉尾部的换行
						byteBufferChannel.shrink(2);
						break;
					}

					byte[] boundaryMark = HttpStatic.BODY_MARK.getBytes();
					//等待数据
					if (!byteBufferChannel.waitData(boundaryMark, timeout, contiuneRead)) {
						throw new HttpParserException("Http Parser readFromChannel data error");
					}

					int partHeadEndIndex = byteBufferChannel.indexOf(boundaryMark);

					//Part 头读取
					ByteBuffer partHeadBuffer = TByteBuffer.allocateDirect(partHeadEndIndex + 4);
					byteBufferChannel.readHead(partHeadBuffer);

					//构造新的 Bytebuffer 递归解析
					Object[] partArray = new Object[4];
					Map<String, Object> partHeaderMap = new HashMap<String, Object>();

					partHeaderMap = parseHeader(partHeadBuffer, contiuneRead, timeout);

					partArray[HEADER] = partHeaderMap;

					TByteBuffer.release(partHeadBuffer);

					String fileName = getPerprotyEqualValue(partHeaderMap, HttpStatic.CONTENT_DISPOSITION_STRING, "filename");
					if(fileName!=null && fileName.isEmpty()){
						break;
					}

					//解析 Part 报文体
					//重置 index
					boundaryIndex = -1;
					//普通参数处理
					if (fileName == null) {
						//等待数据
						if (!byteBufferChannel.waitData(boundary.getBytes(), timeout, contiuneRead)) {
							throw new HttpParserException("Http Parser readFromChannel data error");
						}

						boundaryIndex = byteBufferChannel.indexOf(boundary.getBytes(Global.CS_UTF_8));


						ByteBuffer bodyByteBuffer = ByteBuffer.allocate(boundaryIndex - 2);
						byteBufferChannel.readHead(bodyByteBuffer);
						partArray[BODY_VALUE] = bodyByteBuffer.array();
					}
					//文件处理
					else {

						String fileExtName = TFile.getFileExtension(fileName);
						fileExtName = fileExtName==null || fileExtName.equals(Global.EMPTY_STRING) ? "tmp" : fileExtName;

						//拼文件名
						String localFileName =TString.assembly(UPLOAD_PATH, Global.FRAMEWORK_NAME, System.currentTimeMillis(), ".", fileExtName);
						File localFile = new File(localFileName);

						//文件是否接收完成
						boolean isFileRecvDone = false;

						while (true){
							int dataLength = byteBufferChannel.size();
							//等待数据, 1毫秒超时
							if (byteBufferChannel.waitData(boundary.getBytes(), 0, contiuneRead)) {
								isFileRecvDone = true;
							}

							if(!isFileRecvDone) {
								if(dataLength!=0) {
									byteBufferChannel.saveToFile(localFileName, dataLength);
									//累计请求大小
									totalLength = totalLength + dataLength;
								}

								//请求过大的处理
								if(totalLength > requestMaxSize * 1024){
									TFile.deleteFile(new File(localFileName));
									throw new RequestTooLarge("Request is too large: {max size: " + requestMaxSize*1024 + ", expect size: " + totalLength + "}");
								}

								continue;
							} else {
								boundaryIndex = byteBufferChannel.indexOf(boundary.getBytes(Global.CS_UTF_8));
								int length = boundaryIndex == -1 ? byteBufferChannel.size() : (boundaryIndex - 2);
								if (boundaryIndex > 0) {
									byteBufferChannel.saveToFile(localFileName, length);
									totalLength = totalLength + dataLength;
								}
							}




							if(!isFileRecvDone){
								TEnv.sleep(100);
							} else {
								break;
							}

						}

						if(boundaryIndex == -1){
							localFile.delete();
							throw new HttpParserException("Http Parser not enough data with " + boundary);
						} else {
							partArray[BODY_VALUE] = null;
							partArray[BODY_FILE] = localFileName.getBytes();
						}

						if(localFile.exists()) {
							localFile.deleteOnExit();
						}
					}

					//加入bodyPartList中
					bodyPartList.add(partArray);
				}
				//将存有多个 part 的 list 放入packetMap
				packetMap[BODY_PARTS] = bodyPartList;
			}

			//2. 解析 HTTP 响应 body 内容段的 chunked
			else if(HttpStatic.CHUNKED_STRING.equals(transferEncoding)){

				ByteBufferChannel chunkedByteBufferChannel = new ByteBufferChannel(3);
				String chunkedLengthLine = "";

				while(chunkedLengthLine!=null){

					// 等待数据
					if(!byteBufferChannel.waitData("\r\n".getBytes(), timeout, contiuneRead)){
						throw new HttpParserException("Http Parser readFromChannel data error");
					}

					chunkedLengthLine = byteBufferChannel.readLine().trim();

					if("0".equals(chunkedLengthLine)){
						break;
					}

					if(chunkedLengthLine.isEmpty()){
						continue;
					}

					int chunkedLength = 0;
					//读取chunked长度
					try {
						chunkedLength = Integer.parseInt(chunkedLengthLine, 16);
					}catch(Exception e){
						Logger.error(e);
						break;
					}

					// 等待数据
					if(!byteBufferChannel.waitData(chunkedLength, timeout, contiuneRead)){
						throw new HttpParserException("Http Parser readFromChannel data error");
					}

					int readSize = 0;
					if(chunkedLength > 0) {
						//按长度读取chunked内容
						ByteBuffer chunkedByteBuffer = TByteBuffer.allocateDirect(chunkedLength);
						readSize = byteBufferChannel.readHead(chunkedByteBuffer);

						//累计请求大小
						totalLength = totalLength + readSize;
						//请求过大的处理
						if(readSize != chunkedLength){
							throw new HttpParserException("Http Parser readFromChannel chunked data error");
						}

						//如果多次读取则拼接
						chunkedByteBufferChannel.writeEnd(chunkedByteBuffer);
						TByteBuffer.release(chunkedByteBuffer);
					}

					//请求过大的处理
					if(totalLength > requestMaxSize * 1024){
						throw new RequestTooLarge("Request is too large: {max size: " + requestMaxSize*1024 + ", expect size: " + totalLength + "}");
					}

					//跳过换行符号
					byteBufferChannel.shrink(2);
				}

				byte[] value = dealBodyContent(headerMap, chunkedByteBufferChannel.array());
				chunkedByteBufferChannel.release();
				packetMap[BODY_VALUE] = value;
				byteBufferChannel.shrink(2);
			}

			//3. HTTP(请求和响应) 报文的内容段中Content-Length 提供长度,按长度读取 body 内容段
			else if(contentLength!=null){
				int contentSize = Integer.parseInt(contentLength.toString());

				//累计请求大小
				totalLength = totalLength + contentSize;

				//请求过大的处理
				if(totalLength > requestMaxSize * 1024){
					throw new HttpParserException("Request is too large: {max size: " + requestMaxSize*1024 + ", expect size: " + totalLength + "}");
				}


				// 等待数据
				if(!byteBufferChannel.waitData(contentSize, timeout, contiuneRead)){
					throw new HttpParserException("Http Parser readFromChannel data error");
				}

				byte[] contentBytes = new byte[contentSize];
				byteBufferChannel.get(contentBytes);
				byteBufferChannel.shrink(0, contentSize);

				byte[] value = dealBodyContent(headerMap, contentBytes);
				packetMap[BODY_VALUE] = value;
			}

			break;
		}

		return packetMap;
	}

	/**
	 * 解析报文成 HttpRequest 对象
	 * @param session socket 会话对象
	 * @param byteBufferChannel  输入字节流
	 * @param timeOut 读取超时时间参数
	 * @param requestMaxSize 上传文件的最大尺寸, 单位: kb
	 * @return   返回请求报文
	 * @throws IOException IO 异常
	 */
	@SuppressWarnings("unchecked")
	public static Request parseRequest(IoSession session, ByteBufferChannel byteBufferChannel, int timeOut, long requestMaxSize) throws IOException {
		return parseRequest(THREAD_REQUEST.get(), session, byteBufferChannel, timeOut, requestMaxSize);
	}

	/**
	 * 解析报文成 HttpRequest 对象
	 * @param request 请求对象
	 * @param session socket 会话对象
	 * @param byteBufferChannel  输入字节流
	 * @param timeOut 读取超时时间参数
	 * @param requestMaxSize 上传文件的最大尺寸, 单位: kb
	 * @return   返回请求报文
	 * @throws IOException IO 异常
	 */
	@SuppressWarnings("unchecked")
	public static Request parseRequest(Request request, IoSession session, ByteBufferChannel byteBufferChannel, int timeOut, long requestMaxSize) throws IOException {
		boolean isCache = WebContext.isCache();

		Object[] packetMap = THREAD_PACKET_MAP.get();
		packetMap = parser(session, packetMap, PARSER_TYPE_REQUEST, byteBufferChannel, timeOut, requestMaxSize);

		//如果解析的Map为空,则直接返回空
		if(byteBufferChannel.isReleased()){
			return null;
		}

		request.clear();

		//是否使用的时缓存的数据
		boolean bodyFlag = false;
		boolean bodyPartFlag = false;

		//填充报文到请求对象
		for(int key=0;key<packetMap.length;key++) {
			Object value = packetMap[key];

			if(value == null) {
				continue;
			}

			switch (key) {
				case PL_METHOD:
					request.protocol().setMethod(value.toString());
					break;
				case PL_PROTOCOL:
					request.protocol().setProtocol(value.toString());
					break;
				case PL_QUERY_STRING:
					request.protocol().setQueryString(value.toString());
					break;
				case PL_VERSION:
					request.protocol().setVersion(value.toString());
					break;
				case PL_PATH:
					request.protocol().setPath(value.toString());
					break;
				case HEADER_MARK:
					request.setMark((Long)value);
					break;
				case BODY_VALUE:
					bodyFlag = true;
					request.setHasBody(true);
					request.body().write((byte[]) value);
					break;
				case BODY_PARTS:
					bodyFlag = true;
					bodyPartFlag = true;
					request.setHasBody(true);
					List<Object[]> parsedPartArray = (List<Object[]>) value;
					//遍历 part List,并构建 Part 对象
					for (Object[] parsedPart : parsedPartArray) {
						Part part = new Part();
						//将 part Map中的值,并填充到新构建的 Part 对象中
						for (int partKey = 0; partKey < parsedPart.length; partKey++) {
							Object partValue = parsedPart[partKey];
							if(partValue == null) {
								continue;
							}

							//填充 Value 中的值到 body 中
							if (partKey == BODY_VALUE) {
								part.body().changeToBytes();
								part.body().write((byte[]) partValue);
							} else if (partKey == BODY_FILE) {
								String filePath = new String((byte[]) partValue);
								part.body().changeToFile(new File(filePath));
							} else if (partKey == HEADER) {
								for (Entry<String, Object> parsedPartHeaderItem : ((Map<String, Object>) partValue).entrySet()) {
									//填充 header
									String partedHeaderKey = parsedPartHeaderItem.getKey();
									String partedHeaderValue = parsedPartHeaderItem.getValue().toString();
									part.header().put(partedHeaderKey, partedHeaderValue);
									if (HttpStatic.CONTENT_DISPOSITION_STRING.equals(partedHeaderKey)) {
										//对Content-Disposition中的"name=xxx"进行处理,方便直接使用
										Map<String, String> contentDispositionValue = HttpParser.getEqualMap(partedHeaderValue);
										part.header().putAll(contentDispositionValue);
									}
								}
							}
						}
						request.parts().add(part);
					}
					parsedPartArray.clear();
					break;
				case HEADER:
					request.header().setHeaders((Map<String, String>) value);
					break;
			}
		}

		Arrays.fill(packetMap, null);;

		//设置 post 请求的 mark
		if(isCache && bodyFlag && request.getMark() != null && !bodyPartFlag) {
			Integer bodyMark = request.body().getMark();
			request.setMark(request.getMark() | bodyMark);
		}

		return request;
	}

	/**
	 * 解析报文成 HttpResponse 对象
	 * @param session socket 会话对象
	 * @param byteBufferChannel  输入字节流
	 * @param timeOut 读取超时时间参数
	 * @return   返回响应报文
	 * @throws IOException IO 异常
	 */
	@SuppressWarnings("unchecked")
	public static Response parseResponse(IoSession session, ByteBufferChannel byteBufferChannel, int timeOut) throws IOException {
		return parseResponse(THREAD_RESPONSE.get(), session, byteBufferChannel, timeOut);

	}

	/**
	 * 解析报文成 HttpResponse 对象
	 * @param response Resposne 响应对象
	 * @param session socket 会话对象
	 * @param byteBufferChannel  输入字节流
	 * @param timeOut 读取超时时间参数
	 * @return   返回响应报文
	 * @throws IOException IO 异常
	 */
	@SuppressWarnings("unchecked")
	public static Response parseResponse(Response response, IoSession session, ByteBufferChannel byteBufferChannel, int timeOut) throws IOException {
		Object[] packetMap = THREAD_PACKET_MAP.get();
		packetMap = parser(session, packetMap, PARSER_TYPE_RESPONSE, byteBufferChannel, timeOut, -1);

		//如果解析的Map为空,则直接返回空
		if(byteBufferChannel.isReleased()){
			return null;
		}

		response.clear();
		boolean bodyFlag = false;

		//填充报文到响应对象
		for(int key=0;key<packetMap.length;key++) {
			Object value = packetMap[key];

			if(value == null) {
				continue;
			}

			switch (key) {
				case PL_PROTOCOL:
					response.protocol().setProtocol(value.toString());
					break;
				case PL_VERSION:
					response.protocol().setVersion(value.toString());
					break;
				case PL_STATUS:
					response.protocol().setStatus(Integer.parseInt(value.toString()));
					break;
				case PL_STATUS_CODE:
					response.protocol().setStatusCode(value.toString());
					break;
				case BODY_VALUE:
					bodyFlag = true;
					response.body().write((byte[]) value);
					response.setHasBody(true);
					break;
				case HEADER:
					response.header().setHeaders((Map<String, String>) value);
					break;
			}
		}
		Arrays.fill(packetMap, null);
		return response;
	}

	public static void resetThreadLocal(){
		THREAD_REQUEST.set(new Request());
		THREAD_RESPONSE.set(new Response());
	}
}
