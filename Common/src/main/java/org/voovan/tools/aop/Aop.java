package org.voovan.tools.aop;

import org.voovan.tools.*;
import org.voovan.tools.aop.annotation.After;
import org.voovan.tools.aop.annotation.Around;
import org.voovan.tools.aop.annotation.Before;
import org.voovan.tools.aop.annotation.Exception;
import org.voovan.tools.log.Logger;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.function.Predicate;

/**
 * 切面主类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class Aop {
    private static boolean IS_AOP_ON = false;
    private static Instrumentation instrumentation;

    /**
     * 构造函数
     * @param scanPackage 扫描的包路径
     * @throws Exception IO 异常
     */
    public static void init(String scanPackage) throws java.lang.Exception {
        init(null, scanPackage);
    }

    /**
     * 构造函数
     * @param scanPackage 扫描的包路径
     * @param agentJarPath AgentJar 文件
     * @throws IOException IO 异常
     * @throws AttachNotSupportedException 附加指定进程失败
     * @throws AgentLoadException Agent 加载异常
     * @throws AgentInitializationException Agent 初始化异常
     * @throws ClassNotFoundException 类找不到异常
     */
    public static void init(String agentJarPath, String scanPackage) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, ClassNotFoundException {
        if(scanPackage==null){
            return;
        }

        Aop.scanAopClass(scanPackage);
        IS_AOP_ON = true;
        instrumentation = TEnv.agentAttach(agentJarPath);
        if(instrumentation!=null) {
            instrumentation.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String classPath, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    String className = classPath.replaceAll(File.separator, ".");
                    if(!isSystemType(className)) {
                        return Inject(className, classfileBuffer);
                    } else {
                        return classfileBuffer;
                    }
                }
            });
            Logger.info("[AOP] Enable aop success ");
        } else {
            Logger.error("[AOP] Enable aop failed ");
        }
    }

    /**
     * 切面代码注入
     * @param className 注入的类全限定名
     * @param classfileBuffer 注入的类的字节码
     * @return 注入代码后的类的字节码
     */
    public static byte[] Inject(String className, byte[] classfileBuffer){

        CtClass ctClass = null;
        try {
            if(className.startsWith("sun") || className.startsWith("com.sun") || className.startsWith("com.oracle")){
                return classfileBuffer;
            }

            try {
                ctClass = AopUtils.CLASSPOOL.get(className);
            } catch (NotFoundException e){
                return classfileBuffer;
            }
            //撤销上次的修改
            ctClass.detach();

            for(CtMethod ctMethod : ctClass.getMethods()){

                //遍历可用于当前方法注入的切面点
                List<CutPointInfo> avaliableCutPointInfo = (List<CutPointInfo>) CollectionSearch.newInstance(AopUtils.CUT_POINTINFO_LIST)
                        .setParallelStream(false)
                        .addCondition(new Predicate() {
                            @Override
                            public boolean test(Object o) {
                                CutPointInfo cutPointInfo = (CutPointInfo)o;

                                String cutPointClassName = cutPointInfo.getClazzName().replaceAll("\\.", "\\\\.");
                                cutPointClassName = cutPointClassName.replaceAll("\\*", ".*?");
                                if(TString.searchByRegex(className, cutPointClassName).length > 0){
                                    return true;
                                } else {
                                    return false;
                                }
                            }
                        }) //比对类名称
                        .addCondition(new Predicate() {
                            @Override
                            public boolean test(Object o) {
                                CutPointInfo cutPointInfo = (CutPointInfo)o;

                                if(cutPointInfo.getMethodName().equals("*")){
                                    return true;
                                }

                                String innerMethodName = cutPointInfo.getMethodName().replaceAll("\\*", ".*?");

                                if(TString.searchByRegex(ctMethod.getName(), innerMethodName).length > 0){
                                    return true;
                                } else {
                                    return false;
                                }
                            }
                        }) //比对方法名称
                        .addCondition(new Predicate() {
                            @Override
                            public boolean test(Object o) {
                                boolean parameterTypeEqual = false;
                                boolean resultTypeEqual = false;

                                CutPointInfo cutPointInfo = (CutPointInfo)o;
                                try {

                                    //是否匹配任意形式的参数
                                    if(cutPointInfo.getParameterTypes().length>0 && cutPointInfo.getParameterTypes()[0].equals("..")){
                                        parameterTypeEqual = true;
                                    }

                                    //比对参数数量是否相等, 接着逐个参数进行比对
                                    else if (cutPointInfo.getParameterTypes().length == ctMethod.getParameterTypes().length) {
                                        for (int x = 0; x < cutPointInfo.getParameterTypes().length; x++) {
                                            CtClass methodParameterCtClass = ctMethod.getParameterTypes()[x];
                                            if (!methodParameterCtClass.getName().equals(cutPointInfo.getParameterTypes()[x])) {
                                                return false;
                                            }
                                        }

                                        parameterTypeEqual = true;
                                    }

                                    //返回类型为通用匹配, 或者相同的时候
                                    if("*".equals(cutPointInfo.getResultType()) || ctMethod.getReturnType().getName().equals(cutPointInfo.getResultType())){
                                        resultTypeEqual = true;
                                    }

                                    return parameterTypeEqual && resultTypeEqual;
                                } catch (java.lang.Exception e){
                                    return false;
                                }
                            }
                        }) //比对方法参数, 返回值
                        .search();

                //切面代码注入
                for(CutPointInfo cutPointInfo : avaliableCutPointInfo){
                    try {

                        //Before 方法
                        if (cutPointInfo.getType() == -1) {
                            ctMethod.insertBefore("{" + cutPointInfo.getMethod().getDeclaringClass().getName() + "." + cutPointInfo.getMethod().getName() + "(new org.voovan.tools.aop.InterceptInfo($class, \""+ctMethod.getName()+"\", this, $sig, $args, null, null, null));}");
                            System.out.println("[AOP] CutPoint before: " + cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName());
                        }

                        //After 方法
                        if (cutPointInfo.getType() == 1) {
                            ctMethod.insertAfter("{"+ cutPointInfo.getMethod().getDeclaringClass().getName() + "." + cutPointInfo.getMethod().getName() + "(new org.voovan.tools.aop.InterceptInfo($class, \""+ctMethod.getName()+"\", this, $sig, $args, $type, ($w)$_, null));}");
                            System.out.println("[AOP] CutPoint after: " + cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName());
                        }

                        //Catch 方法
                        if (cutPointInfo.getType() == 2) {
                            CtClass exceptionType = ClassPool.getDefault().get("java.lang.Exception");
                            ctMethod.addCatch("{"+ cutPointInfo.getMethod().getDeclaringClass().getName() + "." + cutPointInfo.getMethod().getName() + "(new org.voovan.tools.aop.InterceptInfo($class, \""+ctMethod.getName()+"\", this, $sig, $args, null, null, $e));  throw $e;}", exceptionType);
                            System.out.println("[AOP] CutPoint after: " + cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName());
                        }

                        //Around 方法
                        if (cutPointInfo.getType() == 3){
                            String methodName = ctMethod.getName();
                            CtMethod ctNewMethod = CtNewMethod.copy(ctMethod, ctClass, null);
                            ctNewMethod.setName(methodName);
                            ctMethod.setName(ctMethod.getName()+"$origin");

                            //获取原函数的注解
                            AnnotationsAttribute attribute = (AnnotationsAttribute)ctMethod.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);
                            ctMethod.getMethodInfo().removeAttribute(attribute.getName());

                            //迁移原函数的注解到新函数
                            ctNewMethod.getMethodInfo().addAttribute(attribute);

                            ctClass.addMethod(ctNewMethod);
                            ctNewMethod.setBody("{ return "+ cutPointInfo.getMethod().getDeclaringClass().getName() + "." + cutPointInfo.getMethod().getName() + "(new org.voovan.tools.aop.InterceptInfo($class, \""+ctNewMethod.getName()+"\", this, $sig, $args, null, null, null));}");
                        }
                    } catch (CannotCompileException e) {
                        e.printStackTrace();
                    }
                }
            }


            ctClass.debugDump = "./dump";
            classfileBuffer = ctClass.toBytecode();

        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }

        return classfileBuffer;
    }

    /**
     * 获得装箱类型
     * @param primitiveType 原始类型
     * @return 装箱类型
     */
    public static String getPackageType(String primitiveType){
        switch (primitiveType){
            case "int": return "java.lang.Integer";
            case "byte": return "java.lang.Byte";
            case "short": return "java.lang.Short";
            case "long": return "java.lang.Long";
            case "float": return "java.lang.Float";
            case "double": return "java.lang.Double";
            case "char": return "java.lang.Character";
            case "boolean": return "java.lang.Boolean";
            default : return null;
        }
    }

    private static List<String> systemPackages = TObject.asList("java.","sun.","javax.","com.sun","com.oracle");

    /**
     * 判读是否是 JDK 中定义的类(java包下的所有类)
     * @param className Class 对象完全限定名
     * @return true: 是JDK 中定义的类, false:非JDK 中定义的类
     */
    public static boolean isSystemType(String className){
        if( className.indexOf(".")==-1){
            return true;
        }

        //排除的包中的 class 不加载
        for(String systemPackage : systemPackages){
            if(className.startsWith(systemPackage)){
                return true;
            }
        }

        return false;
    }

    /**
     * Javassist 扫描所有的切面注入点
     * @param scanPackage 扫描的包路径
     * @throws IOException IO 异常
     * @throws ClassNotFoundException 类未找到异常
     */
    public static void scanAopClass(String scanPackage) throws IOException, ClassNotFoundException {

        List<CtClass> aopClasses = AopUtils.searchClassInJavassist(scanPackage, new Class[]{org.voovan.tools.aop.annotation.Aop.class});

        for(CtClass clazz : aopClasses){
            CtMethod[] methods = clazz.getMethods();
            for(CtMethod method : methods){
                Before onBefore = (Before) method.getAnnotation(Before.class);
                After onAfter = (After)method.getAnnotation(After.class);
                Exception onException = (Exception)method.getAnnotation(Exception.class);
                Around onAround = (Around)method.getAnnotation(Around.class);

                if(onBefore!=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onBefore.value());
                    cutPointInfo.setType(-1);
                    cutPointInfo.setMethod(method);
                    AopUtils.CUT_POINTINFO_LIST.add(cutPointInfo);
                }

                if(onAfter!=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onAfter.value());
                    cutPointInfo.setType(1);
                    cutPointInfo.setMethod(method);
                    AopUtils.CUT_POINTINFO_LIST.add(cutPointInfo);
                }

                if(onException !=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onException.value());
                    cutPointInfo.setType(2);
                    cutPointInfo.setMethod(method);
                    AopUtils.CUT_POINTINFO_LIST.add(cutPointInfo);
                }

                if(onAround !=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onAround.value());
                    cutPointInfo.setType(3);
                    cutPointInfo.setMethod(method);
                    AopUtils.CUT_POINTINFO_LIST.add(cutPointInfo);
                }
            }
        }
    }
}
