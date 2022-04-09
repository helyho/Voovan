package org.voovan.tools.collection;
import org.rocksdb.*;
import org.voovan.tools.TByte;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.Varint;
import org.voovan.tools.exception.ParseException;
import org.voovan.tools.exception.RocksMapException;
import org.voovan.tools.log.Logger;
import org.voovan.tools.serialize.Serialize;
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

    public enum Type {
        READ_ONLY, SECONDARY, TRANSACTION;
    }

    //--------------------- 公共静态变量 --------------------
    static {
        RocksDB.loadLibrary();
    }

    public final static String DEFAULT_DB_NAME = "Default";

    //缓存 db 和他对应的 TransactionDB
    private static Map<String, RocksDB> ROCKSDB_MAP = new ConcurrentHashMap<String, RocksDB>();

    //缓存 TransactionDB 和列族句柄的关系
    private static Map<RocksDB, Map<String, ColumnFamilyHandle>> COLUMN_FAMILY_HANDLE_MAP = new ConcurrentHashMap<RocksDB, Map<String, ColumnFamilyHandle>>();

    //数据文件的默认保存路径
    private static String DEFAULT_DB_PATH           = ".rocksdb" + File.separator;
    private static String DEFAULT_WAL_PATH          = DEFAULT_DB_PATH + ".wal"    + File.separator;
    private static String DEFAULT_BACKUP_PATH       = DEFAULT_DB_PATH + ".backup" + File.separator;
    private static String DEFAULT_LOG_PATH          = DEFAULT_DB_PATH + "logs"    + File.separator;
    private static String DEFAULT_SECONDARY_DB_PATH = ".rocksdb.secondary" + File.separator;


    public static void setRootPath(String rootPath) {
        rootPath = rootPath.endsWith(File.separator) ? rootPath : rootPath + File.separator;
        DEFAULT_DB_PATH     = rootPath;
        DEFAULT_WAL_PATH    = rootPath + ".wal"    + File.separator;
        DEFAULT_BACKUP_PATH = rootPath + ".backup" + File.separator;
        DEFAULT_LOG_PATH    = rootPath + "logs"    + File.separator;
        DEFAULT_SECONDARY_DB_PATH = TString.removeSuffix(DEFAULT_DB_PATH) + ".sec";
    }

    /**
     * 获取默认数据存储路径
     * @return 默认数存储据路径
     */
    public static String getDefaultDbPath() {
        return DEFAULT_DB_PATH;
    }

    /**
     * 设置默认数据存储据路径
     * @param defaultDbPath 默认数存储据路径
     */
    public static void setDefaultDbPath(String defaultDbPath) {
        DEFAULT_DB_PATH = defaultDbPath.endsWith(File.separator) ? defaultDbPath : defaultDbPath + File.separator;
        DEFAULT_SECONDARY_DB_PATH  = TString.removeSuffix(DEFAULT_DB_PATH) + ".sec" + File.separator;;
    }

    /**
     * 默认WAL数据存储据路径
     * @return WAL数存储据路径
     */
    public static String getDefaultWalPath() {
        return DEFAULT_WAL_PATH;
    }

    /**
     * 设置WAL数据存储据路径
     * @param defaultWalPath WAL数存储据路径
     */
    public static void setDefaultWalPath(String defaultWalPath) {
        DEFAULT_WAL_PATH = defaultWalPath.endsWith(File.separator) ? defaultWalPath : defaultWalPath + File.separator;
    }


    /**
     * 默认数据备份存储据路径
     * @return WAL数存储据路径
     */
    public static String getDefaultBackupPath() {
        return DEFAULT_BACKUP_PATH;
    }

    /**
     * 设置数据备份存储据路径
     * @param defaultBackupPath WAL数存储据路径
     */
    public static void setDefaultBackupPath(String defaultBackupPath) {
        DEFAULT_BACKUP_PATH = defaultBackupPath.endsWith(File.separator) ? defaultBackupPath : defaultBackupPath + File.separator;;
    }

    /**
     * 获取默认日志路径
     * @return 默认数存储据路径
     */
    public static String getDefaultLogPath() {
        return DEFAULT_LOG_PATH;
    }

    /**
     * 设置默认日志路径
     * @param defaultLogPath 默认数存储据路径
     */
    public static void setDefaultLogPath(String defaultLogPath) {
        DEFAULT_LOG_PATH = defaultLogPath.endsWith(File.separator) ? defaultLogPath : defaultLogPath + File.separator;
    }

    /**
     * 获取副本打开的路径
     * @return 副本打开的路径
     */
    public static String getDefaultSecondaryDbPath() {
        return DEFAULT_SECONDARY_DB_PATH;
    }

    /**
     * 设置副本打开的路径
     */
    public static void setDefaultSecondaryDbPath(String defaultSecondaryDbPath) {
        DEFAULT_SECONDARY_DB_PATH = defaultSecondaryDbPath.endsWith(File.separator) ? defaultSecondaryDbPath : defaultSecondaryDbPath + File.separator;
    }

    /**
     * 根据名称获取列族
     * @param rocksDB RocksDB 对象
     * @param columnFamilyName 列族名称
     * @return 列族句柄
     */
    private static ColumnFamilyHandle getColumnFamilyHandler(RocksDB rocksDB, String columnFamilyName) {
        return COLUMN_FAMILY_HANDLE_MAP.get(rocksDB).get(columnFamilyName);
    }

    /**
     * 关闭 RocksDB 极其句柄
     * @param rocksDB RocksDB 对象
     */
    private static void closeRocksDB(RocksDB rocksDB) {
        for (ColumnFamilyHandle columnFamilyHandle : COLUMN_FAMILY_HANDLE_MAP.get(rocksDB).values()) {
            columnFamilyHandle.close();
        }

        try {
            rocksDB.syncWal();
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        rocksDB.close();
        COLUMN_FAMILY_HANDLE_MAP.remove(rocksDB);
    }

    //--------------------- 成员变量 --------------------
    public transient DBOptions            dbOptions;

    public transient ReadOptions          readOptions;
    public transient WriteOptions         writeOptions;
    public transient ColumnFamilyOptions  columnFamilyOptions;
    public transient BackupableDBOptions  backupableDBOptions;

    private transient RocksDB                     rocksDB;
    private transient ColumnFamilyHandle          dataColumnFamilyHandle;
    private transient ThreadLocal<Transaction>    threadLocalTransaction      = new ThreadLocal<Transaction>();
    private transient ThreadLocal<Integer>        threadLocalSavePointCount   = ThreadLocal.withInitial(()->new Integer(0));

    private transient String dbName;
    private transient String dataPath;
    private transient String walPath;
    private transient String logPath;
    private transient String secondaryPath;

    private transient String backupPath;
    private transient String columnFamilyName;
    private transient Type type;
    private transient Boolean isDuplicate = false;
    private transient int transactionLockTimeout = 5000;
    private transient Serialize serialize;
    private boolean useSingleRemove = false;

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
     * @param dbName 数据库的名称, 基于数据保存目录的相对路径
     * @param columnFamilyName 列族名称
     */
    public RocksMap(String dbName, String columnFamilyName) {
        this(dbName, columnFamilyName, null, null, null, null, null);
    }

    /**
     * 构造方法
     * @param type Rocksdb 开启的模式 参考: RocksMap.Type
     */
    public RocksMap(Type type) {
        this(null, null, null, null, null, null, type);
    }

    /**
     * 构造方法
     * @param columnFamilyName 列族名称
     * @param type Rocksdb 开启的模式 参考: RocksMap.Type
     */
    public RocksMap(String columnFamilyName, Type type) {
        this(null, columnFamilyName, null, null, null, null, type);
    }

    /**
     * 构造方法
     * @param dbName 数据库的名称, 基于数据保存目录的相对路径
     * @param columnFamilyName 列族名称
     * @param type Rocksdb 开启的模式 参考: RocksMap.Type
     */
    public RocksMap(String dbName, String columnFamilyName, Type type) {
        this(dbName, columnFamilyName, null, null, null, null, type);
    }

    /**
     * 构造方法
     * @param dbName 数据库的名称, 基于数据保存目录的相对路径
     * @param columnFamilyName 列族名称
     * @param dbOptions DBOptions 配置对象
     * @param readOptions ReadOptions 配置对象
     * @param writeOptions WriteOptions 配置对象
     * @param columnFamilyOptions 列族配置对象
     * @param type Rocksdb 开启的模式 参考: RocksMap.Type
     */
    public RocksMap(String dbName, String columnFamilyName, ColumnFamilyOptions columnFamilyOptions, DBOptions dbOptions, ReadOptions readOptions, WriteOptions writeOptions, Type type) {

        this.dbName = dbName == null ? DEFAULT_DB_NAME : dbName;
        this.columnFamilyName = columnFamilyName == null ? "voovan_default" : columnFamilyName;
        this.readOptions = readOptions == null ? new ReadOptions() : readOptions;
        this.writeOptions = writeOptions == null ? new WriteOptions() : writeOptions;
        this.columnFamilyOptions = columnFamilyOptions == null ? new ColumnFamilyOptions() : columnFamilyOptions;
        this.type = type == null ? Type.TRANSACTION : type;

        Options options = new Options();
        options.setCreateIfMissing(true);
        options.setCreateMissingColumnFamilies(true);

        if(dbOptions == null) {
            //Rocksdb 数据库配置
            this.dbOptions = new DBOptions(options);
        } else {
            this.dbOptions = dbOptions;
        }

        this.dataPath       = DEFAULT_DB_PATH           + this.dbName + File.separator;
        this.walPath        = DEFAULT_WAL_PATH          + this.dbName + File.separator;
        this.backupPath     = DEFAULT_BACKUP_PATH       + this.dbName + File.separator;
        this.walPath        = DEFAULT_WAL_PATH          + this.dbName + File.separator;
        this.logPath        = DEFAULT_LOG_PATH          + this.dbName + File.separator;

        this.secondaryPath  = DEFAULT_SECONDARY_DB_PATH + File.separator + this.dbName + File.separator;


        TFile.mkdir(dataPath);
        TFile.mkdir(walPath);
        TFile.mkdir(backupPath);
        TFile.mkdir(logPath);

        if(type == Type.SECONDARY) {
            TFile.mkdir(secondaryPath);
        }

        this.dbOptions.setWalDir(walPath);
        this.backupableDBOptions = new BackupableDBOptions(backupPath);
        this.dbOptions.setDbLogDir(logPath);

        rocksDB = ROCKSDB_MAP.get(this.dbName);

        try {
            if (rocksDB == null) {
                //默认列族列表
                List<ColumnFamilyDescriptor> DEFAULT_CF_DESCRIPTOR_LIST = new ArrayList<ColumnFamilyDescriptor>();

                //加载已经存在的所有列族
                {
                    List<byte[]> columnFamilyNameBytes = RocksDB.listColumnFamilies(new Options(), dataPath);
                    if (columnFamilyNameBytes.size() > 0) {
                        for (byte[] columnFamilyNameByte : columnFamilyNameBytes) {
                            ColumnFamilyDescriptor columnFamilyDescriptor = new ColumnFamilyDescriptor(columnFamilyNameByte, this.columnFamilyOptions);
                            DEFAULT_CF_DESCRIPTOR_LIST.add(columnFamilyDescriptor);
                        }
                    }

                    //如果为空创建默认列族
                    if (DEFAULT_CF_DESCRIPTOR_LIST.size() == 0) {
                        DEFAULT_CF_DESCRIPTOR_LIST.add( new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, this.columnFamilyOptions));
                    }
                }
                //用来接收ColumnFamilyHandle
                List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<ColumnFamilyHandle>();

                //打开 Rocksdb
                switch(this.type) {
                    case READ_ONLY: {
                        rocksDB = TransactionDB.openReadOnly(this.dbOptions, dataPath, DEFAULT_CF_DESCRIPTOR_LIST, columnFamilyHandleList);
                        break;
                    }
                    case TRANSACTION: {
                        this.dbOptions.setMaxOpenFiles(-1);
                        TransactionDBOptions transactionDBOptions = new TransactionDBOptions();
                        if(dbOptions.unorderedWrite() == true) {
                            dbOptions.setTwoWriteQueues(true);
                            transactionDBOptions.setWritePolicy(TxnDBWritePolicy.WRITE_PREPARED);
                        }
                        rocksDB = TransactionDB.open(this.dbOptions, transactionDBOptions, dataPath, DEFAULT_CF_DESCRIPTOR_LIST, columnFamilyHandleList);
                        ROCKSDB_MAP.put(this.dbName, rocksDB);
                        break;
                    }
                    case SECONDARY: {
                        rocksDB = RocksDB.openAsSecondary(this.dbOptions, dataPath, secondaryPath, DEFAULT_CF_DESCRIPTOR_LIST, columnFamilyHandleList);
                        break;
                    }
                }

                Map<String, ColumnFamilyHandle> columnFamilyHandleMap = new ConcurrentHashMap<String, ColumnFamilyHandle>();
                for(ColumnFamilyHandle columnFamilyHandle : columnFamilyHandleList) {
                    columnFamilyHandleMap.put(new String(columnFamilyHandle.getName()), columnFamilyHandle);
                }

                COLUMN_FAMILY_HANDLE_MAP.put(rocksDB, columnFamilyHandleMap);
            }

            choseColumnFamily(this.columnFamilyName);

        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap initilize failed, " + e.getMessage(), e);
        }
    }

    private RocksMap(RocksMap<K,V> rocksMap, String columnFamilyName, boolean shareTransaction){
        this.dbOptions           = new DBOptions(rocksMap.dbOptions);
        this.readOptions         = new ReadOptions(rocksMap.readOptions);
        this.writeOptions        = new WriteOptions(rocksMap.writeOptions);
        this.columnFamilyOptions = new ColumnFamilyOptions(rocksMap.columnFamilyOptions);

        this.rocksDB = rocksMap.rocksDB;
        //是否使用父对象的实物对象
        if(shareTransaction) {
            this.threadLocalTransaction = rocksMap.threadLocalTransaction;
            this.threadLocalSavePointCount = rocksMap.threadLocalSavePointCount;
        } else {
            this.threadLocalTransaction = ThreadLocal.withInitial(()->null);
            this.threadLocalSavePointCount = ThreadLocal.withInitial(()->new Integer(0));
        }

        this.dbName =  rocksMap.dbName;
        this.columnFamilyName = columnFamilyName;
        this.type = rocksMap.type;
        this.transactionLockTimeout = rocksMap.transactionLockTimeout;
        this.isDuplicate = true;

        this.choseColumnFamily(columnFamilyName);
    }

    /**
     * 复制出一个列族不同,但事务共享的 RocksMap
     * @param cfName 列族名称
     * @return 事务共享的 RocksMap
     */
    public RocksMap<K,V> duplicate(String cfName){
        return new RocksMap<K, V>(this, cfName, true);
    }

    /**
     * 复制出一个列族不同的 RocksMAp
     * @param cfName 列族名称
     * @param shareTransaction true: 共享事务, false: 不共享事务
     * @return 事务共享的 RocksMap
     */
    public RocksMap<K,V> duplicate(String cfName, boolean shareTransaction){
        return new RocksMap<K, V>(this, cfName, shareTransaction);
    }

    public byte[] serialize(Object obj) {
        serialize = serialize == null ? TSerialize.SERIALIZE : serialize;
        return obj == null ? new byte[0] : TSerialize.serialize(obj);
    }

    public Object unserialize(byte[] obj) {
        serialize = serialize == null ? TSerialize.SERIALIZE : serialize;
        return obj==null || obj.length == 0 ? null : TSerialize.unserialize(obj);
    }

    public String getDbName() {
        return dbName;
    }

    public String getDataPath() {
        return dataPath;
    }

    public String getWalPath() {
        return walPath;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public String getLogPath() {
        return logPath;
    }

    public String getColumnFamilyName() {
        return columnFamilyName;
    }

    public Type getType() {
        return type;
    }

    public int savePointCount() {
        return threadLocalSavePointCount.get();
    }

    public Boolean getDuplicate() {
        return isDuplicate;
    }

    public DBOptions getDbOptions() {
        return dbOptions;
    }

    public ReadOptions getReadOptions() {
        return readOptions;
    }

    public WriteOptions getWriteOptions() {
        return writeOptions;
    }

    public ColumnFamilyOptions getColumnFamilyOptions() {
        return columnFamilyOptions;
    }

    public BackupableDBOptions getBackupableDBOptions() {
        return backupableDBOptions;
    }

    public Serialize getSerialize() {
        return serialize;
    }

    public void setSerialize(Serialize serialize) {
        this.serialize = serialize;
    }

    public boolean isUseSingleRemove() {
        return useSingleRemove;
    }

    public void setUseSingleRemove(boolean useSingleRemove) {
        this.useSingleRemove = useSingleRemove;
    }

    public RocksDB getRocksDB(){
        return rocksDB;
    }


    /**
     * 创建一个备份
     * @param beforeFlush 是否在备份前执行 flush
     * @return 备份路径
     */
    public String createBackup(boolean beforeFlush) {
        try {
            BackupEngine backupEngine = BackupEngine.open(rocksDB.getEnv(), backupableDBOptions);
            backupEngine.createNewBackup(this.rocksDB, beforeFlush);
            return backupableDBOptions.backupDir();
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap createBackup failed , " + e.getMessage(), e);
        }
    }

    public String createBackup() {
        return createBackup(false);
    }

    /**
     * 获取备份信息
     * @return 备份信息清单
     */
    public List<BackupInfo> getBackupInfo() {
        try {
            BackupEngine backupEngine = BackupEngine.open(RocksEnv.getDefault(), backupableDBOptions);
            return backupEngine.getBackupInfo();
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap getBackupInfo failed , " + e.getMessage(), e);
        }
    }

    /**
     * 从最后一次备份恢复数据
     * @param keepLogfile 是否覆盖原有 wal 日志
     */
    public void restoreLatestBackup(Boolean keepLogfile) {
        try {
            RestoreOptions restoreOptions = new RestoreOptions(keepLogfile);

            BackupEngine backupEngine = BackupEngine.open(RocksEnv.getDefault(), backupableDBOptions);
            backupEngine.restoreDbFromLatestBackup(dataPath, walPath, restoreOptions);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap restoreFromLatestBackup failed , " + e.getMessage(), e);
        }
    }

    public void restoreLatestBackup() {
        restoreLatestBackup(false);
    }

    /**
     * 从指定备份恢复数据
     * @param backupId 备份 id 标识
     * @param keepLogfile 是否覆盖原有 wal 日志
     */
    public void restore(int backupId, Boolean keepLogfile) {
        try {
            RestoreOptions restoreOptions = new RestoreOptions(keepLogfile);

            BackupEngine backupEngine = BackupEngine.open(RocksEnv.getDefault(), backupableDBOptions);
            backupEngine.restoreDbFromBackup(backupId, dataPath, walPath, restoreOptions);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap restore failed , " + e.getMessage(), e);
        }
    }

    public void restore(int backupId) throws RocksDBException {
        restore(backupId, false);
    }



    public void deleteBackup(int backupId) throws RocksDBException {
        try {
            BackupEngine backupEngine = BackupEngine.open(RocksEnv.getDefault(), backupableDBOptions);
            backupEngine.deleteBackup(backupId);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap deleteBackup failed , " + e.getMessage(), e);
        }
    }


    /**
     * 清理备份
     * @param number 保留的备份书
     */
    public void PurgeOldBackups(int number) {
        try {
            BackupEngine backupEngine = BackupEngine.open(RocksEnv.getDefault(), backupableDBOptions);
            backupEngine.purgeOldBackups(number);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap PurgeOldBackups failed , " + e.getMessage(), e);
        }
    }


    public int getColumnFamilyId(){
        return getColumnFamilyId(this.columnFamilyName);
    }

    public void compact(){
        try {
            rocksDB.compactRange(dataColumnFamilyHandle);
        } catch (RocksDBException e) {
            throw new RocksMapException("compact failed", e);
        }
    }

    public void compactRange(K start, K end){
        try {
            rocksDB.compactRange(dataColumnFamilyHandle, serialize(start), serialize(end));
        } catch (RocksDBException e) {
            throw new RocksMapException("compact failed", e);
        }
    }

    private String getProperty(ColumnFamilyHandle columnFamilyHandle, String name) {
        try {
            return rocksDB.getProperty(columnFamilyHandle, "rocksdb." + name);
        } catch (RocksDBException e) {
            throw new RocksMapException("getProperty failed", e);
        }
    }

    public String getProperty(String columnFamilyName, String name) {
        try {
            ColumnFamilyHandle columnFamilyHandle = getColumnFamilyHandler(rocksDB, columnFamilyName);
            if(columnFamilyHandle != null) {
                return rocksDB.getProperty(columnFamilyHandle, "rocksdb." + name);
            } else {
                return "ColumnFamily: " + columnFamilyName + " not exists";
            }
        } catch (RocksDBException e) {
            throw new RocksMapException("getProperty failed", e);
        }
    }

    public String getProperty(String name) {
        try {
            return rocksDB.getProperty(this.dataColumnFamilyHandle, "rocksdb." + name);
        } catch (RocksDBException e) {
            throw new RocksMapException("getProperty failed", e);
        }
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
    public List<RocksWalRecord> getWalSince(Long sequenceNumber, boolean withSerial) {
        return getWalBetween(sequenceNumber, null, null, withSerial);
    }

    /**
     * 获取某个序号以后的更新操作记录
     * @param startSequence 起始序号
     * @param filter 过滤器,用来过滤可用的操作类型和列族
     * @param withSerial 是否进行反序列化
     * @return 日志记录集合
     */
    public List<RocksWalRecord> getWalSince(Long startSequence, BiFunction<Integer, Integer, Boolean> filter, boolean withSerial) {
        return getWalBetween(startSequence, null, filter, withSerial);
    }

    /**
     * 获取某个序号以后的更新操作记录
     * @param startSequence 起始序号
     * @param endSequence 结束序号
     * @param withSerial 是否进行反序列化
     * @return 日志记录集合
     */
    public List<RocksWalRecord> getWalSince(Long startSequence, Long endSequence, boolean withSerial) {
        return getWalBetween(startSequence, endSequence, null, withSerial);
    }

    /**
     * 获取某个序号以后的更新操作记录
     *          包含 start 和 end
     * @param startSequence 起始序号
     * @param endSequence 结束序号
     * @param filter 过滤器,用来过滤可用的操作类型和列族
     * @param withSerial 是否进行反序列化
     * @return 日志记录集合
     */
    public List<RocksWalRecord> getWalBetween(Long startSequence, Long endSequence, BiFunction<Integer, Integer, Boolean> filter, boolean  withSerial) {
        ArrayList<RocksWalRecord> rocksWalRecords = new ArrayList<RocksWalRecord>();
        try (TransactionLogIterator transactionLogIterator = rocksDB.getUpdatesSince(startSequence)) {

            if(startSequence > getLastSequence()) {
                return rocksWalRecords;
            }

            if(endSequence!=null && startSequence > endSequence) {
                throw new RocksMapException("startSequence is large than endSequence");
            }

            long seq = 0l;
            while (transactionLogIterator.isValid()) {
                TransactionLogIterator.BatchResult batchResult = transactionLogIterator.getBatch();

                if (batchResult.sequenceNumber() < startSequence) {
                    transactionLogIterator.next();
                    continue;
                }

                if(endSequence!=null && batchResult.sequenceNumber() > endSequence) {
                    break;
                }

                seq = batchResult.sequenceNumber();
                try (WriteBatch writeBatch = batchResult.writeBatch()) {
                    List<RocksWalRecord> rocksWalRecordBySeq = RocksWalRecord.parse(this, ByteBuffer.wrap(writeBatch.data()), filter, withSerial);
                    rocksWalRecords.addAll(rocksWalRecordBySeq);
                    writeBatch.clear();
                }

                transactionLogIterator.next();

            }

            if(rocksWalRecords.size() > 0)
                Logger.debug("wal between: " + startSequence + "->" + endSequence +  ", "  + rocksWalRecords.get(0).getSequence() + "->" + rocksWalRecords.get(rocksWalRecords.size()-1).getSequence());
            return rocksWalRecords;
        } catch (RocksDBException e) {
            throw new RocksMapException("getWalBetween failed, " + e.getMessage(), e);
        }
    }

    public int getColumnFamilyId(String columnFamilyName){
        ColumnFamilyHandle columnFamilyHandle = getColumnFamilyHandler(rocksDB, columnFamilyName);
        if(columnFamilyHandle!=null){
            return columnFamilyHandle.getID();
        } else {
            throw new RocksMapException("ColumnFamily [" + columnFamilyName +"] not found.");
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
                ColumnFamilyDescriptor columnFamilyDescriptor = new ColumnFamilyDescriptor(cfName.getBytes(), columnFamilyOptions);
                dataColumnFamilyHandle = rocksDB.createColumnFamily(columnFamilyDescriptor);
                COLUMN_FAMILY_HANDLE_MAP.get(rocksDB).putIfAbsent(new String(dataColumnFamilyHandle.getName()), dataColumnFamilyHandle);
            }

            this.columnFamilyName = cfName;

            return this;
        } catch(RocksDBException e){
            throw new RocksMapException("RocksMap initilize failed, " + e.getMessage(), e);
        }
    }

    /**
     * 获取事务锁超时时间
     * @return 事务锁超时时间
     */
    public int getTransactionLockTimeout() {
        return transactionLockTimeout;
    }

    /**
     * 设置事务锁超时时间
     * @param transactionLockTimeout 事务锁超时时间
     */
    public void setTransactionLockTimeout(int transactionLockTimeout) {
        this.transactionLockTimeout = transactionLockTimeout;
    }

    /**
     * 开启式事务模式 (立即失败, 死锁检测,无快照)
     *      同一个线程共线给一个事务
     *      使用内置公共事务通过 savepoint 来失败回滚, 但统一提交, 性能会好很多, 但是由于很多层嵌套的 savepont 在高并发时使用这种方式时回导致提交会慢很多
     * @param transFunction 事务执行器, 返回 Null 则事务回滚, 其他则事务提交
     * @param <T>  范型
     * @return 非 null: 事务成功, null: 事务失败
     */
    public <T> T withTransaction(Function<RocksMap<K, V>, T> transFunction) {
        return withTransaction(writeOptions, -1, true, false, transFunction);
    }


    /**
     * 开启式事务模式 (立即失败, 死锁检测,无快照)
     *      同一个线程共线给一个事务
     *      使用内置公共事务通过 savepoint 来失败回滚, 但统一提交, 性能会好很多, 但是由于很多层嵌套的 savepont 在高并发时使用这种方式时回导致提交会慢很多
     * @param transWriteOptions 事务内使用的 WriteOptions 对象
     * @param transFunction 事务执行器, 返回 Null 则事务回滚, 其他则事务提交
     * @param <T>  范型
     * @return 非 null: 事务成功, null: 事务失败
     */
    public <T> T withTransaction(WriteOptions transWriteOptions, Function<RocksMap<K, V>, T> transFunction) {
        return withTransaction(transWriteOptions, -1, true, false, transFunction);
    }

    /**
     * 开启式事务模式
     *      同一个线程共线给一个事务
     *      使用内置公共事务通过 savepoint 来失败回滚, 但统一提交, 性能会好很多, 但是由于很多层嵌套的 savepont 在高并发时使用这种方式时回导致提交会慢很多
     * @param transWriteOptions 事务内使用的 WriteOptions 对象
     * @param expire         提交时锁超时时间
     * @param deadlockDetect 死锁检测是否打开
     * @param withSnapShot   是否启用快照事务
     * @param transFunction 事务执行器, 返回 Null 则事务回滚, 其他则事务提交
     * @param <T>  范型
     * @return 非 null: 事务成功, null: 事务失败
     */
    public <T> T withTransaction(WriteOptions transWriteOptions, long expire, boolean deadlockDetect, boolean withSnapShot, Function<RocksMap<K, V>, T> transFunction) {
        beginTransaction(transWriteOptions, expire, deadlockDetect, withSnapShot);

        try {
            T object = transFunction.apply(this);
            if (object == null) {
                rollback(false);
            } else {
                commit();
            }
            return object;
        } catch (Throwable e) {
            try {
                rollback(false);
            } catch(Exception re) {
                Logger.error("RocksMap withTransaction.catch.rollback error", re);
            }
            throw e;
        }
    }

    /**
     * 开启事务
     *      同一个线程共线给一个事务
     *      默认: 锁提交等待时间-1, 死锁检测:true, 是否启用快照事务: false
     */
    public void beginTransaction() {
        beginTransaction(writeOptions, -1, true, false);
    }

    /**
     * 开启事务
     *      同一个线程共线给一个事务
     *      默认: 锁提交等待时间-1, 死锁检测:true, 是否启用快照事务: false
     * @param transWriteOptions 事务内使用的 WriteOptions 对象
     */
    public void beginTransaction(WriteOptions transWriteOptions) {
        beginTransaction(transWriteOptions, -1, true, false);
    }

    /**
     * 开启事务
     *      同一个线程共线给一个事务
     *      事务都是读事务，无论操作的记录间是否有交集，都不会锁定。
     *      事务包含读、写事务：
     *      所有的读事务不会锁定，读到的数据取决于snapshot设置。
     *      写事务之间如果不存在记录交集，不会锁定。
     *      写事务之间如果存在记录交集，此时如果未设置snapshot，则交集部分的记录是可以串行提交的。如果设置了snapshot，则第一个写事务(写锁队列的head)会成功，其他写事务会失败(之前的事务修改了该记录的情况下)。
     * @param expire         提交时锁超时时间
     * @param deadlockDetect 死锁检测是否打开
     * @param withSnapShot   是否启用快照事务
     */
    public void beginTransaction(long expire, boolean deadlockDetect, boolean withSnapShot) {
        baseBeginTransaction(writeOptions, expire, deadlockDetect, withSnapShot);
    }


    /**
     * 开启事务
     *      同一个线程共线给一个事务
     *      事务都是读事务，无论操作的记录间是否有交集，都不会锁定。
     *      事务包含读、写事务：
     *      所有的读事务不会锁定，读到的数据取决于snapshot设置。
     *      写事务之间如果不存在记录交集，不会锁定。
     *      写事务之间如果存在记录交集，此时如果未设置snapshot，则交集部分的记录是可以串行提交的。如果设置了snapshot，则第一个写事务(写锁队列的head)会成功，其他写事务会失败(之前的事务修改了该记录的情况下)。
     * @param transWriteOptions 事务内使用的 WriteOptions 对象
     * @param expire         提交时锁超时时间
     * @param deadlockDetect 死锁检测是否打开
     * @param withSnapShot   是否启用快照事务
     */
    public void beginTransaction(WriteOptions transWriteOptions, long expire, boolean deadlockDetect, boolean withSnapShot) {
        baseBeginTransaction(transWriteOptions, expire, deadlockDetect, withSnapShot);
    }

    /**
     * 开启事务
     *      同一个线程共线给一个事务
     *      事务都是读事务，无论操作的记录间是否有交集，都不会锁定。
     *      事务包含读、写事务：
     *      所有的读事务不会锁定，读到的数据取决于snapshot设置。
     *      写事务之间如果不存在记录交集，不会锁定。
     *      写事务之间如果存在记录交集，此时如果未设置snapshot，则交集部分的记录是可以串行提交的。如果设置了snapshot，则第一个写事务(写锁队列的head)会成功，其他写事务会失败(之前的事务修改了该记录的情况下)。
     * @param transWriteOptions 事务内使用的 WriteOptions 对象
     * @param expire         提交时锁超时时间
     * @param deadlockDetect 死锁检测是否打开
     * @param withSnapShot   是否启用快照事务
     * @return Transaction 事务对象
     */
    private Transaction baseBeginTransaction(WriteOptions transWriteOptions, long expire, boolean deadlockDetect, boolean withSnapShot) {
        Transaction transaction = threadLocalTransaction.get();
        if(transaction==null) {
            transaction = createTransaction(transWriteOptions, expire, deadlockDetect, withSnapShot);
            threadLocalTransaction.set(transaction);
        } else {
            savePoint();
        }

        return transaction;
    }

    private Transaction createTransaction(WriteOptions transWriteOptions, long expire, boolean deadlockDetect, boolean withSnapShot) {
        if(type == Type.READ_ONLY || type == Type.SECONDARY){
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


        Transaction transaction = ((TransactionDB) rocksDB).beginTransaction(transWriteOptions, transactionOptions);
        transaction.setWriteOptions(transWriteOptions==null ? this.writeOptions : transWriteOptions);

        transactionOptions.close();

        return transaction;
    }

    private void closeTransaction() {
        Transaction transaction = threadLocalTransaction.get();
        if(transaction!=null) {
            transaction.close();
            threadLocalTransaction.set(null);
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
            threadLocalSavePointCount.set(threadLocalSavePointCount.get()+1);
        } catch (RocksDBException e) {
            throw new RocksMapException("commit failed, " + e.getMessage(), e);
        }
    }

    public void rollbackSavePoint(){
        Transaction transaction = getTransaction();

        try {
            transaction.rollbackToSavePoint();
            threadLocalSavePointCount.set(threadLocalSavePointCount.get()-1);
        } catch (RocksDBException e) {
            throw new RocksMapException("commit failed, " + e.getMessage(), e);
        }
    }

    /**
     * 事务提交
     */
    public void commit() {
        Transaction transaction = getTransaction();
        if(threadLocalSavePointCount.get() == 0) {
            commit(transaction);
        } else {
            threadLocalSavePointCount.set(threadLocalSavePointCount.get()-1);
        }
    }

    /**
     * 事务提交
     */
    private void commit(Transaction transaction) {
        if (transaction != null) {
            try {
                transaction.commit();
            } catch (RocksDBException e) {
                throw new RocksMapException("RocksMap commit failed, " + e.getMessage(), e);
            } finally {
                closeTransaction();
            }
        } else {
            throw new RocksMapException("RocksMap is not in transaction model");
        }
    }

    /**
     * 事务回滚
     */
    public void rollback() {
        rollback(true);
    }

    /**
     * 事务回滚
     * @param all true: 直接彻底回滚当前事务, false: 回滚到上一个 savepoint
     */
    public void rollback(boolean all) {
        Transaction transaction = getTransaction();

        if(all) {
            rollback(transaction);
        } else if(threadLocalSavePointCount.get()==0) {
            rollback(transaction);
        } else {
            rollbackSavePoint();
        }
    }

    /**
     * 事务回滚
     */
    private void rollback(Transaction transaction) {
        if (transaction != null) {
            try {
                transaction.rollback();
            } catch(RocksDBException e){
                throw new RocksMapException("RocksMap rollback failed, " + e.getMessage(), e);
            } finally{
                closeTransaction();
            }
        } else {
            throw new RocksMapException("RocksMap is not in transaction model");
        }

    }

    /**
     * Secondary 追赶主实例的数据
     */
    public void tryCatchUpWithPrimary() {
        if(type != Type.SECONDARY){
            throw new RocksMapException("RocksMap Not supported operation in not secondary mode");
        }

        try {
            rocksDB.tryCatchUpWithPrimary();
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap try catch up with primary failed", e);
        }
    }

    @Override
    public Comparator<? super K> comparator() {
        return null;
    }


    /**
     * 获取一个迭代器
     * @return RocksIterator 对象
     */
    public RocksIterator getIterator(){
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
        try (RocksIterator iterator = getIterator()){
            byte[] fromKeyBytes = serialize(fromKey);
            byte[] toKeyBytes = serialize(toKey);

            if (fromKeyBytes == null) {
                iterator.seekToFirst();
            } else {
                iterator.seek(fromKeyBytes);
            }

            while (iterator.isValid()) {
                byte[] key = iterator.key();

                boolean fromKeyCompare = fromKeyBytes==null || TByte.byteArrayCompare(fromKeyBytes, key) <= 0;
                boolean toKeyCompare = toKey==null || TByte.byteArrayCompare(toKeyBytes, key) >= 0;

                if(!toKeyCompare || !fromKeyCompare) {
                    return subMap;
                }

                if(fromKeyCompare && toKeyCompare) {
                    subMap.put((K) unserialize(iterator.key()), (V) unserialize(iterator.value()));
                }
//                if (toKey == null || !Arrays.equals(toKeyBytes, key)) {
//                    subMap.put((K) unserialize(iterator.key()), (V) unserialize(iterator.value()));
//                } else {
//                    subMap.put((K) unserialize(iterator.key()), (V) unserialize(iterator.value()));
//                    break;
//                }
                iterator.next();
            }

            return subMap;
        }
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
        try (RocksIterator iterator = getIterator()) {

            iterator.seekToFirst();
            if (iterator.isValid()) {
                return (K) unserialize(iterator.key());
            }

            return null;
        }
    }

    @Override
    public K lastKey() {
        try (RocksIterator iterator = getIterator()) {

            iterator.seekToLast();
            if (iterator.isValid()) {
                return (K) unserialize(iterator.key());
            }

            return null;
        }
    }


    /**
     * 遍历所有数据来获取 kv 记录的数量, 会消耗很多性能
     */
    @Override
    public int size() {
//        try {
//            return Integer.valueOf(rocksDB.getProperty(dataColumnFamilyHandle, "rocksdb.estimate-num-keys"));
//        } catch (RocksDBException e) {
//            e.printStackTrace();
//        }

        int count = 0;
        RocksIterator iterator = null;

        try {
            iterator = getIterator();

            iterator.seekToFirst();

            while (iterator.isValid()) {
                iterator.next();
                count++;
            }
            return count;
        } finally {
            if(iterator!=null){
                iterator.close();
            }
        }
    }

    @Override
    public boolean isEmpty() {
        try (RocksIterator iterator = getIterator()){
            iterator.seekToFirst();
            return !iterator.isValid();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        byte[] values = null;
        try {
            Transaction transaction = threadLocalTransaction.get();
            if(transaction!=null) {
                values = transaction.get(dataColumnFamilyHandle, readOptions, serialize(key));
            } else {
                values = rocksDB.get(dataColumnFamilyHandle, readOptions, serialize(key));
            }
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap containsKey " + key + " failed, " + e.getMessage(), e);
        }

        return values!=null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取并锁定, 默认独占模式
     * @param key 将被加锁的 key
     * @return key 对应的 value
     */
    public V lock(Object key){
        return lock(key, true);
    }

    /**
     * 释放锁
     * @param key 将被释放锁的 key
     */
    public void unlock(Object key) {
        Transaction transaction = getTransaction();
        transaction.undoGetForUpdate(serialize(key));
    }

    /**
     * 获取并加锁
     * @param key 将被加锁的 key
     * @param exclusive 是否独占模式
     * @return key 对应的 value
     */
    public V lock(Object key, boolean exclusive){
        Transaction transaction = getTransaction();

        try {
            byte[] values = transaction.getForUpdate(readOptions, dataColumnFamilyHandle, serialize(key), exclusive);
            return (V) unserialize(values);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap lock " + key + " failed, " + e.getMessage(), e);
        }
    }

    private byte[] get(byte[] keyBytes) {
        try {
            byte[] values = null;
            Transaction transaction = threadLocalTransaction.get();
            if (transaction != null) {
                values = transaction.getForUpdate(readOptions, dataColumnFamilyHandle, keyBytes, true);
            } else {
                values = rocksDB.get(dataColumnFamilyHandle, readOptions, keyBytes);
            }

            return values;
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap get failed, " + e.getMessage(), e);
        }
    }

    @Override
    public V get(Object key) {
        if(key == null){
            throw new NullPointerException();
        }

        byte[] values = get(serialize(key));
        return (V) unserialize(values);
    }

    /**
     * 无事务直接获取
     * @param key 当前 key
     * @return 不在当前事务中的 value
     */
    public V directGet(Object key) {
        if(key == null){
            throw new NullPointerException();
        }

        try {
            byte[] values = rocksDB.get(dataColumnFamilyHandle, readOptions, serialize(key));
            return (V) unserialize(values);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap getWithoutTransaction failed, " + e.getMessage(), e);
        }
    }

    public List<V> getAll(Collection<K> keys) {
        ArrayList<V> values = new ArrayList<V>(keys.size());

        Iterator<K> keysIterator = keys.iterator();
        while (keysIterator.hasNext()) {
            values.add(get(keysIterator.next()));
        }

        return values;
    }

    private byte[] put(byte[] keyBytes, byte[] valueBytes, boolean isRetVal) {
        if(keyBytes == null || valueBytes == null){
            throw new NullPointerException();
        }

        try {
            byte[] oldValueBytes = null;
            if(isRetVal) {
                oldValueBytes = get(keyBytes);
            }

            Transaction transaction = threadLocalTransaction.get();
            if (transaction != null) {
                transaction.put(dataColumnFamilyHandle, keyBytes, valueBytes);
            } else {
                rocksDB.put(dataColumnFamilyHandle, writeOptions, keyBytes, valueBytes);
            }

            return oldValueBytes;
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap put failed, " + e.getMessage(), e);
        }
    }

    public void empty(Object key) {
        put(serialize(key), null);
    }

    public Object put(Object key, Object value, boolean isRetVal) {
        return unserialize(put(serialize(key), serialize(value), isRetVal));
    }

    @Override
    public Object put(Object key, Object value) {
        return unserialize(put(serialize(key), serialize(value), true));
    }

    @Override
    public V putIfAbsent(K key, V value) {
        byte[] keyBytes = serialize(key);
        byte[] valueBytes = serialize(value);

        //这里使用独立的事务是未了防止默认事务提交导致失效
        Transaction transaction = baseBeginTransaction(writeOptions, -1, true, false);

        try {
            byte[] oldValueBytes = transaction.getForUpdate(readOptions, dataColumnFamilyHandle, keyBytes, true);

            if(oldValueBytes == null){
                transaction.put(dataColumnFamilyHandle, keyBytes, valueBytes);
                return null;
            } else {
                return (V) unserialize(oldValueBytes);
            }
        } catch (RocksDBException e) {
            rollback();
            throw new RocksMapException("RocksMap putIfAbsent error, " + e.getMessage(), e);
        } finally {
            commit();
        }
    }

    /**
     * 判断 key 是否不存在
     * @param key key对象
     * @return 当返回 false 的时候 key 一定不存在, 当返回 true 的时候, key 有可能不存在, 参考 boomfilter
     */
    public boolean keyMayExists(K key) {
        return keyMayExists(serialize(key));
    }

    private boolean keyMayExists(byte[] keyBytes) {
        return rocksDB.keyMayExist(dataColumnFamilyHandle, keyBytes, null);
    }


    /**
     * 判断 key 是否不存在
     * @param key key对象
     * @return 当返回 false 的时候 key 不存在, 当返回 true 的时候, key 存在
     */
    public boolean isKeyExists(K key) {
        return isKeyExists(serialize(key));
    }

    private boolean isKeyExists(byte[] keyBytes) {
        boolean result = keyMayExists(keyBytes);

        if(result) {
            try {
                return rocksDB.get(dataColumnFamilyHandle, readOptions, keyBytes)!= null;
            } catch (RocksDBException e) {
                throw new RocksMapException("RocksMap isKeyExists failed , " + e.getMessage(), e);
            }
        }
        return result;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        byte[] keyBytes = serialize(key);
        byte[] newValueBytes = serialize(newValue);
        byte[] oldValueBytes = serialize(oldValue);

        //这里使用独立的事务是未了防止默认事务提交导致失效
        Transaction transaction = baseBeginTransaction(writeOptions, -1, true, false);

        try {
            byte[] oldDbValueBytes = transaction.getForUpdate(readOptions, dataColumnFamilyHandle, keyBytes, true);
            if(oldDbValueBytes!=null && Arrays.equals(oldDbValueBytes, oldValueBytes)){
                transaction.put(dataColumnFamilyHandle, keyBytes, newValueBytes);
                return true;
            } else {
                return false;
            }

        } catch (RocksDBException e) {
            rollback();
            throw new RocksMapException("RocksMap replace failed , " + e.getMessage(), e);
        } finally {
            commit();
        }
    }

    /**
     * 删除某个 key
     * @param keyBytes key 对象
     * @param isRetVal 是否返回被移除的 value
     * @return 返回值, 在 isRetVal=false 时, 总是为 null
     */
    private byte[] remove(byte[] keyBytes, boolean isRetVal) {
        if(keyBytes == null){
            throw new NullPointerException();
        }

        byte[] valueBytes = null;
        if(isRetVal) {
            valueBytes = get(keyBytes);
        }

        try {

            if (!isRetVal || valueBytes != null) {
                Transaction transaction = threadLocalTransaction.get();
                if (transaction != null) {
                    if(useSingleRemove) {
                        transaction.singleDelete(dataColumnFamilyHandle, keyBytes);
                    } else {
                        transaction.delete(dataColumnFamilyHandle, keyBytes);
                    }
                } else {
                    if(useSingleRemove) {
                        rocksDB.singleDelete(dataColumnFamilyHandle, keyBytes);
                    } else {
                        rocksDB.delete(dataColumnFamilyHandle, writeOptions, keyBytes);
                    }
                }
            }
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap remove failed , " + e.getMessage(), e);
        }
        return valueBytes;
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

        byte[] valuesByte = remove(serialize(key), isRetVal);
        return (V) unserialize(valuesByte);
    }

    @Override
    public V remove(Object key) {
        return remove(key, true);
    }

    public void removeAll(Collection<K> keys) {
        if(keys == null){
            throw new NullPointerException();
        }

        try {
            Transaction transaction = threadLocalTransaction.get();

            WriteBatch writeBatch = null;
            if (transaction == null) {
                writeBatch = THREAD_LOCAL_WRITE_BATCH.get();
                writeBatch.clear();

                for(K key : keys) {
                    if(key == null){
                        continue;
                    }

                    try {
                        if(useSingleRemove) {
                            writeBatch.singleDelete(dataColumnFamilyHandle, serialize(key));
                        } else {
                            writeBatch.delete(dataColumnFamilyHandle, serialize(key));
                        }
                    } catch (RocksDBException e) {
                        throw new RocksMapException("RocksMap removeAll " + key + " failed", e);
                    }
                }
                rocksDB.write(writeOptions, writeBatch);
            } else {
                for(K key : keys) {
                    if(key == null){
                        continue;
                    }

                    try {
                        if(useSingleRemove) {
                            transaction.singleDelete(dataColumnFamilyHandle, serialize(key));
                        } else {
                            transaction.delete(dataColumnFamilyHandle, serialize(key));
                        }
                    } catch (RocksDBException e) {
                        throw new RocksMapException("RocksMap removeAll " + key + " failed", e);
                    }
                }
            }

        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap removeAll write failed", e);
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
    public void removeRange(K fromKey, K toKey){
        removeRange(fromKey, toKey, true);
    }

    /**
     * Removes the database entries in the range ["beginKey", "endKey"), i.e.,
     * including "beginKey" and excluding "endKey". a non-OK status on error. It
     * is not an error if no keys exist in the range ["beginKey", "endKey").
     * @param fromKey 其实 key
     * @param toKey 结束 key
     * @param useTransaction 是否嵌套至已有事务
     *
     */
    public void removeRange(K fromKey, K toKey, boolean useTransaction) {
        Transaction transaction = useTransaction ? threadLocalTransaction.get() : null;
        byte[] fromKeyBytes = serialize(fromKey);
        byte[] toKeyBytes = serialize(toKey);

        try (RocksIterator iterator = getIterator()) {
            iterator.seek(fromKeyBytes);
            while (iterator.isValid()) {
                if (!Arrays.equals(iterator.key(), toKeyBytes)) {
                    if(transaction==null) {
                        rocksDB.delete(dataColumnFamilyHandle, iterator.key());
                    } else {
                        transaction.delete(dataColumnFamilyHandle, iterator.key());
                    }
                    iterator.next();
                } else {
                    break;
                }
            }
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap removeAll failed", e);
        }
    }

    public static ThreadLocal<WriteBatch> THREAD_LOCAL_WRITE_BATCH = ThreadLocal.withInitial(()->new WriteBatch());
    @Override
    public void putAll(Map m) {
        try {
            Transaction transaction = threadLocalTransaction.get();

            WriteBatch writeBatch = null;
            if (transaction == null) {
                writeBatch = THREAD_LOCAL_WRITE_BATCH.get();
                writeBatch.clear();
                Iterator<Entry> iterator = m.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry entry = iterator.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    writeBatch.put(dataColumnFamilyHandle, serialize(key), serialize(value));
                }
                rocksDB.write(writeOptions, writeBatch);
            } else {
                Iterator<Entry> iterator = m.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry entry = iterator.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    transaction.put(dataColumnFamilyHandle, serialize(key), serialize(value));
                }
            }

        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap putAll failed", e);
        }
    }

    /**
     * 刷新当前列族数据到文件
     * @param sync 同步刷新
     * @param allowStall 是否允许写入暂停
     */
    public void flush(boolean sync, boolean allowStall){
        try {
            FlushOptions flushOptions = new FlushOptions();
            flushOptions.setWaitForFlush(sync);
            flushOptions.setAllowWriteStall(allowStall);
            rocksDB.flush(flushOptions, this.dataColumnFamilyHandle);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap flush failed", e);
        }
    }

    /**
     * 刷新当前列族数据到文件
     * @param allowStall 允许 stall
     */
    public void flush(boolean allowStall){
        flush(true, allowStall);
    }

    /**
     * 刷新当前列族数据到文件, not stall
     */
    public void flush(){
        flush(true, false);
    }

    /**
     * 刷新所有列族数据到文件
     * @param sync 同步刷新
     * @param allowStall 是否允许写入暂停
     */
    public void flushAll(boolean sync, boolean allowStall) {
        try {
            Map<String, ColumnFamilyHandle> columnFamilyHandleMap = RocksMap.COLUMN_FAMILY_HANDLE_MAP.get(rocksDB);
            List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList(columnFamilyHandleMap.values());
            FlushOptions flushOptions = new FlushOptions();
            flushOptions.setWaitForFlush(sync);
            flushOptions.setAllowWriteStall(allowStall);
            rocksDB.flush(flushOptions, columnFamilyHandleList);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap flush all failed", e);
        }
    }

    /**
     * 刷新所有列族数据到文件
     * @param allowStall 是否允许写入暂停
     */
    public void flushAll(boolean allowStall) {
        flushAll(true, allowStall);
    }

    /**
     * 刷新所有列族数据到文件, not stall
     */
    public void flushAll() {
        flushAll(true, false);
    }

    /**
     * 刷新WAL数据到文件
     * @param sync 同步刷新
     */
    public void flushWal(boolean sync){
        try {
            rocksDB.flushWal(sync);
        } catch (RocksDBException e) {
            throw new RocksMapException("RocksMap flushWal failed", e);
        }
    }

    public void flushWal() {
        flushWal(true);
    }

    /**
     * 逐个清理数据
     */
    public void clearOneByOne() {
        Iterator iterator = this.iterator();
        while(iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    @Override
    public void clear() {
        if(type == Type.READ_ONLY || type == Type.SECONDARY){
            Logger.error("Clear failed, ", new RocksDBException("Not supported operation in read only mode"));
            return;
        }

        try {
            synchronized (rocksDB) {
                drop();
                ColumnFamilyDescriptor columnFamilyDescriptor = new ColumnFamilyDescriptor(columnFamilyName.getBytes(), columnFamilyOptions);
                dataColumnFamilyHandle = rocksDB.createColumnFamily(columnFamilyDescriptor);
                COLUMN_FAMILY_HANDLE_MAP.get(rocksDB).put(new String(dataColumnFamilyHandle.getName()), dataColumnFamilyHandle);
                //设置列族
                dataColumnFamilyHandle = getColumnFamilyHandler(rocksDB, this.columnFamilyName);
            }
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
            COLUMN_FAMILY_HANDLE_MAP.get(rocksDB).remove(new String(dataColumnFamilyHandle.getName()));
            dataColumnFamilyHandle.close();
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
        LinkedHashSet<K> keySet = new LinkedHashSet<K>();
        try (RocksIterator iterator = getIterator()){
            iterator.seekToFirst();
            while (iterator.isValid()) {
                K k = (K) unserialize(iterator.key());
                keySet.add(k);
                iterator.next();
            }

            return keySet;
        }
    }

    @Override
    public Collection values() {
        ArrayList<V> values = new ArrayList<V>();

        try (RocksIterator iterator = getIterator()){
            iterator.seekToFirst();
            while (iterator.isValid()) {
                V value = (V) unserialize(iterator.value());
                values.add(value);
                iterator.next();
            }

            return values;
        }
    }

    /**
     * 数据拷贝到内存中, 所以对这个 Set 的修改不会在 Rocksdb 中生效
     * @return 保存了 Entry 的 set
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        LinkedHashSet<Entry<K,V>> entrySet =  new LinkedHashSet<Entry<K,V>>();
        try (RocksIterator iterator = getIterator()) {
            iterator.seekToFirst();
            while (iterator.isValid()) {
                entrySet.add(new RocksMapEntry<K, V>(this, iterator.key(), iterator.value()));
                iterator.next();
            }

            return entrySet;
        }
    }

    /**
     * 按前缀查找关联的 key
     * @param key key 的前缀
     * @return 找到的 Map 数据
     */
    public Map<K,V> startWith(K key) {
        return startWith(key, 0, 0);
    }


    /**
     * 按前缀查找关联的 key
     * @param key key 的前缀
     * @param size 返回记录数
     * @return 找到的 Map 数据
     */
    public Map<K,V> startWith(K key, int size) {
        return startWith(key, 0, size);
    }

    /**
     * 按前缀查找关联的 key
     * @param key key 的前缀
     * @param skipSize 跳过记录数
     * @param size 返回记录数
     * @return 找到的 Map 数据
     */
    public Map<K,V> startWith(K key, int skipSize, int size) {
        byte[] keyBytes = serialize(key);
        TreeMap<K,V> entryMap =  new TreeMap<K,V>();

        try (RocksMapIterator iterator = new RocksMapIterator(this, key, null, skipSize, size)) {
            while (iterator.directNext(true)) {
                if (TByte.byteArrayStartWith(iterator.keyBytes(), keyBytes)) {
                    entryMap.put((K) iterator.key(), (V) iterator.value());
                } else {
                    break;
                }
            }

            return entryMap;
        }
    }

    @Override
    public void close() {
        //关闭事务
        Transaction transaction = threadLocalTransaction.get();
        if(transaction!=null){
            try {
                transaction.rollback();
            } catch (RocksDBException e) {
                throw new RocksMapException("RocksMap rollback on close failed", e);
            } finally {
                closeTransaction();
            }
        }

        dbOptions.close();
        readOptions.close();
        writeOptions.close();
        columnFamilyOptions.close();

        dbOptions = null;
        readOptions = null;
        writeOptions = null;
        columnFamilyOptions = null;

        if(!isDuplicate) {
            RocksMap.closeRocksDB(rocksDB);
        }
    }

    /**
     * 构造一个有范围的迭代器
     * @param fromKey 起始 key
     * @param toKey 结束 key
     * @param skipSize 跳过记录数
     * @param size 返回记录数
     * @return 迭代器对象
     */
    public RocksMapIterator<K,V> iterator(K fromKey, K toKey, int skipSize, int size){
        return new RocksMapIterator(this, fromKey, toKey, skipSize, size);
    }

    /**
     * 构造一个有范围的迭代器
     * @param fromKey 起始 key
     * @param toKey 结束 key
     * @return 迭代器对象
     */
    public RocksMapIterator<K,V> iterator(K fromKey, K toKey){
        return new RocksMapIterator(this, fromKey, toKey, 0, 0);
    }

    /**
     * 构造一个有范围的迭代器
     * @param skipSize 跳过记录数
     * @param size 返回记录数
     * @return 迭代器对象
     */
    public RocksMapIterator<K,V> iterator(int skipSize, int size){
        return new RocksMapIterator(this, null, null, skipSize, size);
    }

    /**
     * 构造一个有范围的迭代器
     * @param size 迭代的记录数
     * @return 迭代器对象
     */
    public RocksMapIterator<K,V> iterator(int size){
        return new RocksMapIterator(this, null, null, 0, size);
    }

    /**
     * 构造一个迭代器
     * @return 迭代器对象
     */
    public RocksMapIterator<K,V>  iterator(){
        return new RocksMapIterator(this, null, null, 0, 0);
    }

    /**
     * 数据清理执行器
     * @param checker 数据清理逻辑, true: 继续扫描, false: 停止扫描
     */
    public void scan(Function<RocksMap<K,V>.RocksMapEntry<K,V>, Boolean> checker) {
        scan(null, null, checker, false);
    }


    /**
     * 数据清理执行器
     * @param checker 数据清理逻辑, true: 继续扫描, false: 停止扫描
     * @param inTranscation 是否启用事务
     */
    public void scan(Function<RocksMap<K,V>.RocksMapEntry<K,V>, Boolean> checker, boolean inTranscation) {
        scan(null, null, checker, inTranscation);
    }

    /**
     * 数据清理执行器
     * @param fromKey 起始 key
     * @param toKey   结束 key
     * @param checker 数据清理逻辑, true: 继续扫描, false: 停止扫描
     */
    public void scan(K fromKey, K toKey, Function<RocksMap<K,V>.RocksMapEntry<K,V>, Boolean> checker) {
        scan(fromKey, toKey, checker, false);
    }

    /**
     * 数据清理执行器
     * @param fromKey 起始 key
     * @param toKey   结束 key
     * @param checker 数据清理逻辑, true: 继续扫描, false: 停止扫描
     * @param inTranscation 是否启用事务
     */
    public void scan(K fromKey, K toKey, Function<RocksMap<K,V>.RocksMapEntry<K,V>, Boolean> checker, boolean inTranscation) {
        RocksMap<K,V> innerRocksMap = this.duplicate(this.getColumnFamilyName(), false);

        Runnable runnable = ()->{
            try(RocksMap<K,V>.RocksMapIterator<K,V> iterator = innerRocksMap.iterator(fromKey, toKey)) {
                RocksMap<K, V>.RocksMapEntry<K, V> rocksMapEntry = null;
                while ((rocksMapEntry = iterator.next())!=null) {
                    if (checker.apply(rocksMapEntry)) {
                        continue;
                    } else {
                        break;
                    }
                }

            }
        };

        if(inTranscation) {
            innerRocksMap.withTransaction(obj -> {
                runnable.run();
                return null;
            });
        } else {
            runnable.run();
        }

        innerRocksMap.close();
    }

    public class RocksMapEntry<K, V> implements Map.Entry<K, V>, Comparable<RocksMapEntry> {
        private RocksMap<K,V> rocksMap;
        private byte[] keyBytes;
        private K k;
        private byte[] valueBytes;
        private V v;

        protected RocksMapEntry(RocksMap<K, V> rocksMap, byte[] keyBytes, byte[] valueBytes) {
            this.rocksMap = rocksMap;
            this.keyBytes = keyBytes;
            this.valueBytes = valueBytes;
        }

        @Override
        public K getKey() {
            if(k==null){
                this.k = (K) unserialize(keyBytes);
            }
            return k;
        }

        @Override
        public V getValue() {
            if(v==null) {
                this.v = (V) unserialize(valueBytes);
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
            rocksMap.put(keyBytes, serialize(value));
            return value;
        }

        @Override
        public int compareTo(RocksMapEntry o) {
            return TByte.byteArrayCompare(this.keyBytes, o.keyBytes);
        }

        @Override
        public String toString(){
            return getKey() + "=" + getValue();
        }

        public RocksMap<K, V> getRocksMap() {
            return rocksMap;
        }

        public void remove(){
            rocksMap.remove(keyBytes, false);
        }
    }

    public class RocksMapIterator<K,V> implements Iterator<RocksMapEntry<K,V>>, Closeable{

        private RocksMap<K,V> rocksMap;
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
            this.fromKeyBytes = serialize(fromKey);
            this.toKeyBytes = serialize(toKey);
            this.skipSize = skipSize;
            this.size = size;
            if(fromKeyBytes==null) {
                iterator.seekToFirst();
            } else {
                iterator.seek(fromKeyBytes);
            }

            if(skipSize >0) {
                for(int i=0;i<=this.skipSize;i++) {
                    if(!directNext(false)) {
                        break;
                    }
                }
            }

            count = 0;
        }

        /**
         * 获取迭代器当前位置的 Entry
         * @return RocksMapEntry对象
         */
        public RocksMapEntry<K, V> getEntry() {
            return new RocksMapEntry(rocksMap, iterator.key(), iterator.value());
        }

        @Override
        public boolean hasNext() {
            boolean ret = false;
            if(count == 0 && isValid()) {
                return true;
            }

            if(!iterator.isValid()) {
                return false;
            }

            try {
                iterator.next();
                ret = isValid();
            } finally {
                iterator.prev();

                if(size!=0 && count > size - 1){
                    ret = false;
                }
            }

            return ret;
        }

        /**
         * 迭代器当前位数数据是否有效
         * @return true: 有效, false: 无效
         */
        public boolean isValid(){
            boolean ret;
            if (toKeyBytes == null) {
                ret = iterator.isValid();
            } else {
                //1. 迭代器游标可用
                //2. 且
                //3. (toKey==null 为 true) 或者 (迭代器 key 的头几个字节小于等于 toKey 为 true)
                ret = iterator.isValid() && (toKeyBytes.length==0 || TByte.byteArrayCompare(toKeyBytes, iterator.key()) >= 0);
            }
            return ret;
        }

        /**
         * 获取 Key 的值
         * @return Key 的值
         */
        public K key(){
            return (K)unserialize(iterator.key());
        }

        /**
         * 获取 value 的值
         * @return value 的值
         */
        public V value(){
            return (V)unserialize(iterator.value());
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
         * @param valid 是否验证当前迭代可用
         * @return true: 成功, false: 失败
         */
        public boolean directNext(boolean valid) {
            if(count != 0) {
                iterator.next();
            }

            boolean flag = false;

            if(valid) {
                flag = isValid();
            } else {
                flag = iterator.isValid();
            }

            if(flag) {
                count++;
                if(size!=0 && count > size){
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }

        public RocksMapEntry<K,V> nextAndValid(boolean valid) {
            if(directNext(valid)) {
                return getEntry();
            } else {
                return null;
            }
        }

        @Override
        public RocksMapEntry<K,V> next() {
            return nextAndValid(true);
        }

        @Override
        public void remove() {
            try {
                rocksMap.rocksDB.delete(rocksMap.dataColumnFamilyHandle, rocksMap.writeOptions, iterator.key());
            } catch (RocksDBException e) {
                throw new RocksMapException("RocksMapIterator remove failed", e);
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super RocksMapEntry<K, V>> action) {
            throw new UnsupportedOperationException();
        }

        public void close(){
            iterator.close();
        }
    }

    public static class RocksWalRecord {
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

        private RocksWalRecord(long sequence, int type, int columnFamilyId) {
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
         * @param rocksMap RocksMap 对象
         * @param byteBuffer 字节缓冲器
         * @param withSerial 是否进行反序列化
         * @return 日志记录集合
         */
        public static List<RocksWalRecord> parse(RocksMap rocksMap, ByteBuffer byteBuffer, boolean withSerial) {
            return parse(rocksMap, byteBuffer, null, withSerial);
        }

        /**
         * 解析某个序号以后的更新操作记录
         * @param rocksMap RocksMap 对象
         * @param byteBuffer 字节缓冲器
         * @param filter 过滤器,用来过滤可用的操作类型和列族
         * @param withSerial 是否进行反序列化
         * @return 日志记录集合
         */
        public static List<RocksWalRecord> parse(RocksMap rocksMap, ByteBuffer byteBuffer, BiFunction<Integer, Integer, Boolean> filter, boolean withSerial) {
            ByteOrder originByteOrder = byteBuffer.order();
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            if(byteBuffer.remaining() < 13) {
                throw new ParseException("not a correct recorder");
            }
            //序号
            Long sequence = byteBuffer.getLong();

            //本次Wal操作包含键值数
            int recordCount = byteBuffer.getInt();



            List<RocksWalRecord> rocksWalRecords = new ArrayList<RocksWalRecord>();

            for(int count=0;
                count < recordCount && byteBuffer.hasRemaining();
                count++) {

                RocksWalRecord rocksWalRecord = parseOperation(rocksMap, byteBuffer, sequence, filter, withSerial);
                if (rocksWalRecord != null) {
                    rocksWalRecords.add(rocksWalRecord);
                }
            }

            byteBuffer.order(originByteOrder);
            return rocksWalRecords;

        }

        /**
         * 解析某个序号的操作记录
         * @param rocksMap RocksMap 对象
         * @param byteBuffer 字节缓冲器
         * @param sequence 序号
         * @param withSerial 是否进行反序列化
         * @return 日志记录集合
         */
        public static RocksWalRecord parseOperation(RocksMap rocksMap, ByteBuffer byteBuffer, long sequence, boolean withSerial){
            return parseOperation(rocksMap, byteBuffer, sequence, withSerial);
        }

        /**
         * 解析某个序号的操作记录
         * @param rocksMap RocksMap 对象
         * @param byteBuffer 字节缓冲器
         * @param sequence 序号
         * @param filter 过滤器,用来过滤可用的操作类型和列族
         * @param withSerial 是否进行反序列化
         * @return 日志记录集合
         */
        public static RocksWalRecord parseOperation(RocksMap rocksMap, ByteBuffer byteBuffer, long sequence, BiFunction<Integer, Integer, Boolean> filter, boolean withSerial){
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
                    type == TYPE_COLUMNFAMILY_SINGLE_DELETION ||
                    type == TYPE_COLUMNFAMILY_RANGE_DELETION) {
                columnFamilyId = Varint.varintToInt(byteBuffer);
            }


            RocksWalRecord rocksWalRecord = null;
            if (type < TYPE_ELEMENT_COUNT.length) {

                //应用过滤器
                boolean isEnable = filter==null || filter.apply(columnFamilyId, type);

                if(isEnable) {
                    rocksWalRecord = new RocksWalRecord(sequence, type, columnFamilyId);
                }

                for (int i = 0; i < TYPE_ELEMENT_COUNT[type] && byteBuffer.hasRemaining(); i++) {
                    int chunkSize = Varint.varintToInt(byteBuffer);

                    if(isEnable) {
                        byte[] chunkBytes = new byte[chunkSize];
                        byteBuffer.get(chunkBytes);

                        Object chunk = chunkBytes;
                        if (withSerial) {
                            chunk = rocksMap.unserialize(chunkBytes);
                        } else {
                            chunk = chunkBytes;
                        }

                        rocksWalRecord.getChunks().add(chunk);
                    } else {
                        byteBuffer.position(byteBuffer.position() + chunkSize);
                    }
                }
            }

            return rocksWalRecord;
        }
    }

    /**
     * 按批次读取Wal, wal 读取器
     */
    public static class RocksWalReader {
        private Object mark;
        private RocksMap rocksMap;
        private Long lastSequence;
        private int batchSeqsize;
        private List<Integer> columnFamilys;
        private List<Integer> walTypes;

        public RocksWalReader(Object mark, RocksMap rocksMap, int batchSeqsize) {
            this.mark = mark;
            this.rocksMap = rocksMap;
            this.batchSeqsize = batchSeqsize;
            this.rocksMap = rocksMap;

            this.lastSequence = (Long) this.rocksMap.get(mark);
            this.lastSequence = this.lastSequence==null ? 0 : this.lastSequence;

            Logger.debug("Start sequence: " + this.lastSequence);
        }

        public long getLastSequence() {
            return lastSequence;
        }

        public List<Integer> getColumnFamily() {
            return columnFamilys;
        }

        public void setColumnFamily(List<Integer> columnFamilys) {
            this.columnFamilys = columnFamilys;

        }

        public int getBatchSeqsize() {
            return batchSeqsize;
        }

        public List<Integer> getWalTypes() {
            return walTypes;
        }

        public void setWalTypes(List<Integer> walTypes) {
            this.walTypes = walTypes;
        }

        public void processing(RocksWalProcessor rocksWalProcessor){
            //wal 日志同步至数据库
            Long endSequence = rocksMap.getLastSequence();

            //数量控制
            if(lastSequence + batchSeqsize < endSequence){
                endSequence = lastSequence + batchSeqsize;
            }

            try {
                List<RocksWalRecord> rocksWalRecords = rocksMap.getWalBetween(lastSequence, endSequence, (columnFamilyId, type) -> {
                    return (walTypes == null ? true : walTypes.contains(type)) &&
                            (columnFamilys == null ? true : columnFamilys.contains(columnFamilyId));

                }, true);


                boolean processSucc = true;
                if (rocksWalRecords.size() > 0) {
                    //调用处理器
                    processSucc = rocksWalProcessor.process(endSequence, rocksWalRecords);
                }

                if(processSucc) {
                    rocksMap.put(mark, endSequence);
                    lastSequence = endSequence;
                } else {
                    Logger.warnf("process failed, {}->{}", lastSequence, endSequence);
                }

            } catch (RocksMapException ex) {
                if(!ex.getMessage().contains("while stat a file for size")) {
                    throw ex;
                }
            }
        }

        /**
         * 日志处理器
         */
        public static interface RocksWalProcessor {
            public boolean process(Long endSequence, List<RocksWalRecord> rocksWalRecords);
        }
    }
}
