package org.voovan.tools.ioc;

import org.voovan.tools.AnnotataionScaner;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.exception.IOCException;
import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.ioc.annotation.Destory;
import org.voovan.tools.ioc.annotation.Priority;
import org.voovan.tools.ioc.annotation.Initialize;
import org.voovan.tools.ioc.entity.BeanDefinition;
import org.voovan.tools.ioc.entity.MethodDefinition;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.voovan.tools.ioc.IOCUtils.DEFAULT_SCOPE;
import static org.voovan.tools.ioc.IOCUtils.getScope;

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

    private final static Container DEFAULT_CONTAINER;

    private static List<String> scanPaths = new ArrayList<>();

    private static List<Class> ENTRANCE = new ArrayList<>();

    /**
     * 容器状态
     * 0: 未初始化, 1:初始化中, 2: 初始化完成
     */
    private static int status = 0; 

    static {
        String iocConfig = TEnv.getSystemProperty("IocConfig", String.class);
        String applicationConfigFile = "conf/application.json";
        	if(TFile.fileExists("conf/application.hcl")) {
					applicationConfigFile = "conf/application.hcl";
				}
        iocConfig = iocConfig == null? TEnv.getEnv("VOOVAN_IOC_CONFIG", applicationConfigFile) : iocConfig;
        try {
            //判断是否是 url 形式, 如果不是则进行转换
            if(TString.regexMatch(iocConfig, "^[a-z,A-Z]*?://")==0) {
                iocConfig = "file://" + TFile.getSystemPath(iocConfig); 
            }

            DEFAULT_CONTAINER = new Container(DEFAULT_SCOPE, new Config(new URL(iocConfig)));
        } catch (IOException e) {
            throw new IOCException("Load IOC config failed", e);
        }
        CONTAINER_MAP.put(DEFAULT_SCOPE, DEFAULT_CONTAINER);
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

    /**
     * 返回容器状态
     * @return 0: 未初始化, 1:初始化中, 2: 初始化完成
     */
    public static int getStatus() {
        return status;
    }

    public static void init() {
        if(status!=0) {
            return;
        }

        status = 1;

        List<String> configPaths = DEFAULT_CONTAINER.get("ScanPaths", null);
        if(configPaths == null) {
            scanPaths.add(TEnv.bootPackage()); 
        } else {
            Context.scanPaths.addAll(configPaths);
        }

        for (String scanPath : scanPaths) {
            try {
                //扫描类并加载 Bean, Method 定义
                AnnotataionScaner.scan(scanPath, clazz -> {
                    loadClass(clazz);
                    loadMethod(clazz);

                    if(clazz.isAnnotationPresent(Priority.class)) {
                        ENTRANCE.add(clazz);
                    }
                }, Bean.class);

                //初始化 @Entrance
                initPriority();

                //初始化 对象和方法上的 @bean
                initBean();

                Logger.info("IOC context initialize finished");
            } catch (Exception e) {
                Logger.errorf("Scan compoment failed", e);
            }
        }

        status = 2;
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
            initLifeCycleMethod(beanDefinition,clazz);
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
            String scope = getScope(clazz);
            Container container = getContainer(scope);
            Definitions definitions = container.getDefinitions();

            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                MethodDefinition methodDefinition = definitions.addMethodDefinition(method);
                initLifeCycleMethod(methodDefinition, methodDefinition.getReturnType());
            }
        }
    }

    public static void initLifeCycleMethod(BeanDefinition beanDefinition, Class clazz) {
        Method[] initAndDestoryMethod = new Method[2];
        Method[] methods = TReflect.getMethods(clazz);
        for (Method method : methods) {
            Initialize initMethod = method.getAnnotation(Initialize.class);
            if(initMethod!=null) {
                beanDefinition.setInit(method);
            }

            Destory destoryMethod = method.getAnnotation(Destory.class);
            if(destoryMethod!=null){
                beanDefinition.setDestory(method);
            }
        }
    }

    public static void initPriority() {
        //升序排序, 索引越大优先级越高
        Collections.sort(ENTRANCE, new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                int o1Priority = TReflect.getAnnotationValue(o1.getAnnotation(Priority.class),"priority");
                int o2priority = TReflect.getAnnotationValue(o2.getAnnotation(Priority.class),"priority");

                return o2priority - o1Priority; 
            }
        });

        for(Class clazz : ENTRANCE) {
            String scope = getScope(clazz);
            Container container = getContainer(scope);
            Definitions definitions = container.getDefinitions();
            BeanDefinition beanDefinition = container.getDefinitions().getBeanDefinition(clazz);
            container.initBean(beanDefinition, false);
            container.initMethodBean(beanDefinition.getClazz(), false);
        }
    }


    private static void initBean() {
        for (Container container : CONTAINER_MAP.values()) {
            Definitions definitions = container.getDefinitions();
            for (BeanDefinition beanDefinition : definitions.getBeanDefinitions().values()) {
                if(container.exists(beanDefinition.getName())) {
                    continue;
                }

                if(!beanDefinition.isLazy()) {
                    container.initBean(beanDefinition, false);
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

    public static <T> T get(String scope, String anchor, T defaultVal) {
        return getContainer(scope).get(anchor, defaultVal);
    }

    public static <T> T get(String anchor, T defaultVal) {
        return get(IOCUtils.DEFAULT_SCOPE, anchor, defaultVal);
    }

    public static <T> T get(String anchor) {
        return get(IOCUtils.DEFAULT_SCOPE, anchor, null);
    }

    public static <T> void addExtBean(String scope, String beanName, T value) {
        getContainer(scope).addExtBean(beanName, value);
    }

    public static <T> void addExtBean(String beanName, T value) {
        addExtBean(DEFAULT_SCOPE, beanName, value);
    }

    public static <T> void addExtBean(T value) {
        addExtBean(DEFAULT_SCOPE, null, value);
    }

}
