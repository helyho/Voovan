package org.voovan.tools;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * 类文字命名
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

    public Cipher(String algorithm) throws NoSuchAlgorithmException {
        if(algorithm==null){
            throw new NoSuchAlgorithmException("algorithm may be null");
        }
        this.algorithm = algorithm;
    }

    public Cipher(String algorithm, String mode, String fillMode) throws NoSuchAlgorithmException {
        if(algorithm==null  || mode == null || fillMode == null){
            throw new NoSuchAlgorithmException("algorithm / mode / fillMode may be null");
        }
        this.algorithm = algorithm;
        this.mode = mode;
        this.fillMode = fillMode;
    }

    public byte[] generateSymmetryKey() throws NoSuchAlgorithmException {
        this.secretKey = KeyGenerator.getInstance(algorithm).generateKey();
        return secretKey.getEncoded();
    }

    public KeyPairStore generatPairKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        KeyPair keyPair =  keyPairGenerator.generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        KeyPairStore keyPairStore = new KeyPairStore(keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded());
        return keyPairStore;
    }

    public SecretKey loadSymmetryKey(byte[] keyBytes) {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        return new SecretKeySpec(keyBytes, algorithm);
    }

    public PublicKey loadPublicKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory =  KeyFactory.getInstance(algorithm);
        return keyFactory.generatePublic(x509EncodedKeySpec);
    }

    public PrivateKey loadPrivateKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory =  KeyFactory.getInstance(algorithm);
        return keyFactory.generatePrivate(x509EncodedKeySpec);
    }

    public byte[] encode(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

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

    public byte[] decode(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
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
