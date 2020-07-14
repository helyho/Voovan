package org.voovan.tools.weave;

import javassist.*;
import org.voovan.tools.exception.WeaveException;
import org.voovan.tools.weave.aop.AopWeave;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;
import org.voovan.tools.pool.annotation.PooledObject;
import org.voovan.tools.reflect.TReflect;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * 代码织入主类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class Weave {
    /**
     * 构造函数
     * @param weaveConfig Aop配置对象
     */
    public static void init(WeaveConfig weaveConfig) {
        if(weaveConfig ==null){
            return;
        }

        try {
            // 扫描带有 Aop 的切点方法
            AopWeave.scanAopClass(weaveConfig.getScan());

            Instrumentation instrumentation = TEnv.agentAttach(weaveConfig.getAgent());
            if (instrumentation != null) {
                instrumentation.addTransformer(new ClassFileTransformer() {
                    @Override
                    public byte[] transform(ClassLoader loader, String classPath, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                        String className = classPath.replaceAll("/", ".");
                        if (!TReflect.isSystemType(className) && weaveConfig.isInject(className)) {
                            return weave(className, classfileBuffer);
                        } else {
                            return classfileBuffer;
                        }
                    }
                });
                Logger.simple("[WEAVE] Weave init success");
            } else {
                Logger.error("[WEAVE] Weave init failed, instrumentation is null");
            }
        } catch (Exception e) {
            throw new WeaveException("Weave init failed", e);
        }
    }

    /**
     * 代码织入
     * @param className 织入的类全限定名
     * @param classfileBuffer 织入的类的字节码
     * @return 织入代码后的类的字节码
     */
    public static byte[] weave(String className, byte[] classfileBuffer) {

        CtClass ctClass = null;

        try {
            ctClass = WeaveUtils.getCtClass(className);
        } catch (NotFoundException e){
            return classfileBuffer;
        }

        try {

            //AOP 织入
            {
                CtClass wavedClass = AopWeave.weave(ctClass);

                classfileBuffer = wavedClass == null ? classfileBuffer : wavedClass.toBytecode();
            }

            //ObjectPool 织入
            {
                CtClass wavedClass = wrapPoolObject(ctClass);

                classfileBuffer = wavedClass == null ? classfileBuffer : wavedClass.toBytecode();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return classfileBuffer;
    }

    /**
     * 对象池类对象织入
     * @param ctClass CtClass 对象
     * @return 织入后的 CtClass 对象
     */
    public static CtClass wrapPoolObject(CtClass ctClass) {
        try {
            if(ctClass.getAnnotation(PooledObject.class)!=null) {
                ctClass.defrost();

                CtClass poolBaseCtClass = WeaveUtils.getCtClass("org.voovan.tools.pool.IPooledObject");
                //增加接口
                ctClass.addInterface(poolBaseCtClass);

                CtField ctField = CtField.make("private long poolObjectId;", ctClass);
                ctClass.addField(ctField);

                for(CtMethod method : poolBaseCtClass.getDeclaredMethods()) {
                    // 方法 getPoolObjectId
                    if(method.getName().equals("getPoolObjectId")) {
                        CtMethod newMethod = CtNewMethod.copy(method, ctClass, null);
                        newMethod.setBody("{return poolObjectId;}");
                        ctClass.addMethod(newMethod);
                    }

                    // 方法 setPoolObjectId
                    if(method.getName().equals("setPoolObjectId")) {
                        CtMethod newMethod = CtNewMethod.copy(method, ctClass, null);
                        newMethod.setBody("{poolObjectId = $1;}");
                        ctClass.addMethod(newMethod);
                    }
                }
            }
        } catch (Exception e) {
            throw new WeaveException("Weave wrapPoolObject failed", e);
        }

        return ctClass;
    }
}
