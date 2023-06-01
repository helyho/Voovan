package org.voovan.tools;

import org.voovan.tools.TEnv;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 注解扫描器
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class AnnotataionScaner {
    /**
     * 扫描器
     * @param scatPath      扫描的包路径
     * @param handler       扫描到的类处理聚丙
     * @param filterClazzes 类过滤器
     * @throws InterruptedException 异常
     */
    public static void scan(String scatPath, Consumer<Class> handler, Class ... filterClazzes) throws InterruptedException {
        List<Class> classes = TEnv.searchClassInEnv(scatPath, filterClazzes);
        for (Class clazz : classes) {
            handler.accept(clazz);
        }
    }
}
