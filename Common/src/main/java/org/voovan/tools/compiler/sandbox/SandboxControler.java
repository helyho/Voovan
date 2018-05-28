package org.voovan.tools.compiler.sandbox;

import org.voovan.tools.TObject;
import org.voovan.tools.TProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 沙盒控制对象
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SandboxControler {
    private List<String> forbiddenClasses;  //null 全部允许访问, size=0为全部禁止
    private List<String> network;           //null 全部允许访问, size=0为全部禁止
    private List<String> file;              //null 全部允许访问, size=0为全部禁止
    private List<String> packageAccess;     //null 全部允许访问, size=0为全部禁止
    private List<String> packageDefintion;  //null 全部允许访问, size=0为全部禁止

    private boolean thread;
    private boolean link;
    private boolean properties;
    private boolean printJob;
    private boolean exec;
    private boolean exit;
    private boolean factory;
    private boolean securityAccess;
    private boolean createClassLoader;

    public SandboxControler(){
        network = null;
        file = null;
        packageAccess = null;
        packageDefintion = null;

        thread = true;
        link = true;
        properties = true;
        printJob= true;
        exec = true;
        exit = true;
        securityAccess = true;
        createClassLoader = true;

        loadConfig();
    }

    /**
     * 从配置文件读取配置信息
     */
    public void loadConfig(){
        this.forbiddenClasses = setControlField(ControlType.LIST, TProperties.getString("sandbox", "forbiddenClasses"));
        this.network = setControlField(ControlType.LIST, TProperties.getString("sandbox", "network"));
        this.file = setControlField(ControlType.LIST, TProperties.getString("sandbox", "file"));
        this.packageAccess = setControlField(ControlType.LIST, TProperties.getString("sandbox", "packageAccess"));
        this.packageDefintion = setControlField(ControlType.LIST, TProperties.getString("sandbox", "packageDefintion"));
        this.thread = setControlField(ControlType.BOOLEAN, TProperties.getString("sandbox", "thread"));
        this.link = setControlField(ControlType.BOOLEAN, TProperties.getString("sandbox", "link"));
        this.properties = setControlField(ControlType.BOOLEAN, TProperties.getString("sandbox", "properties"));
        this.printJob = setControlField(ControlType.BOOLEAN, TProperties.getString("sandbox", "printJob"));
        this.exec = setControlField(ControlType.BOOLEAN, TProperties.getString("sandbox", "exec"));
        this.exit = setControlField(ControlType.BOOLEAN, TProperties.getString("sandbox", "exit"));
        this.factory = setControlField(ControlType.BOOLEAN, TProperties.getString("sandbox", "factory"));
        this.securityAccess = setControlField(ControlType.BOOLEAN, TProperties.getString("sandbox", "securityAccess"));
        this.createClassLoader = setControlField(ControlType.BOOLEAN, TProperties.getString("sandbox", "createClassLoader"));
    }

    /**
     * 配置控制类型美居
     */
    public enum  ControlType{
        LIST, BOOLEAN
    }

    /**
     * 设置控制参数
     * @param controlType 控制类型
     * @param value 文本数据
     * @param <T> 范型
     * @return 配置参数值
     */
    private <T> T setControlField(ControlType controlType, String value){

        Object result = null;

        if(value == null){
            if(controlType == ControlType.LIST){
                result = null;
            }else if(controlType == ControlType.BOOLEAN){
                result = true;
            }
        } else if(value.equals("ALL_ALLOW")){
            if(controlType == ControlType.LIST){
                result = null;
            }else if(controlType == ControlType.BOOLEAN){
                result = true;
            }

        } else if(value.equals("ALL_DENY")){
            if(controlType == ControlType.LIST){
                result = new ArrayList<String>();
            }else if(controlType == ControlType.BOOLEAN){
                result = false;
            }
        } else {
            if(controlType == ControlType.LIST){
                result = TObject.asList(value.split(","));
            }
        }
        return (T)result;
    }

    public List<String> getForbiddenClasses() {
        return forbiddenClasses;
    }

    public void setForbiddenClasses(List<String> forbiddenClasses) {
        this.forbiddenClasses = forbiddenClasses;
    }

    public List<String> getNetwork() {
        return network;
    }

    public void setNetwork(List<String> network) {
        this.network = network;
    }

    public List<String> getFile() {
        return file;
    }

    public void setFile(List<String> file) {
        this.file = file;
    }

    public boolean isThread() {
        return thread;
    }

    public void setThread(boolean thread) {
        this.thread = thread;
    }

    public boolean isLink() {
        return link;
    }

    public void setLink(boolean link) {
        this.link = link;
    }

    public boolean isProperties() {
        return properties;
    }

    public void setProperties(boolean properties) {
        this.properties = properties;
    }

    public boolean isPrintJob() {
        return printJob;
    }

    public void setPrintJob(boolean printJob) {
        this.printJob = printJob;
    }

    public boolean isExec() {
        return exec;
    }

    public void setExec(boolean exec) {
        this.exec = exec;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    public boolean isFactory() {
        return factory;
    }

    public void setFactory(boolean factory) {
        this.factory = factory;
    }

    public boolean isSecurityAccess() {
        return securityAccess;
    }

    public void setSecurityAccess(boolean securityAccess) {
        this.securityAccess = securityAccess;
    }

    public boolean isCreateClassLoader() {
        return createClassLoader;
    }

    public void setCreateClassLoader(boolean createClassLoader) {
        this.createClassLoader = createClassLoader;
    }

    public List<String> getPackageAccess() {
        return packageAccess;
    }

    public void setPackageAccess(List<String> packageAccess) {
        this.packageAccess = packageAccess;
    }

    public List<String> getPackageDefintion() {
        return packageDefintion;
    }

    public void setPackageDefintion(List<String> packageDefintion) {
        this.packageDefintion = packageDefintion;
    }
}
