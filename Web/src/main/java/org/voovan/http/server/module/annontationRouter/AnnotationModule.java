package org.voovan.http.server.module.annontationRouter;

import org.voovan.Global;
import org.voovan.http.server.HttpModule;
import org.voovan.http.server.module.annontationRouter.router.AnnotationRouter;
import org.voovan.http.server.module.annontationRouter.router.AnnotationRouterFilter;
import org.voovan.tools.TObject;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

/**
 * 注解路由模块
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AnnotationModule extends HttpModule {

    private HashWheelTask scanRouterTask;
    private AnnotationRouterFilter annotationRouterFilter;

    /**
     * 获取扫描注解路由的包路径
     * @return 注解路由的包路劲
     */
    public String getScanRouterPackage(){
        return (String)getParamters("ScanRouterPackage");
    }

    /**
     * 获取注解路由的扫描时间间隔
     * @return 注解路由的扫描时间间隔
     */
    public int getScanRouterInterval(){
        return (int)getParamters("ScanRouterInterval");
    }

    /**
     * 获取注解路由过滤器
     * @return 注解路由的扫描时间间隔
     */
    public AnnotationRouterFilter getAnnotationRouterFilter(){
        if(annotationRouterFilter == null) {
            try {
                annotationRouterFilter = TReflect.newInstance(getParamters("AnnotationRouterFilter").toString());
            } catch (Exception e) {
                annotationRouterFilter = AnnotationRouterFilter.EMPYT;
            }
        }

        return annotationRouterFilter == AnnotationRouterFilter.EMPYT ? null : annotationRouterFilter;
    }

    @Override
    public void install() {
        final AnnotationModule annotationModule = this;
        String scanRouterPackate = getScanRouterPackage();
        if (scanRouterPackate != null) {
            AnnotationRouter.scanRouterClassAndRegister(annotationModule);
        }

        if(scanRouterPackate != null && getScanRouterInterval() > 0){

            scanRouterTask = new HashWheelTask() {
                @Override
                public void run() {
                    //查找并刷新新的@Route 注解类
                    AnnotationRouter.scanRouterClassAndRegister(annotationModule);
                }
            };

            //更新 ClassPath, 步长1秒, 槽数60个;
            Global.getHashWheelTimer().addTask(scanRouterTask, getScanRouterInterval());

            Logger.simple("[SYSTEM] Module ["+this.getModuleConfig().getName()+"] Router scan package: "+ this.getScanRouterPackage());
            Logger.simple("[SYSTEM] Module ["+this.getModuleConfig().getName()+"] Router scan interval: "+ this.getScanRouterInterval());

            Logger.simple("[SYSTEM] Module ["+this.getModuleConfig().getName()+"] Start auto scan annotation router.");
        }
    }

    @Override
    public void unInstall() {
        if(scanRouterTask!=null) {
            scanRouterTask.cancel();
        }
    }
}
