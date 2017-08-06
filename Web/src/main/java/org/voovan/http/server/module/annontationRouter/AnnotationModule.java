package org.voovan.http.server.module.annontationRouter;

import org.voovan.http.server.HttpModule;
import org.voovan.http.server.module.annontationRouter.router.AnnotationRouter;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AnnotationModule extends HttpModule {

    public String getScanRouterPackage(){
        return (String)getParamters("ScanRouterPackage");
    }

    public int getScanRouterInterval(){
        return (int)getParamters("ScanRouterInterval");
    }

    @Override
    public void install() {
        final AnnotationModule httpModule = this;
        String scanRouterPackate = getScanRouterPackage();
        if (scanRouterPackate != null) {
            AnnotationRouter.scanRouterClassAndRegister(httpModule);
        }

        if(scanRouterPackate != null && getScanRouterInterval() > 0){
            //更新 ClassPath, 步长1秒, 槽数60个;
            org.voovan.Global.getHashWheelTimer().addTask(new HashWheelTask() {
                @Override
                public void run() {
                    //查找并刷新新的@Route 注解类
                    AnnotationRouter.scanRouterClassAndRegister(httpModule);
                }
            }, getScanRouterInterval());

            Logger.simple("Start scan annotation router.");
        }
    }
}
