package org.voovan.tools.collection;
import org.rocksdb.*;
import org.voovan.tools.TByte;
import org.voovan.tools.TFile;
import org.voovan.tools.exception.RocksMapException;
import org.voovan.tools.log.Logger;
import org.voovan.tools.serialize.TSerialize;

import java.io.Closeable;
import java.io.File;
import java.util.*;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
    private static String DEFAULT_DB_PATH = ".rocksdb"+ File.separator;
    private static String DEFAULT_WAL_PATH = DEFAULT_DB_PATH + ".wal"+ File.separator;

    public static String getDefaultDbPath() {
        return DEFAULT_DB_PATH;
    }

    public static void setDefaultDbPath(String defaultDbPath) {
        DEFAULT_DB_PATH = defaultDbPath.endsWith(File.separator) ? defaultDbPath : defaultDbPath + File.separator;
    }

    public static String getDefaultWalPath() {
        return DEFAULT_WAL_PATH;
    }

    public static void setDefaultWalPath(String defaultWalPath) {
        DEFAULT_WAL_PATH = defaultWalPath.endsWith(File.separator) ? defaultWalPath : defaultWalPath + File.separator;;
    }

    private static ColumnFamilyHandle getColumnFamilyHandler(RocksDB rocksDB, String cfName) {
        try {
            for (ColumnFamilyHandle columnFamilyHandle : CF_HANDLE_MAP.get(rocksDB)) {
                if (Arrays.equals(columnFamilyHandle.getName(), cfName.getBytes())) {
                    return columnFamilyHandle;
                }
            }

            return null;
        } catch (RocksDBException e){
            throw new RocksMapException("getColumnFamilyHandler failed", e);
        }
    }


    //--------------------- 成员变量 --------------------
    public DBOptions dbOptions;
    public ReadOptions readOptions;
    public WriteOptions writeOptions;

    private RocksDB rocksDB;
    private ColumnFamilyDescriptor dataColumnFamilyDescriptor;
    private ColumnFamilyHandle dataColumnFamilyHandle;
    private ThreadLocal<Transaction> threadLocalTransaction = new ThreadLocal<Transaction>();

    private String dbname;
    private String cfName;
    private Boolean readOnly;
    private int transactionLockTimeout = 5000;

    /**
     * 构造方法
     */
    public RocksMap() {
        this(null, null, null, null, null, null);
    }

    /**
     * 构造方法
     * @param cfName 列族名称
     */
    public RocksMap(String cfName) {
        this(null, cfName, null, null, null, null);
    }

    /**
     * 构造方法
     * @param dbname 数据库的名称, 基于数据保存目录的相对路径
     * @param cfName 列族名称
     */
    public RocksMap(String dbname, String cfName) {
        this(dbname, cfName, null, null, null, null);
    }

    /**
     * 构造方法
     * @param readOnly 是否以只读模式打开
     */
    public RocksMap(boolean readOnly) {
        this(null, null, null, null, null, readOnly);
    }

    /**
     * 构造方法
     * @param cfName 列族名称
     * @param readOnly 是否以只读模式打开
     */
    public RocksMap(String cfName, boolean readOnly) {
        this(null, cfName, null, null, null, readOnly);
    }

    /**
     * 构造方法
     * @param dbname 数据库的名称, 基于数据保存目录的相对路径
     * @param cfName 列族名称
     * @param readOnly 是否以只读模式打开
     */
    public RocksMap(String dbname, String cfName,boolean readOnly) {
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
     */
    public RocksMap(String dbname, String cfName, DBOptions dbOptions, ReadOptions readOptions, WriteOptions writeOptions, Boolean readOnly) {
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
            this.dbOptions = new DBOptions(options);
        } else {
            this.dbOptions = dbOptions;
        }

        this.dbOptions.setWalDir(DEFAULT_WAL_PATH +this.dbname);


        TFile.mkdir(DEFAULT_DB_PATH + this.dbname + "/");
        TFile.mkdir(this.dbOptions.walDir());

        rocksDB = ROCKSDB_MAP.get(this.dbname);

        try {
            if (rocksDB == null || this.readOnly) {
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

                //打开 Rocksdb
                if (this.readOnly) {
                    rocksDB = TransactionDB.openReadOnly(this.dbOptions, DEFAULT_DB_PATH + this.dbname + "/", DEFAULT_CF_DESCRIPTOR_LIST, columnFamilyHandleList);
                } else {
                    rocksDB = TransactionDB.open(this.dbOptions, new TransactionDBOptions(), DEFAULT_DB_PATH + this.dbname + "/", DEFAULT_CF_DESCRIPTOR_LIST, columnFamilyHandleList);
                    ROCKSDB_MAP.put(this.dbname, rocksDB);
                }

                CF_HANDLE_MAP.put(rocksDB, columnFamilyHandleList);
            }

            choseColumnFamily(this.cfName);

        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap initilize failed", e);
        }
    }

    private RocksMap(RocksMap<K,V> rocksMap, String cfName){
        this.dbOptions = rocksMap.dbOptions;
        this.readOptions = rocksMap.readOptions;
        this.writeOptions = rocksMap.writeOptions;

        this.rocksDB = rocksMap.rocksDB;
        this.dataColumnFamilyDescriptor = rocksMap.dataColumnFamilyDescriptor;
        this.dataColumnFamilyHandle = rocksMap.dataColumnFamilyHandle;
        this.threadLocalTransaction = rocksMap.threadLocalTransaction;

        this.dbname =  rocksMap.dbname;
        this.cfName = cfName;
        this.readOnly = rocksMap.readOnly;
        this.transactionLockTimeout = rocksMap.transactionLockTimeout;

        this.choseColumnFamily(cfName);
    }

    /**
     * 创建一个列族不同,但事务共享的 RocksMap
     * @param cfName 列族名称
     * @return 事务共享的 RocksMap
     */
    public RocksMap<K,V> share(String cfName){
        return new RocksMap<K, V>(this, cfName);
    }

    /**
     * 在多个 Columnt 之间切换
     * @param cfName columnFamily 的名称
     * @return RocksMap 对象
     */
    public RocksMap<K, V> choseColumnFamily(String cfName) {
        try {
            //设置列族
            dataColumnFamilyHandle = getColumnFamilyHandler(rocksDB, cfName);

            //如果没有则创建一个列族
            if (dataColumnFamilyHandle == null) {
                dataColumnFamilyDescriptor = new ColumnFamilyDescriptor(cfName.getBytes());
                dataColumnFamilyHandle = rocksDB.createColumnFamily(dataColumnFamilyDescriptor);
                CF_HANDLE_MAP.get(rocksDB).add(dataColumnFamilyHandle);
            } else {
                dataColumnFamilyDescriptor = new ColumnFamilyDescriptor(dataColumnFamilyHandle.getName());
            }

            this.cfName = cfName;

            return this;
        } catch(RocksDBException e){
            throw new RocksMapException("RocksMap initilize failed", e);
        }
    }

    public int getTransactionLockTimeout() {
        return transactionLockTimeout;
    }

    public void setTransactionLockTimeout(int transactionLockTimeout) {
        this.transactionLockTimeout = transactionLockTimeout;
    }

    /**
     * 同步锁, 开启式事务模式
     * @param transFunction 事务业务对象
     * @return true: 事务成功, false: 事务失败
     */
    public boolean withTransaction(Function<RocksMap, Boolean> transFunction){
        Transaction innerTransaction = createTransaction(-1, false, false);

        try {
            if (transFunction.apply(this)) {
                commit(innerTransaction);
                return true;
            } else {
                rollback(innerTransaction);
                return false;
            }
        } catch (Exception e) {
            rollback(innerTransaction);
            throw new RocksMapException("withTransaction failed", e);
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
        Transaction transaction = createTransaction(expire, deadlockDetect, withSnapShot);
        threadLocalTransaction.set(transaction);
    }

    private Transaction createTransaction(long expire, boolean deadlockDetect, boolean withSnapShot) {
        if(readOnly){
            throw new RocksMapException("RocksMap Not supported operation in read only mode");
        }
        Transaction transaction = threadLocalTransaction.get();
        if(transaction==null) {
            TransactionOptions transactionOptions = new TransactionOptions();

            //事务超时时间
            transactionOptions.setExpiration(expire);

            //是否执行死锁检测
            transactionOptions.setDeadlockDetect(deadlockDetect);

            //是否启用快照事务模式
            transactionOptions.setSetSnapshot(withSnapShot);

            //设置快照超时时间
            transactionOptions.setLockTimeout(transactionLockTimeout);

            transaction = ((TransactionDB) rocksDB).beginTransaction(writeOptions, transactionOptions);
            return transaction;
        } else {
            throw new RocksMapException("RocksMap is readonly or already in transaction model");
        }
    }

    private Transaction getTransaction(){
        Transaction transaction = threadLocalTransaction.get();
        if(transaction==null){
            throw new RocksMapException("RocksMap is not in transaction model");
        }

        return transaction;
    }

    public void savePoint() {
        Transaction transaction = getTransaction();

        try {
            transaction.setSavePoint();
        } catch (RocksDBException e) {
            throw new RocksMapException("commit failed", e);
        }
    }

    public void rollbackSavePoint(){
        Transaction transaction = getTransaction();

        try {
            transaction.rollbackToSavePoint();
        } catch (RocksDBException e) {
            throw new RocksMapException("commit failed", e);
        }
    }

    /**
     * 事务提交
     */
    public void commit() {
        Transaction transaction = getTransaction();

        commit(transaction);
        threadLocalTransaction.set(null);
    }

    /**
     * 事务提交
     */
    private void commit(Transaction transaction) {
        try {
            if (transaction != null) {
                transaction.commit();
            } else {
                throw new RocksMapException("RocksMap is not in transaction model");
            }
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap commit failed", e);
        }
    }

    /**
     * 事务回滚
     */
    public void rollback() {
        Transaction transaction = getTransaction();

        rollback(transaction);
        threadLocalTransaction.set(null);
    }

    /**
     * 事务回滚
     */
    private void rollback(Transaction transaction) {
        try {
            if (transaction != null) {
                transaction.rollback();
            } else {
                throw new RocksMapException("RocksMap is not in transaction model");
            }
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap rollback failed", e);
        }
    }

    @Override
    public Comparator<? super K> comparator() {
        return null;
    }

    private RocksIterator getIterator(){
        Transaction transaction = threadLocalTransaction.get();
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
            if(toKey==null || !Arrays.equals(toKeyBytes, key)) {
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

        int count = 0;
        RocksIterator iterator = null;

        Transaction transaction = threadLocalTransaction.get();
        if(transaction!=null) {
            iterator = transaction.getIterator(readOptions, dataColumnFamilyHandle);
        } else {
            iterator = rocksDB.newIterator(dataColumnFamilyHandle, readOptions);
        }

        iterator.seekToFirst();

        while(iterator.isValid()){
            iterator.next();
            count++;
        }
        return count;
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
            Transaction transaction = threadLocalTransaction.get();
            if(transaction!=null) {
                values = transaction.get(dataColumnFamilyHandle, readOptions, TSerialize.serialize(key));
            } else {
                values = rocksDB.get(dataColumnFamilyHandle, TSerialize.serialize(key));
            }
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap containsKey " + key + " failed", e);
        }

        return values!=null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取并锁定, 默认独占模式
     * @param key 将被锁定的 key
     * @return key 对应的 value
     */
    public V lock(Object key){
        getTransaction();
        return getForUpdate(key, true);
    }

    /**
     * 获取并锁定
     * @param key 将被锁定的 key
     * @param exclusive 是否独占模式
     * @return key 对应的 value
     */
    public V getForUpdate(Object key, boolean exclusive){
        Transaction transaction = getTransaction();

        try {
            byte[] values = transaction.getForUpdate(readOptions, dataColumnFamilyHandle, TSerialize.serialize(key), exclusive);
            return values==null ? null : (V) TSerialize.unserialize(values);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap getForUpdate " + key + " failed", e);
        }
    }

    private byte[] get(byte[] keyBytes) {
        try {
            byte[] values = null;
            Transaction transaction = threadLocalTransaction.get();
            if (transaction != null) {
                values = transaction.get(dataColumnFamilyHandle, readOptions, keyBytes);
            } else {
                values = rocksDB.get(dataColumnFamilyHandle, readOptions, keyBytes);
            }

            return values;
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap get failed", e);
        }
    }

    @Override
    public V get(Object key) {
        if(key == null){
            throw new NullPointerException();
        }

        byte[] values = get(TSerialize.serialize(key));
        return values==null ? null : (V) TSerialize.unserialize(values);
    }

    private void put(byte[] keyBytes, byte[] valueBytes) {
        try {
            Transaction transaction = threadLocalTransaction.get();
            if (transaction != null) {
                transaction.put(dataColumnFamilyHandle, keyBytes, valueBytes);
            } else {
                rocksDB.put(dataColumnFamilyHandle, keyBytes, valueBytes);
            }
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap commit failed", e);
        }
    }

    @Override
    public Object put(Object key, Object value) {
        if(key == null || value == null){
            throw new NullPointerException();
        }

        put(TSerialize.serialize(key), TSerialize.serialize(value));
        return value;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        byte[] keyBytes = TSerialize.serialize(key);
        byte[] valueBytes = TSerialize.serialize(value);

        Transaction transaction = createTransaction(-1, false, false);

        try {
            byte[] oldValueBytes = transaction.getForUpdate(readOptions, dataColumnFamilyHandle, keyBytes, true);

            if(oldValueBytes == null){
                transaction.setSnapshotOnNextOperation();
                transaction.put(dataColumnFamilyHandle, keyBytes, valueBytes);
                return null;
            } else {
                return (V) TSerialize.unserialize(oldValueBytes);
            }
        } catch (RocksDBException e) {
            rollback(transaction);
            throw new RocksMapException("RocksMap putIfAbsent error", e);
        } finally {
            commit(transaction);
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        byte[] keyBytes = TSerialize.serialize(key);
        byte[] newValueBytes = TSerialize.serialize(newValue);
        byte[] oldValueBytes = TSerialize.serialize(oldValue);

        Transaction transaction = createTransaction(-1, false, false);

        try {
            byte[] oldDbValueBytes = transaction.getForUpdate(readOptions, dataColumnFamilyHandle, keyBytes, true);
            if(oldDbValueBytes!=null && Arrays.equals(oldDbValueBytes, oldValueBytes)){
                transaction.put(dataColumnFamilyHandle, keyBytes, newValueBytes);
                return true;
            } else {
                return false;
            }

        } catch (RocksDBException e) {
            rollback(transaction);
            throw new RocksMapException("RocksMap replace error: ", e);
        } finally {
            commit(transaction);
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
                Transaction transaction = threadLocalTransaction.get();
                if(transaction!=null) {
                    transaction.singleDelete(dataColumnFamilyHandle, TSerialize.serialize(key));
                } else {
                    rocksDB.singleDelete(dataColumnFamilyHandle, TSerialize.serialize(key));
                }
            }
            return (V)value;
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap remove " + key + " failed", e);
        }
    }

    public void removeAll(K[] keys) {
        if(keys == null){
            throw new NullPointerException();
        }

        byte[][] keyParts = new byte[keys.length][];
        for(int i=0;i<keys.length;i++) {
            keyParts[i] = TSerialize.serialize(keys[i]);
        }

        Transaction transaction = createTransaction(-1, false, false);
        try {
            if(keyParts.length != 0) {
                transaction.delete(dataColumnFamilyHandle, keyParts);
            }
        } catch (RocksDBException e) {
            rollback(transaction);
            throw new RocksMapException("RocksMap removeAll failed", e);
        } finally {
            commit(transaction);
        }
    }

    /**
     * Removes the database entries in the range ["beginKey", "endKey"), i.e.,
     * including "beginKey" and excluding "endKey". a non-OK status on error. It
     * is not an error if no keys exist in the range ["beginKey", "endKey").
     * @param fromKey 其实 key
     * @param toKey 结束 key
     *
     */
    public void removeRange(K fromKey, K toKey) {
        try {
            rocksDB.deleteRange(dataColumnFamilyHandle, writeOptions, TSerialize.serialize(fromKey), TSerialize.serialize(toKey));
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap removeAll failed", e);
        }
    }

    public static ThreadLocal<WriteBatch> THREAD_LOCAL_WRITE_BATCH = ThreadLocal.withInitial(()->new WriteBatch());
    @Override
    public void putAll(Map m) {
        try {
            WriteBatch writeBatch = THREAD_LOCAL_WRITE_BATCH.get();
            Iterator<Entry> iterator = m.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry entry = iterator.next();
                Object key = entry.getKey();
                Object value = entry.getValue();

                writeBatch.put(dataColumnFamilyHandle, TSerialize.serialize(key), TSerialize.serialize(value));
            }

            rocksDB.write(writeOptions, writeBatch);
            writeBatch.clear();
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap putAll failed", e);
        }
    }

    /**
     * 刷新数据到文件
     */
    public void flush(){
        try {
            rocksDB.compactRange(dataColumnFamilyHandle);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap flush failed", e);
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
            throw new RocksMapException("RocksMap clear failed", e);
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
            throw new RocksMapException("RocksMap drop failed", e);
        }
    }

    /**
     * 数据拷贝到内存中, 所以对这个 Set 的修改不会在 Rocksdb 中生效
     * @return 保存了 Key 的 set
     */
    @Override
    public Set keySet() {
        TreeSet<K> keySet = new TreeSet<K>();
        RocksIterator iterator = null;

        Transaction transaction = threadLocalTransaction.get();
        if(transaction!=null) {
            iterator = transaction.getIterator(readOptions, dataColumnFamilyHandle);
        } else {
            iterator = rocksDB.newIterator(dataColumnFamilyHandle, readOptions);
        }
        iterator.seekToFirst();
        while(iterator.isValid()){
            K k = (K) TSerialize.unserialize(iterator.key());
            keySet.add(k);
            iterator.next();
        }

        return keySet;
    }

    @Override
    public Collection values() {
        ArrayList<V> values = new ArrayList<V>();
        RocksIterator iterator = null;

        Transaction transaction = threadLocalTransaction.get();
        if(transaction!=null) {
            iterator = transaction.getIterator(readOptions, dataColumnFamilyHandle);
        } else {
            iterator = rocksDB.newIterator(dataColumnFamilyHandle, readOptions);
        }
        iterator.seekToFirst();
        while(iterator.isValid()){
            V value = (V) TSerialize.unserialize(iterator.value());
            values.add(value);
            iterator.next();
        }

        return values;
    }

    /**
     * 数据拷贝到内存中, 所以对这个 Set 的修改不会在 Rocksdb 中生效
     * @return 保存了 Entry 的 set
     */
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

    /**
     * 按前缀查找关联的 key
     * @param key key 的前缀
     * @return 找到的 Map 数据
     */
    public Map<K,V> startWith(K key) {
        byte[] keyBytes = TSerialize.serialize(key);
        TreeMap<K,V> entryMap =  new TreeMap<K,V>();

        RocksIterator iterator = getIterator();
        iterator.seek(keyBytes);
        while(iterator.isValid()){
            byte[] iteratorkeyBytes = iterator.key();
            if(TByte.byteArrayStartWith(iteratorkeyBytes, keyBytes)) {
                entryMap.put((K) TSerialize.unserialize(iteratorkeyBytes), (V) TSerialize.unserialize(iterator.value()));
                iterator.next();
            } else {
                break;
            }
        }

        return entryMap;
    }

    @Override
    public void close() {
        Transaction transaction = threadLocalTransaction.get();
        if(transaction!=null){
            try {
                transaction.rollback();
            } catch (RocksDBException e) {
                throw new RocksMapException("RocksMap rollback on close failed", e);
            }
        }
        dataColumnFamilyHandle.close();
    }
}
