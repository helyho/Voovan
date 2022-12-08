package org.voovan.tools.ioc;

import org.voovan.tools.AnnotataionScaner;
import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.ioc.entity.BeanDefinition;
import org.voovan.tools.ioc.entity.MethodDefinition;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import static org.voovan.tools.ioc.Utils.DEFAULT_SCOPE;
import static org.voovan.tools.ioc.Utils.getScope;

/**
 * Class name
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class Context {

    private static ConcurrentHashMap<String, Container> CONTAINER_MAP = new ConcurrentHashMap<>();

    private static String[] scanPaths;

    private static boolean inited = false; //0: 未初始化, 1: 初始化完成

    public static ConcurrentHashMap<String, Container> getContainerMap() {
        return CONTAINER_MAP;
    }

    public static String[] getScanPaths() {
        return scanPaths;
    }

    public static boolean isIsInited() {
        return inited;
    }

    public static void init(String... scanPaths) {
        Context.scanPaths = scanPaths;
        for (String scanPath : scanPaths) {
            try {
                //扫描类并加载 Bean, Method 定义
                AnnotataionScaner.scan(scanPath, clazz -> {
                    loadClass(clazz);
                    loadMethod(clazz);
                }, Bean.class);

                //初始化 bean, method
                initBean();
            } catch (Exception e) {
                Logger.errorf("Scan compoment failed", e);
            }
        }
        inited = true;
    }


    private static void initBean() {
        for (Container container : CONTAINER_MAP.values()) {
            Definitions definitions = container.getDefinitions();
            for (BeanDefinition beanDefinition : definitions.getBeanDefinitions().values()) {
                container.initBean(beanDefinition);
            }

            for (BeanDefinition beanDefinition : definitions.getBeanDefinitions().values()) {
                container.initMethodBean(beanDefinition.getClazz());
            }
        }
    }

    /**
     * 解析 class 的 bean 定义
     *
     * @param clazz 解析这个 class 的 bean 定义
     */
    private static void loadClass(Class clazz) {
        String scope = getScope(clazz);

        Container container = getContainer(scope);
        Definitions definitions = container.getDefinitions();

        BeanDefinition beanDefinition = definitions.addBeanDefinition(clazz);
    }

    /**
     * 解析 method 的 bean 定义
     *
     * @param clazz 解析这个类的 method 的 bean 定义
     */
    private static void loadMethod(Class clazz) {
        Method[] methods = TReflect.getMethods(clazz);
        for (Method method : methods) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean == null) {
                continue;
            }
            String scope = getScope(method);

            Container container = getContainer(scope);
            Definitions definitions = container.getDefinitions();

            MethodDefinition methodDefinition = container.getDefinitions().addMethodDefinition(method);
        }
    }

    /**
     * 获取指定作用域的 Container
     *
     * @param scope 作用域
     * @return 指定作用域的 Container
     */
    public static Container getContainer(String scope) {
        if(scope == null){
            scope = DEFAULT_SCOPE;
        }
        String finalScope = scope;
        return CONTAINER_MAP.computeIfAbsent(scope, key -> new Container(finalScope));
    }

    public static Container getDefaultContainer() {
        return getContainer(DEFAULT_SCOPE);
    }

    public <T> T get(String scope, Object mark, T defaultVal) {
        return getContainer(scope).get(mark, defaultVal);
    }

    public <T> T get(Object mark, T defaultVal) {
        return get(Utils.DEFAULT_SCOPE, mark, defaultVal);
    }

    public <T> T get(Object mark) {
        return get(Utils.DEFAULT_SCOPE, mark, null);
    }

    public <T> T addExtBean(String scope, String beanName, T value) {
        return getContainer(scope).addExtBean(beanName, value);
    }

    public <T> T addExtBean(String beanName, T value) {
        return addExtBean(DEFAULT_SCOPE, beanName, value);
    }

    public <T> T addExtBean(T value) {
        return addExtBean(DEFAULT_SCOPE, null, value);
    }

}
