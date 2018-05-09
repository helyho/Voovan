package org.voovan.test.tools.security;

import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;
import org.voovan.tools.security.ECCSignature;
import junit.framework.TestCase;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class ECCSignatureUnit extends TestCase{

    public void testECCSignature() {
        try {
            ECCSignature eccSignature = new ECCSignature();
            eccSignature.generateKey();

            byte[] signed = eccSignature.signature("abc".getBytes());

            Logger.simple(eccSignature.verify("abc".getBytes(), signed));

        }catch(Exception e) {
            throw new RuntimeException(e);
        }
        TEnv.sleep(1000);
    }

}
