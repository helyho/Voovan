package org.voovan.tools.compiler.sandbox;

import org.voovan.tools.TString;

import java.net.InetAddress;
import java.security.Permission;
import java.util.List;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DynamicCompilerSecurityManager extends SecurityManager{
    private SecurityManager systemSecurityManager;
    private SecurityModel securityModel;

    public DynamicCompilerSecurityManager(SecurityModel securityModel){
        this.securityModel = securityModel;

        systemSecurityManager = System.getSecurityManager();
        if(systemSecurityManager instanceof  DynamicCompilerSecurityManager){
            systemSecurityManager = ((DynamicCompilerSecurityManager)systemSecurityManager).getSystemSecurityManager();
        }
    }

    public SecurityManager getSystemSecurityManager(){
        return systemSecurityManager;
    }

    private boolean isDynamicObject(){
        for(StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()){
            //动态
            if(stackTraceElement.getClassName().contains("$VDC$")){
                //动态 class
                return true;
            }
        }

        return false;
    }

    public boolean isInList(List<String> restricts, String param){
        if(restricts == null){
            return true;
        }

        boolean result = false;
        for(String restrict : restricts){
            if(TString.regexMatch(param,restrict)>0){
                result  = true;
                break;
            }
        }

        return result;
    }

    public boolean commonCheck(Object condiction, Object param){

        if(!isDynamicObject()){
            return true;
        }

        if(condiction instanceof Boolean && param==null){
            return (boolean)condiction;
        }

        if(condiction instanceof List){
            return isInList((List)condiction, (String)param);
        }

        return true;

    }

    @Override
    public void checkPermission(Permission perm) {
        if(systemSecurityManager!=null){
            systemSecurityManager.checkPermission(perm);
        }
    }

    public void throwException(String resource){
        throw new SecurityException("Access deny with an protected resource [" + resource + "] in Sandbox mode");
    }

    @Override
    public void checkAccess(Thread t) {
        //线程的 stop, suspend, resume, setPriority, setName, setDaemon这些操作
        if(!commonCheck(securityModel.isThread(), null)){
            throwException("Thread operation");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkAccess(t);
        }
    }

    @Override
    public void checkAccess(ThreadGroup g) {
        //线程的created, setDaemon, setMaxPriority, stop, suspend, resume, destroy这些操作
        if(!commonCheck(securityModel.isThread(), null)){
            throwException("ThreadGroup operation");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkAccess(g);
        }
    }

    @Override
    public void checkExit(int status) {
        //System.exit 操作
        if(!commonCheck(securityModel.isExit(), null)){
            throwException("Exit operation");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkExit(status);
        }
    }

    @Override
    public void checkExec(String cmd) {
        //启动新的进程的操作
        if(!commonCheck(securityModel.isExec(), null)){
            throwException("Execute " + cmd);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkExec(cmd);
        }
    }

    @Override
    public void checkLink(String lib) {
        //读取JNI库操作
        if(!commonCheck(securityModel.isLink(), null)){
            throwException("Link "+lib);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkLink(lib);
        }

    }

    @Override
    public void checkRead(String file) {
        //读文件操作
        if(!commonCheck(securityModel.getFile(), file)){
            throwException("Read "+file);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkRead(file);
        }

    }

    @Override
    public void checkWrite(String file) {
        //修改文件操作
        if(!commonCheck(securityModel.getFile(), file)){
            throwException("Write "+file);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkWrite(file);
        }
    }

    @Override
    public void checkDelete(String file) {
        //删除文件操作
        if(!commonCheck(securityModel.getFile(), file)){
            throwException("Delete "+file);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkDelete(file);
        }
    }

    @Override
    public void checkConnect(String host, int port) {
        //Socket 连接操作
        String addrsss = host + ":" + port;
        if(!commonCheck(securityModel.getNetwork(), addrsss)){
            throwException("Connect "+addrsss);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkConnect(host, port);
        }
    }

    @Override
    public void checkConnect(String host, int port, Object context) {
        //Socket 连接操作
        String addrsss = host + ":" + port;
        if(!commonCheck(securityModel.getNetwork(), addrsss)){
            throwException("Connect "+addrsss);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkConnect(host, port, context);
        }
    }

    @Override
    public void checkListen(int port) {
        //Socket 监听操作
        String addrsss = ":"+port;
        if(!commonCheck(securityModel.getNetwork(), addrsss)){
            throwException("Listen "+addrsss);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkListen(port);
        }
    }

    @Override
    public void checkAccept(String host, int port) {
        //接受连接操作
        String addrsss = host+":"+port;
        if(!commonCheck(securityModel.getNetwork(), addrsss)){
            throwException("Accept "+addrsss);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkAccept(host, port);
        }
    }

    @Override
    public void checkMulticast(InetAddress maddr) {
        //广播操作
        String addrsss = maddr.getHostAddress();
        if(!commonCheck(securityModel.getNetwork(), addrsss)){
            throwException("Multicast "+addrsss);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkMulticast(maddr);
        }
    }

    @Override
    public void checkPropertiesAccess() {
        //系统属性的访问操作
        if(!commonCheck(securityModel.isProperties(), null)){
            throwException("System Property");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkPropertiesAccess();
        }
    }

    @Override
    public void checkPropertyAccess(String key) {
        //特定系统属性访问操作
        if(!commonCheck(securityModel.isProperties(), null)){
            throwException("System Property");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkPropertyAccess(key);
        }
    }

    @Override
    public void checkPrintJobAccess() {
        //文件打印操作
        if(!commonCheck(securityModel.isPrintJob(), null)){
            throwException("Print job");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkPrintJobAccess();
        }
    }

    @Override
    public void checkPackageAccess(String pkg) {
        //包访问操作
        if(!commonCheck(securityModel.getPackageAccess(), pkg)) {
            throwException("Package "+pkg);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkPackageAccess(pkg);
        }
    }

    @Override
    public void checkPackageDefinition(String pkg) {
        //包定义操作
        if (!commonCheck(securityModel.getPackageDefintion(), pkg)) {
            throwException("Package "+pkg);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkPackageDefinition(pkg);
        }
    }

    @Override
    public void checkSetFactory() {
        //ServerSocket or Socket 的工厂操作
        if(!commonCheck(securityModel.isFactory(), null)){
            throwException("Socket factory");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkSetFactory();
        }
    }
}
