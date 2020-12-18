package org.voovan.test.tools.collection;

import org.rocksdb.AbstractWalFilter;
import org.rocksdb.RocksDBException;
import org.rocksdb.WalProcessingOption;
import org.rocksdb.WriteBatch;

import java.util.Map;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RocksDBWalFilter extends AbstractWalFilter {
    @Override
    public void columnFamilyLogNumberMap(Map<Integer, Long> map, Map<String, Integer> map1) {
        System.out.println(111);
    }

    @Override
    public LogRecordFoundResult logRecordFound(long l, String s, WriteBatch writeBatch, WriteBatch writeBatch1) {
        try {
            writeBatch1.putLogData( writeBatch.data());
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return new LogRecordFoundResult(WalProcessingOption.CONTINUE_PROCESSING, true);
    }

    @Override
    public String name() {
        return "RocksDBWalFilter";
    }
}
