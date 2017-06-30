package org.voovan.tools.complier;

import org.voovan.tools.*;
import org.voovan.tools.reflect.TReflect;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * 类文字命名
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
                    "    public Object execute(Object ... args){\n" +
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
    private Object instance;

    /**
     * 构造函数
     *
     * @param name 命名的名称
     */
    public DynamicFunction(String name, String code) {
        this.name = name;
        this.prepareArgCode = null;
        this.importCode = "";
        this.bodyCode = "";
        this.code = code;
        this.javaCode = "";
        this.clazz = null;
        this.instance = null;
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
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
     * @return
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
        className = name + TString.generateShortUUID();
    }

    /**
     * 生成可调用参数
     */
    private void genPrepareArgCode() {
        StringBuffer stringBuffer = new StringBuffer();
        for (Map.Entry<Integer, List<String>> prepareArg : prepareArgs.entrySet()) {
            int argIndex = prepareArg.getKey();
            String className = prepareArgs.getValue(argIndex, 0);
            String name = prepareArgs.getValue(argIndex, 1);
            stringBuffer.append("        ").append(className).append(" ").append(name).append(" = ")
                    .append("TObject.cast(args[").append(argIndex).append("]);").append(TFile.getLineSeparator());
        }
        this.prepareArgCode = stringBuffer.toString().trim();
    }

    /**
     * 解析用户代码
     */
    public void parseCode() {
        if(code == null){
            throw new NullPointerException("Function code is null.");
        }

        if (!code.contains("return ")) {
            code = code + "\r\n        return null;";
        }
        ByteBufferChannel byteBufferChannel = new ByteBufferChannel();
        byteBufferChannel.writeEnd(ByteBuffer.wrap(code.getBytes()));
        while(true) {
            String lineCode = byteBufferChannel.readLine();
            if(lineCode==null){
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
     * 编译代码
     *
     * @return 返回编译后得到的 Class 对象
     * @throws ClassNotFoundException 反射异常
     */
    public void compileCode() throws ReflectiveOperationException {
        genCode();

        DynamicCompiler compiler = new DynamicCompiler();
        if (compiler.compileCode(this.javaCode)) {
            this.clazz = Class.forName("org.voovan.tools.complier.temporary." + this.getClassName());
            //实例化动态编译的对象使其可悲调用
            this.instance = TReflect.newInstance(clazz);
        } else {
            throw new ReflectiveOperationException("Compile code error.");
        }
    }

    /**
     * 执行
     * @param args 调用参数
     * @param <T> 范型
     * @return 返回的类型
     * @throws ReflectiveOperationException 反射异常
     */
    public <T> T call(Object ... args) throws ReflectiveOperationException {
        if(this.instance == null){
            throw new NullPointerException("Function instance is null.");
        }
        Object result = TReflect.invokeMethod(instance, "execute", new Object[]{args});
        return TObject.cast(result);
    }

}
