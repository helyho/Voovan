package org.voovan.tools.hotswap;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;

/**
 * 热部署核心类
 *      采用 JavaAgent 方式进行类的热部署
 *      使用这种热部署方式,不能新增方法和属性,也就说类的结构不能发生变化
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
    private int reloadIntervals;
    private HashWheelTask reloadTask;

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

        instrumentation = TEnv.agentAttach(null);
        loadCustomClass();

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

        //如果是内部类则排除
        if(clazz.isMemberClass()){
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
     * @param  intervals 自动重读的时间间隔, 单位: 秒
     * @throws UnmodifiableClassException  不可修改的 Class 异常
     * @throws ClassNotFoundException Class未找到异常
     */
    public void autoReload(int intervals) throws UnmodifiableClassException, ClassNotFoundException {
        this.reloadIntervals = intervals;

        cancelAutoReload();

        Logger.info("[HOTSWAP] Start auto reload and hotswap every " + intervals + " seconds");

        reloadTask = new HashWheelTask() {
            @Override
            public void run() {
                try {
                    List<ClassFileInfo> changedFiles = fileWatcher();
                    reloadClass(changedFiles);
                } catch (UnmodifiableClassException |ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };

        Global.getHashWheelTimer().addTask(reloadTask, intervals, true);
    }

    /**
     * 取消自动热加载操作
     */
    public void cancelAutoReload(){
        if(reloadTask != null) {
            reloadTask.cancel();
        }
    }

    /**
     * Class 文件信息
     */
    public class ClassFileInfo {
        private Class clazz;
        private long lastModified;

        public ClassFileInfo(Class clazz, boolean isAutoChek) {
            this.clazz = clazz;
            if(isAutoChek) {
                this.lastModified = TEnv.getClassModifyTime(clazz);
            }
        }



        public boolean isChanged(){

            long currentModified = TEnv.getClassModifyTime(clazz);

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

        public byte[] getBytes() {
            return TEnv.loadClassBytes(clazz);
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
