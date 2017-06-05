package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class TByteBufferUnit extends TestCase {

    private ByteBuffer b ;

    public void setUp() throws IOException {
        b = ByteBuffer.allocate(10);
        b.put("helyho".getBytes());
    }

    public void testToArray(){
        int limit = b.limit();
        assertEquals(TByteBuffer.toArray(b).length, limit);
    }

    public void testToString(){
        assertEquals(TByteBuffer.toString(b).trim(), "helyho");
    }

    public void testReallocate(){
        TByteBuffer.reallocate(b, 20);
        Logger.simple(b+"\r\n"+TByteBuffer.toString(b));
        assertEquals(20, b.capacity());
        assertEquals("helyho", TByteBuffer.toString(b).trim());
    }

    public void testMoveData(){
        if(b.capacity()!=20) {
            TByteBuffer.reallocate(b, 20);
        }
        b.position(2);
        TByteBuffer.moveData(b,-2);
        Logger.simple(b+"\r\n"+TByteBuffer.toString(b));
        assertEquals(20, b.capacity());
        assertEquals("lyho", TByteBuffer.toString(b).trim());

        TByteBuffer.moveData(b,2);
        Logger.simple(b+"\r\n"+TByteBuffer.toString(b));
        assertEquals(20, b.capacity());
        assertEquals("lylyho", TByteBuffer.toString(b).trim());

    }
}
