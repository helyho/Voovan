package org.voovan;

import org.voovan.tools.TObject;
import org.voovan.tools.TProperties;
import org.voovan.tools.UniqueId;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;
import org.voovan.tools.task.TaskManager;
import org.voovan.tools.threadpool.ThreadPool;

import java.nio.charset.Charset;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 全局对象
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Global {

    public static final String STR_EQUAL     = "=";
    public static final String STR_QUOTE     = "\"";
    public static final String STR_SPACE     = " ";
    public static final String STR_SLASH     = "\\";
    public static final String STR_BACKSLASH = "/";
    public static final String STR_QUESTION  = "\\?";
    public static final String STR_CR        = "\r";
    public static final String STR_LF        = "\n";
    public static final String STR_COLON     = ":";
    public static final String STR_S_QUOTE   = "'";
    public static final String STR_POINT     = ".";
    public static final String STR_LC_BRACES = "{";
    public static final String STR_RC_BRACES = "}";
    public static final String STR_LS_BRACES = "[";
    public static final String STR_RS_BRACES = "]";
    public static final String STR_COMMA     = ",";
    public static final String STR_STAR      = "*";
    public static final String STR_SHAPE      = "#";
    public static final String STR_AT        = "@";
    public static final String STR_EOF       = "\0";

    public static final char CHAR_EQUAL     = '=';
    public static final char CHAR_QUOTE     = '\"';
    public static final char CHAR_SPACE     = ' ';
    public static final char CHAR_SLASH     = '\\';
    public static final char CHAR_BACKSLASH = '/';
    public static final char CHAR_QUESTION  = '?';
    public static final char CHAR_CR        = '\r';
    public static final char CHAR_LF        = '\n';
    public static final char CHAR_COLON     = ':';
    public static final char CHAR_S_QUOTE   = '\'';
    public static final char CHAR_LC_BRACES = '{';
    public static final char CHAR_RC_BRACES = '}';
    public static final char CHAR_LS_BRACES = '[';
    public static final char CHAR_RS_BRACES = ']';
    public static final char CHAR_COMMA     = ',';
    public static final char CHAR_POINT     = '.';
    public static final char CHAR_STAR      = '*';
    public static final char CHAR_SHAPE     = '#';
    public static final char CHAR_AT        = '@';
    public static final char CHAR_EOF       = '\0';



    public static final byte BYTE_EQUAL         = 61;           // '='
    public static final byte BYTE_SLASH         = 92;           // "\\"
    public static final byte BYTE_BACKSLASH     = 47;           // '/'
    public static final byte BYTE_SPACE         = 32;           // ' '
    public static final byte BYTE_QUESTION      = 63;           // '?'
    public static final byte BYTE_CR            = 13;           // '/r'
    public static final byte BYTE_LF            = 10;           // '/n'
    public static final byte BYTE_COLON         = 58;           // ':'
    public static final byte BYTE_QUOTE         = 34;           // '"'
    public static final byte BYTE_S_QUOTE       = 39;           // '\''
    public static final byte BYTE_POINT         = 46;           // '.'
    public static final byte BYTE_LC_BRACES     = 123;          // '{''
    public static final byte BYTE_RC_BRACES     = 125;          // '}''
    public static final byte BYTE_LS_BRACES     = 91;           // '[''
    public static final byte BYTE_RS_BRACES     = 93;           // ']''
    public static final byte BYTE_STAR          = 42;           // '*'
    public static final byte BYTE_SHAPE        = 35;           // '#'
    public static final byte BYTE_AT           = 64;           // '@'
    public static final byte BYTE_EOF           = 0;            // '\0'

    public static final String EMPTY_STRING = "";

    public static final Charset CS_ASCII = Charset.forName("US-ASCII");
    public static final Charset CS_UTF_8 = Charset.forName("UTF-8");
    public static final Charset CS_GBK = Charset.forName("GBK");

    public static String NAME = "Voovan";

    public static volatile Boolean NO_HEAP_MANUAL_RELEASE;
    public static volatile String REMOTE_CLASS_SOURCE;


    public static UniqueId UNIQUE_ID = new UniqueId((int) (Math.random()*1024));

    static {
        if(NO_HEAP_MANUAL_RELEASE == null) {
            boolean value = false;
            value = TProperties.getBoolean("framework", "NoHeapManualRelease");
            REMOTE_CLASS_SOURCE = TProperties.getString("framework", "RemoteClassSource");
            NO_HEAP_MANUAL_RELEASE = TObject.nullDefault(value, true);
            System.out.println("[SYSTEM] NoHeap Manual Release: " + NO_HEAP_MANUAL_RELEASE);
        }
    }

    /**
     * 非对内存是否采用手工释放
     * @return true: 收工释放, false: JVM自动释放
     */
    private static boolean getNoHeapManualRelease() {
        return NO_HEAP_MANUAL_RELEASE;
    }

    private enum ThreadPoolEnum {
        THREAD_POOL;

        private ThreadPoolExecutor threadPoolExecutor;

        ThreadPoolEnum(){
            threadPoolExecutor = ThreadPool.getNewThreadPool("Default");
        }

        public ThreadPoolExecutor getValue(){
            return threadPoolExecutor;
        }
    }

    /**
     * 返回公用线程池
     * @return 公用线程池
     */
    public static ThreadPoolExecutor getThreadPool(){
        return ThreadPoolEnum.THREAD_POOL.getValue();
    }


    private enum HashTimeWheelEnum {
        HASHWHEEL;

        private HashWheelTimer hashWheelTimer;
        HashTimeWheelEnum (){
            hashWheelTimer = new HashWheelTimer(60, 1000);
            hashWheelTimer.rotate();
        }

        public HashWheelTimer getValue(){
            return hashWheelTimer;
        }
    }

    /**
     * 获取一个全局的秒定时器
     *      60个槽位, 每个槽位步长1s
     * @return HashWheelTimer对象
     */
    public static HashWheelTimer getHashWheelTimer(){
        return HashTimeWheelEnum.HASHWHEEL.getValue();
    }


    private enum TaskManagerEnum {
        TASK_MANAGER;

        private TaskManager taskManager;
        TaskManagerEnum(){
            taskManager = new TaskManager();
            taskManager.scanTask();
        }
        public TaskManager getValue(){
            return taskManager;
        }
    }

    /**
     * 获取一个全局的秒定时器
     *      60个槽位, 每个槽位步长1ms
     * @return HashWheelTimer对象
     */
    public static TaskManager getTaskManager(){

        return TaskManagerEnum.TASK_MANAGER.getValue();
    }

    /**
     * 获取当前 Voovan 版本号
     * @return Voovan 版本号
     */
    public static String getVersion(){
        return "4.0.10";
    }
}
