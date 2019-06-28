package org.voovan.tools.collection;
import org.rocksdb.*;
import org.voovan.tools.TByte;
import org.voovan.tools.TFile;
import org.voovan.tools.Varint;
import org.voovan.tools.exception.ParseException;
import org.voovan.tools.exception.RocksMapException;
import org.voovan.tools.log.Logger;
import org.voovan.tools.serialize.TSerialize;

import java.io.Closeable;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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
    public ColumnFamilyOptions columnFamilyOptions;

    private RocksDB rocksDB;
    private ColumnFamilyDescriptor dataColumnFamilyDescriptor;
    private ColumnFamilyHandle dataColumnFamilyHandle;
    private ThreadLocal<Transaction> threadLocalTransaction = new ThreadLocal<Transaction>();

    private String dbname;
    private String columnFamilyName;
    private Boolean readOnly;
    private int transactionLockTimeout = 5000;
    private int savePointCount = 0;

    /**
     * 构造方法
     */
    public RocksMap() {
        this(null, null, null, null, null, null, null);
    }

    /**
     * 构造方法
     * @param columnFamilyName 列族名称
     */
    public RocksMap(String columnFamilyName) {
        this(null, columnFamilyName, null, null, null, null, null);
    }

    /**
     * 构造方法
     * @param dbname 数据库的名称, 基于数据保存目录的相对路径
     * @param columnFamilyName 列族名称
     */
    public RocksMap(String dbname, String columnFamilyName) {
        this(dbname, columnFamilyName, null, null, null, null, null);
    }

    /**
     * 构造方法
     * @param readOnly 是否以只读模式打开
     */
    public RocksMap(boolean readOnly) {
        this(null, null, null, null, null, null, readOnly);
    }

    /**
     * 构造方法
     * @param columnFamilyName 列族名称
     * @param readOnly 是否以只读模式打开
     */
    public RocksMap(String columnFamilyName, boolean readOnly) {
        this(null, columnFamilyName, null, null, null, null, readOnly);
    }

    /**
     * 构造方法
     * @param dbname 数据库的名称, 基于数据保存目录的相对路径
     * @param columnFamilyName 列族名称
     * @param readOnly 是否以只读模式打开
     */
    public RocksMap(String dbname, String columnFamilyName, boolean readOnly) {
        this(dbname, columnFamilyName, null, null, null, null, readOnly);
    }

    /**
     * 构造方法
     * @param dbname 数据库的名称, 基于数据保存目录的相对路径
     * @param columnFamilyName 列族名称
     * @param dbOptions DBOptions 配置对象
     * @param readOptions ReadOptions 配置对象
     * @param writeOptions WriteOptions 配置对象
     * @param columnFamilyOptions 列族配置对象
     * @param readOnly 是否以只读模式打开
     */
    public RocksMap(String dbname, String columnFamilyName, ColumnFamilyOptions columnFamilyOptions, DBOptions dbOptions, ReadOptions readOptions, WriteOptions writeOptions, Boolean readOnly) {
        this.dbname = dbname == null ? "voovan_default" : dbname;
        this.columnFamilyName = columnFamilyName == null ? "voovan_default" : columnFamilyName;
        this.readOptions = readOptions == null ? new ReadOptions() : readOptions;
        this.writeOptions = writeOptions == null ? new WriteOptions() : writeOptions;
        this.columnFamilyOptions = columnFamilyOptions == null ? new ColumnFamilyOptions() : columnFamilyOptions;
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

        this.dbOptions.useDirectIoForFlushAndCompaction();

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
                            ColumnFamilyDescriptor columnFamilyDescriptor = new ColumnFamilyDescriptor(columnFamilyNameByte, this.columnFamilyOptions);
                            if (Arrays.equals(this.columnFamilyName.getBytes(), columnFamilyNameByte)) {
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

            choseColumnFamily(this.columnFamilyName);

        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap initilize failed", e);
        }
    }

    private RocksMap(RocksMap<K,V> rocksMap, String columnFamilyName, boolean useSameTransaction){
        this.dbOptions = rocksMap.dbOptions;
        this.readOptions = rocksMap.readOptions;
        this.writeOptions = rocksMap.writeOptions;
        this.columnFamilyOptions = rocksMap.columnFamilyOptions;

        this.rocksDB = rocksMap.rocksDB;
        //是否使用父对象的实物对象
        if(useSameTransaction) {
            this.threadLocalTransaction = rocksMap.threadLocalTransaction;
        } else {
            this.threadLocalTransaction = ThreadLocal.withInitial(()->this.createTransaction(-1, false, false));
        }

        this.dbname =  rocksMap.dbname;
        this.columnFamilyName = columnFamilyName;
        this.readOnly = rocksMap.readOnly;
        this.transactionLockTimeout = rocksMap.transactionLockTimeout;
        this.savePointCount = rocksMap.savePointCount;

        this.choseColumnFamily(columnFamilyName);
    }

    /**
     * 获取最后的序号
     * @return 返回最后的日志序号
     */
    public Long getLastSequence() {
        return rocksDB.getLatestSequenceNumber();
    }

    /**
     * 获取某个序号以后的更新操作记录
     * @param sequenceNumber 序号
     * @param withSerial 是否进行序列化行为
     * @return 日志记录集合
     */
    public List<LogRecord> getLogsSince(Long sequenceNumber, boolean withSerial) {
        return getLogsBetween(sequenceNumber, null, null, withSerial);
    }

    /**
     * 获取某个序号以后的更新操作记录
     * @param startSequence 起始序号
     * @param filter 过滤器,用来过滤可用的操作类型和列族
     * @param withSerial 是否进行反序列化
     * @return 日志记录集合
     */
    public List<LogRecord> getLogsSince(Long startSequence, BiFunction<Integer, Integer, Boolean> filter, boolean withSerial) {
        return getLogsBetween(startSequence, null, filter, withSerial);
    }

    /**
     * 获取某个序号以后的更新操作记录
     * @param startSequence 起始序号
     * @param endSequence 结束序号
     * @param withSerial 是否进行反序列化
     * @return 日志记录集合
     */
    public List<LogRecord> getLogsSince(Long startSequence, Long endSequence, boolean withSerial) {
        return getLogsBetween(startSequence, endSequence, null, withSerial);
    }

    /**
     * 获取某个序号以后的更新操作记录
     * @param startSequence 起始序号
     * @param endSequence 结束序号
     * @param filter 过滤器,用来过滤可用的操作类型和列族
     * @param withSerial 是否进行反序列化
     * @return 日志记录集合
     */
    public List<LogRecord> getLogsBetween(Long startSequence, Long endSequence,  BiFunction<Integer, Integer, Boolean> filter, boolean  withSerial) {
        try {
            TransactionLogIterator transactionLogIterator = rocksDB.getUpdatesSince(startSequence);


            ArrayList<LogRecord> logRecords = new ArrayList<LogRecord>();

            while (transactionLogIterator.isValid()) {
                TransactionLogIterator.BatchResult batchResult = transactionLogIterator.getBatch();

                if(endSequence!=null && batchResult.sequenceNumber() > endSequence) {
                    break;
                }

                List<LogRecord> logRecordBySeq = LogRecord.parse(ByteBuffer.wrap(batchResult.writeBatch().data()), filter, withSerial);

                logRecords.addAll(logRecordBySeq);

                transactionLogIterator.next();
            }

            transactionLogIterator.close();

            return logRecords;
        } catch (RocksDBException e) {
            throw new RocksMapException("getUpdatesSince failed", e);
        }
    }

    /**
     * 创建一个列族不同,但事务共享的 RocksMap
     * @param cfName 列族名称
     * @return 事务共享的 RocksMap
     */
    public RocksMap<K,V> share(String cfName){
        return new RocksMap<K, V>(this, cfName, true);
    }

    public RocksDB getRocksDB(){
        return rocksDB;
    }

    public int getColumnFamilyId(String cfName){
        ColumnFamilyHandle columnFamilyHandle = getColumnFamilyHandler(rocksDB, cfName);
        if(columnFamilyHandle!=null){
            return columnFamilyHandle.getID();
        } else {
            throw new RocksMapException("ColumnFamily [" + cfName +"] not found.");
        }
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
                dataColumnFamilyDescriptor = new ColumnFamilyDescriptor(cfName.getBytes(), columnFamilyOptions);
                dataColumnFamilyHandle = rocksDB.createColumnFamily(dataColumnFamilyDescriptor);
                CF_HANDLE_MAP.get(rocksDB).add(dataColumnFamilyHandle);
            } else {
                dataColumnFamilyDescriptor = new ColumnFamilyDescriptor(dataColumnFamilyHandle.getName(), columnFamilyOptions);
            }

            this.columnFamilyName = cfName;

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
    public boolean withTransaction(Function<RocksMap, Boolean> transFunction) {

        RocksMap<K,V> transactionRocksMap = new RocksMap<K, V>(this, columnFamilyName, false);


        //需要和 rocksMap 共享一个 事务
        try {
            transactionRocksMap.beginTransaction();

            if (transFunction.apply(transactionRocksMap)) {
                transactionRocksMap.commit();
                return true;
            } else {
                transactionRocksMap.rollback();
                return false;
            }
        } catch (Exception e) {
            transactionRocksMap.rollback();
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
        Transaction transaction = threadLocalTransaction.get();
        if(transaction==null) {
            transaction = createTransaction(expire, deadlockDetect, withSnapShot);
            threadLocalTransaction.set(transaction);
        } else {
            savePoint();
        }
    }

    private Transaction createTransaction(long expire, boolean deadlockDetect, boolean withSnapShot) {
        if(readOnly){
            throw new RocksMapException("RocksMap Not supported operation in read only mode");
        }

        TransactionOptions transactionOptions = new TransactionOptions();

        //事务超时时间
        transactionOptions.setExpiration(expire);

        //是否执行死锁检测
        transactionOptions.setDeadlockDetect(deadlockDetect);

        //是否启用快照事务模式
        transactionOptions.setSetSnapshot(withSnapShot);

        //设置快照超时时间
        transactionOptions.setLockTimeout(transactionLockTimeout);

        return((TransactionDB) rocksDB).beginTransaction(writeOptions, transactionOptions);
    }

    private Transaction getTransaction(){
        Transaction transaction = threadLocalTransaction.get();
        if(transaction==null){
            throw new RocksMapException("RocksMap is not in transaction model");
        }

        return transaction;
    }

    private void savePoint() {
        Transaction transaction = getTransaction();

        try {
            transaction.setSavePoint();
            savePointCount++;
        } catch (RocksDBException e) {
            throw new RocksMapException("commit failed", e);
        }
    }

    private void rollbackSavePoint(){
        Transaction transaction = getTransaction();

        try {
            transaction.rollbackToSavePoint();
            savePointCount--;
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

        if(savePointCount!=0) {
            rollbackSavePoint();
        } else {
            rollback(transaction);
            threadLocalTransaction.set(null);
        }
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
                subMap.put((K) TSerialize.unserialize(iterator.key()), (V)TSerialize.unserialize(iterator.value()));
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

    public List<V> getAll(Collection<K> keys) {
        try {
            ArrayList<byte[]> keysBytes = new ArrayList<byte[]>();
            ArrayList<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<ColumnFamilyHandle>();
            Iterator keysIterator = keys.iterator();
            for (int i = 0; i < keys.size(); i++) {
                keysBytes.add(TSerialize.serialize(keysIterator.next()));
                columnFamilyHandles.add(dataColumnFamilyHandle);
            }

            Transaction transaction = threadLocalTransaction.get();
            List<byte[]> valuesBytes;
            if (transaction != null) {
                valuesBytes = Arrays.asList(transaction.multiGet(readOptions, columnFamilyHandles, keysBytes.toArray(new byte[0][0])));
            } else {
                valuesBytes = rocksDB.multiGetAsList(readOptions, columnFamilyHandles, keysBytes);
            }

            ArrayList<V> values = new ArrayList<V>();
            for (byte[] valueByte : valuesBytes) {
                values.add((V) TSerialize.unserialize(valueByte));
            }
            return values;
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap getAll failed", e);
        }

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
            throw new RocksMapException("RocksMap put failed", e);
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

        //这里使用独立的事务是未了防止默认事务提交导致失效
        Transaction innerTransaction = createTransaction(-1, false, false);

        try {
            byte[] oldValueBytes = innerTransaction.getForUpdate(readOptions, dataColumnFamilyHandle, keyBytes, true);

            if(oldValueBytes == null){
                innerTransaction.setSnapshotOnNextOperation();
                innerTransaction.put(dataColumnFamilyHandle, keyBytes, valueBytes);
                return null;
            } else {
                return (V) TSerialize.unserialize(oldValueBytes);
            }
        } catch (RocksDBException e) {
            rollback(innerTransaction);
            throw new RocksMapException("RocksMap putIfAbsent error", e);
        } finally {
            commit(innerTransaction);
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        byte[] keyBytes = TSerialize.serialize(key);
        byte[] newValueBytes = TSerialize.serialize(newValue);
        byte[] oldValueBytes = TSerialize.serialize(oldValue);

        //这里使用独立的事务是未了防止默认事务提交导致失效
        Transaction innerTransaction = createTransaction(-1, false, false);

        try {
            byte[] oldDbValueBytes = innerTransaction.getForUpdate(readOptions, dataColumnFamilyHandle, keyBytes, true);
            if(oldDbValueBytes!=null && Arrays.equals(oldDbValueBytes, oldValueBytes)){
                innerTransaction.put(dataColumnFamilyHandle, keyBytes, newValueBytes);
                return true;
            } else {
                return false;
            }

        } catch (RocksDBException e) {
            rollback(innerTransaction);
            throw new RocksMapException("RocksMap replace error: ", e);
        } finally {
            commit(innerTransaction);
        }
    }

    /**
     * 删除某个 key
     * @param key key 对象
     * @param isRetVal 是否返回被移除的 value
     * @return 返回值, 在 isRetVal=false 时, 总是为 null
     */
    public V remove(Object key, boolean isRetVal) {
        if(key == null){
            throw new NullPointerException();
        }

        try {
            V value = null;
            if(isRetVal) {
                value = get(key);
            }

            if(!isRetVal || value != null) {
                Transaction transaction = threadLocalTransaction.get();
                if(transaction!=null) {
                    transaction.delete(dataColumnFamilyHandle, TSerialize.serialize(key));
                } else {
                    rocksDB.delete(dataColumnFamilyHandle, TSerialize.serialize(key));
                }
            }
            return (V)value;
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap remove " + key + " failed", e);
        }
    }

    @Override
    public V remove(Object key) {
        return remove(key, true);
    }

    public void removeAll(Collection<K> keys) {
        if(keys == null){
            throw new NullPointerException();
        }

        WriteBatch writeBatch = THREAD_LOCAL_WRITE_BATCH.get();
        for(K key : keys) {
            if(key == null){
                continue;
            }

            try {
                writeBatch.delete(dataColumnFamilyHandle, TSerialize.serialize(key));
            } catch (RocksDBException e) {
                throw new RocksMapException("RocksMap removeAll " + key + " failed", e);
            }
        }
        try {
            rocksDB.write(writeOptions, writeBatch);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap removeAll write failed", e);
        }
        writeBatch.clear();
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
            dataColumnFamilyHandle = getColumnFamilyHandler(rocksDB, this.columnFamilyName);
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
        TreeSet<Entry<K,V>> entrySet =  new TreeSet<Entry<K,V>>();
        RocksIterator iterator = getIterator();
        iterator.seekToFirst();
        while(iterator.isValid()){
            entrySet.add(new RocksMapEntry<K, V>(iterator.key(), iterator.value()));
            iterator.next();
        }

        return entrySet;
    }

    /**
     * 按前缀查找关联的 key
     * @param key key 的前缀
     * @return 找到的 Map 数据
     */
    public Map<K,V> startWith(K key) {
        return startWith(key, 0,0);
    }

    /**
     * 按前缀查找关联的 key
     * @param key key 的前缀
     * @return 找到的 Map 数据
     */
    public Map<K,V> startWith(K key, int skipSize, int size) {
        byte[] keyBytes = TSerialize.serialize(key);
        TreeMap<K,V> entryMap =  new TreeMap<K,V>();

        RocksMapIterator iterator = new RocksMapIterator(this, key, null, skipSize, size);
        while(iterator.hasNext()){
            iterator.next();
            if(TByte.byteArrayStartWith(iterator.keyBytes(), keyBytes)) {
                entryMap.put((K) iterator.key(), (V) iterator.value());
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

    /**
     * 构造一个有范围的迭代器
     * @param fromKey 起始 key
     * @param toKey 结束 key
     * @param size 迭代的记录数
     * @return 迭代器对象
     */
    public RocksMapIterator iterator(K fromKey, K toKey, int skipSize, int size){
        return new RocksMapIterator(this, fromKey, toKey, skipSize, size);
    }

    /**
     * 构造一个有范围的迭代器
     * @param fromKey 起始 key
     * @param toKey 结束 key
     * @return 迭代器对象
     */
    public RocksMapIterator iterator(K fromKey, K toKey){
        return new RocksMapIterator(this, fromKey, toKey, 0, 0);
    }

    /**
     * 构造一个有范围的迭代器
     * @param size 迭代的记录数
     * @return 迭代器对象
     */
    public RocksMapIterator iterator(int skipSize, int size){
        return new RocksMapIterator(this, null, null, skipSize, size);
    }

    /**
     * 构造一个有范围的迭代器
     * @param size 迭代的记录数
     * @return 迭代器对象
     */
    public RocksMapIterator iterator(int size){
        return new RocksMapIterator(this, null, null, 0, size);
    }

    /**
     * 构造一个迭代器
     * @return 迭代器对象
     */
    public RocksMapIterator iterator(){
        return new RocksMapIterator(this, null, null, 0, 0);
    }

    public class RocksMapEntry<K, V> implements Map.Entry<K, V>, Comparable<RocksMapEntry> {
        private byte[] keyBytes;
        private K k;
        private byte[] valueBytes;
        private V v;

        protected RocksMapEntry(byte[] keyBytes, byte[] valueBytes) {
            this.keyBytes = keyBytes;
            this.valueBytes = valueBytes;
        }

        @Override
        public K getKey() {
            if(k==null){
                this.k = (K) TSerialize.unserialize(keyBytes);
            }
            return k;
        }

        @Override
        public V getValue() {
            if(v==null) {
                this.v = (V) TSerialize.unserialize(valueBytes);
            }
            return v;
        }

        public byte[] getKeyBytes() {
            return keyBytes;
        }

        public byte[] getValueBytes() {
            return valueBytes;
        }

        @Override
        public V setValue(V value) {
            return v;
        }

        @Override
        public int compareTo(RocksMapEntry o) {
            return TByte.byteArrayCompare(this.keyBytes, o.keyBytes);
        }
    }

    public class RocksMapIterator<Entry> implements Iterator<Entry>{

        private RocksMap rocksMap;
        private RocksIterator iterator;
        private byte[] fromKeyBytes;
        private byte[] toKeyBytes;
        private int skipSize;
        private int size = 0;
        private int count=0;

        //["beginKey", "endKey")
        protected RocksMapIterator(RocksMap rocksMap, K fromKey, K toKey, int skipSize, int size) {
            this.rocksMap = rocksMap;
            this.iterator = rocksMap.getIterator();
            this.fromKeyBytes = TSerialize.serialize(fromKey);
            this.toKeyBytes = TSerialize.serialize(toKey);
            this.skipSize = skipSize;
            this.size = size;

            if(fromKeyBytes==null) {
                iterator.seekToFirst();
            } else {
                iterator.seek(fromKeyBytes);
            }

            if(skipSize >0) {
                for(int i=0;i<=this.skipSize;i++) {
                    if(!directNext()) {
                        break;
                    }
                }
            }

            count = 0;
        }

        @Override
        public boolean hasNext() {
            if(count == 0 && iterator.isValid()) {
                return true;
            }

            try {
                iterator.next();
                if (toKeyBytes == null) {
                    return iterator.isValid();
                } else {
                    return iterator.isValid() && !(Arrays.equals(iterator.key(), toKeyBytes));
                }
            } finally {
                iterator.prev();
                if(size!=0 && count > size - 1){
                    return false;
                }
            }
        }

        /**
         * 获取 Key 的值
         * @return Key 的值
         */
        public K key(){
            return (K)TSerialize.unserialize(iterator.key());
        }

        /**
         * 获取 value 的值
         * @return value 的值
         */
        public V value(){
            return (V)TSerialize.unserialize(iterator.value());
        }

        /**
         * 获取 Key 的值
         * @return Key 的值
         */
        public byte[] keyBytes(){
            return iterator.key();
        }

        /**
         * 获取 value 的值
         * @return value 的值
         */
        public byte[] valueBytes(){
            return iterator.value();
        }

        /**
         * 只是执行 next 不反序列化数据
         */
        public boolean directNext() {
            if(count != 0) {
                iterator.next();
            }

            if(iterator.isValid()) {
                count++;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Entry next() {
            if(directNext()) {
                return (Entry) new RocksMapEntry(iterator.key(), iterator.value());
            } else {
                return null;
            }
        }

        @Override
        public void remove() {
            try {
                rocksMap.rocksDB.delete(rocksMap.dataColumnFamilyHandle, iterator.key());
            } catch (RocksDBException e) {
                throw new RocksMapException("RocksMapIterator remove failed", e);
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super Entry> action) {
            throw new UnsupportedOperationException();
        }
    }

    public static class LogRecord {
        // WriteBatch::rep_ :=
        //    sequence: fixed64
        //    count: fixed32
        //    data: record[count]
        // record :=
        //    kTypeValue varstring varstring
        //    kTypeDeletion varstring
        //    kTypeSingleDeletion varstring
        //    kTypeRangeDeletion varstring varstring
        //    kTypeMerge varstring varstring
        //    kTypeColumnFamilyValue varint32 varstring varstring
        //    kTypeColumnFamilyDeletion varint32 varstring
        //    kTypeColumnFamilySingleDeletion varint32 varstring
        //    kTypeColumnFamilyRangeDeletion varint32 varstring varstring
        //    kTypeColumnFamilyMerge varint32 varstring varstring
        //    kTypeBeginPrepareXID varstring
        //    kTypeEndPrepareXID
        //    kTypeCommitXID varstring
        //    kTypeRollbackXID varstring
        //    kTypeBeginPersistedPrepareXID varstring
        //    kTypeBeginUnprepareXID varstring
        //    kTypeNoop
        // varstring :=
        //    len: varint32
        //    data: uint8[len]

        public static int TYPE_DELETION = 0x0;
        public static int TYPE_VALUE = 0x1;
        public static int TYPE_MERGE = 0x2;
        public static int TYPE_LOGDATA = 0x3;               // WAL only.
        public static int TYPE_COLUMNFAMILY_DELETION = 0x4;  // WAL only.    <-----
        public static int TYPE_COLUMNFAMILY_VALUE = 0x5;     // WAL only.   <-----
        public static int TYPE_COLUMNFAMILY_MERGE = 0x6;     // WAL only.
        public static int TYPE_SINGLE_DELETION = 0x7;
        public static int TYPE_COLUMNFAMILY_SINGLE_DELETION = 0x8;  // WAL only.
        public static int TYPE_BEGIN_PREPARE_XID = 0x9;             // WAL only.
        public static int TYPE_END_PREPARE_XID = 0xA;               // WAL only.
        public static int TYPE_COMMIT_XID = 0xB;                   // WAL only.
        public static int TYPE_ROLLBACK_XID = 0xC;                 // WAL only.
        public static int TYPE_NOOP = 0xD;                        // WAL only.
        public static int TYPE_COLUMNFAMILY_RANGE_DELETION = 0xE;   // WAL only.
        public static int TYPE_RANGE_DELETION = 0xF;               // meta block
        public static int TYPE_COLUMNFAMILY_BLOB_INDEX = 0x10;      // Blob DB only
        public static int TYPE_BLOB_INDEX = 0x11;                  // Blob DB only
        public static int TYPE_BEGIN_PERSISTED_PREPARE_XID = 0x12;  // WAL only
        public static int TYPE_BEGIN_UNPREPARE_XID = 0x13;  // WAL only.

        //定义每种操作的数据项数据
        public static int[] TYPE_ELEMENT_COUNT = new int[]{
                1,  //0    kTypeDeletion varstring
                2,  //1    kTypeValue varstring varstring
                2,  //2    kTypeMerge varstring varstring
                0,  //3    ?
                1,  //4    kTypeColumnFamilyDeletion varint32 varstring
                2,  //5    kTypeColumnFamilyValue varint32 varstring varstring
                2,  //6    kTypeColumnFamilyMerge varint32 varstring varstring
                1,  //7    kTypeSingleDeletion varstring
                1,  //8    kTypeColumnFamilySingleDeletion varint32 varstring
                1,  //9    kTypeBeginPrepareXID varstring
                0,  //A    kTypeEndPrepareXID
                1,  //B    kTypeCommitXID varstring
                1,  //C    kTypeRollbackXID varstring
                0,  //D    kTypeNoop
                2,  //E    kTypeColumnFamilyRangeDeletion varint32 varstring varstring
                0,  //F    kTypeRangeDeletion varstring varstring
                0,  //10   kTypeColumnFamilyBlobIndex
                2,  //11   kTypeBlobIndex varstring varstring
                1,  //12   kTypeBeginPersistedPrepareXID varstring
                1   //13   kTypeBeginUnprepareXID varstring
        };

        private long sequence;
        private int type;
        private int columnFamilyId = 0;

        private ArrayList<Object> chunks;

        private LogRecord(long sequence, int type, int columnFamilyId) {
            this.sequence = sequence;
            this.type = type;
            this.columnFamilyId = columnFamilyId;
            chunks = new ArrayList<Object>();
        }

        public long getSequence() {
            return sequence;
        }

        private void setSequence(long sequence) {
            this.sequence = sequence;
        }

        public int getType() {
            return type;
        }

        private void setType(int type) {
            this.type = type;
        }

        public int getColumnFamilyId() {
            return columnFamilyId;
        }

        private void setColumnFamilyId(int columnFamilyId) {
            this.columnFamilyId = columnFamilyId;
        }

        public ArrayList<Object> getChunks() {
            return chunks;
        }

        /**
         * 解析某个序号以后的更新操作记录
         * @param byteBuffer 字节缓冲器
         * @param withSerial 是否进行反序列化
         * @return 日志记录集合
         */
        public static List<LogRecord> parse(ByteBuffer byteBuffer, boolean withSerial) {
            return parse(byteBuffer, null, withSerial);
        }

        /**
         * 解析某个序号以后的更新操作记录
         * @param byteBuffer 字节缓冲器
         * @param filter 过滤器,用来过滤可用的操作类型和列族
         * @param withSerial 是否进行反序列化
         * @return 日志记录集合
         */
        public static List<LogRecord> parse(ByteBuffer byteBuffer, BiFunction<Integer, Integer, Boolean> filter, boolean withSerial) {
            ByteOrder originByteOrder = byteBuffer.order();
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            if(byteBuffer.remaining() < 13) {
                throw new ParseException("not a correct recorder");
            }
            //序号
            Long sequence = byteBuffer.getLong();

            //本次Wal操作包含键值数
            int recordCount = byteBuffer.getInt();



            List<LogRecord> logRecords = new ArrayList<LogRecord>();

            for(int count=0;byteBuffer.hasRemaining();count++) {

                LogRecord logRecord = parseOperation(byteBuffer, sequence, filter, withSerial);
                if(logRecord!=null) {
                    logRecords.add(logRecord);
                }
            }

            byteBuffer.order(originByteOrder);
            return logRecords;

        }

        /**
         * 解析某个序号的操作记录
         * @param byteBuffer 字节缓冲器
         * @param sequence 序号
         * @param withSerial 是否进行反序列化
         * @return 日志记录集合
         */
        public static LogRecord parseOperation(ByteBuffer byteBuffer, long sequence, boolean withSerial){
            return parseOperation(byteBuffer, sequence, withSerial);
        }

        /**
         * 解析某个序号的操作记录
         * @param byteBuffer 字节缓冲器
         * @param sequence 序号
         * @param filter 过滤器,用来过滤可用的操作类型和列族
         * @param withSerial 是否进行反序列化
         * @return 日志记录集合
         */
        public static LogRecord parseOperation(ByteBuffer byteBuffer, long sequence, BiFunction<Integer, Integer, Boolean> filter, boolean withSerial){
            //操作类型
            int type = byteBuffer.get();

            if (type == TYPE_NOOP) {
                if(byteBuffer.hasRemaining()) {
                    type = byteBuffer.get();
                } else {
                    return null;
                }
            }

            int columnFamilyId=0;
            if (type == TYPE_COLUMNFAMILY_DELETION ||
                    type == TYPE_COLUMNFAMILY_VALUE ||
                    type == TYPE_COLUMNFAMILY_MERGE ||
                    type == TYPE_COLUMNFAMILY_SINGLE_DELETION) {
                columnFamilyId = byteBuffer.get();
            }


            LogRecord logRecord = null;
            if (type < TYPE_ELEMENT_COUNT.length) {

                //应用过滤器
                boolean isEnable = filter==null || filter.apply(columnFamilyId, type);

                if(isEnable) {
                    logRecord = new LogRecord(sequence, type, columnFamilyId);
                }

                for (int i = 0; i < TYPE_ELEMENT_COUNT[type] && byteBuffer.hasRemaining(); i++) {
                    int chunkSize = Varint.varintToInt(byteBuffer);

                    if(isEnable) {
                        byte[] chunkBytes = new byte[chunkSize];
                        byteBuffer.get(chunkBytes);

                        Object chunk = chunkBytes;
                        if (withSerial) {
                            chunk = TSerialize.unserialize(chunkBytes);
                        } else {
                            chunk = chunkBytes;
                        }

                        logRecord.getChunks().add(chunk);
                    } else {
                        byteBuffer.position(byteBuffer.position() + chunkSize);
                    }
                }
            }

            return logRecord;
        }
    }

}
