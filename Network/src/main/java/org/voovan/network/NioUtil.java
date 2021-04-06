package org.voovan.network;

import org.voovan.tools.TEnv;
import org.voovan.tools.TUnsafe;
import org.voovan.tools.buffer.TByteBuffer;
import org.voovan.tools.collection.ArraySet;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import sun.nio.ch.SelectorImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Consumer;

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


	//==================================================================================================================
	public static Long SC_FD_FIELD_OFFSET = null;
	public static Long DC_FD_FIELD_OFFSET = null;

	static  {
		try {
			Class socketChannelClazz = Class.forName("sun.nio.ch.SocketChannelImpl");
			Field scFDField = TReflect.findField(socketChannelClazz, "fd");
			SC_FD_FIELD_OFFSET = TUnsafe.getFieldOffset(scFDField);

			Class DatagramChannelClazz = Class.forName("sun.nio.ch.DatagramChannelImpl");
			Field dcFDField = TReflect.findField(DatagramChannelClazz, "fd");
			DC_FD_FIELD_OFFSET = TUnsafe.getFieldOffset(dcFDField);
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	/**
	 * 绑定 FileDescriptor 到 socketContext
	 * @param socketContext SocketContext 对象
	 */
	public static void bindFileDescriptor(SocketContext socketContext) {
		if(socketContext.connectModel != ConnectModel.LISTENER) {
			long offset = socketContext.connectType == ConnectType.TCP ? SC_FD_FIELD_OFFSET : DC_FD_FIELD_OFFSET;
			socketContext.setFileDescriptor((FileDescriptor) TUnsafe.getUnsafe().getObject(socketContext.socketChannel(), offset));
		}
	}

	//==================================================================================================================
	private static MethodHandle READ_METHOD_HANDLE = null;
	private static MethodHandle WRITE_METHOD_HANDLE = null;
	private static MethodHandle SELECTOR_METHOD_HANDLE = null;

	static {
		try {
			if(SocketContext.DIRECT_IO) {
				final Class fileDispatcherClazz = Class.forName("sun.nio.ch.FileDispatcherImpl");
				Method read0 = TReflect.findMethod(fileDispatcherClazz, "read0", FileDescriptor.class, long.class, int.class);
				READ_METHOD_HANDLE = MethodHandles.lookup().unreflect(read0);

				Method write1 = TReflect.findMethod(fileDispatcherClazz, "write0", FileDescriptor.class, long.class, int.class);
				WRITE_METHOD_HANDLE = MethodHandles.lookup().unreflect(write1);
			}

			if(SocketContext.DIRECT_IO && SELECTOR_METHOD_HANDLE == null) {
				Selector selector = SelectorProvider.provider().openSelector();
				Class clazz = selector.getClass();

				Method selectorMethod = null;
				if(TEnv.JDK_VERSION > 9) {
					selectorMethod = TReflect.findMethod(selector.getClass(), "doSelect", Consumer.class, long.class);
				} else {
					selectorMethod = TReflect.findMethod(selector.getClass(), "doSelect", long.class);
				}
				SELECTOR_METHOD_HANDLE = MethodHandles.lookup().unreflect(selectorMethod);
				selector.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void transformSelector(Selector selector, ArraySet<SelectionKey> selectedKeys) {
		try {
			TReflect.setFieldValue(selector, NioUtil.selectedKeysField, selectedKeys);
			TReflect.setFieldValue(selector, NioUtil.publicSelectedKeysField, selectedKeys);
		} catch (ReflectiveOperationException e) {
			Logger.error(e);
		}
	}


	public static int select(Selector selector, long wait) throws Throwable {
		if(SELECTOR_METHOD_HANDLE!=null) {
			if(TEnv.JDK_VERSION > 9) {
				return (int) SELECTOR_METHOD_HANDLE.invoke(selector, null, SocketContext.SELECT_INTERVAL);
			} else {
				return (int) SELECTOR_METHOD_HANDLE.invoke(selector, SocketContext.SELECT_INTERVAL);
			}
		} else {
			return selector.select(SocketContext.SELECT_INTERVAL);
		}
	}

	public static int read(SocketContext socketContext, ByteBuffer byteBuffer) throws Throwable {
		int readSize = -1;
		if(READ_METHOD_HANDLE!=null) {
			FileDescriptor fileDescriptor = socketContext.getFileDescriptor();
			if (fileDescriptor != null) {
				long offset = TByteBuffer.getAddress(byteBuffer) + byteBuffer.position();
				int length = byteBuffer.remaining();
				readSize = (int) READ_METHOD_HANDLE.invokeExact(fileDescriptor, offset, length);
				if (readSize > 0) {
					byteBuffer.position(byteBuffer.position() + readSize);
				}
			}
		} else {
			readSize = ((SocketChannel)socketContext.socketChannel()).read(byteBuffer);
		}

		return readSize;
	}

	public static int write(SocketContext socketContext, ByteBuffer byteBuffer) throws Throwable {
		int sendSize = -1;
		if(WRITE_METHOD_HANDLE!=null) {
			FileDescriptor fileDescriptor = socketContext.getFileDescriptor();
			if(fileDescriptor!=null) {
				long offset = TByteBuffer.getAddress(byteBuffer) + byteBuffer.position();
				int length = byteBuffer.remaining();
				sendSize = (int) WRITE_METHOD_HANDLE.invokeExact(fileDescriptor, offset, length);
				if (sendSize > 0) {
					byteBuffer.position(byteBuffer.position() + sendSize);
				}
			}
		} else {
			sendSize = ((SocketChannel)socketContext.socketChannel()).write(byteBuffer);
		}

		return sendSize;
	}


	//==================================================================================================================
	/**
	 * 为 SelectionKey 增加一个操作
	 * @param key SelectionKey 对象
	 * @param ops 操作
	 */
	public static void addOps(SelectionKey key, int ops){
		key.interestOps(key.interestOps() | ops);
	}

	/**
	 * 为 SelectionKey 移除一个操作
	 * @param key SelectionKey 对象
	 * @param ops 操作
	 */
	public static void removeOps(SelectionKey key, int ops){
		int oldOps = key.interestOps();
		oldOps &= ~ops;
		key.interestOps(oldOps);
	}


}
