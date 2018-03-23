package org.voovan.tools.security;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 封装 JDK 的加密解密算法
 *
 * AES/CBC/NoPadding (128)
 * AES/CBC/PKCS5Padding (128)
 * AES/ECB/NoPadding (128)
 * AES/ECB/PKCS5Padding (128)
 * DES/CBC/NoPadding (56)
 * DES/CBC/PKCS5Padding (56)
 * DES/ECB/NoPadding (56)
 * DES/ECB/PKCS5Padding (56)
 * DESede/CBC/NoPadding (168)
 * DESede/CBC/PKCS5Padding (168)
 * DESede/ECB/NoPadding (168)
 * DESede/ECB/PKCS5Padding (168)
 * RSA/ECB/PKCS1Padding (1024, 2048)
 * RSA/ECB/OAEPWithSHA-1AndMGF1Padding (1024, 2048)
 * RSA/ECB/OAEPWithSHA-256AndMGF1Padding (1024, 2048)
 *
 * - 第一段是加密算法的名称，如DESede实际上是3-DES。
 *      这一段还可以放其它的对称加密算法，如Blowfish等。
 * - 第二段是分组加密的模式，除了CBC和ECB之外，还可以是NONE/CFB/QFB等。最常用的就是CBC和ECB了。
 *      DES采用分组加密的方式，将明文按8字节（64位）分组分别加密。如果每个组独立处理，则是ECB。
 *      CBC的处理方式是先用初始向量IV对第一组加密，再用第一组的密文作为密钥对第二组加密，然后依次完成整个加密操作。
 *      如果明文中有两个分组的内容相同，ECB会得到完全一样的密文，但CBC则不会。
 * - 第三段是指最后一个分组的填充方式。
 *      大部分情况下，明文并非刚好64位的倍数。对于最后一个分组，如果长度小于64位，则需要用数据填充至64位。
 *      PKCS5Padding是常用的填充方式，如果没有指定，默认的方式就是它。
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Cipher {
    private String algorithm;   //AES, RSA
    private String mode;        //ECB, CBC, CTR, OCF, CFB
    private String fillMode;    //PKCS1Padding, PKCS5Padding, PKCS7Padding,NoPadding
    private SecretKey secretKey;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    /**
     * 构造函数
     * @param algorithm 加密算法名称
     * @throws NoSuchAlgorithmException 算法指定异常
     */
    public Cipher(String algorithm) throws NoSuchAlgorithmException {
        if(algorithm==null){
            throw new NoSuchAlgorithmException("algorithm may be null");
        }
        this.algorithm = algorithm;
    }

    /**
     * 构造函数
     * @param algorithm 加密算法名称
     * @param mode      加密形式
     * @param fillMode  密钥填充方式
     * @throws NoSuchAlgorithmException 算法指定异常
     */
    public Cipher(String algorithm, String mode, String fillMode) throws NoSuchAlgorithmException {
        if(algorithm==null  || mode == null || fillMode == null){
            throw new NoSuchAlgorithmException("algorithm / mode / fillMode may be null");
        }
        this.algorithm = algorithm;
        this.mode = mode;
        this.fillMode = fillMode;
    }

    /**
     * 生成一个对称加密的密钥
     * @return 密钥
     * @throws NoSuchAlgorithmException 算法指定异常
     */
    public byte[] generateSymmetryKey() throws NoSuchAlgorithmException {
        this.secretKey = KeyGenerator.getInstance(algorithm).generateKey();
        return secretKey.getEncoded();
    }

    /**
     * 生成一对非对称加密的公钥和私钥
     * @return 保存公钥和私钥的对象
     * @throws NoSuchAlgorithmException 算法指定异常
     */
    public KeyPairStore generatPairKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        KeyPair keyPair =  keyPairGenerator.generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        KeyPairStore keyPairStore = new KeyPairStore(keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded());
        return keyPairStore;
    }

    /**
     * 读取对称加密的密钥
     * @param keyBytes 密钥
     * @return Key 对象
     */
    public SecretKey loadSymmetryKey(byte[] keyBytes) {
        this.secretKey = new SecretKeySpec(keyBytes, algorithm);
        return secretKey;
    }

    /**
     * 读取非对称加密的公钥
     * @param keyBytes 公钥
     * @return 公钥 Key 对象
     * @throws NoSuchAlgorithmException 算法异常
     * @throws InvalidKeySpecException 密钥异常
     */
    public PublicKey loadPublicKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory =  KeyFactory.getInstance(algorithm);
        this.publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
        return publicKey;
    }

    /**
     * 读取非对称加密的私钥
     * @param keyBytes 私钥
     * @return 私钥 Key 对象
     * @throws NoSuchAlgorithmException 算法异常
     * @throws InvalidKeySpecException 密钥异常
     */
    public PrivateKey loadPrivateKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory =  KeyFactory.getInstance(algorithm);
        this.privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        return privateKey;
    }

    /**
     * 加密算法
     * @param data 明文
     * @return 密文
     * @throws NoSuchPaddingException    填充模式异常
     * @throws NoSuchAlgorithmException  算法异常
     * @throws InvalidKeyException       密钥异常
     * @throws BadPaddingException       填充操作异常
     * @throws IllegalBlockSizeException 异常
     */
    public byte[] encrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        String tmp = algorithm + (mode!=null ? ("/"+mode) : "") + (fillMode!=null ? ("/"+fillMode) : "");
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(tmp);
        Key key = null;
        if(secretKey != null){
            key = secretKey;
        } else if(publicKey != null){
            key = publicKey;
        }

        if(key == null){
            throw new InvalidKeyException("Avaliable key is not found");
        }

        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * 加密算法
     * @param data 密文
     * @return 明文
     * @throws NoSuchPaddingException    填充模式异常
     * @throws NoSuchAlgorithmException  算法异常
     * @throws InvalidKeyException       密钥异常
     * @throws BadPaddingException       填充操作异常
     * @throws IllegalBlockSizeException 异常
     */
    public byte[] decrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        String tmp = algorithm + (mode!=null ? ("/"+mode) : "") + (fillMode!=null ? ("/"+fillMode) : "");
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(tmp);
        Key key = null;
        if(secretKey != null){
            key = secretKey;
        } else if(privateKey != null){
            key = privateKey;
        }

        if(key == null){
            throw new InvalidKeyException("Avaliable key is not found");
        }

        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * 保存非对称加密算法的公钥和私钥
     */
    public class KeyPairStore {
        private byte[] publicKey;
        private byte[] privateKey;

        public KeyPairStore(byte[] publicKey, byte[] privateKey){
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public byte[] getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(byte[] publicKey) {
            this.publicKey = publicKey;
        }

        public byte[] getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(byte[] privateKey) {
            this.privateKey = privateKey;
        }
    }

}
