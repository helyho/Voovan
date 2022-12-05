package org.voovan.tools.log;

import org.voovan.Global;
import org.voovan.tools.*;
import org.voovan.tools.hashwheeltimer.HashWheelTask;

import java.io.*;
import java.util.*;

/**
 *	格式化日志信息并输出<br>
 *
 *使用包括特殊的定义{} <br>
 *{s}:  一个空格<br>
 *{t}:  制表符<br>
 *{n}:  换行<br>
 *================================<br>
 *{I}:  消息内容,即要展示的日志内容<br>
 *{F}:  源码文件名<br>
 *{SI}: 栈信息输出<br>
 *{L}:  当前代码的行号<br>
 *{M}:  当前代码的方法名<br>
 *{C}:  当前代码的类名称<br>
 *{T}:  当前线程名<br>
 *{D}:  当前系统时间<br>
 *{R}:  从启动到当前代码执行的事件<br>
 * ================================
 * {F?} 前景颜色: ?为0-7,分别为: 0=黑,1=红,2=绿,3=黄,4=蓝,5=紫,6=青,7=白<br>
 * {B?} 背景颜色: ?为0-7,分别为: 0=黑,1=红,2=绿,3=黄,4=蓝,5=紫,6=青,7=白<br>
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Formater {
    public static int ALL;
    public static int INFO;
    public static int FRAMEWORK;
    public static int SQL;
    public static int DEBUG;
    public static int TRACE;
    public static int WARN;
    public static int ERROR;
    public static int FATAL;
    public static int SIMPLE;

    private static String DATE = TDateTime.now("yyyyMMdd");

    public static List<String> LOG_LEVEL = getLogLevels();

    static {
        if(LOG_LEVEL.contains("ALL")) {
            LOG_LEVEL = TObject.asList("ALL", "INFO", "FRAMEWORK", "SQL", "DEBUG", "TRACE", "WARN", "ERROR", "FATAL", "SIMPLE");
        }

        ALL         = LOG_LEVEL.indexOf("ALL");
        INFO        = LOG_LEVEL.indexOf("INFO");
        FRAMEWORK   = LOG_LEVEL.indexOf("FRAMEWORK");
        SQL         = LOG_LEVEL.indexOf("SQL");
        DEBUG       = LOG_LEVEL.indexOf("DEBUG");
        TRACE       = LOG_LEVEL.indexOf("TRACE");
        WARN        = LOG_LEVEL.indexOf("WARN");
        ERROR       = LOG_LEVEL.indexOf("ERROR");
        FATAL       = LOG_LEVEL.indexOf("FATAL");
        SIMPLE      = LOG_LEVEL.indexOf("SIMPLE");

        Global.getHashWheelTimer().addTask(new HashWheelTask() {
            @Override
            public void run() {
                DATE = TDateTime.now("yyyyMMdd");
            }
        }, 1);
    }

    public static FastThreadLocal<List<String>>  THREAD_LOG_LEVEL = FastThreadLocal.withInitial(()-> {
        return getLogLevels();
    });

    private String template;
    private LoggerThread loggerThread;
    private String dateStamp;
    private int maxLineLength = -1;
    private String lineHead;
    private String lineTail;


    public static List<String> getLogLevels() {
        if(LOG_LEVEL==null) {
            LOG_LEVEL = TObject.asList(LoggerStatic.getLogConfig("LogLevel", LoggerStatic.LOG_LEVEL).split(","));
        }
        return LOG_LEVEL;
    }

    protected String getDateStamp() {
        return dateStamp;
    }

    protected void setDateStamp(String dateStamp) {
        this.dateStamp = dateStamp;
    }

    /**
     * 构造函数
     * @param template 模板
     */
    public Formater(String template) {
        this.template = template;
        this.dateStamp = DATE;

        final Formater finalFormater = this;
        Global.getHashWheelTimer().addTask(new HashWheelTask() {
            @Override
            public void run() {
                if(Logger.isEnable()) {
                    if(loggerThread == null) {
                        return;
                    }

                    try {
                        if (loggerThread.pause()) {
                            if (finalFormater == null || finalFormater.loggerThread == null) {
                                cancel();
                            }

                            //压缩历史日志文件
                            packLogFile();


                            //如果日志发生变化则产生新的文件
                            if (!finalFormater.getDateStamp().equals(DATE)) {
                                finalFormater.loggerThread.setOutputStreams(getOutputStreams());
                                finalFormater.setDateStamp(DATE);
                            }
                        }
                    } finally {
                        //恢复日志输出
                        loggerThread.unpause();
                    }
                }
            }
        }, 1);
    }

    /**
     * 获取日志记录级别信息
     * @return 获取日志记录级别信息
     */
    public List<String> getLogLevel() {
        return THREAD_LOG_LEVEL.get();
    }

    private int realLength(String str){
        return str.replaceAll("\\{\\{n\\}\\}","").replaceAll("\\{\\{.*\\}\\}"," ").replaceAll("\033\\[\\d{2}m", "").length();
    }

    /**
     * 消息缩进
     * @param message 消息对象
     * @return 随进后的消息
     */
    private String lineFormat(Message message){
        return TString.tokenReplace(template, message.getTokens()) + TFile.getLineSeparator();
    }

    /**
     * 构造消息格式化 Token
     * @param message 消息对象
     */
    public void fillTokens(Message message){
        Map<String, String> tokens = new HashMap<String, String>();
        message.setTokens(tokens);

        //Message和栈信息公用
        tokens.put("t", "\t");
        tokens.put("s", " ");
        tokens.put("n", TFile.getLineSeparator());
        tokens.put("I", message.getMessage().toString());

        if(LoggerStatic.HAS_COLOR) {

            if (!TEnv.OS_NAME.toUpperCase().contains("WINDOWS")) {
                tokens.put("F0", "\033[30m");
                tokens.put("F1", "\033[31m");
                tokens.put("F2", "\033[32m");
                tokens.put("F3", "\033[33m");
                tokens.put("F4", "\033[34m");
                tokens.put("F5", "\033[35m");
                tokens.put("F6", "\033[36m");
                tokens.put("F7", "\033[37m");
                tokens.put("FD", "\033[39m");

                tokens.put("B0", "\033[40m");
                tokens.put("B1", "\033[41m");
                tokens.put("B2", "\033[42m");
                tokens.put("B3", "\033[43m");
                tokens.put("B4", "\033[44m");
                tokens.put("B5", "\033[45m");
                tokens.put("B6", "\033[46m");
                tokens.put("B7", "\033[47m");
                tokens.put("BD", "\033[49m");
            } else {
                tokens.put("F0", "");
                tokens.put("F1", "");
                tokens.put("F2", "");
                tokens.put("F3", "");
                tokens.put("F4", "");
                tokens.put("F5", "");
                tokens.put("F6", "");
                tokens.put("F7", "");
                tokens.put("FD", "");

                tokens.put("B0", "");
                tokens.put("B1", "");
                tokens.put("B2", "");
                tokens.put("B3", "");
                tokens.put("B4", "");
                tokens.put("B5", "");
                tokens.put("B6", "");
                tokens.put("B7", "");
                tokens.put("BD", "");
            }
        }

        if(LoggerStatic.HAS_LEVEL) {
            tokens.put("P", TObject.nullDefault(message.getLevel(), "INFO"));                //信息级别
        }

        if(LoggerStatic.HAS_THREAD) {
            tokens.put("T", message.getThread().getName());                                            //线程
        }

        if(LoggerStatic.HAS_DATE) {
            tokens.put("D", TDateTime.format(message.getTimestamp(), "yyyy-MM-dd HH:mm:ss:SS z"));                      //当前时间
        }

        if(LoggerStatic.HAS_RUNTIME) {
            tokens.put("R", TDateTime.formatElapsedSecs(message.getRunTime())); //系统运行时间
        }

        if(LoggerStatic.HAS_STACK) {
            StackTraceElement stackTraceElement = message.getStackTraceElement();
            tokens.put("SI", stackTraceElement.toString());                                 //堆栈信息
            tokens.put("L", Integer.toString((stackTraceElement.getLineNumber())));			//行号
            tokens.put("M", stackTraceElement.getMethodName());								//方法名
            tokens.put("F", stackTraceElement.getFileName());								//源文件名

            String className = stackTraceElement.getClassName();
            if(LoggerStatic.SHORT_PACKAGE_NAME) {
                TEnv.shortClassName(className);
            }
            tokens.put("C", className);								//类名
        }
    }

    /**
     * 格式化消息
     * @param message 消息对象
     * @return 格式化后的消息
     */
    public String format(Message message) {
        fillTokens(message);
        return lineFormat(message);
    }

    /**
     * 简单格式化
     * @param message 消息对象
     * @return 格式化后的消息
     */
    public String simpleFormat(Message message){
        fillTokens(message);
        return TString.tokenReplace(message.getMessage().toString(), message.getTokens());
    }

    /**
     * 写入消息
     * @param msg 消息字符串
     */
    public synchronized void writeLog(Message msg) {
        if(Logger.isEnable()){
            if (loggerThread == null || loggerThread.isFinished()) {
                this.loggerThread = LoggerThread.start(getOutputStreams());
            }

            loggerThread.addLogMessage(msg);
        }
    }

    /**
     * 压缩历史日志文件
     */
    private void packLogFile(){
        long packSize = (long) (Double.valueOf(LoggerStatic.getLogConfig("PackSize", "1024")) * 1024L * 1024L);
        final String logFilePath = getFormatedLogFilePath();
        final String tmpFilePath = TFile.getFileDirectory(logFilePath) + ".tmpPackLog";

        File logFile = new File(logFilePath);
        File tmpLogFile = new File(tmpFilePath);

        if(packSize > 0 && logFile.length() > packSize){
            try {
                TFile.moveFile(logFile, tmpLogFile);

                //开启独立线程进行文件压缩
                Thread packThread = new Thread(()->{
                    String logFileExtendName = TFile.getFileExtension(logFilePath);
                    String innerLogFilePath = logFilePath.replace("." + logFileExtendName, "");
                    try {
                        File packFile = new File(innerLogFilePath + "." + TDateTime.now("HHmmss") + "." + logFileExtendName + ".gz");
                        TZip.encodeGZip(tmpLogFile, packFile);
                        TFile.deleteFile(tmpLogFile);
                    } catch (Exception e) {
                        System.out.println("[ERROR] Pack log file " + innerLogFilePath + "error: ");
                        e.printStackTrace();
                    }
                }, "LogPackThread");

                packThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //重置文件输出流
            loggerThread.setOutputStreams(getOutputStreams());
        }
    }

    /**
     * 获取格式化后的日志文件路径
     * @return 返回日志文件名
     */
    public static String getFormatedLogFilePath(){
        String filePath = "";
        String logFile = LoggerStatic.getLogConfig("LogFile", LoggerStatic.LOG_FILE);
        if(logFile!=null) {
            Map<String, String> tokens = new HashMap<String, String>();
            tokens.put("D", DATE);
            tokens.put("WorkDir", TFile.getContextPath());
            filePath = TString.tokenReplace(logFile, tokens);
            String fileDirectory = filePath.substring(0, filePath.lastIndexOf(File.separator));
            File loggerFile = new File(fileDirectory);
            if (!loggerFile.exists()) {
                if(!loggerFile.mkdirs()){
                    System.out.println("Logger file directory error!");
                }
            }
        } else {
            filePath = null;
        }
        return filePath;
    }

    /**
     * 获得一个实例
     * @return 新的实例
     */
    public static Formater newInstance() {
        return new Formater(LoggerStatic.LOG_TEMPLATE);
    }

    /**
     * 获取输出流
     * @return 输出流数组
     */
    protected synchronized static OutputStream[] getOutputStreams(){
        String[] LogTypes = LoggerStatic.getLogConfig("LogType", LoggerStatic.LOG_TYPE).split(",");
        String logFilePath = getFormatedLogFilePath();

        OutputStream[] outputStreams = new OutputStream[LogTypes.length];
        for (int i = 0; i < LogTypes.length; i++) {
            String logType = LogTypes[i].trim();
            switch (logType) {
                case "STDOUT":
                    outputStreams[i] = System.out;
                    break;
                case "STDERR":
                    outputStreams[i] = System.err;
                    break;
                case "FILE":
                    try {
                        outputStreams[i] = new FileOutputStream(logFilePath,true);
                    } catch (FileNotFoundException e) {
                        System.out.println("log file: ["+logFilePath+"] is not found.\r\n");
                    }
                    break;
                default:
                    break;
            }
        }

        return outputStreams;
    }
}
