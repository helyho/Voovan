package org.voovan.test.db;

import org.voovan.db.recorder.annotation.NotInsert;
import org.voovan.db.recorder.annotation.NotUpdate;
import org.voovan.db.recorder.annotation.PrimaryKey;
import org.voovan.db.recorder.annotation.Table;
import org.voovan.tools.TFile;
import org.voovan.tools.security.THash;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * 脚本实体
 * @author helyho
 *
 */
@Table("sc_script")
public class ScriptEntity implements Serializable{
	@PrimaryKey
	@NotInsert
	private int id;

	/**
	 * 脚本路径
	 */
	@NotInsert
	private String packagePath;
	/**
	 * 脚本版本
	 */
	private float version;
	/**
	 * 源码路径
	 */
	private String sourcePath;
	/**
	 * 脚本代码内容
	 */
	private String sourceCode;

	/**
	 * 脚本实体文件更新日期
	 */
	private long fileDate;

	/**
	 * 脚本是否可以自动重载最新的内容
	 * 		1:可重新加载
	 * 		0:不可重新加载
	 */
	private int canReload;

	public ScriptEntity(){
		//默认可重新加载
		canReload = 1;
	}

	/**
	 * 构造函数
	 * @param packagePath		包路径
	 * @param sourceCode		脚本源文件路径
	 */
	public ScriptEntity(String packagePath,String sourceCode){
		this.packagePath = packagePath;
		this.version = 1;
		this.sourceCode = sourceCode;
		canReload = 1;
	}

	/**
	 * 构造函数
	 * @param packagePath	包路径
	 * @param version		脚本文件版本
	 * @param sourcePath	脚本源文件路径
	 */
	public ScriptEntity(String packagePath,float version,String sourcePath){
		this.packagePath = packagePath;
		this.version = version;
		this.sourcePath = sourcePath;
		canReload = 1;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPackagePath() {
		return packagePath;
	}

	public void setPackagePath(String packagePath) {
		this.packagePath = packagePath;
	}

	public float getVersion() {
		return version;
	}

	public void setVersion(float version) {
		this.version = version;
	}

	/**
	 * 获取脚本源代码
	 * 		源代码为空或者代码文件发生变更后读取新文件
	 * @return
	 */
	public String getSourceCode() {
		if (sourceCode == null || isChanged()) {
			try {
				loadSourceCode();
			} catch (Exception e) {
				Logger.error(e);
			}
		}
		return sourceCode;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	/**
	 * 是否可重新读取
	 * @return
	 */
	public boolean canReload() {
		return canReload==1?true:false;
	}

	/**
	 * 通过检查文件时间判断文件是否变更
	 * @return
	 */
	public boolean isChanged(){
		File sourceFile = new File(sourcePath);

		if(!sourceFile.exists())
		{
			Logger.warn("Script File :"+sourcePath+" is not exists.");
			return false;
		}

		if(fileDate==0){
			fileDate = sourceFile.lastModified();
			return true;
		}
		else{
			if(fileDate != sourceFile.lastModified()){
				return true;
			}
		}
		return false;
	}

	/**
	 * 从文件读取脚本内容
	 */
	public synchronized void loadSourceCode(){
		File sourceFile = new File(sourcePath);
		this.fileDate = sourceFile.lastModified();
		sourceCode = new String(TFile.loadFileFromSysPath(sourcePath));
		Logger.fremawork("Reload script code : "+sourcePath);
	}


	/**
	 * 将入参对象的属性 copy 到本地属性
	 * @param source
	 * @throws IOException
	 */
	public void copy(ScriptEntity source){
		this.packagePath = source.packagePath;
		this.version = source.version;
		this.sourcePath = source.sourcePath;
		this.sourceCode = source.sourceCode;
		this.canReload = source.canReload;
		this.fileDate = source.fileDate;
	}

	@Override
	public String toString(){
		return "{PackagePath="+this.packagePath+",Version="+this.version+",SourcePath="+sourcePath+"}";
	}

	/**
	 * 判断两个对象实体是否相等
	 * @param entity
	 * @return
	 */
	@Override
	public boolean equals(Object obj){
		if(obj instanceof ScriptEntity){
			ScriptEntity entity = (ScriptEntity)obj;
			if(entity.packagePath.equals(this.packagePath) && entity.version == this.version){
				return true;
			}
			else{
				return false;
			}
		}
		else{
			return false;
		}
	}

	@Override
	public int hashCode(){
		return genHashCode(packagePath,version);
	}

	public static int genHashCode(String packagePath,float version){
		return THash.hash_time33(packagePath+version);
	}

}
