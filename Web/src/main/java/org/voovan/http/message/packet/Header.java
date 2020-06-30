package org.voovan.http.message.packet;

import org.voovan.http.message.HttpStatic;
import org.voovan.tools.FastThreadLocal;
import org.voovan.tools.json.JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * HTTP 的 header 对象
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Header {
	private static FastThreadLocal<StringBuilder> THREAD_STRING_BUILDER = FastThreadLocal.withInitial(()->new StringBuilder(512));

	private String contentType;
	private String contentLength;
	private String contentEncoding;
	private String transferEncoding;
	private Map<String, String> headers;

	/**
	 * 构造函数
	 */
	public Header(){
		headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	}

	/**
	 * 获取 Header  的 Map 对象
	 * @return HTTP-Header 转换候的 Map
	 */
	public Map<String,String> getHeaders() {
		TreeMap<String, String> newHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		newHeaders.putAll(headers);
		return newHeaders;
	}

	public void setHeaders(Map<String, String> headers) {
		contentType = null;
		contentLength = null;
		contentEncoding = null;
		transferEncoding = null;
		this.headers = headers;
	}

	/**
	 * 通过 name  删除 Header值
	 * @param header header 的 name
	 * @return 移除的header 的 name
	 */
	public String remove(String header){
		String ret = null;
		switch (header) {
			case HttpStatic.CONTENT_TYPE_STRING:
				ret = this.headers.remove(HttpStatic.CONTENT_TYPE_STRING);
				contentType = null;
				break;
			case HttpStatic.CONTENT_ENCODING_STRING:
				ret = this.headers.remove(HttpStatic.CONTENT_ENCODING_STRING);
				contentEncoding = null;
				break;
			case HttpStatic.CONTENT_LENGTH_STRING:
				ret = this.headers.remove(HttpStatic.CONTENT_LENGTH_STRING);
				contentLength = null;
				break;
			case HttpStatic.TRANSFER_ENCODING_STRING:
				ret = this.headers.remove(HttpStatic.TRANSFER_ENCODING_STRING);
				transferEncoding = null;
				break;
			default:
				ret = headers.remove(header);
		}
		return ret;
	}

	/**
	 * 通过 name  判断 Header 是否存在
	 * @param header header 的 name
	 * @return 是否存在
	 */
	public boolean contain(String header){
		boolean ret = false;
		switch (header) {
			case HttpStatic.CONTENT_TYPE_STRING 	 :  ret = get(header) != null;  break;
			case HttpStatic.CONTENT_LENGTH_STRING 	 :  ret = get(header) != null;  break;
			case HttpStatic.CONTENT_ENCODING_STRING  :  ret = get(header) != null;  break;
			case HttpStatic.TRANSFER_ENCODING_STRING :  ret = get(header) != null;  break;
			default: ret = headers.containsKey(header);
		}

		return ret;
	}

	/**
	 * 通过 name 获取 Header值
	 * @param header header 的 name
	 * @return header 的值
	 */
	public String get(String header){
		String ret = null;
		boolean tryGetFromMap = false;
		switch (header) {
			case HttpStatic.CONTENT_TYPE_STRING 	 : 	{
				ret = contentType == null ? contentType = headers.get(header) : contentType;
				break;
			}
			case HttpStatic.CONTENT_LENGTH_STRING 	 : 	{
				ret = contentLength == null ? contentLength = headers.get(header) : contentLength;
				break;
			}
			case HttpStatic.CONTENT_ENCODING_STRING  : 	{
				ret = contentEncoding == null ? contentEncoding = headers.get(header) : contentEncoding;
				break;
			}
			case HttpStatic.TRANSFER_ENCODING_STRING : 	{
				ret = transferEncoding == null ? transferEncoding = headers.get(header) : transferEncoding;
				break;
			}
			default: ret = headers.get(header);
		}

		return ret;
	}

	/**
	 * 填充 Header
	 * @param header header 的 name
	 * @param value header 的 nam值
	 * @return header 的 name
	 */
	public String put(String header,String value){
		switch (header) {
			case HttpStatic.CONTENT_TYPE_STRING 	 :  this.contentType 		= value; break;
			case HttpStatic.CONTENT_LENGTH_STRING 	 : 	this.contentLength 		= value; break;
			case HttpStatic.CONTENT_ENCODING_STRING  : 	this.contentEncoding 	= value; break;
			case HttpStatic.TRANSFER_ENCODING_STRING : 	this.transferEncoding 	= value; break;
			default : headers.put(header,value);
		}
		return value;
	}

	/**
	 * 填充 Header
	 * @param valueMap Header 的 Map 形式
	 */
	public void putAll(Map<String, String> valueMap){
		for(Entry<String, String> entry : valueMap.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Header的个数
	 * @return header 元素的数量
	 */
	public int size(){
		return headers.size();
	}

	public Header copyFrom(Header otherHeader) {
		this.putAll(otherHeader.getHeaders());
		this.put(HttpStatic.CONTENT_TYPE_STRING, otherHeader.get(HttpStatic.CONTENT_TYPE_STRING));
		this.put(HttpStatic.CONTENT_LENGTH_STRING, otherHeader.get(HttpStatic.CONTENT_LENGTH_STRING));
		this.put(HttpStatic.CONTENT_ENCODING_STRING, otherHeader.get(HttpStatic.CONTENT_ENCODING_STRING));
		this.put(HttpStatic.TRANSFER_ENCODING_STRING, otherHeader.get(HttpStatic.TRANSFER_ENCODING_STRING));
		return this;
	}

	/**
	 * 清空头
	 */
	public void clear(){
		contentType = null;
		contentLength = null;
		contentEncoding = null;
		transferEncoding = null;
		headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	}

	@Override
	public String toString(){
		StringBuilder headerContent = THREAD_STRING_BUILDER.get();
		headerContent.setLength(0);

		if(contentType!=null) {
			headerContent.append(HttpStatic.CONTENT_TYPE_STRING);
			headerContent.append(HttpStatic.HEADER_SPLITER_STRING);
			headerContent.append(contentType);
			headerContent.append(HttpStatic.LINE_MARK_STRING);
		}

		if(contentEncoding!=null) {
			headerContent.append(HttpStatic.CONTENT_ENCODING_STRING);
			headerContent.append(HttpStatic.HEADER_SPLITER_STRING);
			headerContent.append(contentEncoding);
			headerContent.append(HttpStatic.LINE_MARK_STRING);
		}

		if(contentLength!=null) {
			headerContent.append(HttpStatic.CONTENT_LENGTH_STRING);
			headerContent.append(HttpStatic.HEADER_SPLITER_STRING);
			headerContent.append(contentLength);
			headerContent.append(HttpStatic.LINE_MARK_STRING);
		}

		if(transferEncoding!=null) {
			headerContent.append(HttpStatic.TRANSFER_ENCODING_STRING);
			headerContent.append(HttpStatic.HEADER_SPLITER_STRING);
			headerContent.append(transferEncoding);
			headerContent.append(HttpStatic.LINE_MARK_STRING);
		}

		for(Entry<String,String> headerItemEntry : this.headers.entrySet()){
			String key = headerItemEntry.getKey();
			String value = headerItemEntry.getValue();
			if(!key.isEmpty()){
				headerContent.append(key);
				headerContent.append(HttpStatic.HEADER_SPLITER_STRING);
				headerContent.append(value);
				headerContent.append(HttpStatic.LINE_MARK_STRING);
			}
		}
		return headerContent.toString();
	}
}
