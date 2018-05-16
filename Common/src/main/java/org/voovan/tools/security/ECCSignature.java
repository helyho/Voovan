package org.voovan.tools.security;


import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * ECC签名工具类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class ECCSignature {
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private ECPublicKey publicKey;
    private ECPrivateKey privateKey;

    public ECCSignature() {
    }

    public void generateKey() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
        keyPairGenerator.initialize(ecSpec, random);

        KeyPair kp = keyPairGenerator.generateKeyPair();

        publicKey = (ECPublicKey) kp.getPublic();
        privateKey = (ECPrivateKey) kp.getPrivate();
    }

    public byte[] getPublicKey(){
        if(publicKey == null) {
            return publicKey.getEncoded();
        } else {
            return null;
        }
    }

    public byte[] getPrivateKey(){
        if(privateKey == null) {
            return privateKey.getEncoded();
        } else {
            return null;
        }
    }

    public ECPublicKey loadPublicKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory =  KeyFactory.getInstance("ECDSA");
        this.publicKey = (ECPublicKey)keyFactory.generatePublic(x509EncodedKeySpec);
        return publicKey;
    }

    public ECPrivateKey loadPrivateKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory =  KeyFactory.getInstance("ECDSA");
        this.privateKey = (ECPrivateKey)keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        return privateKey;
    }

    public byte[] signature(byte[] originData) throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Signature signature = Signature.getInstance("ECDSA", "BC");
        signature.initSign(privateKey);
        signature.update(originData);
        return signature.sign();
    }

    public boolean verify(byte[] originData, byte[] signatureCode) throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Signature signature = Signature.getInstance("ECDSA", "BC");
        signature.initVerify(publicKey);
        signature.update(originData);
        return signature.verify(signatureCode);
    }


}
