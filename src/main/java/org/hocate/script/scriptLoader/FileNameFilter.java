package org.hocate.script.scriptLoader;

import java.io.File;
import java.io.FileFilter;

/**
 * 文件过滤器
 * @author helyho
 *
 */
public class FileNameFilter implements FileFilter{
	private String fileRegex;
	public FileNameFilter(String fileRegex){
		this.fileRegex = fileRegex;
	}
	
	@Override
	public boolean accept(File file) {
		if (file.isDirectory()){
			return true;
		}
		else if (file.isHidden()){
			return false;
		}
		else if (file.isFile() && file.getName().matches(fileRegex)) {
			return false;
		} else {
			return true;
		}
	}
}