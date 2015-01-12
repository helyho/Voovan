package org.hocate.biz.config;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hocate.script.ScriptManager;
import org.hocate.script.scriptLoader.ScriptDbLoader;
import org.hocate.script.scriptLoader.ScriptLoader;
import org.hocate.tools.TEnv;
import org.hocate.tools.TFile;
import org.hocate.tools.TObject;
import org.hocate.tools.TProperties;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

public class ConfigLoader {
	private static Logger	logger	= Logger.getLogger(ConfigLoader.class);
	private DruidDataSource	dataSource;

	private File			propertiesFile;

	public ConfigLoader() {
		propertiesFile = new File(TEnv.getSysPathFromContext("Config" + File.separator + "config.properties"));
	}

	private void buildDataSource() {
		String druidPath = TEnv.getSysPathFromContext("Config" + File.separator
				+ TProperties.getString(propertiesFile, "Database.Druid"));
		try {
			Properties druidProperites = TProperties.getProperties(new File(druidPath));
			dataSource = TObject.cast(DruidDataSourceFactory.createDataSource(druidProperites));
			dataSource.init();
			logger.info("Database connection pool init finished");
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public DruidDataSource getDataSource() {
		if (dataSource == null) {
			buildDataSource();
		}
		return dataSource;
	}

	private ScriptLoader createScriptLoader() {
		return new ScriptDbLoader(getDataSource());
	}

	public ScriptManager createScriptManager() {
		ScriptManager scriptManager = new ScriptManager(this.createScriptLoader(), TProperties.getInt(propertiesFile,
				"Script.RefreshDelay"));
		// 将脚本管理器放入脚本上下文
		scriptManager.putObject("ScriptManager", scriptManager);
		// 系统脚本跟路径
		String coreScriptRootPath = "org/hocate/script/coreScriptCode/";

		// 加载系统核心类
		String scriptCode = new String(TFile.loadResource(coreScriptRootPath + "System.js"));
		scriptManager.evalCode(scriptCode);
		// 加载网络操作类
		scriptCode = new String(TFile.loadResource(coreScriptRootPath + "Network.js"));
		scriptManager.evalCode(scriptCode);
		// 加载网络操作类
		scriptCode = new String(TFile.loadResource(coreScriptRootPath + "Database.js"));
		scriptManager.evalCode(scriptCode);
		// 加载网络操作类
		// scriptCode = UEnv.loadResource(coreScriptRootPath+"HttpServer.js");
		// scriptManager.evalCode(scriptCode);

		return scriptManager;
	}
}
