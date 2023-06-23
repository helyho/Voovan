package org.voovan.http.server.module.annontationRouter;

import org.voovan.Global;
import org.voovan.http.server.HttpDispatcher;
import org.voovan.http.server.HttpModule;
import org.voovan.http.server.WebServer;
import org.voovan.http.server.module.annontationRouter.annotation.Router;
import org.voovan.http.server.module.annontationRouter.annotation.WebSocket;
import org.voovan.http.server.module.annontationRouter.router.AnnotationRouter;
import org.voovan.http.server.module.annontationRouter.router.AnnotationRouterFilter;
import org.voovan.http.server.module.annontationRouter.router.AsyncRunnerSelector;
import org.voovan.http.server.module.annontationRouter.swagger.SwaggerApi;
import org.voovan.tools.TObject;
import org.voovan.tools.TPerformance;
import org.voovan.tools.TString;
import org.voovan.tools.event.EventRunnerGroup;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.AnnotataionScaner;
import org.voovan.tools.TEnv;
import org.voovan.tools.ioc.Context;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注解路由模块
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AnnotationModule extends HttpModule {
    public static String DEFAULT_SCAN_ROUTER_PACKAGE = TEnv.bootPackage();
    public static int DEFAULT_SCAN_ROUTER_INTERVAL = -1;

    public ConcurrentHashMap<Method, String> METHOD_URL_MAP = new ConcurrentHashMap<Method, String>();
    public ConcurrentHashMap<String, Method> URL_METHOD_MAP = new ConcurrentHashMap<String, Method>();
    private HashWheelTask scanRouterTask;
    private AnnotationRouterFilter annotationRouterFilter;

    private EventRunnerGroup asyncEventRunnerGroup;
    private AsyncRunnerSelector asyncRunnerSelector;

    private int asyncRouterCounter = 0;


    /**
     * 获取扫描注解路由的包路径
     * @return 注解路由的包路劲
     */
    public String getScanRouterPackage(){
        return TObject.nullDefault ((String)getParamters("ScanRouterPackage"), DEFAULT_SCAN_ROUTER_PACKAGE);
    }

    /**
     * 获取注解路由的扫描时间间隔
     * @return 注解路由的扫描时间间隔
     */
    public int getScanRouterInterval(){
        return (int) TObject.nullDefault(getParamters("ScanRouterInterval"), DEFAULT_SCAN_ROUTER_INTERVAL);
    }


    public int getAsyncRunnerSize(){
        return (int) TObject.nullDefault(getParamters("asyncRunnerSize"), TPerformance.getProcessorCount() + 1);
    }

    public boolean getAsyncRunnerSteal(){
        return (Boolean) TObject.nullDefault(getParamters("isAsyncRunnerSteal"), true);
    }

    public void genAsyncRunnerSelector(){
        String className = (String)TObject.nullDefault(getParamters("asyncRunnerSelector"), "");
        if (!TString.isNullOrEmpty(className)) {
            try {
                asyncRunnerSelector = TReflect.newInstance(className, null);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public AsyncRunnerSelector getAsyncThreadSelector() {
        return asyncRunnerSelector;
    }

    public void increaceAsyncRouterCounter(){
        asyncRouterCounter++;
    }


    public EventRunnerGroup getAsyncEventRunnerGroup() {
        return asyncEventRunnerGroup;
    }

    /**
     * 获取注解路由过滤器
     * @return 注解路由过滤器
     */
    public AnnotationRouterFilter getAnnotationRouterFilter(){
        if(annotationRouterFilter == null) {
            try {
                annotationRouterFilter = TReflect.newInstance(getParamters("Filter").toString());
                Context.addExtBean(annotationRouterFilter);
            } catch (Exception e) {
                annotationRouterFilter = null;
            }
        }

        return annotationRouterFilter == null ? null : annotationRouterFilter;
    }

    /**
     * 扫描包含Router注解的类
     */
    public void scanRouterClassAndRegister() {
        int routeMethodNum = 0;

        String modulePath = this.getModuleConfig().getPath();
        modulePath = HttpDispatcher.fixRoutePath(modulePath);

        WebServer webServer = this.getWebServer();
        try {
            //查找包含 Router 注解的类
            String[] scanRouterPackageArr = this.getScanRouterPackage().split(";");
            for(String scanRouterPackage : scanRouterPackageArr) {
                scanRouterPackage = scanRouterPackage.trim();

                AnnotataionScaner.scan(scanRouterPackage, cls->{
                    AnnotationRouter.routerRegister(this, cls);
                }, Router.class);

                AnnotataionScaner.scan(scanRouterPackage,cls->{
                    AnnotationRouter.webSocketRegister(this, cls);
                }, WebSocket.class);
            }

        } catch (Exception e){
            Logger.error("Scan router class error.", e);
        }
    }

    @Override
    public void install() {
        final AnnotationModule annotationModule = this;
        String scanRouterPackage = getScanRouterPackage();

        Logger.simple("[HTTP] Module ["+this.getModuleConfig().getName()+"] Router scan package: "+ this.getScanRouterPackage());
        Logger.simple("[HTTP] Module ["+this.getModuleConfig().getName()+"] Router scan interval: "+ this.getScanRouterInterval());

        if (scanRouterPackage != null) {
            scanRouterClassAndRegister();
        }

        if(asyncRouterCounter >0) {
            genAsyncRunnerSelector();
            asyncEventRunnerGroup = EventRunnerGroup.newInstance("WEB-ROUTER-ASYNC", getAsyncRunnerSize(), getAsyncRunnerSteal());
            asyncEventRunnerGroup.process();
        }

        if(scanRouterPackage != null && getScanRouterInterval() > 0) {

            scanRouterTask = new HashWheelTask() {
                @Override
                public void run() {
                    //查找并刷新新的@Route 注解类
                    scanRouterClassAndRegister();
                }
            };

            //更新 ClassPath, 步长1秒, 槽数60个;
            Global.schedual(scanRouterTask, getScanRouterInterval());
        }

        try {
            SwaggerApi.init(this);
        } catch (Throwable e) {
            Logger.error("[SWAGGER] error: ", e);
        }
    }

    @Override
    public void unInstall() {
        if(scanRouterTask!=null) {
            scanRouterTask.cancel();
        }
    }
}
