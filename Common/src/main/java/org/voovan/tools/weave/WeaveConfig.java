package org.voovan.tools.weave;

/**
 * 代码织入配置对象
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class WeaveConfig {
    private String agent;   //AgentJar 文件
    private String scan;    //扫描的包路径, 可逗号分割多个包
    private String inject;  //注入的包路径, 可逗号分割多个包

    public WeaveConfig(String agent, String scan, String Injinjectect) {
        this.agent = agent;
        this.scan = scan;
        this.inject = inject;
    }

    public WeaveConfig(String scan, String inject) {
        this.scan = scan;
        this.inject = inject;
    }

    public WeaveConfig(String scanAndWeave) {
        this.scan = scanAndWeave;
        this.inject = scanAndWeave;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getScan() {
        return scan;
    }

    public void setScan(String scan) {
        this.scan = scan;
    }

    public String getInject() {
        return inject;
    }

    public void setInject(String inject) {
        this.inject = inject;
    }

    public boolean isInject(String className) {
        for(String injectPackage : inject.split(",")){
            if(className.startsWith(injectPackage)){
                return true;
            }
        }

        return false;
    }
}
