package org.voovan.network.filter;

import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.network.exception.IoFilterException;
import org.voovan.tools.Cipher;
import org.voovan.tools.log.Logger;

/**
 * 对称密钥加密截断器
 *
 * @author: helyho
 * Project: DBase
 * Create: 2017/11/1 14:46
 */
public class CipherFilter implements IoFilter {


	Cipher cipher;

	/**
	 * 构造函数
	 * @param cipher 包含对称密钥的算法类
	 */
	public CipherFilter(Cipher cipher){
		this.cipher = cipher;
	}

	@Override
	public Object decode(IoSession session, Object object) throws IoFilterException {
		if(object.getClass() == ByteFilter.BYTE_ARRAY_CLASS ) {
			try {
				return cipher.decrypt((byte[]) object);
			} catch (Exception e) {
				Logger.error("CipherFilter decode error, socket will be close", e);
				session.close();
			}
		}

		return null;
	}

	@Override
	public Object encode(IoSession session, Object object) throws IoFilterException {
		if(object.getClass() == ByteFilter.BYTE_ARRAY_CLASS ) {
			try {
				return cipher.encrypt((byte[])object);
			} catch (Exception e) {
				Logger.error("CipherFilter encode error, socket will be close", e);
				session.close();
			}
		}

		return null;
	}
}
