package org.voovan.tools;

import org.voovan.Global;
import org.voovan.tools.reflect.TReflect;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * JavaAgent对象
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DynamicAgent {
    private static Instrumentation instrumentation;
    private static Object lockObject = new Object();

    public DynamicAgent() {
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        synchronized(lockObject) {
            if(instrumentation == null) {
                instrumentation = inst;
            }
        }

        if(instrumentation!=null){
            instrumentation.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String classPath, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    String className = classPath.replaceAll("/",".");
                    if(Global.REMOTE_CLASS_SOURCE != null &&  !TReflect.isSystemType(className)) {
                        return getClassBytes(className);
                    } else {
                        return classfileBuffer;
                    }
                }
            });
        }
    }

    /**
     * 读取 Class 的 byte 字节
     * @param classPath 类路径
     * @return 返回类的字节码
     */
    public static byte[] getClassBytes(String classPath){
        String path = classPath.replace(".", "/") + ".class";
        return TFile.loadResource(path);
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
