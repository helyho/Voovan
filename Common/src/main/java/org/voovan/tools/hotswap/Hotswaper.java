package org.voovan.tools.hotswap;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import org.voovan.Global;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 热部署核心类
 *      采用 JavaAgent 方式进行类的热部署
 *      使用这种热部署方式,不能新增方法和属性,也就说类的结构不能发生变化
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Hotswaper {
    public static List<Consumer<Class>> WATCHERS = new ArrayList<Consumer<Class>>();

    private static Hotswaper HOT_SWAPER;

    private HashMap<String, ClassFileInfo> classFileInfos;
    private HashMap<String, Long> currentLastModifyCache;
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
    private Hotswaper() throws IOException, AgentInitializationException, AgentLoadException, AttachNotSupportedException {
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
    private Hotswaper(String agentJarPath) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        init(new File(agentJarPath));
    }

    private void init(File agentJar) throws AgentInitializationException, AgentLoadException, AttachNotSupportedException, IOException {
        reloadIntervals = 5;
        classFileInfos = new HashMap<String, ClassFileInfo>();
        currentLastModifyCache = new HashMap<String, Long>();

        excludePackages = TObject.asList("java.","sun.","javax.","com.sun","com.oracle");

        TEnv.agentAttach(null);

        if(TEnv.instrumentation==null){
            throw new AgentInitializationException("instrumentation is not inited");
        }

        loadCustomClass();
    }

    /**
     * 读取所有的用户类
     * @return 用户类信息
     */
    private void loadCustomClass(){
        for(Class clazz : TEnv.instrumentation.getAllLoadedClasses()){
            if(isExcludeClass(clazz)){
                continue;
            }

            ClassFileInfo classFileInfo = new ClassFileInfo(clazz, true);
            classFileInfos.put(clazz.toString(), classFileInfo);
        }
    }

    /**
     * 检查是否是热部署排除在外的class
     * @param clazz class 对象
     * @return true:排除的 class, false:未排除的 class
     */
    private boolean isExcludeClass(Class clazz){
        try {

            String packageName = clazz.getPackage().getName();

            //基本类型部排除
            if (clazz.isPrimitive()) {
                return true;
            }

            //如果是内部类则排除
            if (clazz.isMemberClass()) {
                return true;
            }

            //如果是有 java 编译的 (如: lambda)
            if (clazz.isSynthetic()) {
                return true;
            }

            //CodeSource 对象为空的不加载
            if (clazz.getProtectionDomain().getCodeSource() == null) {
                return true;
            }

            //没有完全现定名的 class 不加载
            if (packageName == null) {
                return true;
            }

            //排除的包中的 class 不加载
            for (String excludePackage : excludePackages) {
                if (packageName.startsWith(excludePackage)) {
                    return true;
                }
            }

            //加载过的 class 不加载
            if(classFileInfos.containsKey(clazz.toString())) {
                return true;
            }

            return false;
        } catch (Throwable e){
            return true;
        }
    }

    /**
     * 文件变更监视器
     * @return 发生变更的 ClassFileInfo 对象
     */
    private List<ClassFileInfo> fileWatcher(){
        currentLastModifyCache.clear();
        loadCustomClass();
        List<ClassFileInfo> changedFiles = new ArrayList<ClassFileInfo>();
        for(ClassFileInfo classFileInfo : classFileInfos.values()){
            if(classFileInfo.isChanged()){
                changedFiles.add(classFileInfo);
            }
        }
        return changedFiles;
    }

    /**
     * 重新热加载Class
     * @param clazzDefines 有过变更的文件信息
     * @throws UnmodifiableClassException  不可修改的 Class 异常
     * @throws ClassNotFoundException Class未找到异常
     */
    public static void reloadClass(Map<Class, byte[]> clazzDefines) throws UnmodifiableClassException, ClassNotFoundException {
        for(Map.Entry<Class, byte[]> clazzDefine : clazzDefines.entrySet()){
            Class clazz = clazzDefine.getKey();
            byte[] classBytes = clazzDefine.getValue();

            ClassDefinition classDefinition = new ClassDefinition(clazz, classBytes);
            try {
                Logger.info("[HOTSWAP] " + TDateTime.now() + " " +  clazz.getName() + " will reload.");
                TEnv.instrumentation.redefineClasses(classDefinition);

                for(Consumer<Class> wather : WATCHERS){
                    wather.accept(clazz);
                }

            } catch (Exception e) {
                Logger.error("[HOTSWAP] " + TDateTime.now() + " class->" + clazz.getCanonicalName() + " reload failed \r\n", e);
            }
        }
    }

    /**
     * 重新热加载Class
     * @param changedFiles 有过变更的文件信息
     * @throws UnmodifiableClassException  不可修改的 Class 异常
     * @throws ClassNotFoundException Class未找到异常
     */
    public void reloadClass(List<ClassFileInfo> changedFiles) throws UnmodifiableClassException, ClassNotFoundException {
        HashMap<Class, byte[]> classDefines = new HashMap<Class, byte[]>();
        for(ClassFileInfo classFileInfo : changedFiles) {
            classDefines.put(classFileInfo.getClazz(), classFileInfo.getBytes());
        }

        reloadClass(classDefines);
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

        Logger.simple("[HOTSWAP] "  + TDateTime.now() + " Start auto reload and hotswap every " + intervals + " seconds");

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

    public Map<String, ClassFileInfo> getClassFileInfos() {
        return classFileInfos;
    }


    public synchronized static Hotswaper get() throws AgentInitializationException, AgentLoadException, AttachNotSupportedException, IOException {
        if(HOT_SWAPER == null) {
            return new Hotswaper();
        }

        return HOT_SWAPER;
    }

    public synchronized static Hotswaper get(String agentJarPath) throws AgentInitializationException, AgentLoadException, AttachNotSupportedException, IOException {
        if(HOT_SWAPER == null) {
            return new Hotswaper(agentJarPath);
        }

        return HOT_SWAPER;
    }

    /**
     * Class 文件信息
     */
    public class ClassFileInfo {
        private Class clazz;
        private String location;
        private long lastModified ;
        private long packageLastModify = -1L;

        public ClassFileInfo(Class clazz, boolean isAutoChek) {
            this.clazz = clazz;
            if(isAutoChek) {
                this.location = TEnv.getClassLocation(clazz);
                if(this.location!= null && this.location.endsWith("jar")) {
                    this.packageLastModify = TFile.getFileLastModify(location);
                }
                this.lastModified = TEnv.getClassModifyTime(clazz);
            }
        }

        public boolean isChanged(){
            long currentModified ;

            if(packageLastModify!=-1){
                currentModified = currentLastModifyCache.computeIfAbsent(clazz.toString(), clazz->TFile.getFileLastModify(location));

                //jar 包更新时间无变化则不继续检查具体的包内文件
                if(packageLastModify==currentModified) {
                    return false;
                }
            }

            currentModified = TEnv.getClassModifyTime(clazz);

            if(currentModified < 0 ){
                return false;
            }

            if(currentModified != lastModified){
                lastModified = currentModified;
                return true;
            } else {
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

        public boolean equals(ClassFileInfo classFileInfo) {
            return classFileInfo.getClazz() == this.clazz;
        }

        @Override
        public String toString() {
            return  clazz + "-> " + lastModified ;
        }
    }
}
