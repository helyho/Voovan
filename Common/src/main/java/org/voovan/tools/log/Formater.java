package org.voovan.tools.log;

import org.voovan.Global;
import org.voovan.tools.*;
import org.voovan.tools.hashwheeltimer.HashWheelTask;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
    private String template;
    private LoggerThread loggerThread;
    private List<String> logLevel;
    private String dateStamp;
    private int maxLineLength = -1;
    private String lineHead;
    private String lineTail;

    private static String DATE = TDateTime.now("yyyyMMdd");

    static{
        Global.getHashWheelTimer().addTask(new HashWheelTask() {
            @Override
            public void run() {
                DATE = TDateTime.now("yyyyMMdd");
            }
        }, 1);
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

        logLevel = new Vector<String>();
        logLevel.addAll(TObject.asList(LoggerStatic.getLogConfig("LogLevel", LoggerStatic.LOG_LEVEL).split(",")));
        this.dateStamp = DATE;

        final Formater finalFormater = this;
        Global.getHashWheelTimer().addTask(new HashWheelTask() {
            @Override
            public void run() {
                if(Logger.isEnable()) {
                    //压缩历史日志文件
                    packLogFile();

                    //如果日志发生变化则产生新的文件
                    if (!finalFormater.getDateStamp().equals(DATE)) {
                        loggerThread.setOutputStreams(getOutputStreams());
                        finalFormater.setDateStamp(DATE);
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
        return logLevel;
    }

    /**
     * 获得当前栈元素信息
     * @return 栈信息元素
     */
    public static StackTraceElement currentStackLine() {
        StackTraceElement[] stackTraceElements = TEnv.getStackElements();
        return stackTraceElements[8];
    }

    /**
     * 获取当前线程名称
     * @return 当前线程名
     */
    private static String currentThreadName() {
        Thread currentThread = Thread.currentThread();
        return currentThread.getName();
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
        tokens.put("I", message.getMessage());

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
            tokens.put("T", currentThreadName());                                            //线程
        }

        if(LoggerStatic.HAS_DATE) {
            tokens.put("D", TDateTime.now("yyyy-MM-dd HH:mm:ss:SS z"));                      //当前时间
        }

        if(LoggerStatic.HAS_RUNTIME) {
            tokens.put("R", TDateTime.formatElapsedSecs(TPerformance.getRuningTime()/1000)); //系统运行时间
        }

        if(LoggerStatic.HAS_STACK) {
            StackTraceElement stackTraceElement = currentStackLine();
            tokens.put("SI", stackTraceElement.toString());                                 //堆栈信息
            tokens.put("L", Integer.toString((stackTraceElement.getLineNumber())));			//行号
            tokens.put("M", stackTraceElement.getMethodName());								//方法名
            tokens.put("F", stackTraceElement.getFileName());								//源文件名
            tokens.put("C", stackTraceElement.getClassName());								//类名
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
        //消息缩进
        fillTokens(message);
        return TString.tokenReplace(message.getMessage(), message.getTokens());
    }

    /**
     * 写入消息对象,在进行格式化后的写入
     * @param message 消息对象
     */
    public void writeFormatedLog(Message message) {
        if("SIMPLE".equals(message.getLevel())){
            writeLog(simpleFormat(message)+"\r\n");
        }else{
            writeLog(format(message));
        }
    }

    /**
     * 写入消息
     * @param msg 消息字符串
     */
    public synchronized void writeLog(String msg) {
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
            //暂停日志输出
            try {
                if (loggerThread.pause()) {
                    try {
                        TFile.moveFile(logFile, tmpLogFile);

                        //开启独立线程进行文件压缩
                        Global.getThreadPool().execute(()->{
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
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //重置文件输出流
                    loggerThread.setOutputStreams(getOutputStreams());
                }
            } finally {
                //恢复日志输出
                loggerThread.unpause();
            }
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
        }else{
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
    protected static OutputStream[] getOutputStreams(){
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
