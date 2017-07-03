package org.voovan.tools.compiler.function;

import org.voovan.tools.*;
import org.voovan.tools.compiler.DynamicCompiler;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 动态函数管理类
 *      为了安全问题,默认不外部包导入功能,如果需要可以使用 setEnableImportInCode 方法设置
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DynamicFunction {
    private final static String CODE_TEMPLATE = new String(TFile.loadResource("org/voovan/tools/compiler/function/CodeTemplate.txt"));

    //导入类预置
    private List<Class> importClasses;

    //参数预置
    private MultiMap<Integer, Object> args;
    private String argCode;

    //是否支持代码中的导入
    private boolean enableImportInCode;


    //动态编译相关的对象
    private String packageName;
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
     * @param file    脚本文件路径
     * @param charset 脚本文件编码
     * @throws UnsupportedEncodingException
     */
    public DynamicFunction(File file, String charset) throws UnsupportedEncodingException {
        init();
        String fileName = TFile.getFileName(file.getPath());
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        this.name = fileName;
        this.codeFile = file;
        this.fileCharset = charset;
        this.lastFileTimeStamp = file.lastModified();
    }

    /**
     * 初始化
     */
    private void init() {
        this.packageName = "org.voovan.tools.compiler.temporary";
        this.name = null;
        this.argCode = null;
        this.importCode = "";
        this.bodyCode = "";
        this.code = null;
        this.javaCode = "";
        this.clazz = Object.class;
        this.codeFile = null;

        needCompile = true;
        enableImportInCode = false;

        this.importClasses = new ArrayList<Class>();
        this.args = new MultiMap<Integer, Object>();
    }

    /**
     * 获取包名
     *      默认:org.voovan.tools.compiler.temporary
     * @return  包名
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * 设置包名
     *      默认:org.voovan.tools.compiler.temporary
     * @param packageName 包名
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * 获得命名的名称
     *       用于标定这个动态编译的函数
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

    /**
     * 获取是否支持代码中的 import
     * @return
     */
    public boolean isEnableImportInCode() {
        return enableImportInCode;
    }

    /**
     * 设置是否支持代码中 import
     * @param enableImportInCode
     */
    public void enableImportInCode(boolean enableImportInCode) {
        this.enableImportInCode = enableImportInCode;
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
     * @param argClazz  调用参数的类
     * @param name      调用参数的名称
     */
    public void addPrepareArg(int argIndex, Class argClazz, String name) {
        args.putValues(argIndex, argClazz, name);
    }

    /**
     * 移除一个调用参数
     *
     * @param argIndex 调用参数的索引
     */
    public void removePrepareArg(int argIndex) {
        args.remove(argIndex);
    }

    /**
     * 增加预置到处类
     * @param clazz 导入类对象
     */
    public void addImport(Class clazz){
        this.importClasses.add(clazz);
    }

    /**
     * 移除预置导入类
     * @param clazz 导入类对象
     */
    public void removeImport(Class clazz){
        this.importClasses.remove(clazz);
    }

    /**
     *
     * @param index 导入类对象所在的索引
     */
    public void removeImport(int index){
        this.importClasses.remove(index);
    }

    /**
     * 生成预置导入代码
     */
    private void genImports(){
        this.importCode = "";

        for(Class importClass : importClasses) {
            this.importCode = this.importCode + "import " + importClass.getCanonicalName() + ";";
        }

        this.importCode = this.importCode + TFile.getLineSeparator();
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
    private void genArgCode() {
        this.argCode = "";
        for (Map.Entry<Integer, List<Object>> prepareArg : args.entrySet()) {
            int argIndex = prepareArg.getKey();
            Class argClazz = TObject.cast(args.getValue(argIndex, 0));
            String name = TObject.cast(args.getValue(argIndex, 1));
            this.argCode = this.argCode + "        " + argClazz.getCanonicalName() + " " + name +
                    " = TObject.cast(args[" + argIndex + "]);" + TFile.getLineSeparator();
        }
        this.argCode = this.argCode.trim();
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
        ByteBufferChannel byteBufferChannel = new ByteBufferChannel();
        byteBufferChannel.writeEnd(ByteBuffer.wrap(this.code.getBytes()));
        while (true) {
            String lineCode = byteBufferChannel.readLine();
            if (lineCode == null) {
                break;
            }
            if (lineCode.trim().startsWith("import ")) {
                if(enableImportInCode) {
                    this.importCode = importCode + lineCode;
                }
            } else {
                this.bodyCode = bodyCode + lineCode;
            }
        }

        this.bodyCode = TString.indent(this.bodyCode, 8);
        byteBufferChannel.release();
    }

    /**
     * 生成代码
     *
     * @return 生成 java 代码;
     */
    private String genCode() {

        genImports();
        genClassName();
        genArgCode();
        parseCode();

        this.javaCode = TString.tokenReplace(CODE_TEMPLATE, TObject.asMap(
                "PACKAGE", packageName, //包名
                "IMPORT", importCode,   //解析获得
                "CLASSNAME", className, //类名
                "PREPAREARG", argCode,  //参数
                "CODE", bodyCode        //解析获得
        ));

        return this.javaCode;
    }

    /**
     * 编译用户代码
     *
     * @return 返回编译后得到的 Class 对象
     * @throws ClassNotFoundException 反射异常
     */
    public void compileCode() throws ReflectiveOperationException {
        synchronized (this.clazz) {

            if (this.clazz != Object.class && codeFile != null) {
                checkFileChanged();
            }

            if (this.clazz == Object.class || needCompile) {
                genCode();

                DynamicCompiler compiler = new DynamicCompiler();
                if (compiler.compileCode(this.javaCode)) {
                    this.clazz = compiler.getClazz();
                    this.className = this.clazz.getCanonicalName();
                    needCompile = false;
                } else {
                    Logger.simple(code);
                    throw new ReflectiveOperationException("Compile code error.");
                }
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
            compileCode();
            Object result = TReflect.invokeMethod(this.clazz, "execute", new Object[]{args});
            return TObject.cast(result);
        }
    }

}
