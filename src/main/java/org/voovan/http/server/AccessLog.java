package org.voovan.http.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

/**
 * HttpServer访问日志对象
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AccessLog implements Runnable {
	private ArrayBlockingQueue<String>	logQueue;
	private OutputStream outputStream;
	
	
	
	/**
	 * 构造函数
	 * @throws FileNotFoundException 
	 */
	public AccessLog() throws FileNotFoundException {
		this.logQueue = new ArrayBlockingQueue<String>(100000);
		String accessLogFileName = TEnv.getContextPath()+File.separator+"logs"+File.separator+"access.log";
		outputStream = new FileOutputStream(accessLogFileName,true);
	}

	
	public void writeAccessLog(HttpRequest request,HttpResponse response){
		StringBuffer content = new StringBuffer();
		content.append("["+TDateTime.now()+"]");
		content.append(" "+request.getRemoteAddres()+":"+request.getRemotePort());
		content.append(" "+request.protocol().getProtocol()+"/"+request.protocol().getVersion()+" "+request.protocol().getMethod());
		content.append(" "+response.protocol().getStatus());
		content.append(" "+response.body().getBodyBytes().length);
		content.append("\t "+request.protocol().getPath());
		content.append("\t "+TObject.nullDefault(request.header().get("User-Agent"),""));
		content.append("\t "+TObject.nullDefault(request.header().get("Referer"),""));
		content.append("\r\n");
		logQueue.add( content.toString() );
	}
	
	/**
	 * 关闭所有的OutputStream
	 */
	public void closeOutputStreams() {
		try {
			if (outputStream != null) {
				outputStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 增加消息
	 * 
	 * @param string
	 */
	public synchronized void addLogMessage(String msg) {
		logQueue.add(msg);
	}

	@Override
	public void run() {
		int loopCount = 0;
		while (true) {
			try {
				loopCount++;
				String accessLogMessage = logQueue.poll(500, TimeUnit.MILLISECONDS);
				if (accessLogMessage != null) {
					if (outputStream != null) {
						outputStream.write(accessLogMessage.getBytes());
						outputStream.flush();
					}
				}else if(isTerminate()){
					break;
				}else if (loopCount>=10*2 && logQueue.size() == 0) {
					break;
				}else if(outputStream==null){
					break;
				}
				TEnv.sleep(1);
			} catch (IOException | InterruptedException e) {
				Logger.error(e);
			}
		}
	}
	
	/**
	 * 检查线程是否处于结束状态
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean isTerminate(){
		//应用结束的线程标识
		List<String> destoryThreadNames = TObject.newList("DestroyJavaVM","ReaderThread");
		
		//获取系统内所有的线程
		Thread[] jvmThread = TEnv.getJVMThreads();
		
		//遍历是否包含线程结束标识
		for(Thread threadObj : jvmThread){
			for(String destoryThreadName : destoryThreadNames){
				if(threadObj.getName().contains(destoryThreadName)){
					return true;
				}
			}
		}
		return false;
	}
	
}
