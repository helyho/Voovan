package org.voovan.tools.compiler.hotswap;

import java.lang.instrument.Instrumentation;

/**
 * 类文字命名
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
        Object var2 = lockObject;
        synchronized(lockObject) {
            if(instrumentation == null) {
                instrumentation = inst;
                System.out.println("0->" + inst);
            } else {
                System.out.println("1->" + inst);
            }

        }
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
