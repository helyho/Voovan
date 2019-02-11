package org.voovan.http.message;

import org.voovan.Global;
import org.voovan.tools.UniqueId;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpItem {
	public static final HttpItem EMPTY = new HttpItem("");


	private final byte[] bytes;
	private final String string;

	private final transient int id;

	public HttpItem(String string) {
		this.string = string;
		this.bytes = string.getBytes();

		id = (int)Global.UNIQUE_ID.nextInt();

		HttpStatic.HTTP_ITEM_MAP.putIfAbsent(string, this);
	}

	public byte[] getBytes() {
		return bytes;
	}

	public String getString() {
		return string;
	}

	public int getId() {
		return id;
	}
}
