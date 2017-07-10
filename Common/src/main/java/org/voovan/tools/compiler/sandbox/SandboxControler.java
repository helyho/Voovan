package org.voovan.tools.compiler.sandbox;

import java.security.AllPermission;
import java.util.List;
import java.util.Vector;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SandboxControler {
    private List<String> network;           //null 全部允许访问
    private List<String> file;              //null 全部允许访问
    private boolean thread;
    private boolean link;
    private boolean properties;
    private boolean printJob;
    private boolean exec;
    private boolean exit;
    private boolean factory;
    private boolean securityAccess;
    private boolean createClassLoader;
    private List<String> packageAccess;     //null 全部允许访问
    private List<String> packageDefintion;  //null 全部允许访问

    public SandboxControler(){
        network = null;
        file = null;
        thread = true;
        link = true;
        properties = true;
        printJob= true;
        exec = true;
        exit = true;
        packageAccess = null;
        packageDefintion = null;
        securityAccess = true;
        createClassLoader = true;
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
