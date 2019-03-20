package org.voovan.network;

import java.lang.reflect.Field;
import java.nio.channels.SelectionKey;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Nio通信工具类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class NioUtil {

	public static Field selectedKeysField;
	public static Field publicSelectedKeysField;

	static {
		Object res = AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				try {
					return Class.forName("sun.nio.ch.SelectorImpl");
				} catch (Throwable cause) {
					return cause;
				}
			}
		});

		final Class selectorImplClass = (Class) res;
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				try {
					selectedKeysField = selectorImplClass
							.getDeclaredField("selectedKeys");
					publicSelectedKeysField = selectorImplClass
							.getDeclaredField("publicSelectedKeys");

					selectedKeysField.setAccessible(true);
					publicSelectedKeysField.setAccessible(true);
					return null;
				} catch (Exception e) {
					return e;
				}
			}
		});
	}

	public static void addOps(SelectionKey key, int ops){
		key.interestOps(key.interestOps() | ops);
	}

	public static void removeOps(SelectionKey key, int ops){
		int oldOps = key.interestOps();
		oldOps &= ~ops;
		key.interestOps(oldOps);
	}


}
