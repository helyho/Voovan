package org.voovan.tools.ioc;

import org.voovan.tools.AnnotataionScaner;
import org.voovan.tools.TObject;
import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.ioc.entity.BeanDefinition;
import org.voovan.tools.ioc.entity.MethodDefinition;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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

    private final static ConcurrentHashMap<String, Container> CONTAINER_MAP = new ConcurrentHashMap<>();

    private final static Container DEFAULT_CONTAINER = new Container(DEFAULT_SCOPE);

    private static List<String> scanPaths = new ArrayList<>();

    private static boolean inited = false; //0: 未初始化, 1: 初始化完成

    static {
        CONTAINER_MAP.put(DEFAULT_SCOPE, DEFAULT_CONTAINER);
        init();
    }

    public static ConcurrentHashMap<String, Container> getContainerMap() {
        return CONTAINER_MAP;
    }

    public static List<String> getScanPaths() {
        return scanPaths;
    }

    public static void setScanPaths(String ... paths) {
        scanPaths.addAll(TObject.asList(paths));
    }

    public static boolean isInited() {
        return inited;
    }

    public static void init() {
        List<String> configPaths = DEFAULT_CONTAINER.get("ScanPaths", null);
        if(configPaths == null) {
            Logger.warnf("ScanPaths is not defined or 'conf/application.json' not exists, Config isn't load!");
            return;
        } else {
            Context.scanPaths.addAll(configPaths);
        }

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

    /**
     * 解析 class 的 bean 定义
     *
     * @param clazz 解析这个 class 的 bean 定义
     */
    public static void loadClass(Class clazz) {
        if(clazz.getAnnotation(Bean.class)!=null) {
            String scope = getScope(clazz);

            Container container = getContainer(scope);
            Definitions definitions = container.getDefinitions();

            BeanDefinition beanDefinition = definitions.addBeanDefinition(clazz);
        }
    }

    /**
     * 解析 method 的 bean 定义
     *
     * @param clazz 解析这个类的 method 的 bean 定义
     */
    public static void loadMethod(Class clazz) {
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


    private static void initBean() {
        for (Container container : CONTAINER_MAP.values()) {
            Definitions definitions = container.getDefinitions();
            for (BeanDefinition beanDefinition : definitions.getBeanDefinitions().values()) {
                if(!beanDefinition.isLazy()) {
                    container.initBean(beanDefinition, false);
                }
            }

            for (BeanDefinition beanDefinition : definitions.getBeanDefinitions().values()) {
                if(!beanDefinition.isLazy()) {
                    container.initMethodBean(beanDefinition.getClazz(), false);
                }
            }
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
        return CONTAINER_MAP.computeIfAbsent(scope, key -> new Container(key));
    }

    public static void addContainer(Container container){
        if(container == null) {
            return;
        }

        CONTAINER_MAP.put(container.getScope(), container);
    }

    public static Container getDefaultContainer() {
        return getContainer(DEFAULT_SCOPE);
    }

    public static <T> T get(String scope, Object mark, T defaultVal) {
        return getContainer(scope).get(mark, defaultVal);
    }

    public static <T> T get(Object mark, T defaultVal) {
        return get(Utils.DEFAULT_SCOPE, mark, defaultVal);
    }

    public static <T> T get(Object mark) {
        return get(Utils.DEFAULT_SCOPE, mark, null);
    }

    public static <T> T addExtBean(String scope, String beanName, T value) {
        return getContainer(scope).addExtBean(beanName, value);
    }

    public static <T> T addExtBean(String beanName, T value) {
        return addExtBean(DEFAULT_SCOPE, beanName, value);
    }

    public static <T> T addExtBean(T value) {
        return addExtBean(DEFAULT_SCOPE, null, value);
    }

}
