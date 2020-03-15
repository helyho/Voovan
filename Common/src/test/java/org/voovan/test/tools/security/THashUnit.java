package org.voovan.test.tools.security;

import junit.framework.TestCase;
import org.voovan.tools.TByte;
import org.voovan.tools.TEnv;
import org.voovan.tools.UniqueId;
import org.voovan.tools.security.THash;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class THashUnit extends TestCase {

    public void testBench() {
        UniqueId uniqueId = new UniqueId();
        for(int x=0;x<3;x++) {
            TEnv.measure("test-fnv="+x, () -> {
                for (int i = 0; i < 500000; i++) {
                    THash.HashFNV1(TByte.getBytes(uniqueId.nextNumber()), 0, 8);
                }
            });

            TEnv.sleep(100);

            TEnv.measure("test-32="+x, () -> {
                for (int i = 0; i < 500000; i++) {
                    THash.murmurHash2_32(TByte.getBytes(uniqueId.nextNumber()), 0, 8, 0);
                }
            });

            TEnv.measure("test-32="+x, () -> {
                for (int i = 0; i < 500000; i++) {
                    THash.murmurHash2_64(TByte.getBytes(uniqueId.nextNumber()), 0, 8, 0);
                }
            });

            TEnv.sleep(100);

            TEnv.measure("test-32="+x, () -> {
                for (int i = 0; i < 500000; i++) {
                    THash.murmurHash3_32(TByte.getBytes(uniqueId.nextNumber()), 0, 8, 0);
                }
            });

            TEnv.sleep(100);


            TEnv.measure("test-64="+x, () -> {
                for (int i = 0; i < 500000; i++) {
                    THash.murmurHash3_128(TByte.getBytes(uniqueId.nextNumber()), 0, 8, 0);
                }
            });

            TEnv.sleep(100);

            System.out.println("======================================================================");
        }
    }
}
