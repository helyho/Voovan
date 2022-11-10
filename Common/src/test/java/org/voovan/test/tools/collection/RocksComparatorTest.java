package org.voovan.test.tools.collection;

import org.rocksdb.AbstractComparator;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Slice;
import org.voovan.tools.buffer.TByteBuffer;
import org.voovan.tools.serialize.TSerialize;

import java.nio.ByteBuffer;

/**
 * 类文字命名
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RocksComparatorTest extends AbstractComparator {
    public RocksComparatorTest(ComparatorOptions copt) {
        super(copt);
    }

    @Override
    public String name() {
        return "MyComparator";
    }

    @Override
    public int compare(ByteBuffer a, ByteBuffer b) {
        return (int) ((Long)TSerialize.unserialize(TByteBuffer.toArray(a)) - (Long)TSerialize.unserialize(TByteBuffer.toArray(b)));
    }

    private static int compare(final byte[] a, final byte[] b) {
        return ByteBuffer.wrap(a).compareTo(ByteBuffer.wrap(b));
    }
}
