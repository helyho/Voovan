package org.voovan.test.tools.collection;

import org.rocksdb.Comparator;
import org.rocksdb.ComparatorOptions;
import org.rocksdb.Slice;
import org.voovan.tools.serialize.TSerialize;

import java.nio.ByteBuffer;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RocksComparatorTest extends Comparator {
    public RocksComparatorTest(ComparatorOptions copt) {
        super(copt);
    }

    @Override
    public String name() {
        return "MyComparator";
    }

    @Override
    public int compare(Slice a, Slice b) {
        return (int) ((Long)TSerialize.unserialize(a.data()) - (Long)TSerialize.unserialize(b.data()));
    }

    private static int compare(final byte[] a, final byte[] b) {
        return ByteBuffer.wrap(a).compareTo(ByteBuffer.wrap(b));
    }
}
