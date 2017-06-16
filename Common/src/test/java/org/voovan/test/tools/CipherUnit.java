package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.Cipher;
import org.voovan.tools.log.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class CipherUnit extends TestCase{

    public void testCipher() throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException {
        Cipher cipher = new Cipher("AES","ECB","PKCS5Padding");
//        cipher.generatPairKey();
        cipher.generateSymmetryKey();
        byte[] msg = cipher.encode("asdfadf".getBytes());
        Logger.simple(new String(msg));
        byte[] msg1 = cipher.decode(msg);
        Logger.simple(new String(msg1));
    }
}
