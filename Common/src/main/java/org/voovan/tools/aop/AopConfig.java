package org.voovan.tools.aop;

/**
 * AOP配置对象
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class AopConfig {
    private String agentJarPath;   //AgentJar 文件
    private String scanPackages;   //扫描的包路径, 可逗号分割多个包
    private String injectPackages;  //注入的包路径, 可逗号分割多个包

    public AopConfig(String agentJarPath, String scanPackages, String injectPackage) {
        this.agentJarPath = agentJarPath;
        this.scanPackages = scanPackages;
        this.injectPackages = injectPackage;
    }

    public AopConfig(String scanPackages, String injectPackage) {
        this.scanPackages = scanPackages;
        this.injectPackages = injectPackage;
    }

    public AopConfig(String scanAndInjectPackages) {
        this.scanPackages = scanAndInjectPackages;
        this.injectPackages = scanAndInjectPackages;
    }

    public String getAgentJarPath() {
        return agentJarPath;
    }

    public void setAgentJarPath(String agentJarPath) {
        this.agentJarPath = agentJarPath;
    }

    public String getScanPackages() {
        return scanPackages;
    }

    public void setScanPackages(String scanPackages) {
        this.scanPackages = scanPackages;
    }

    public String getInjectPackages() {
        return injectPackages;
    }

    public void setInjectPackages(String injectPackages) {
        this.injectPackages = injectPackages;
    }

    public boolean isInject(String className) {
        for(String injectPackage : injectPackages.split(",")){
            if(className.startsWith(injectPackage)){
                return true;
            }
        }

        return false;
    }
}
