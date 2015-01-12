package org.hocate.tools;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.hocate.log.Logger;

/**
 * hash 计算类
 * @author helyho
 *
 */
public class THash {

	/**
	 * BASE64解密
	 * 
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static byte[] decryptBASE64(String key) {
		return Base64.getDecoder().decode(key);
	}

	/**
	 * BASE64加密
	 * 
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static String encryptBASE64(byte[] key) {
		return Base64.getEncoder().encodeToString(key);
	}
	
	public static String encryptMD5(String str){
		return digest("MD5", str);
	}
	
	public static String encryptSHA(String str){
		return digest("SHA", str);
	}
	
	private static String digest(String code,String str) {  
        MessageDigest messageDigest = null;  
  
        try {  
            messageDigest = MessageDigest.getInstance(code);  
  
            messageDigest.reset();  
  
            messageDigest.update(str.getBytes("UTF-8"));  
        } catch (NoSuchAlgorithmException e) {  
        	Logger.error("NoSuchAlgorithmException caught!");  
            System.exit(-1);  
        } catch (UnsupportedEncodingException e) {  
            e.printStackTrace();  
        }  
  
        byte[] byteArray = messageDigest.digest();  
  
        StringBuffer md5StrBuff = new StringBuffer();  
  
        for (int i = 0; i < byteArray.length; i++) {              
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)  
                md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));  
            else  
                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));  
        }  
  
        return md5StrBuff.toString();  
    }  

	public static int hash_time33(String source) {
		int hash = 0;
		for (int i = 0; i < source.length(); i++) {
			hash = hash * 33 + Integer.valueOf(source.charAt(i));
		}
		return hash;
	}
	
	public static void main(String[] args) throws Exception {
		Logger.simple(THash.encryptSHA("org.hocate.test"+1.0));
		Logger.simple(THash.hash_time33("org.hocate.test"+1.0));
	}
}