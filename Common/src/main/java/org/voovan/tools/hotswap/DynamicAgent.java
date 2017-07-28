package org.voovan.tools.hotswap;

import java.lang.instrument.Instrumentation;

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
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
