package org.voovan.tools.aop;

import org.voovan.tools.*;
import org.voovan.tools.aop.annotation.After;
import org.voovan.tools.aop.annotation.Before;
import org.voovan.tools.log.Logger;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import javassist.*;

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
     * @throws IOException IO 异常
     * @throws AttachNotSupportedException 附加指定进程失败
     * @throws AgentLoadException Agent 加载异常
     * @throws AgentInitializationException Agent 初始化异常
     */
    public static void init(String scanPackage) throws Exception {
        init(null, scanPackage);
    }

    /**
     * 构造函数
     * @param agentJarPath AgentJar 文件
     * @throws IOException IO 异常
     * @throws AttachNotSupportedException 附加指定进程失败
     * @throws AgentLoadException Agent 加载异常
     * @throws AgentInitializationException Agent 初始化异常
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
                    return Inject(className, classfileBuffer);
                }
            });
            Logger.error("[AOP] Enable aop success ");
        } else {
            Logger.error("[AOP] Enable aop failed ");
        }
    }

    /**
     * 切面代码注入
     * @param className 注入的类全限定名
     * @param classfileBuffer 注入的类的字节码
     * @return
     */
    public static byte[] Inject(String className, byte[] classfileBuffer){

        CtClass ctClass = null;
        try {
            if(className.startsWith("sun") || className.startsWith("com.sun") || className.startsWith("com.oracle")){
                return classfileBuffer;
            }
            ctClass = AopUtils.CLASSPOOL.get(className);
            //撤销上次的修改
            ctClass.detach();

            for(CtMethod ctMethod : ctClass.getMethods()){

                //遍历可用于当前方法注入的切面点
                List<CutPointInfo> avaliableCutPointInfo = (List<CutPointInfo>) CollectionSearch.newInstance(AopUtils.CUT_POINTINFO_LIST)
                        .setParallelStream(false)
                        .addCondition("clazzName", className) //比对类名称
                        .addCondition("methodName", ctMethod.getName()) //比对方法名称
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
                                } catch (Exception e){
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
                            ctMethod.insertBefore("{" + cutPointInfo.getMethod().getDeclaringClass().getName() + "." + cutPointInfo.getMethod().getName() + "(new org.voovan.tools.aop.InterceptInfo(\""+ctClass.getName()+"\",\""+ctMethod.getName()+"\", $args, null));}");
                            System.out.println("[AOP] CutPoint before: " + cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName());
                        }

                        //After 方法
                        if (cutPointInfo.getType() == 1) {
                            ctMethod.insertAfter("{" + cutPointInfo.getMethod().getDeclaringClass().getName() + "." + cutPointInfo.getMethod().getName() + "(new org.voovan.tools.aop.InterceptInfo(\""+ctClass.getName()+"\",\""+ctMethod.getName()+"\",$args, $_));}");
                            System.out.println("[AOP] CutPoint after: " + cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName());
                        }

                    } catch (CannotCompileException e) {
                        e.printStackTrace();
                    }
                }
            }

            classfileBuffer = ctClass.toBytecode();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return classfileBuffer;
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
                Before before = (Before) method.getAnnotation(Before.class);
                After after = (After)method.getAnnotation(After.class);

                if(before!=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(before.value());
                    cutPointInfo.setType(-1);
                    cutPointInfo.setMethod(method);
                    AopUtils.CUT_POINTINFO_LIST.add(cutPointInfo);
                }

                if(after!=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(after.value());
                    cutPointInfo.setType(1);
                    cutPointInfo.setMethod(method);
                    AopUtils.CUT_POINTINFO_LIST.add(cutPointInfo);
                }
            }
        }
    }
}
