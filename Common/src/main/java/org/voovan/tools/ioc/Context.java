package org.voovan.tools.ioc;

import org.voovan.tools.AnnotataionScaner;
import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.ioc.entity.BeanDefinition;
import org.voovan.tools.ioc.entity.MethodDefinition;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class name
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class Context {

    public static ConcurrentHashMap<String, Container> CONTAINER_MAP = new ConcurrentHashMap<>();

    String[] scanPaths;

    public Context(String... scanPaths) {
        this.scanPaths = scanPaths;
    }

    public void init() {
        for(String scanPath : scanPaths) {
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
    }


    public void initBean() throws ReflectiveOperationException {
        for(Container container : CONTAINER_MAP.values()) {
            Definitions definitions = container.getDefinitions();
            for(BeanDefinition beanDefinition : definitions.getBeanDefinitions().values()){
                if(!beanDefinition.isLazy()) {
                    container.initBean(beanDefinition.getName());
                }
            }

            for(BeanDefinition beanDefinition : definitions.getBeanDefinitions().values()) {
                container.initMethodBean(beanDefinition.getClazz());
            }
        }
    }

    /**
     * 解析 class 的 bean 定义
     * @param clazz 解析这个 class 的 bean 定义
     */
    public void loadClass(Class clazz) {
        String scope = Definitions.getScope(clazz);

        Container container = getContainer(scope);
        Definitions definitions = container.getDefinitions();

        BeanDefinition beanDefinition = definitions.addBeanDefinition(clazz);
    }

    /**
     * 解析 method 的 bean 定义
     * @param clazz 解析这个类的 method 的 bean 定义
     */
    public void loadMethod(Class clazz) {
        Method[] methods = TReflect.getMethods(clazz);
        for(Method method : methods) {
            Bean bean = method.getAnnotation(Bean.class);
            if(bean==null) {
                continue;
            }
            String scope = Definitions.getScope(method);

            Container container = getContainer(scope);
            Definitions definitions = container.getDefinitions();

            MethodDefinition methodDefinition = container.getDefinitions().addMethodDefinition(method);
        }
    }

    /**
     * 获取指定作用域的 Container
     * @param scope 作用域
     * @return 指定作用域的 Container
     */
    public Container getContainer(String scope) {
        return CONTAINER_MAP.computeIfAbsent(scope, key->new Container(scope));
    }

}
