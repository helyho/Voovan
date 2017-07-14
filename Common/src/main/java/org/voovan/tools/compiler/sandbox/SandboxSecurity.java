package org.voovan.tools.compiler.sandbox;

import org.voovan.tools.TString;

import java.net.InetAddress;
import java.security.Permission;
import java.util.List;

/**
 * 沙盒安全控制类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SandboxSecurity extends SecurityManager {
    private SecurityManager systemSecurityManager;
    private SandboxControler sandboxControler;

    /**
     * 构造函数
     * @param sandboxControler 安全模型
     */
    public SandboxSecurity(SandboxControler sandboxControler) {
        this.sandboxControler = sandboxControler;

        systemSecurityManager = System.getSecurityManager();
        if(systemSecurityManager instanceof SandboxSecurity){
            systemSecurityManager = ((SandboxSecurity)systemSecurityManager).getSystemSecurityManager();
        }
    }

    /**
     * 获取系统的安全管理器
     * @return 系统的安全管理器
     */
    public SecurityManager getSystemSecurityManager(){
        return systemSecurityManager;
    }

    /**
     * 判断是否是动态对象
     *      从当前线程中检查是否存在动态编译的类
     * @return true:是, false:否
     */
    private boolean isDynamicObject(){
        for(StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()){
            if(stackTraceElement.getClassName().contains("$VDC$")){
                return true;
            }
        }

        return false;
    }

    /**
     * 用正则的方式判断是否存在于字符串列表中
     * @param restricts 约束 List
     * @param param 被判断参数
     * @return true:在列表中, falsel:不在列表中
     */
    public boolean isInList(List<String> restricts, String param){
        if(restricts == null){
            return true;
        }

        boolean result = false;
        for(String restrict : restricts){
            if(TString.regexMatch(param,restrict.trim())>0){
                result  = true;
                break;
            }
        }

        return result;
    }

    /**
     * 用正则的方式判断是否不存在于字符串列表中
     * @param restricts 约束 List
     * @param param 被判断参数
     * @return true:不在列表中, falsel:在列表中
     */
    public boolean isNotInList(List<String> restricts, String param){
        if(restricts == null){
            return true;
        }

        boolean result = false;
        for(String restrict : restricts){
            if(TString.regexMatch(param,restrict.trim())>0){
                result  = false;
                break;
            }
        }

        return result;
    }

    /**
     * 通用判断函数
     * @param condiction 判断条件可以为 List 或者 boolean 数据
     * @param param 被判断参数
     * @return true:检查成功符合效验条件, false: 检查失败不符合效验条件,返回这个结果可能在被调用处抛出异常
     */
    public boolean commonCheck(Object condiction, Object param){

        boolean result = true;

        if(isDynamicObject() && condiction instanceof Boolean && param==null){
            result = (boolean)condiction;
        } else if(isDynamicObject() && condiction instanceof List){
            result = isInList((List)condiction, (String)param);
        }

        return result;
    }

    /**
     * 抛出异常
     * @param resource 异常信息
     */
    public void throwException(String resource){
        throw new SecurityException("Access to protected resource [" + resource + "] is restricted in Sandbox mode");
    }

    /**
     * 进制某些 class 在动态调用时被加载到 JVM
     * @param className 类名
     * @throws ClassNotFoundException 类无法找到异常
     */
    public void checkLoadClass(String className) throws ClassNotFoundException {
        //ServerSocket or Socket 的工厂操作
        if(isDynamicObject() && !isNotInList(sandboxControler.getForbiddenClasses(), className)){
            throw new ClassNotFoundException("Access to protected resource [ Load Class: " + className + "] is restricted in Sandbox mode");
        }
    }

    @Override
    public void checkPermission(Permission perm) {
        if(systemSecurityManager!=null){
            systemSecurityManager.checkPermission(perm);
        }
    }

    @Override
    public void checkAccess(Thread t) {
        //线程的 stop, suspend, resume, setPriority, setName, setDaemon这些操作
        if(!commonCheck(sandboxControler.isThread(), null)){
            throwException("Thread Operation");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkAccess(t);
        }
    }

    @Override
    public void checkAccess(ThreadGroup g) {
        //线程的created, setDaemon, setMaxPriority, stop, suspend, resume, destroy这些操作
        if(!commonCheck(sandboxControler.isThread(), null)){
            throwException("ThreadGroup Operation");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkAccess(g);
        }
    }

    @Override
    public void checkExit(int status) {
        //System.exit 操作
        if(!commonCheck(sandboxControler.isExit(), null)){
            throwException("Exit Operation");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkExit(status);
        }
    }

    @Override
    public void checkExec(String cmd) {
        //启动新的进程的操作
        if(!commonCheck(sandboxControler.isExec(), null)){
            throwException("Execute " + cmd);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkExec(cmd);
        }
    }

    @Override
    public void checkLink(String lib) {
        //读取JNI库操作
        if(!commonCheck(sandboxControler.isLink(), null)){
            throwException("Link "+lib);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkLink(lib);
        }

    }

    @Override
    public void checkRead(String file) {
        //读文件操作
        if(!commonCheck(sandboxControler.getFile(), file)){
            throwException("Read "+file);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkRead(file);
        }

    }

    @Override
    public void checkWrite(String file) {
        //修改文件操作
        if(!commonCheck(sandboxControler.getFile(), file)){
            throwException("Write "+file);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkWrite(file);
        }
    }

    @Override
    public void checkDelete(String file) {
        //删除文件操作
        if(!commonCheck(sandboxControler.getFile(), file)){
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
        if(!commonCheck(sandboxControler.getNetwork(), addrsss)){
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
        if(!commonCheck(sandboxControler.getNetwork(), addrsss)){
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
        if(!commonCheck(sandboxControler.getNetwork(), addrsss)){
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
        if(!commonCheck(sandboxControler.getNetwork(), addrsss)){
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
        if(!commonCheck(sandboxControler.getNetwork(), addrsss)){
            throwException("Multicast "+addrsss);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkMulticast(maddr);
        }
    }

    @Override
    public void checkPropertiesAccess() {
        //系统属性的访问操作
        if(!commonCheck(sandboxControler.isProperties(), null)){
            throwException("System Property");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkPropertiesAccess();
        }
    }

    @Override
    public void checkPropertyAccess(String key) {
        //特定系统属性访问操作
        if(!commonCheck(sandboxControler.isProperties(), null)){
            throwException("System Property");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkPropertyAccess(key);
        }
    }

    @Override
    public void checkPrintJobAccess() {
        //文件打印操作
        if(!commonCheck(sandboxControler.isPrintJob(), null)){
            throwException("Print Job");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkPrintJobAccess();
        }
    }

    @Override
    public void checkPackageAccess(String pkg) {
        //包访问操作
        if(!commonCheck(sandboxControler.getPackageAccess(), pkg)) {
            throwException("Package "+pkg);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkPackageAccess(pkg);
        }
    }

    @Override
    public void checkPackageDefinition(String pkg) {
        //包定义操作
        if (!commonCheck(sandboxControler.getPackageDefintion(), pkg)) {
            throwException("Package "+pkg);
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkPackageDefinition(pkg);
        }
    }

    @Override
    public void checkSetFactory() {
        //ServerSocket or Socket 的工厂操作
        if(!commonCheck(sandboxControler.isFactory(), null)){
            throwException("Socket Factory");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkSetFactory();
        }
    }

    public void checkSecurityAccess(String target){
        //ServerSocket or Socket 的工厂操作
        if(!commonCheck(sandboxControler.isSecurityAccess(), null)){
            throwException("Security Access");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkSecurityAccess(target);
        }
    }

    public void checkCreateClassLoader(){
        //ServerSocket or Socket 的工厂操作
        if(!commonCheck(sandboxControler.isCreateClassLoader(), null)){
            throwException("Create ClassLoader");
        }

        if(systemSecurityManager!=null){
            systemSecurityManager.checkCreateClassLoader();
        }
    }
}
