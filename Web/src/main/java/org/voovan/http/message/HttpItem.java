package org.voovan.http.message;

import org.voovan.Global;
import org.voovan.tools.security.THash;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpItem {
	public final static int HTTP_ITEM_MAX_LENGTH = 1024;

	public static final Map<String, HttpItem> HTTP_ITEM_MAP = new ConcurrentHashMap<String, HttpItem>();
	public static final Map[]  HTTP_ITEM_LENGTH_LIST = new Map[HTTP_ITEM_MAX_LENGTH];
	static {
		for(int i=0;i< HTTP_ITEM_LENGTH_LIST.length; i++){
			HTTP_ITEM_LENGTH_LIST[i] = new ConcurrentHashMap<Integer, HttpItem>();
		}
	}

	public static final HttpItem EMPTY = new HttpItem("");

	private final byte[] bytes;
	private final String string;
	private int hashcode = 0;

	public HttpItem(String string) {
		this.string = string;
		this.bytes = string.getBytes();
		this.hashcode = THash.hashTime31(bytes, 0, bytes.length);

		HTTP_ITEM_MAP.putIfAbsent(string, this);
		HTTP_ITEM_LENGTH_LIST[bytes.length].put(hashcode, this);
	}

	public HttpItem(byte[] bytesArg, int offset, int length) {
		byte[] bytes = new byte[length];
		System.arraycopy(bytesArg, offset, bytes, 0, length);
		this.bytes = bytes;
		this.string = new String(bytes, Global.CS_ASCII);

		this.hashcode = THash.hashTime31(bytes, 0, length);

		HTTP_ITEM_MAP.putIfAbsent(string, this);
		HTTP_ITEM_LENGTH_LIST[bytes.length].put(hashcode, this);
	}

	public byte[] getBytes() {
		return bytes;
	}

	public String getString() {
		return string;
	}

	public static HttpItem getHttpItem(byte[] bytes, int offset, int length){
		int hashcode = THash.hashTime31(bytes, offset, length);
		HttpItem httpItem = ((Map<Integer, HttpItem>)HTTP_ITEM_LENGTH_LIST[length]).get(hashcode);
		if(httpItem == null){
			httpItem = new HttpItem(bytes, offset, length);
		}

		return httpItem;
	}
}
