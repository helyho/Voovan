package org.voovan.tools.security;

import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * hash 算法类
 *
 * https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest
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

	public static String digest(String code,String str) {

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
	 * Time31算法
	 * @param source  字节数据
	 * @param offset 字节数据偏移量
	 * @param length 长度* @param source 待加密字符串
	 * @return 加密结果
     */
	public static int hashTime31(byte[] source, int offset, int length) {
		int hash = 0;
		for (int i = offset; i < length; i++) {
			hash = ((hash << 5) - hash) + source[i];
		}
		return hash;
	}




	/**
	 * Time31算法
	 * @param byteBuffer  字节数据
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @return 加密结果
	 */
	public static int hashTime31(ByteBuffer byteBuffer, int offset, int length) {
		int hash = 0;
		for (int i = offset; i < length; i++) {
			hash = ((hash << 5) - hash) + byteBuffer.get(i);
		}
		return hash;
	}



	/**
	 * Time31算法
	 * @param str 字符串
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @param seed 上次 hash 的种子
	 * @return 加密结果
	 */
	public static int hashTime31(String str, int offset, int length, int seed) {
		int hash = seed;
		for (int i = offset; i < length; i++) {
			hash = ((hash << 5) - hash) + str.charAt(i);
		}
		return hash;
	}

	/**
	 * Time31算法
	 * @param str 字符串
	 * @param offset 字节数据偏移量
	 * @param length 长度* @param source 待加密字符串
	 * @return 加密结果
	 */
	public static int hashTime31(String str, int offset, int length) {

		return hashTime31(str, offset, length, 0);
	}


	/**
	 * Time31算法
	 * @param str 字符串
	 * @return 加密结果
	 */
	public static int hashTime31(String str) {

		return hashTime31(str, 0, str.length(), 0);
	}

    /**
     * Time31算法
     * @param strs 字符串数组
     * @return 加密结果
     */
	public static int hashTime31(String ... strs) {
		int hash = 0;
		for(int i=0;i<strs.length;i++){
			String val = strs[i];
			if(val !=null){
				hash = hash + hashTime31(val, 0, val.length(), hash);
			}
		}

		return hash;
	}

	/**
	 * 改进的32位FNV算法1
	 * @param data 数组
	 * @param offset 数据偏移量
	 * @param length 长度
	 * @return int值
	 */
	public static int HashFNV1(byte[] data, int offset, int length)
	{
		final int p = 16777619;
		int hash = (int)2166136261L;
		for (int i = offset; i < length; i++) {
			byte b = data[i];
			hash = (hash ^ b) * p;
		}

		hash += hash << 13;
		hash ^= hash >> 7;
		hash += hash << 3;
		hash ^= hash >> 17;
		hash += hash << 5;
		return hash;

	}

	/**
	 * 改进的32位FNV算法1
	 * @param byteBuffer  字节数据
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @return int值
	 */
	public static int HashFNV1(ByteBuffer byteBuffer, int offset, int length)
	{
		final int p = 16777619;
		int hash = (int)2166136261L;
		for (int i = offset; i < length; i++) {
			byte b = byteBuffer.get(i);
			hash = (hash ^ b) * p;
		}

		hash += hash << 13;
		hash ^= hash >> 7;
		hash += hash << 3;
		hash ^= hash >> 17;
		hash += hash << 5;
		return hash;
	}

	/**
	 * 改进的32位FNV算法1
	 * @param str 字符串
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @param seed 上次 hash 的种子
	 * @return int值
	 */
	public static int HashFNV1(String str, int offset, int length, int seed)
	{
		final int p = 16777619;
		int hash = seed;
		for (int i = offset; i < length; i++) {
			byte b = (byte)str.charAt(i);
			hash = (hash ^ b) * p;
		}

		hash += hash << 13;
		hash ^= hash >> 7;
		hash += hash << 3;
		hash ^= hash >> 17;
		hash += hash << 5;
		return hash;
	}

	/**
	 * 改进的32位FNV算法1
	 * @param str 字符串
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @return int值
	 */
	public static int HashFNV1(String str, int offset, int length) {
		return HashFNV1(str, offset, length, (int)2166136261L);
	}


	/**
	 * 改进的32位FNV算法1
	 * @param str 字符串
	 * @return int值
	 */
	public static int HashFNV1(String str) {
		return HashFNV1(str, 0, str.length(), (int)2166136261L);
	}

	/**
	 * Time31算法
	 * @param strs 字符串数组
	 * @return 加密结果
	 */
	public static int HashFNV1(String ... strs) {
		int hash = (int)2166136261L;
		for(int i=0;i<strs.length;i++){
			String val = strs[i];
			if(val !=null){
				hash = hash + HashFNV1(val, 0, val.length(), hash);
			}
		}

		return hash;
	}

	public static int murmurHash2_32(final byte[] data, int offset, int length, int seed) {
		final int m = 0x5bd1e995;
		final int r = 24;

		int h = seed^length;
		int length4 = length/4;

		for (int i=offset; i<length4; i++) {
			final int i4 = i*4;
			int k = (data[i4+0]&0xff) +((data[i4+1]&0xff)<<8)
					+((data[i4+2]&0xff)<<16) +((data[i4+3]&0xff)<<24);
			k *= m;
			k ^= k >>> r;
			k *= m;
			h *= m;
			h ^= k;
		}

		// Handle the last few bytes of the input array
		switch (length%4) {
			case 3: h ^= (data[(length&~3) +2]&0xff) << 16;
			case 2: h ^= (data[(length&~3) +1]&0xff) << 8;
			case 1: h ^= (data[length&~3]&0xff);
				h *= m;
		}

		h ^= h >>> 13;
		h *= m;
		h ^= h >>> 15;

		return h;
	}

	public static long murmurHash2_64(final byte[] data, int offset, int length, int seed) {
		final long m = 0xc6a4a7935bd1e995L;
		final int r = 47;

		long h = (seed&0xffffffffl)^(length*m);

		int length8 = length/8;

		for (int i=offset; i<length8; i++) {
			final int i8 = i*8;
			long k =  ((long)data[i8+0]&0xff)      +(((long)data[i8+1]&0xff)<<8)
					+(((long)data[i8+2]&0xff)<<16) +(((long)data[i8+3]&0xff)<<24)
					+(((long)data[i8+4]&0xff)<<32) +(((long)data[i8+5]&0xff)<<40)
					+(((long)data[i8+6]&0xff)<<48) +(((long)data[i8+7]&0xff)<<56);

			k *= m;
			k ^= k >>> r;
			k *= m;

			h ^= k;
			h *= m;
		}

		switch (length%8) {
			case 7: h ^= (long)(data[(length&~7)+6]&0xff) << 48;
			case 6: h ^= (long)(data[(length&~7)+5]&0xff) << 40;
			case 5: h ^= (long)(data[(length&~7)+4]&0xff) << 32;
			case 4: h ^= (long)(data[(length&~7)+3]&0xff) << 24;
			case 3: h ^= (long)(data[(length&~7)+2]&0xff) << 16;
			case 2: h ^= (long)(data[(length&~7)+1]&0xff) << 8;
			case 1: h ^= (long)(data[length&~7]&0xff);
				h *= m;
		};

		h ^= h >>> r;
		h *= m;
		h ^= h >>> r;

		return h;
	}

	public static int murmurHash3_32(byte[] data, int offset, int len, int seed) {
		return THash.MurmurHash3.x86_32(data, offset, len, seed);
	}

	public static int murmurHash3_32(CharSequence data, int offset, int len, int seed) {
		return THash.MurmurHash3.x86_32(data, offset, len, seed);
	}

	public static long[] murmurHash3_128(byte[] data, int offset, int len, int seed) {
		return THash.MurmurHash3.x64_128(data, offset, len, seed);
	}

	private static class MurmurHash3 {

		public static final int fmix32(int h) {
			h ^= h >>> 16;
			h *= 0x85ebca6b;
			h ^= h >>> 13;
			h *= 0xc2b2ae35;
			h ^= h >>> 16;
			return h;
		}

		public static final long fmix64(long k) {
			k ^= k >>> 33;
			k *= 0xff51afd7ed558ccdL;
			k ^= k >>> 33;
			k *= 0xc4ceb9fe1a85ec53L;
			k ^= k >>> 33;
			return k;
		}

		public static final long getLongLittleEndian(byte[] buf, int offset) {
			return     ((long)buf[offset+7]    << 56)
					| ((buf[offset+6] & 0xffL) << 48)
					| ((buf[offset+5] & 0xffL) << 40)
					| ((buf[offset+4] & 0xffL) << 32)
					| ((buf[offset+3] & 0xffL) << 24)
					| ((buf[offset+2] & 0xffL) << 16)
					| ((buf[offset+1] & 0xffL) << 8)
					| ((buf[offset  ] & 0xffL));
		}


		public static int x86_32(byte[] data, int offset, int len, int seed) {

			final int c1 = 0xcc9e2d51;
			final int c2 = 0x1b873593;

			int h1 = seed;
			int roundedEnd = offset + (len & 0xfffffffc);

			for (int i=offset; i<roundedEnd; i+=4) {
				// little endian load order
				int k1 = (data[i] & 0xff) | ((data[i+1] & 0xff) << 8) | ((data[i+2] & 0xff) << 16) | (data[i+3] << 24);
				k1 *= c1;
				k1 = (k1 << 15) | (k1 >>> 17);
				k1 *= c2;

				h1 ^= k1;
				h1 = (h1 << 13) | (h1 >>> 19);
				h1 = h1*5+0xe6546b64;
			}

			// tail
			int k1 = 0;

			switch(len & 0x03) {
				case 3:
					k1 = (data[roundedEnd + 2] & 0xff) << 16;
				case 2:
					k1 |= (data[roundedEnd + 1] & 0xff) << 8;
				case 1:
					k1 |= (data[roundedEnd] & 0xff);
					k1 *= c1;
					k1 = (k1 << 15) | (k1 >>> 17);
					k1 *= c2;
					h1 ^= k1;
			}

			h1 ^= len;

			h1 ^= h1 >>> 16;
			h1 *= 0x85ebca6b;
			h1 ^= h1 >>> 13;
			h1 *= 0xc2b2ae35;
			h1 ^= h1 >>> 16;

			return h1;
		}

		public static int x86_32(CharSequence data, int offset, int len, int seed) {

			final int c1 = 0xcc9e2d51;
			final int c2 = 0x1b873593;

			int h1 = seed;

			int pos = offset;
			int end = offset + len;
			int k1 = 0;
			int k2 = 0;
			int shift = 0;
			int bits = 0;
			int nBytes = 0;


			while (pos < end) {
				int code = data.charAt(pos++);
				if (code < 0x80) {
					k2 = code;
					bits = 8;
				}
				else if (code < 0x800) {
					k2 = (0xC0 | (code >> 6))
							| ((0x80 | (code & 0x3F)) << 8);
					bits = 16;
				}
				else if (code < 0xD800 || code > 0xDFFF || pos>=end) {
					// we check for pos>=end to encode an unpaired surrogate as 3 bytes.
					k2 = (0xE0 | (code >> 12))
							| ((0x80 | ((code >> 6) & 0x3F)) << 8)
							| ((0x80 | (code & 0x3F)) << 16);
					bits = 24;
				} else {
					int utf32 = (int) data.charAt(pos++);
					utf32 = ((code - 0xD7C0) << 10) + (utf32 & 0x3FF);
					k2 = (0xff & (0xF0 | (utf32 >> 18)))
							| ((0x80 | ((utf32 >> 12) & 0x3F))) << 8
							| ((0x80 | ((utf32 >> 6) & 0x3F))) << 16
							|  (0x80 | (utf32 & 0x3F)) << 24;
					bits = 32;
				}


				k1 |= k2 << shift;

				shift += bits;
				if (shift >= 32) {

					k1 *= c1;
					k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
					k1 *= c2;

					h1 ^= k1;
					h1 = (h1 << 13) | (h1 >>> 19);  // ROTL32(h1,13);
					h1 = h1*5+0xe6546b64;

					shift -= 32;
					if (shift != 0) {
						k1 = k2 >>> (bits-shift);   // bits used == bits - newshift
					} else {
						k1 = 0;
					}
					nBytes += 4;
				}

			}

			if (shift > 0) {
				nBytes += shift >> 3;
				k1 *= c1;
				k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
				k1 *= c2;
				h1 ^= k1;
			}

			h1 ^= nBytes;

			h1 ^= h1 >>> 16;
			h1 *= 0x85ebca6b;
			h1 ^= h1 >>> 13;
			h1 *= 0xc2b2ae35;
			h1 ^= h1 >>> 16;

			return h1;
		}


		public static long[] x64_128(byte[] key, int offset, int len, int seed) {
			long h1 = seed & 0x00000000FFFFFFFFL;
			long h2 = seed & 0x00000000FFFFFFFFL;

			final long c1 = 0x87c37b91114253d5L;
			final long c2 = 0x4cf5ad432745937fL;

			int roundedEnd = offset + (len & 0xFFFFFFF0);  // round down to 16 byte block
			for (int i=offset; i<roundedEnd; i+=16) {
				long k1 = getLongLittleEndian(key, i);
				long k2 = getLongLittleEndian(key, i+8);
				k1 *= c1; k1  = Long.rotateLeft(k1,31); k1 *= c2; h1 ^= k1;
				h1 = Long.rotateLeft(h1,27); h1 += h2; h1 = h1*5+0x52dce729;
				k2 *= c2; k2  = Long.rotateLeft(k2,33); k2 *= c1; h2 ^= k2;
				h2 = Long.rotateLeft(h2,31); h2 += h1; h2 = h2*5+0x38495ab5;
			}

			long k1 = 0;
			long k2 = 0;

			switch (len & 15) {
				case 15: k2  = (key[roundedEnd+14] & 0xffL) << 48;
				case 14: k2 |= (key[roundedEnd+13] & 0xffL) << 40;
				case 13: k2 |= (key[roundedEnd+12] & 0xffL) << 32;
				case 12: k2 |= (key[roundedEnd+11] & 0xffL) << 24;
				case 11: k2 |= (key[roundedEnd+10] & 0xffL) << 16;
				case 10: k2 |= (key[roundedEnd+ 9] & 0xffL) << 8;
				case  9: k2 |= (key[roundedEnd+ 8] & 0xffL);
					k2 *= c2; k2  = Long.rotateLeft(k2, 33); k2 *= c1; h2 ^= k2;
				case  8: k1  = ((long)key[roundedEnd+7]) << 56;
				case  7: k1 |= (key[roundedEnd+6] & 0xffL) << 48;
				case  6: k1 |= (key[roundedEnd+5] & 0xffL) << 40;
				case  5: k1 |= (key[roundedEnd+4] & 0xffL) << 32;
				case  4: k1 |= (key[roundedEnd+3] & 0xffL) << 24;
				case  3: k1 |= (key[roundedEnd+2] & 0xffL) << 16;
				case  2: k1 |= (key[roundedEnd+1] & 0xffL) << 8;
				case  1: k1 |= (key[roundedEnd  ] & 0xffL);
					k1 *= c1; k1  = Long.rotateLeft(k1,31); k1 *= c2; h1 ^= k1;
			}

			h1 ^= len; h2 ^= len;

			h1 += h2;
			h2 += h1;

			h1 = fmix64(h1);
			h2 = fmix64(h2);

			h1 += h2;
			h2 += h1;

			return new long[]{h1, h2};
		}

	}
}