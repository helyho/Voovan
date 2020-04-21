package org.voovan.tools.weave.aop;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import org.voovan.Global;
import org.voovan.tools.TString;
import org.voovan.tools.collection.CollectionSearch;
import org.voovan.tools.json.JSON;
import org.voovan.tools.weave.WeaveUtils;
import org.voovan.tools.weave.aop.annotation.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Aop 字节码织入类
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class AopWeave {
    public static List<CutPointInfo> CUT_POINTINFO_LIST = new ArrayList<CutPointInfo>();

    /**
     * Aop 类对象变更
     * @param ctClass CtClass 对象
     * @return 织入后的 CtClass 对象
     */
    public static CtClass weave(CtClass ctClass) {
        try {
            /**
             * Aop 注入
             */
            //扫描目标方法并进行注入
            for(CtMethod originMethod : ctClass.getDeclaredMethods()){

                //遍历可用于当前方法注入的切面点
                List<CtClass> classes = WeaveUtils.getAllSuperCtClass(ctClass);
                classes.add(0, ctClass);
                List<CutPointInfo> avaliableCutPointInfo = (List<CutPointInfo>) CollectionSearch.newInstance(CUT_POINTINFO_LIST)
                        .setParallelStream(false)
                        .addCondition(new Predicate() {
                            @Override
                            public boolean test(Object o) {
                                CutPointInfo cutPointInfo = (CutPointInfo)o;

                                for(CtClass ctClazz : classes) {
                                    String ctClassName = ctClazz.getName();
                                    String cutPointClassName = cutPointInfo.getClazzName().replaceAll("\\.", "\\\\.");
                                    cutPointClassName = cutPointClassName.replaceAll("\\*", ".*?");
                                    if (TString.regexMatch(ctClassName, cutPointClassName) > 0) {
                                        return true;
                                    }
                                }

                                return false;
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

                                if(TString.regexMatch(originMethod.getName(), innerMethodName) > 0){
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

                        String targetInfo = ctClass.getName() + "@" + originMethod.getName();
                        //Before 方法
                        if (cutPointInfo.getType() == -1) {
                            originMethod.insertBefore("{" + cutPointClassName + "." + cutPointMethodName + "(new org.voovan.tools.weave.aop.InterceptInfo($class, \""+originMethod.getName()+"\", "
                                    + thisParam +", $sig, $args, null, null, null));}");
                            System.out.println("[AOP] Code weaved -> BEFORE:\t\t " + cutPointInfo.getClazzName() + "@" + cutPointMethodName + " -> " + targetInfo);
                        }

                        //After 方法
                        if (cutPointInfo.getType() == 1) {
                            originMethod.insertAfter("{"+ cutPointClassName + "." + cutPointMethodName + "(new org.voovan.tools.weave.aop.InterceptInfo($class, \""+originMethod.getName()+"\", "
                                    + thisParam +", $sig, $args, $type, ($w)$_, null));}");
                            System.out.println("[AOP] Code weaved -> AFTER:\t\t     " + cutPointInfo.getClazzName() + "@" + cutPointMethodName + " -> " + targetInfo);
                        }

                        //Exception 方法
                        if (cutPointInfo.getType() == 2) {
                            CtClass exceptionType = ClassPool.getDefault().get("java.lang.Exception");
                            originMethod.addCatch("{"+ cutPointClassName + "." + cutPointMethodName + "(new org.voovan.tools.weave.aop.InterceptInfo($class, \""+originMethod.getName()+"\", "
                                    + thisParam +", $sig, $args, null, null, $e));  throw $e;}", exceptionType);
                            System.out.println("[AOP] Code weaved -> EXCEPTION:\t\t " + cutPointInfo.getClazzName() + "@" + cutPointMethodName + " -> " + targetInfo);
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
                            ctNewMethod.setBody("{ return "+ cutPointClassName + "." + cutPointMethodName + "(new org.voovan.tools.weave.aop.InterceptInfo($class, \""+ originMethodName +"\", "
                                    + thisParam +", $sig, $args, null, null, null));}");
                            System.out.println("[AOP] Code weaved -> AROUND:\t\t " + cutPointInfo.getClazzName() + "@" + cutPointMethodName + " -> " + cutPointInfo.getClazzName());
                        }
                    } catch (CannotCompileException e) {
                        e.printStackTrace();
                    }
                }
            }

            if(Global.IS_DEBUG_MODE) {
                ctClass.debugDump = "./dump";
            }

            return ctClass;


        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Javassist 扫描所有的切面注入点
     * @param scanPackage 扫描的包路径
     * @throws IOException IO 异常
     * @throws ClassNotFoundException 类未找到异常
     */
    public static void scanAopClass(String scanPackage) throws IOException, ClassNotFoundException {
        System.out.println("[AOP] Scan from package: " + scanPackage);
        List<CtClass> CtClasses = WeaveUtils.searchCtClassInJavassist(scanPackage);

        for(CtClass clazz : CtClasses) {
            Annotation annotation = (Annotation) clazz.getAnnotation(Aop.class);
            if(annotation==null){
                continue;
            }

            CtMethod[] methods = clazz.getDeclaredMethods();
            for(CtMethod cutPointMethod : methods){
                Before onBefore = (Before) cutPointMethod.getAnnotation(Before.class);
                After onAfter = (After)cutPointMethod.getAnnotation(After.class);
                Abnormal onAbnormal = (Abnormal)cutPointMethod.getAnnotation(Abnormal.class);
                Around onAround = (Around)cutPointMethod.getAnnotation(Around.class);

                if(onBefore!=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onBefore.value());
                    cutPointInfo.setType(-1);
                    cutPointInfo.setCutPointMethod(cutPointMethod);
                    cutPointInfo.setInterceptLambda(onBefore.lambda());
                    CUT_POINTINFO_LIST.add(cutPointInfo);
                    System.out.println("[AOP] Add cutpoint: " + JSON.toJSON(cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName()));
                }

                if(onAfter!=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onAfter.value());
                    cutPointInfo.setType(1);
                    cutPointInfo.setCutPointMethod(cutPointMethod);
                    cutPointInfo.setInterceptLambda(onAfter.lambda());
                    CUT_POINTINFO_LIST.add(cutPointInfo);
                    System.out.println("[AOP] Add cutpoint: " + JSON.toJSON(cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName()));
                }

                if(onAbnormal !=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onAbnormal.value());
                    cutPointInfo.setType(2);
                    cutPointInfo.setCutPointMethod(cutPointMethod);
                    cutPointInfo.setInterceptLambda(onAbnormal.lambda());
                    CUT_POINTINFO_LIST.add(cutPointInfo);
                    System.out.println("[AOP] Add cutpoint: " + JSON.toJSON(cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName()));
                }

                if(onAround !=null){
                    CutPointInfo cutPointInfo = CutPointInfo.parse(onAround.value());
                    cutPointInfo.setType(3);
                    cutPointInfo.setCutPointMethod(cutPointMethod);
                    cutPointInfo.setInterceptLambda(onAround.lambda());
                    CUT_POINTINFO_LIST.add(cutPointInfo);
                    System.out.println("[AOP] Add cutpoint: " + JSON.toJSON(cutPointInfo.getClazzName() + "@" + cutPointInfo.getMethodName()));
                }
            }
        }
    }
}
