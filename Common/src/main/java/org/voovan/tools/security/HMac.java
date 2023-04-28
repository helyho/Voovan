package org.voovan.tools.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.voovan.tools.TByte;
import org.voovan.tools.log.Logger;

/**
 * HMac 工具类
 *
 * @author helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class HMac {
    private String algorithm;
    private Mac mac;

    /**
     * SunJCE	HmacMD5
     * SunJCE	HmacSHA1
     * SunJCE	HmacSHA224
     * SunJCE	HmacSHA256
     * SunJCE	HmacSHA384
     * SunJCE	HmacSHA512
     * @param algorithm 算法
     * @param key 用于 hash 的 key
     */
    public HMac(String algorithm, String key) {
        this.algorithm = algorithm;
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "Hmac" + this.algorithm);
        try {
            this.mac = Mac.getInstance("HmacSHA1");
            this.mac.init(signingKey);
        } catch (Exception e) {
            Logger.errorf("HMac init failed: {}", e, algorithm);
        }
    }

    public byte[] sign(byte[] waitSign) {
        return mac.doFinal(waitSign);
    }


    public boolean verify(byte[] waitSign, byte[] verifySign) {
         byte[] currentSign = sign(waitSign);
         if(TByte.byteArrayCompare(currentSign, verifySign)==0) {
            return true;
         }
         return false;
    }
}
