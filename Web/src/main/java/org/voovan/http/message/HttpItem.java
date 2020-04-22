package org.voovan.http.message;

import org.voovan.Global;
import org.voovan.tools.collection.IntKeyMap;
import org.voovan.tools.security.THash;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Http协议中的元素
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpItem {
	public final static int HTTP_ITEM_MAX_LENGTH = 1024;

	public static final IntKeyMap  HTTP_ITEM_MAP = new IntKeyMap(HTTP_ITEM_MAX_LENGTH);

	public static final HttpItem EMPTY = new HttpItem("");

	private final byte[] bytes;
	private final String value;
	private int hashcode = 0;

	public HttpItem(String value) {
		this.value = value;
		this.bytes = value.getBytes();
		this.hashcode = THash.HashFNV1(bytes, 0, bytes.length);

		HTTP_ITEM_MAP.put(hashcode, this);
	}

	public HttpItem(byte[] valueBytes, int offset, int length) {
		byte[] bytes = new byte[length];
		System.arraycopy(valueBytes, offset, bytes, 0, length);
		this.bytes = bytes;
		this.value = new String(bytes, Global.CS_ASCII);

		this.hashcode = THash.HashFNV1(bytes, 0, length);

		HTTP_ITEM_MAP.put(hashcode, this);
	}

	public HttpItem(byte[] valueBytes, int offset, int length, int hash) {
		byte[] bytes = new byte[length];
		System.arraycopy(valueBytes, offset, bytes, 0, length);
		this.bytes = bytes;
		this.value = new String(bytes, Global.CS_ASCII);

		this.hashcode = hash;

		HTTP_ITEM_MAP.put(hashcode, this);
	}

	public byte[] getBytes() {
		return bytes;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int hashCode(){
		return this.hashcode;
	}

	public static HttpItem getHttpItem(byte[] bytes, int offset, int length){
		int hash = THash.HashFNV1(bytes, offset, length);
		HttpItem httpItem = (HttpItem) HTTP_ITEM_MAP.get(hash);
		if(httpItem == null){
			httpItem = new HttpItem(bytes, offset, length);
		}


		//hash 冲突简单解决方案 比较长度, 起始/中位/结束位置的数据是否相等
//		int midOffset = length / 2;
//		if(length == httpItem.getBytes().length
//				&& bytes[offset] == httpItem.bytes[0]
//				&& bytes[offset + midOffset] == httpItem.bytes[midOffset]
//				&& bytes[offset + length - 1] == httpItem.bytes[length-1]) {
//			return httpItem;
//		} else {
//			return new HttpItem(bytes, offset, length, hash);
//		}

		return httpItem;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof HttpItem)) return false;
		HttpItem httpItem = (HttpItem) o;
		return hashcode == httpItem.hashcode;
	}

	@Override
	public String toString() {
		return value;
	}
}
