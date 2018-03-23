package org.voovan.tools.security;

import org.voovan.tools.log.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * hash 算法类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class THash {

	/**
	 * BASE64解密
	 * 
	 * @param key 待解密字符串
	 * @return 解密后字符串
	 */
	public static String decryptBASE64(String key) {
		return  new String(TBase64.decode(key));
	}

	/**
	 * BASE64加密
	 *
	 * @param key 待加密字符串
	 * @return 加密后字符串
	 */
	public static String encryptBASE64(String key) {
		return TBase64.encode(key.getBytes());
	}

	/**
	 * MD5加密
	 * @param str 待加密字符串
	 * @return 加密结果
     */
	public static String encryptMD5(String str){
		return digest("MD5", str);
	}

	/**
	 * SHA 加密
	 * @param str 待加密字符串
	 * @return 加密结果
     */
	public static String encryptSHA(String str){
		return digest("SHA", str);
	}
	
	private static String digest(String code,String str) {  

        try {
			//code 可选 hash 算法
            //MD2
            //MD5
            //SHA-1/SHA
            //SHA-256
            //SHA-384
            //SHA-512
			MessageDigest messageDigest = MessageDigest.getInstance(code);
  
            messageDigest.reset();  
  
            messageDigest.update(str.getBytes());

			byte[] byteArray = messageDigest.digest();

			StringBuffer md5StrBuff = new StringBuffer();

			for (int i = 0; i < byteArray.length; i++) {
				if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
					md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));
				else
					md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
			}

			return md5StrBuff.toString();

		} catch (NoSuchAlgorithmException e) {
        	Logger.error("No such algorithm",e);
            return null;
        } 

    }

	/**
	 * Time33算法
	 * @param source 待加密字符串
	 * @return 加密结果
     */
	public static int hash_time33(String source) {
		int hash = 0;
		for (int i = 0; i < source.length(); i++) {
			hash = hash * 33 + Integer.valueOf(source.charAt(i));
		}
		return hash;
	}

}