package org.hocate.script.scriptLoader;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.hocate.script.ScriptEntity;

/**
 * 脚本读取器--文件读取
 * @author helyho
 *
 */
public class ScriptFileLoader implements ScriptLoader {

	/**
	 * 自动校正为由目录分割符结束的路径
	 */
	private String rootPath;
	private Map<String,File> scriptFiles;
	
	public ScriptFileLoader(String rootPath){
		this.rootPath = rootPath.endsWith(File.separator)?rootPath:rootPath+File.separator;
		init();
	}
	
	/**
	 * 初始化函数
	 */
	private void init(){
		File file = new File(rootPath);
		scriptFiles = loadFiles(file);
	}
	
	/**
	 * 从文件目录数读取目录
	 * @param file
	 * @return  Map<相对路径,File 对象>
	 */
	private Map<String,File> loadFiles(File file){
		HashMap<String,File> result = new HashMap<String,File>();
		File[] fileList = file.listFiles(new FileNameFilter("$js") );
		for(File fileItem : fileList){
			if(fileItem.isDirectory()){
				result.putAll(loadFiles(fileItem));
			}
			else if(fileItem.isFile()){
				String fileSeparatorFix = File.separatorChar=='\\' ? "\\\\" : File.separator ;
				String path = fileItem.getPath().replaceAll(fileSeparatorFix, ".").substring(rootPath.length());
				path = path.substring(0,path.lastIndexOf("."));
				result.put(path,fileItem);
			}
		}
		return result;
	}
	
	public String rootPath() {
		return rootPath;
	}

	@Override
	public List<String> packagePaths() {
		Vector<String> packagePaths = new Vector<String>();
		for(String f : scriptFiles.keySet()){
			packagePaths.add(f);
		}
		return packagePaths;
	}
	
	@Override
	public synchronized List<ScriptEntity> scriptEntitys() {
		Vector<ScriptEntity> scriptEntitys = new Vector<ScriptEntity>();
		for(String packagePath: scriptFiles.keySet()){
			ScriptEntity se = new ScriptEntity(packagePath,1.0f,scriptFiles.get(packagePath).getPath());
			scriptEntitys.add(se);
		}
		return scriptEntitys;
	}
	
	@Override
	public synchronized ScriptEntity getScriptEntity(String packagePath) {
		ScriptEntity se = getScriptEntity(packagePath,1.0f);
		return se;
	}
	

	@Override
	public synchronized ScriptEntity getScriptEntity(String packagePath, float version) {
		ScriptEntity se = null;
		if(scriptFiles.get(packagePath)!=null)
			se = new ScriptEntity(packagePath,1.0f,scriptFiles.get(packagePath).getPath());
		return se;
	}

	@Override
	public void reLoad() {
		for(ScriptEntity scripeEntity:scriptEntitys()){
			scripeEntity.reloadInfo(this);
		}
	}
	

}
