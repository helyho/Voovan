package org.voovan.tools.aop;

import org.voovan.tools.CollectionSearch;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
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
     * @param scanPackages 扫描的包路径, 可都好分割多个包
     * @param agentJarPath AgentJar 文件
     * @throws IOException IO 异常
     * @throws AttachNotSupportedException 附加指定进程失败
     * @throws AgentLoadException Agent 加载异常
     * @throws AgentInitializationException Agent 初始化异常
     * @throws ClassNotFoundException 类找不到异常
     */
    public static void init(String agentJarPath, String scanPackages) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, ClassNotFoundException {
        if(scanPackages==null){
            return;
        }

        //扫描带有 Aop 的切点方法
        for(String scanPackage : scanPackages.split(",")) {
            AopUtils.scanAopClass(scanPackage);
        }

        IS_AOP_ON = true;
        instrumentation = TEnv.agentAttach(agentJarPath);
        if(instrumentation!=null) {
            instrumentation.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String classPath, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    String className = classPath.replaceAll(File.separator, ".");
                    if(!TReflect.isSystemType(className)) {
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

                //这里加载两次是为了防止热加载失效
                ctClass = AopUtils.CLASSPOOL.get(className);
                ctClass.detach();
                ctClass = AopUtils.CLASSPOOL.get(className);
            } catch (NotFoundException e){
                return classfileBuffer;
            }
            //撤销上次的修改
            ctClass.detach();

            //扫描目标方法并进行注入
            for(CtMethod originMethod : ctClass.getDeclaredMethods()){

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

                                innerMethodName = "^"+innerMethodName+"$";

                                if(TString.searchByRegex(originMethod.getName(), innerMethodName).length > 0){
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
                                    else if (cutPointInfo.getParameterTypes().length == originMethod.getParameterTypes().length) {
                                        for (int x = 0; x < cutPointInfo.getParameterTypes().length; x++) {
                                            CtClass methodParameterCtClass = originMethod.getParameterTypes()[x];
                                            if (!methodParameterCtClass.getName().equals(cutPointInfo.getParameterTypes()[x])) {
                                                return false;
                                            }
                                        }

                                        parameterTypeEqual = true;
                                    }

                                    //返回类型为通用匹配, 或者相同的时候
                                    if("*".equals(cutPointInfo.getResultType()) || originMethod.getReturnType().getName().equals(cutPointInfo.getResultType())){
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
                        String thisParam = "this";
                        String cutPointClassName = cutPointInfo.getCutPointMethod().getDeclaringClass().getName();
                        String cutPointMethodName = cutPointInfo.getCutPointMethod().getName();
                        boolean isLambda = originMethod.getName().startsWith("lambda$");


                        //是否是静态方法 或者 lambda 表达式的判断

                        if((originMethod.getModifiers() & 8) != 0 || isLambda){
                            thisParam = "null";
                        }

                        if(cutPointInfo.isInterceptLambda() && isLambda){
                            continue;
                        }

                        //Before 方法
                        if (cutPointInfo.getType() == -1) {
                            originMethod.insertBefore("{" + cutPointClassName + "." + cutPointMethodName + "(new org.voovan.tools.aop.InterceptInfo($class, \""+originMethod.getName()+"\", "
                                    + thisParam +", $sig, $args, null, null, null));}");
                            System.out.println("[AOP] Code weaved -> BEFORE:\t\t " + cutPointInfo.getClazzName() + "@" + cutPointMethodName + " -> " + cutPointInfo.getClazzName());
                        }

                        //After 方法
                        if (cutPointInfo.getType() == 1) {
                            originMethod.insertAfter("{"+ cutPointClassName + "." + cutPointMethodName + "(new org.voovan.tools.aop.InterceptInfo($class, \""+originMethod.getName()+"\", "
                                    + thisParam +", $sig, $args, $type, ($w)$_, null));}");
                            System.out.println("[AOP] Code weaved -> AFTER:\t\t     " + cutPointInfo.getClazzName() + "@" + cutPointMethodName + " -> " + cutPointInfo.getClazzName());
                        }

                        //Exception 方法
                        if (cutPointInfo.getType() == 2) {
                            CtClass exceptionType = ClassPool.getDefault().get("java.lang.Exception");
                            originMethod.addCatch("{"+ cutPointClassName + "." + cutPointMethodName + "(new org.voovan.tools.aop.InterceptInfo($class, \""+originMethod.getName()+"\", "
                                    + thisParam +", $sig, $args, null, null, $e));  throw $e;}", exceptionType);
                            System.out.println("[AOP] Code weaved -> EXCEPTION:\t\t " + cutPointInfo.getClazzName() + "@" + cutPointMethodName + " -> " + cutPointInfo.getClazzName());
                        }

                        //Around 方法
                        if (cutPointInfo.getType() == 3){
                            String originMethodName = originMethod.getName();
                            CtMethod ctNewMethod = CtNewMethod.copy(originMethod, ctClass, null);
                            ctNewMethod.setName(originMethodName);
                            originMethod.setName(originMethod.getName()+"$origin");

                            //获取原函数的注解
                            AnnotationsAttribute attribute = (AnnotationsAttribute)originMethod.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);
                            if(attribute!=null) {
                                //移除取原函数的注解
                                originMethod.getMethodInfo().removeAttribute(attribute.getName());

                                //迁移原函数的注解到新函数
                                ctNewMethod.getMethodInfo().addAttribute(attribute);
                            }

                            ctClass.addMethod(ctNewMethod);
                            ctNewMethod.setBody("{ return "+ cutPointClassName + "." + cutPointMethodName + "(new org.voovan.tools.aop.InterceptInfo($class, \""+ originMethodName +"\", "
                                    + thisParam +", $sig, $args, null, null, null));}");
                            System.out.println("[AOP] Code weaved -> AROUND:\t\t " + cutPointInfo.getClazzName() + "@" + cutPointMethodName + " -> " + cutPointInfo.getClazzName());
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
}
