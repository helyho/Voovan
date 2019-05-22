package org.voovan.tools.collection;
import org.rocksdb.*;
import org.voovan.tools.TByte;
import org.voovan.tools.TFile;
import org.voovan.tools.log.Logger;
import org.voovan.tools.serialize.TSerialize;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RocksDB 的 Map 封装
 *
 * @author: helyho
 * ignite-test Framework.
 * WebSite: https://github.com/helyho/ignite-test
 * Licence: Apache v2 License
 */
public class RocksMap<K, V> implements SortedMap<K, V>, Closeable {
    //--------------------- 公共静态变量 --------------------
    private static byte[] DATA_BYTES = "data".getBytes();
    //缓存 db 和他对应的 TransactionDB
    private static Map<String, TransactionDB> ROCKSDB_MAP = new ConcurrentHashMap<String, TransactionDB>();

    //缓存 TransactionDB 和列族句柄的关系
    private static Map<TransactionDB, List<ColumnFamilyHandle>> CF_HANDLE_MAP = new ConcurrentHashMap<TransactionDB, List<ColumnFamilyHandle>>();

    //数据列族定义
    private static ColumnFamilyDescriptor DEFAULE_CF_DESCRIPTOR = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY);
    private static ColumnFamilyDescriptor DATA_CF_DESCRIPTOR = new ColumnFamilyDescriptor(DATA_BYTES);

    //默认列族
    private static List<ColumnFamilyDescriptor> DEFAULT_CF_DESCRIPTOR_LIST = Arrays.asList(
            DEFAULE_CF_DESCRIPTOR,
            DATA_CF_DESCRIPTOR
                    );

    //数据文件的默认保存路径
    private static String DEFAULT_DB_PATH = ".rocksdb/";

    public static String getDefaultDbPath() {
        return DEFAULT_DB_PATH;
    }

    public static void setDefaultDbPath(String defaultDbPath) {
        DEFAULT_DB_PATH = defaultDbPath;
    }

    static {
        RocksDB.loadLibrary();
    }

    //--------------------- 成员变量 --------------------
    public DBOptions dbOptions;
    public ReadOptions readOptions;
    public WriteOptions writeOptions;

    private TransactionDB rocksDB;
    private ColumnFamilyHandle dataColumnFamilyHandle;
    private volatile Transaction transaction;

    private String dbname;
    /**
     * 构造方法
     * @throws RocksDBException RocksDB 异常
     */
    public RocksMap() throws RocksDBException {
        this(null, null, null, null);
    }

    /**
     * 构造方法
     * @param dbname 数据库的名称, 基于数据保存目录的相对路径
     * @throws RocksDBException RocksDB 异常
     */
    public RocksMap(String dbname) throws RocksDBException {
        this(dbname, null, null, null);
    }

    /**
     * 构造方法
     * @param dbname 数据库的名称, 基于数据保存目录的相对路径
     * @param dbOptions DBOptions 配置对象
     * @param readOptions ReadOptions 配置对象
     * @param writeOptions WriteOptions 配置对象
     * @throws RocksDBException RocksDB 异常
     */
    public RocksMap(String dbname, DBOptions dbOptions, ReadOptions readOptions, WriteOptions writeOptions) throws RocksDBException {
        this.dbname = dbname == null ? "default" : dbname;
        this.readOptions = readOptions == null ? new ReadOptions() : readOptions;
        this.writeOptions = writeOptions == null ? new WriteOptions() : writeOptions;

        if(dbOptions == null) {
            //Rocksdb 数据库配置
            dbOptions = new DBOptions();
            dbOptions.setCreateIfMissing(true);
            dbOptions.setCreateMissingColumnFamilies(true);
        } else {
            this.dbOptions = dbOptions;
        }

        rocksDB = ROCKSDB_MAP.get(this.dbname);

        if(rocksDB == null) {
            //用来接收ColumnFamilyHandle
            List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<ColumnFamilyHandle>();

            TFile.mkdir(DEFAULT_DB_PATH + dbname + "/");

            //打开 Rocksdb
            rocksDB = TransactionDB.open(dbOptions, new TransactionDBOptions(), DEFAULT_DB_PATH + this.dbname + "/", DEFAULT_CF_DESCRIPTOR_LIST, columnFamilyHandleList);

            CF_HANDLE_MAP.put(rocksDB, columnFamilyHandleList);
        }

        //设置列族
        dataColumnFamilyHandle = CF_HANDLE_MAP.get(rocksDB).get(1);
    }

    /**
     * 开启事务
     */
    public void beginTransaction() {
        if(transaction==null) {
            this.transaction = rocksDB.beginTransaction(writeOptions);
        } else {
            throw new UnsupportedOperationException("RocksDB is in transaction model, please finish this transaction first");
        }
    }

    /**
     * 事务提交
     * @throws RocksDBException RocksDB 异常
     */
    public void commit() throws RocksDBException {
        if(transaction!=null) {
            this.transaction.commit();
            this.transaction = null;
        } else {
            throw new UnsupportedOperationException("RocksDB is not in transaction model");
        }
    }

    /**
     * 事务回滚
     * @throws RocksDBException RocksDB 异常
     */
    public void rollback() throws RocksDBException {
        if(transaction!=null) {
            this.transaction.rollback();
            this.transaction = null;
        } else {
            throw new UnsupportedOperationException("RocksDB is not in transaction model");
        }
    }

    @Override
    public Comparator<? super K> comparator() {
        return null;
    }

    private RocksIterator getIterator(){
        if(transaction!=null) {
            return transaction.getIterator(readOptions, dataColumnFamilyHandle);
        } else {
            return rocksDB.newIterator(dataColumnFamilyHandle, readOptions);
        }
    }

    @Override
    public SortedMap<K,V> subMap(K fromKey, K toKey) {
        TreeMap<K,V> subMap =  new TreeMap<K,V>();
        RocksIterator iterator = getIterator();


        byte[] fromKeyBytes = TSerialize.serialize(fromKey);
        byte[] toKeyBytes = TSerialize.serialize(toKey);

        if(fromKeyBytes == null){
            iterator.seekToFirst();
        } else {
            iterator.seek(fromKeyBytes);
        }

        while(iterator.isValid()) {
            byte[] key = iterator.key();
            if(toKey==null || TByte.byteArrayCompare(toKeyBytes, key) >= 0) {
                subMap.put((K) TSerialize.unserialize(iterator.key()), (V)TSerialize.unserialize(iterator.value()));
            } else {
                break;
            }
            iterator.next();
        }

        return subMap;
    }

    @Override
    public SortedMap<K,V> tailMap(K fromKey){
        if(fromKey==null){
            return null;
        }
        return subMap(fromKey, null);
    }

    @Override
    public SortedMap<K,V> headMap(K toKey){
        if(toKey == null){
            return null;
        }
        return subMap(null, toKey);
    }

    @Override
    public K firstKey() {
        RocksIterator iterator = getIterator();

        iterator.seekToFirst();
        if(iterator.isValid()){
            return (K) TSerialize.unserialize(iterator.key());
        }

        return null;
    }

    @Override
    public K lastKey() {
        RocksIterator iterator = getIterator();

        iterator.seekToLast();
        if(iterator.isValid()){
            return (K) TSerialize.unserialize(iterator.key());
        }

        return null;
    }


    @Override
    /**
     * 遍历所有数据来获取 kv 记录的数量, 会消耗很多性能
     */
    public int size() {
//        try {
//            return Integer.valueOf(rocksDB.getProperty(dataColumnFamilyHandle, "rocksdb.estimate-num-keys"));
//        } catch (RocksDBException e) {
//            e.printStackTrace();
//        }

        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        RocksIterator iterator = getIterator();
        try {
            iterator.seekToFirst();
            return !iterator.isValid();
        } finally {
            if(iterator!=null) {
                iterator.close();
            }
        }
    }

    @Override
    public boolean containsKey(Object key) {
        byte[] values = null;
        try {
            if(transaction!=null) {
                values = transaction.get(dataColumnFamilyHandle, readOptions, TSerialize.serialize(key));
            } else {
                values = rocksDB.get(dataColumnFamilyHandle, TSerialize.serialize(key));
            }
        } catch (RocksDBException e) {
            Logger.error("containsKey " + key + " failed", e);
            return false;
        }

        return values!=null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V get(Object key) {
        if(key == null){
            throw new NullPointerException();
        }

        try {
            byte[] values = null;
            if(transaction!=null) {
                values = transaction.get(dataColumnFamilyHandle, readOptions, TSerialize.serialize(key));
            } else {
                values = rocksDB.get(dataColumnFamilyHandle, readOptions, TSerialize.serialize(key));
            }
            return values==null ? null : (V) TSerialize.unserialize(values);
        } catch (RocksDBException e) {
            Logger.error("Get " + key + " failed", e);
            return null;
        }
    }

    @Override
    public Object put(Object key, Object value) {
        if(key == null || value == null){
            throw new NullPointerException();
        }

        try {
            if (transaction != null) {
                transaction.put(dataColumnFamilyHandle, TSerialize.serialize(key), TSerialize.serialize(value));
            } else {
                rocksDB.put(dataColumnFamilyHandle, TSerialize.serialize(key), TSerialize.serialize(value));
            }
            return value;
        } catch (RocksDBException e) {
            Logger.error("Put " + key + " failed", e);
            return null;
        }
    }

    @Override
    public V remove(Object key) {
        if(key == null){
            throw new NullPointerException();
        }

        try {

            V value = get(key);

            if(value != null) {
                if(transaction!=null) {
                    transaction.singleDelete(dataColumnFamilyHandle, TSerialize.serialize(key));
                } else {
                    rocksDB.singleDelete(dataColumnFamilyHandle, TSerialize.serialize(key));
                }
//                sizeChange(-1);
            }
            return value;
        } catch (RocksDBException e) {
            Logger.error("remove " + key + " failed", e);
            return null;
        }
    }

    @Override
    public void putAll(Map m) {
        try {
            WriteBatch writeBatch = new WriteBatch();
            Iterator<Entry> iterator = m.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry entry = iterator.next();
                Object key = entry.getKey();
                Object value = entry.getValue();

                writeBatch.put(dataColumnFamilyHandle, TSerialize.serialize(key), TSerialize.serialize(value));
            }

            rocksDB.write(writeOptions, writeBatch);
        } catch (RocksDBException e) {
            Logger.error("putAll failed", e);
        }
    }

    @Override
    public void clear() {
        try {
            rocksDB.dropColumnFamily(dataColumnFamilyHandle);
            CF_HANDLE_MAP.get(rocksDB).add(1, rocksDB.createColumnFamily(DATA_CF_DESCRIPTOR));
            //设置列族
            dataColumnFamilyHandle = CF_HANDLE_MAP.get(rocksDB).get(1);
        } catch (RocksDBException e) {
            Logger.error("clear failed", e);
        }
    }

    @Override
    public Set keySet() {
        TreeSet<K> keySet = new TreeSet<K>();
        RocksIterator iterator = null;
        if(transaction!=null) {
            iterator = transaction.getIterator(readOptions, dataColumnFamilyHandle);
        } else {
            iterator = rocksDB.newIterator(dataColumnFamilyHandle, readOptions);
        }
        iterator.seekToFirst();
        while(iterator.isValid()){
            keySet.add((K) TSerialize.unserialize(iterator.key()));
            iterator.next();
        }

        return keySet;
    }

    @Override
    public Collection values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        TreeMap<K,V> entryMap =  new TreeMap<K,V>();
        RocksIterator iterator = getIterator();
        iterator.seekToFirst();
        while(iterator.isValid()){
            entryMap.put((K) TSerialize.unserialize(iterator.key()), (V)TSerialize.unserialize(iterator.value()));
            iterator.next();
        }

        return entryMap.entrySet();
    }

    @Override
    public void close() throws IOException {
        ROCKSDB_MAP.remove(rocksDB);
        if(transaction!=null){
            try {
                transaction.rollback();
            } catch (RocksDBException e) {
                throw new IOException(e);
            }
        }
        rocksDB.close();
        rocksDB.close();
    }
}
