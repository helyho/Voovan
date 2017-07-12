package org.voovan.tools.compiler.hotswap;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HotSwaper {
    private static final List<String> CLASS_PATH = TEnv.getClassPath();
    private Instrumentation instrumentation;
    private List<ClassFileInfo> classFileInfos;

    public HotSwaper(String agentJarPath) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        VirtualMachine vm = VirtualMachine.attach(Long.toString(TEnv.getCurrentPID()));
        vm.loadAgent(agentJarPath);
        instrumentation = DynamicAgent.getInstrumentation();
        classFileInfos = new ArrayList<ClassFileInfo>();

        for(String classpath : CLASS_PATH){
            List<File> classFiles = TFile.scanFile(new File(classpath), "\\.class$");
            for(File classFile : classFiles){
                if(classFile.getPath().contains("$")){
                    continue;
                }

                String classPath = classFile.getPath().replace(classpath, "");
                classPath = classPath.replace(".class","").replace(File.separator, ".");
                classPath = TString.removePrefix(classPath);

                try {
                    Class clazz = Class.forName(classPath);
                    ClassFileInfo classFileInfo = new ClassFileInfo(classFile, clazz);
                    classFileInfos.add(classFileInfo);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    continue;
                }
            }
        }
    }

    private List<ClassFileInfo> fileWatcher(){
        List<ClassFileInfo> changedFiles = new ArrayList<ClassFileInfo>();
        for(ClassFileInfo classFileInfo : classFileInfos){
            if(classFileInfo.isChanged()){
                changedFiles.add(classFileInfo);
            }
        }
        return changedFiles;
    }

    public void reloadClass(List<ClassFileInfo> changedFiles) throws UnmodifiableClassException, ClassNotFoundException {
        List<ClassDefinition> classDefinitions = new ArrayList<ClassDefinition>();

        for(ClassFileInfo classFileInfo : changedFiles) {
            byte[] bytes = TFile.loadFile(classFileInfo.getClassFile());
            ClassDefinition classDefinition = new ClassDefinition(classFileInfo.getClazz(), bytes);
            Logger.simple("[HOTSWAP] class:" + classFileInfo.getClazz() + " will reload.");
            classDefinitions.add(classDefinition);
        }

        instrumentation.redefineClasses(classDefinitions.toArray(new ClassDefinition[0]));
    }

    public void reloadClass(Class[] classes) throws UnmodifiableClassException, ClassNotFoundException {
        for(Class clazz : classes) {
            File classFile = null;
            for (String classpath : CLASS_PATH){
                classFile = new File(classpath + File.separator + clazz.getCanonicalName().replace(".", File.separator) + ".class");
                if(classFile.exists()){
                    break;
                }else{
                    classFile = null;
                }
            }

            if(classFile!=null) {
                byte[] bytes = TFile.loadFile(classFile);
                ClassDefinition classDefinition = new ClassDefinition(clazz, bytes);
                instrumentation.redefineClasses(new ClassDefinition[]{classDefinition});
            }
        }
    }

    public void autoReloadClass() throws UnmodifiableClassException, ClassNotFoundException {
        List<ClassFileInfo> changedFiles = fileWatcher();
        reloadClass(changedFiles);
    }

    public class ClassFileInfo {
        private File classFile;
        private Class clazz;
        private long lastModified;

        public ClassFileInfo(File classFile, Class clazz) {
            this.classFile = classFile;
            this.clazz = clazz;
            this.lastModified = classFile.lastModified();
        }

        public boolean isChanged(){
            if(getClassFile().lastModified() != getLastModified()){
                lastModified = classFile.lastModified();
                return true;
            }else{
                return false;
            }
        }

        public File getClassFile() {
            return classFile;
        }

        public void setClassFile(File classFile) {
            this.classFile = classFile;
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
    }
}
