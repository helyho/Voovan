package org.voovan.http.server.module.annontationRouter;

import org.voovan.Global;
import org.voovan.http.server.HttpModule;
import org.voovan.http.server.module.annontationRouter.router.AnnotationRouter;
import org.voovan.http.server.module.annontationRouter.router.AnnotationRouterFilter;
import org.voovan.http.server.module.annontationRouter.swagger.SwaggerApi;
import org.voovan.http.server.module.annontationRouter.swagger.entity.Swagger;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注解路由模块
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AnnotationModule extends HttpModule {
    public ConcurrentHashMap<Method, String> METHOD_URL_MAP = new ConcurrentHashMap<Method, String>();
    public ConcurrentHashMap<String, Method> URL_METHOD_MAP = new ConcurrentHashMap<String, Method>();
    public static String DEFAULT_SCAN_ROUTER_PACKAGE = "com;org;net;io";
    public static int DEFAULT_SCAN_ROUTER_INTERVAL = 30;

    private HashWheelTask scanRouterTask;
    private AnnotationRouterFilter annotationRouterFilter;

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

    /**
     * 获取注解路由过滤器
     * @return 注解路由的扫描时间间隔
     */
    public AnnotationRouterFilter getAnnotationRouterFilter(){
        if(annotationRouterFilter == null) {
            try {
                annotationRouterFilter = TReflect.newInstance(getParamters("Filter").toString());
            } catch (Exception e) {
                annotationRouterFilter = AnnotationRouterFilter.EMPYT;
            }
        }

        return annotationRouterFilter == AnnotationRouterFilter.EMPYT ? null : annotationRouterFilter;
    }

    @Override
    public void install() {
        final AnnotationModule annotationModule = this;
        String scanRouterPackage = getScanRouterPackage();
        if (scanRouterPackage != null) {
            AnnotationRouter.scanRouterClassAndRegister(annotationModule);
        }


        if(scanRouterPackage != null && getScanRouterInterval() > 0){

            scanRouterTask = new HashWheelTask() {
                @Override
                public void run() {
                    //查找并刷新新的@Route 注解类
                    AnnotationRouter.scanRouterClassAndRegister(annotationModule);
                }
            };

            //更新 ClassPath, 步长1秒, 槽数60个;
            Global.getHashWheelTimer().addTask(scanRouterTask, getScanRouterInterval());

            Logger.simple("[HTTP] Module ["+this.getModuleConfig().getName()+"] Router scan package: "+ this.getScanRouterPackage());
            Logger.simple("[HTTP] Module ["+this.getModuleConfig().getName()+"] Router scan interval: "+ this.getScanRouterInterval());

            Logger.simple("[HTTP] Module ["+this.getModuleConfig().getName()+"] Start auto scan annotation router.");
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
