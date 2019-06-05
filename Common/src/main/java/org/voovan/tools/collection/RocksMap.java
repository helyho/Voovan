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
    static {
        RocksDB.loadLibrary();
    }


    private static byte[] DATA_BYTES = "data".getBytes();
    //缓存 db 和他对应的 TransactionDB
    private static Map<String, RocksDB> ROCKSDB_MAP = new ConcurrentHashMap<String, RocksDB>();

    //缓存 TransactionDB 和列族句柄的关系
    private static Map<RocksDB, List<ColumnFamilyHandle>> CF_HANDLE_MAP = new ConcurrentHashMap<RocksDB, List<ColumnFamilyHandle>>();

    //默认列族定义
    private static ColumnFamilyDescriptor DEFAULE_CF_DESCRIPTOR = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY);

    //数据文件的默认保存路径
    private static String DEFAULT_DB_PATH = ".rocksdb/";

    public static String getDefaultDbPath() {
        return DEFAULT_DB_PATH;
    }

    public static void setDefaultDbPath(String defaultDbPath) {
        DEFAULT_DB_PATH = defaultDbPath;
    }

    private static ColumnFamilyHandle getColumnFamilyHandler(RocksDB rocksDB, String cfName) throws RocksDBException {
        for(ColumnFamilyHandle columnFamilyHandle : CF_HANDLE_MAP.get(rocksDB)){
            if(Arrays.equals(columnFamilyHandle.getName(), cfName.getBytes())){
                return columnFamilyHandle;
            }
        }

        return null;
    }


    //--------------------- 成员变量 --------------------
    public DBOptions dbOptions;
    public ReadOptions readOptions;
    public WriteOptions writeOptions;

    private RocksDB rocksDB;
    private ColumnFamilyDescriptor dataColumnFamilyDescriptor;
    private ColumnFamilyHandle dataColumnFamilyHandle;
    private volatile Transaction transaction;

    private String dbname;
    private String cfName;
    private Boolean readOnly;

    /**
     * 构造方法
     * @throws RocksDBException RocksDB 异常
     */
    public RocksMap() throws RocksDBException {
        this(null, null, null, null, null, null);
    }

    /**
     * 构造方法
     * @param cfName 列族名称
     * @throws RocksDBException RocksDB 异常
     */
    public RocksMap(String cfName) throws RocksDBException {
        this(null, cfName, null, null, null, null);
    }

    /**
     * 构造方法
     * @param dbname 数据库的名称, 基于数据保存目录的相对路径
     * @param cfName 列族名称
     * @throws RocksDBException RocksDB 异常
     */
    public RocksMap(String dbname, String cfName) throws RocksDBException {
        this(dbname, cfName, null, null, null, null);
    }

    /**
     * 构造方法
     * @param readOnly 是否以只读模式打开
     * @throws RocksDBException RocksDB 异常
     */
    public RocksMap(boolean readOnly) throws RocksDBException {
        this(null, null, null, null, null, readOnly);
    }

    /**
     * 构造方法
     * @param cfName 列族名称
     * @param readOnly 是否以只读模式打开
     * @throws RocksDBException RocksDB 异常
     */
    public RocksMap(String cfName, boolean readOnly) throws RocksDBException {
        this(null, cfName, null, null, null, readOnly);
    }

    /**
     * 构造方法
     * @param dbname 数据库的名称, 基于数据保存目录的相对路径
     * @param cfName 列族名称
     * @param readOnly 是否以只读模式打开
     * @throws RocksDBException RocksDB 异常
     */
    public RocksMap(String dbname, String cfName,boolean readOnly) throws RocksDBException {
        this(dbname, cfName, null, null, null, readOnly);
    }


    /**
     * 构造方法
     * @param dbname 数据库的名称, 基于数据保存目录的相对路径
     * @param cfName 列族名称
     * @param dbOptions DBOptions 配置对象
     * @param readOptions ReadOptions 配置对象
     * @param writeOptions WriteOptions 配置对象
     * @param readOnly 是否以只读模式打开
     * @throws RocksDBException RocksDB 异常
     */
    public RocksMap(String dbname, String cfName, DBOptions dbOptions, ReadOptions readOptions, WriteOptions writeOptions, Boolean readOnly) throws RocksDBException {
        this.dbname = dbname == null ? "voovan_default" : dbname;
        this.cfName = cfName == null ? "voovan_default" : cfName;
        this.readOptions = readOptions == null ? new ReadOptions() : readOptions;
        this.writeOptions = writeOptions == null ? new WriteOptions() : writeOptions;
        this.readOnly = readOnly == null ? false : readOnly;

        Options options = new Options();
        options.setCreateIfMissing(true);
        options.setCreateMissingColumnFamilies(true);

        if(dbOptions == null) {
            //Rocksdb 数据库配置
            dbOptions = new DBOptions(options);
        } else {
            this.dbOptions = dbOptions;
        }

        rocksDB = ROCKSDB_MAP.get(this.dbname);

        if(rocksDB == null || readOnly) {
            //默认列族列表
            List<ColumnFamilyDescriptor> DEFAULT_CF_DESCRIPTOR_LIST = new ArrayList<ColumnFamilyDescriptor>();

            //加载已经存在的所有列族
            {
                List<byte[]> columnFamilyNameBytes = RocksDB.listColumnFamilies(new Options(), DEFAULT_DB_PATH + this.dbname + "/");
                if (columnFamilyNameBytes.size() > 0) {
                    for (byte[] columnFamilyNameByte : columnFamilyNameBytes) {
                        ColumnFamilyDescriptor columnFamilyDescriptor = new ColumnFamilyDescriptor(columnFamilyNameByte);
                        if (Arrays.equals(this.cfName.getBytes(), columnFamilyNameByte)) {
                            dataColumnFamilyDescriptor = columnFamilyDescriptor;
                        }

                        DEFAULT_CF_DESCRIPTOR_LIST.add(columnFamilyDescriptor);
                    }
                }

                //如果为空创建默认列族
                if (DEFAULT_CF_DESCRIPTOR_LIST.size() == 0) {
                    DEFAULT_CF_DESCRIPTOR_LIST.add(DEFAULE_CF_DESCRIPTOR);
                }
            }
            //用来接收ColumnFamilyHandle
            List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<ColumnFamilyHandle>();

            TFile.mkdir(DEFAULT_DB_PATH + this.dbname + "/");

            //打开 Rocksdb
            if(this.readOnly) {
                rocksDB = TransactionDB.openReadOnly(dbOptions, DEFAULT_DB_PATH + this.dbname + "/", DEFAULT_CF_DESCRIPTOR_LIST, columnFamilyHandleList);
            } else {
                rocksDB = TransactionDB.open(dbOptions, new TransactionDBOptions(), DEFAULT_DB_PATH + this.dbname + "/", DEFAULT_CF_DESCRIPTOR_LIST, columnFamilyHandleList);
                ROCKSDB_MAP.put(this.dbname, rocksDB);
            }

            CF_HANDLE_MAP.put(rocksDB, columnFamilyHandleList);
        }

        //设置列族
        dataColumnFamilyHandle = getColumnFamilyHandler(rocksDB, this.cfName);

        //如果没有则创建一个列族
        if (dataColumnFamilyHandle == null) {
            dataColumnFamilyDescriptor = new ColumnFamilyDescriptor(this.cfName.getBytes());
            dataColumnFamilyHandle = rocksDB.createColumnFamily(dataColumnFamilyDescriptor);
            CF_HANDLE_MAP.get(rocksDB).add(dataColumnFamilyHandle);
        } else {
            dataColumnFamilyDescriptor = new ColumnFamilyDescriptor(dataColumnFamilyHandle.getName());
        }

    }

    /**
     * 开启事务
     */
    public void beginTransaction() {
       beginTransaction(-1, false, false);
    }

    /**
     * 开启事务
     * 事务都是读事务，无论操作的记录间是否有交集，都不会锁定。
     * 事务包含读、写事务：
     * 所有的读事务不会锁定，读到的数据取决于snapshot设置。
     * 写事务之间如果不存在记录交集，不会锁定。
     * 写事务之间如果存在记录交集，此时如果未设置snapshot，则交集部分的记录是可以串行提交的。如果设置了snapshot，则第一个写事务(写锁队列的head)会成功，其他写事务会失败(之前的事务修改了该记录的情况下)。
     * @param expire 超时时间
     * @param deadlockDetect 死锁检测是否打开
     * @param withSnapShot 是否启用快照事务
     */
    public void beginTransaction(long expire, boolean deadlockDetect, boolean withSnapShot) {
        if(readOnly){
            Logger.error(new RocksDBException("Not supported operation in read only mode"));
            return;
        }

        TransactionOptions transactionOptions = new TransactionOptions();

        //事务超时时间
        transactionOptions.setExpiration(expire);

        //是否执行死锁检测
        transactionOptions.setDeadlockDetect(deadlockDetect);

        //是否启用快照事务模式
        transactionOptions.setSetSnapshot(withSnapShot);

        if(transaction==null) {
            this.transaction = ((TransactionDB) rocksDB).beginTransaction(writeOptions, transactionOptions);
        } else {
            throw new UnsupportedOperationException("RocksDB is readonly or already in transaction model");
        }
    }

    public Snapshot getSnapShot(){
        return rocksDB.getSnapshot();
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
        if(readOnly){
            Logger.error("Clear failed, ", new RocksDBException("Not supported operation in read only mode"));
            return;
        }

        try {
            drop();
            dataColumnFamilyHandle = rocksDB.createColumnFamily(dataColumnFamilyDescriptor);
            CF_HANDLE_MAP.get(rocksDB).add(dataColumnFamilyHandle);

            //设置列族
            dataColumnFamilyHandle = getColumnFamilyHandler(rocksDB, this.cfName);
        } catch (RocksDBException e) {
            Logger.error("clear failed", e);
        }
    }

    /**
     * 删除这个列族
     */
    public void drop(){
        try {
            rocksDB.dropColumnFamily(dataColumnFamilyHandle);
            CF_HANDLE_MAP.get(rocksDB).remove(dataColumnFamilyHandle);
        } catch (RocksDBException e) {
            Logger.error("drop failed", e);
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
        if(transaction!=null){
            try {
                transaction.rollback();
            } catch (RocksDBException e) {
                throw new IOException(e);
            }
        }
        dataColumnFamilyHandle.close();
    }
}
