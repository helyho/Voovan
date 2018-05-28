package org.voovan.test.tools.security;

import junit.framework.TestCase;
import org.voovan.tools.log.Logger;
import org.voovan.tools.security.Cipher;
import org.voovan.tools.security.TBase64;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class CipherUnit extends TestCase{

    public void testCipher() throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException, InvalidKeySpecException {
        //对称加密
        Logger.simple("===========生成AES密钥===========");
        Cipher cipher = new Cipher("AES","ECB","PKCS5Padding");
        String  ba64Key = TBase64.encode(cipher.generateSymmetryKey());
        Logger.simple("AES密钥: " +ba64Key);

        Logger.simple("===========读取AES密钥加密===========");
        cipher = new Cipher("AES","ECB","PKCS5Padding");
        cipher.loadSymmetryKey(TBase64.decode(ba64Key));
        byte[] msg = cipher.encrypt("asdfadf".getBytes());
        Logger.simple("AES加密后: "+new String(msg));

        Logger.simple("===========读取AES密钥解密===========");
        cipher = new Cipher("AES","ECB","PKCS5Padding");
        cipher.loadSymmetryKey(TBase64.decode(ba64Key));
        byte[] msg1 = cipher.decrypt(msg);
        Logger.simple("AES解密后:" + new String(msg1));

        Logger.simple(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        //非对称加密
        Logger.simple("===========生成 RSA 密钥===========");
        cipher = new Cipher("RSA","ECB","PKCS1Padding");
        Cipher.KeyPairStore kps = cipher.generatPairKey();
        String ba64Private = TBase64.encode(kps.getPrivateKey());
        String ba64Public = TBase64.encode(kps.getPublicKey());
        Logger.simple("RSA公钥: " +ba64Private);
        Logger.simple("RSA私钥: " +ba64Public);


        Logger.simple("===========RSA读取公钥加密===========");
        cipher = new Cipher("RSA","ECB","PKCS1Padding");
        cipher.loadPublicKey(TBase64.decode(ba64Public));
        msg = cipher.encrypt("asdfadf".getBytes());
        Logger.simple(" RSA加密后: "+new String(msg));


        Logger.simple("===========RSA读取私钥解密===========");
        cipher = new Cipher("RSA","ECB","PKCS1Padding");
        cipher.loadPrivateKey(TBase64.decode(ba64Private));
        msg1 = cipher.decrypt(msg);
        Logger.simple("RSA解密后: "+new String(msg1));
    }
}
