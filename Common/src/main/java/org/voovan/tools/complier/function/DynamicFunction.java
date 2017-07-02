package org.voovan.tools.complier.function;

import org.voovan.tools.*;
import org.voovan.tools.complier.DynamicCompiler;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * 动态函数管理类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DynamicFunction {
    private final static String CODE_TEMPLATE =
            "package org.voovan.tools.complier.temporary;\n" +
                    "import org.voovan.tools.TObject;\n" +
                    "{{IMPORT}}\n" +
                    "public class {{CLASSNAME}} {\n" +
                    "    public synchronized static Object execute(Object ... args){\n" +
                    "        {{PREPAREARG}}\n" +
                    "        {{CODE}}\n" +
                    "    }\n" +
                    "}";


    private MultiMap<Integer, String> prepareArgs;
    private String prepareArgCode;

    //动态编译相关的对象
    private String name;
    private String className;
    private String importCode;
    private String bodyCode;
    private String code;
    private String javaCode;
    private Class clazz;


    private File codeFile;
    private String fileCharset;
    private long lastFileTimeStamp;

    private boolean needCompile;


    /**
     * 构造函数
     *
     * @param name 命名的名称
     */
    public DynamicFunction(String name, String code) {
        init();
        this.name = name;
        this.code = code;
    }

    /**
     * 构造函数
     *
     * @param name    命名的名称
     * @param file    脚本文件路径
     * @param charset 脚本文件编码
     * @throws UnsupportedEncodingException
     */
    public DynamicFunction(String name, File file, String charset) throws UnsupportedEncodingException {
        init();
        this.name = name;
        this.codeFile = file;
        this.fileCharset = charset;
        this.lastFileTimeStamp = file.lastModified();
    }

    /**
     * 初始化
     */
    private void init() {
        this.name = null;
        this.prepareArgCode = null;
        this.importCode = "";
        this.bodyCode = "";
        this.code = null;
        this.javaCode = "";
        this.clazz = Object.class;
        this.codeFile = null;
        needCompile = true;
        this.prepareArgs = new MultiMap<Integer, String>();
    }

    /**
     * 获得命名的名称
     *
     * @return 命名的名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置命名的名称
     *
     * @param name 命名的名称
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        if (codeFile != null) {
            try {
                this.code = new String(TFile.loadFile(this.codeFile), this.fileCharset);
            } catch (UnsupportedEncodingException e) {
                Logger.error("Load file " + this.codeFile.getPath() + " error", e);
            }
        }

        return code;
    }

    /**
     * 设置脚本代码
     *
     * @param code 脚本代码
     */
    public void setCode(String code) {
        if (codeFile == null) {
            this.code = code;
            needCompile = true;
        } else {
            throw new RuntimeException("This function used code in file, Can't invoke this method.");
        }
    }

    /**
     * 得到实际编译的类名称
     *
     * @return 实际编译的类名称
     */
    public String getClassName() {
        return className;
    }

    /**
     * 获得编译后的 Class 对象
     *
     * @return 实际编译的类对象
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * 增加一个调用参数
     *
     * @param argIndex  调用参数的索引
     * @param className 调用参数的类
     * @param name      调用参数的名称
     */
    public void addPrepareArg(int argIndex, String className, String name) {
        prepareArgs.putValues(argIndex, className, name);
    }

    /**
     * 移除一个调用参数
     *
     * @param argIndex 调用参数的索引
     */
    public void removePrepareArg(int argIndex) {
        prepareArgs.remove(argIndex);
    }

    /**
     * 生成编译时混淆的类名
     */
    private void genClassName() {
        this.className = name + TString.generateShortUUID();
    }

    /**
     * 生成可调用参数
     */
    private void genPrepareArgCode() {
        this.prepareArgCode = "";
        for (Map.Entry<Integer, List<String>> prepareArg : prepareArgs.entrySet()) {
            int argIndex = prepareArg.getKey();
            String className = prepareArgs.getValue(argIndex, 0);
            String name = prepareArgs.getValue(argIndex, 1);
            this.prepareArgCode = this.prepareArgCode + "        " + className + " " + name +
                    " = TObject.cast(args[" + argIndex + "]);" + TFile.getLineSeparator();
        }
    }

    /**
     * 解析用户代码
     */
    private void parseCode() {
        if (this.codeFile != null) {
            this.code = getCode();
        }

        if (this.code == null) {
            throw new NullPointerException("Function code is null.");
        }

        if (!this.code.contains("return ")) {
            this.code = this.code + "\r\n        return null;";
        }

        this.bodyCode = "";
        this.importCode = "";
        ByteBufferChannel byteBufferChannel = new ByteBufferChannel();
        byteBufferChannel.writeEnd(ByteBuffer.wrap(this.code.getBytes()));
        while (true) {
            String lineCode = byteBufferChannel.readLine();
            if (lineCode == null) {
                break;
            }
            if (lineCode.trim().startsWith("import ")) {
                this.importCode = importCode + lineCode;
            } else {
                this.bodyCode = bodyCode + lineCode;
            }
        }
        byteBufferChannel.release();
    }

    /**
     * 生成代码
     *
     * @return 生成 java 代码;
     */
    private String genCode() {

        genClassName();
        genPrepareArgCode();
        parseCode();

        this.javaCode = TString.tokenReplace(CODE_TEMPLATE, TObject.asMap(
                "IMPORT", importCode,  //解析获得
                "CLASSNAME", className,
                "PREPAREARG", prepareArgCode,
                "CODE", bodyCode  //解析获得
        ));

        return this.javaCode;
    }

    /**
     * 编译用户代码
     *
     * @return 返回编译后得到的 Class 对象
     * @throws ClassNotFoundException 反射异常
     */
    private void compileCode() throws ReflectiveOperationException {
        synchronized (this.clazz) {
            genCode();

            DynamicCompiler compiler = new DynamicCompiler();
            if (compiler.compileCode(this.javaCode)) {
                this.clazz = Class.forName("org.voovan.tools.complier.temporary." + this.getClassName());
                needCompile = false;
            } else {
                Logger.simple(code);
                throw new ReflectiveOperationException("Compile code error.");
            }
        }
    }


    /**
     * 测试文件是否变更
     */
    private void checkFileChanged() {
        if (lastFileTimeStamp != this.codeFile.lastModified()) {
            this.lastFileTimeStamp = this.codeFile.lastModified();
            needCompile = true;
        }

    }

    /**
     * 执行代码
     *
     * @param args 调用参数
     * @param <T>  范型
     * @return 返回的类型
     * @throws ReflectiveOperationException 反射异常
     */
    public <T> T call(Object... args) throws ReflectiveOperationException {
        synchronized (this.clazz) {

            if (this.clazz != Object.class && codeFile != null) {
                checkFileChanged();
            }

            if (this.clazz == Object.class || needCompile) {
                compileCode();
            }

            Object result = TReflect.invokeMethod(this.clazz, "execute", new Object[]{args});
            return TObject.cast(result);
        }
    }

}
