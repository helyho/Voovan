package org.voovan.tools.hotswap;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Hotswaper {
    private Instrumentation instrumentation;
    private List<ClassFileInfo> classFileInfos;
    private List<String> excludePackages;
    private Timer realoadTimer;
    private int reloadIntervals;

    /**
     * 构造函数
     * @throws IOException IO 异常
     * @throws AttachNotSupportedException 附加指定进程失败
     * @throws AgentLoadException Agent 加载异常
     * @throws AgentInitializationException Agent 初始化异常
     */
    public Hotswaper() throws IOException, AgentInitializationException, AgentLoadException, AttachNotSupportedException {
        init(null);
    }

    /**
     * 构造函数
     * @param agentJarPath AgentJar 文件
     * @throws IOException IO 异常
     * @throws AttachNotSupportedException 附加指定进程失败
     * @throws AgentLoadException Agent 加载异常
     * @throws AgentInitializationException Agent 初始化异常
     */
    public Hotswaper(String agentJarPath) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        init(new File(agentJarPath));
    }

    private void init(File agentJar) throws AgentInitializationException, AgentLoadException, AttachNotSupportedException, IOException {
        reloadIntervals = 5;
        classFileInfos = new ArrayList<ClassFileInfo>();

        excludePackages = TObject.asList("java.","sun.","javax.","com.sun","com.oracle");

        if(agentJar == null) {
            agentJar = findAgentJar();
        }

        if(agentJar != null && agentJar.exists()) {
            Logger.info("[HOTSWAP] System choose an agent jar file: "+agentJar.getAbsolutePath());
            agentAttach(agentJar.getPath());
            loadCustomClass();
        } else {
            throw new FileNotFoundException("The agent jar file not found");
        }
    }

    /**
     * 查找 AgentJar 文件
     * @return AgentJar 文件
     */
    private File findAgentJar(){
        List<File> agentJars = TFile.scanFile(new File(TFile.getContextPath()), "voovan-(framework|common)-?(\\d\\.?)*\\.?jar$");
        File agentJar = null;

        for (File jarFile : agentJars) {
            if(agentJar == null){
                agentJar = jarFile;
            }

            if(jarFile.lastModified() < jarFile.lastModified()){
                agentJar = jarFile;
            }
        }

        return agentJar;
    }

    /**
     * 附加 Agentjar 到目标地址
     * @param agentJarPath AgentJar 文件
     * @throws IOException IO 异常
     * @throws AttachNotSupportedException 附加指定进程失败
     * @throws AgentLoadException Agent 加载异常
     * @throws AgentInitializationException Agent 初始化异常
     */
    private void agentAttach(String agentJarPath) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        VirtualMachine vm = VirtualMachine.attach(Long.toString(TEnv.getCurrentPID()));
        vm.loadAgent(agentJarPath);
        instrumentation = DynamicAgent.getInstrumentation();
        vm.detach();
    }

    /**
     * 读取所有的用户类
     * @return 用户类信息
     */
    private void loadCustomClass(){
        for(Class clazz : instrumentation.getAllLoadedClasses()){

            if(isExcludeClass(clazz)){
                continue;
            }

            ClassFileInfo classFileInfo = new ClassFileInfo(clazz, true);
            classFileInfos.add(classFileInfo);
        }
    }

    /**
     * 检查是否是热部署排除在外的class
     * @param clazz class 对象
     * @return true:排除的 class, false:未排除的 class
     */
    private boolean isExcludeClass(Class clazz){
        String className = clazz.getCanonicalName();

        //基本类型部排除
        if(clazz.isPrimitive()){
            return true;
        }

        //没有完全现定名的 class 不加载
        if(className == null){
            return true;
        }

        //CodeSource 对象为空的不加载
        if(clazz.getProtectionDomain().getCodeSource() == null){
            return true;
        }

        //排除的包中的 class 不加载
        for(String excludePackage : excludePackages){
            if(className.startsWith(excludePackage)){
                return true;
            }
        }

        //加载过的 class 不加载
        for(ClassFileInfo classFileInfo: classFileInfos){
            if(classFileInfo.getClazz() == clazz){
                return true;
            }
        }

        return false;
    }

    /**
     * 文件变更监视器
     * @return 发生变更的 ClassFileInfo 对象
     */
    private List<ClassFileInfo> fileWatcher(){
        loadCustomClass();
        List<ClassFileInfo> changedFiles = new ArrayList<ClassFileInfo>();
        for(ClassFileInfo classFileInfo : classFileInfos){
            if(classFileInfo.isChanged()){
                changedFiles.add(classFileInfo);
            }
        }
        return changedFiles;
    }

    /**
     * 重新热加载Class
     * @param changedFiles 有过变更的文件信息
     * @throws UnmodifiableClassException  不可修改的 Class 异常
     * @throws ClassNotFoundException Class未找到异常
     */
    public void reloadClass(List<ClassFileInfo> changedFiles) throws UnmodifiableClassException, ClassNotFoundException {
        List<ClassDefinition> classDefinitions = new ArrayList<ClassDefinition>();

        for(ClassFileInfo classFileInfo : changedFiles) {
            ClassDefinition classDefinition = new ClassDefinition(classFileInfo.getClazz(), classFileInfo.getBytes());
            Logger.info("[HOTSWAP] class:" + classFileInfo.getClazz() + " will reload.");
            classDefinitions.add(classDefinition);
        }

        instrumentation.redefineClasses(classDefinitions.toArray(new ClassDefinition[0]));
    }

    /**
     * 重新热加载 Class
     * @param customClasses class 数组
     * @throws UnmodifiableClassException  不可修改的 Class 异常
     * @throws ClassNotFoundException Class未找到异常
     */
    public void reloadClass(Class[] customClasses) throws UnmodifiableClassException, ClassNotFoundException {
        ArrayList<ClassFileInfo> customClassFileInfos = new ArrayList<ClassFileInfo>();
        for(Class clazz : customClasses){
            ClassFileInfo classFileInfo = new ClassFileInfo(clazz, false);
            customClassFileInfos.add(classFileInfo);
        }

        reloadClass(customClassFileInfos);
    }

    /**
     * 获取当前自动热部署间隔事件
     * @return 自动热部署间隔事件
     */
    public int getReloadIntervals() {
        return reloadIntervals;
    }

    /**
     * 自动热加载 Class
     * @param  intervals 自动重读的时间间隔
     * @throws UnmodifiableClassException  不可修改的 Class 异常
     * @throws ClassNotFoundException Class未找到异常
     */
    public void autoReload(int intervals) throws UnmodifiableClassException, ClassNotFoundException {
        this.reloadIntervals = intervals;

        cancelAutoReload();
        realoadTimer =  new Timer("VOOVAN@HOTSWAP_TIMER");

        Logger.info("[HOTSWAP] Start auto reload and hotswap every " + intervals + " seconds");

        realoadTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(TEnv.isMainThreadShutDown()){
                    realoadTimer.cancel();
                }

                try {
                    List<ClassFileInfo> changedFiles = fileWatcher();
                    reloadClass(changedFiles);
                } catch (UnmodifiableClassException |ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }, 0, intervals*1000);
    }

    /**
     * 取消自动热加载操作
     */
    public void cancelAutoReload(){
        if(realoadTimer != null) {
            realoadTimer.cancel();
        }
    }

    /**
     * Class 文件信息
     */
    public class ClassFileInfo {
        private String location;
        private Class clazz;
        private long lastModified;
        private String classNamePath;

        public ClassFileInfo(Class clazz, boolean isAutoChek) {
            this.location = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
            this.clazz = clazz;
            if(isAutoChek) {
                this.lastModified = getClassLastModified();
            }
        }

        private long getClassLastModified(){
            if(classNamePath == null){
                this.classNamePath = clazz.getCanonicalName().replaceAll("\\.", File.separator)+".class";
            }

            try {
                if (location.endsWith(".jar")) {
                    try(JarFile jarFile = new JarFile(location)) {
                        JarEntry jarEntry = jarFile.getJarEntry(classNamePath);
                        return jarEntry.getTime();
                    }
                } else if (location.endsWith(File.separator)) {
                    return new File(location+classNamePath).lastModified();
                } else {
                    return -1;
                }
            }catch (IOException e){
                return -1;
            }
        }

        public boolean isChanged(){

            long currentModified = getClassLastModified();

            if(currentModified < 0 ){
                return false;
            }

            if(currentModified != lastModified){
                lastModified = currentModified;
                return true;
            }else{
                return false;
            }
        }

        public byte[] getBytes(){
            return TFile.loadResource(classNamePath);
        }

        public Class getClazz() {
            return clazz;
        }

        public void setClazz(Class clazz) {
            this.clazz = clazz;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        public String toString(){
            return clazz.getCanonicalName();
        }

        public boolean equals(ClassFileInfo classFileInfo) {
            return classFileInfo.getClazz() == this.clazz;
        }
    }
}
