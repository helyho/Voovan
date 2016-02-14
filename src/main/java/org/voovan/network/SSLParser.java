package org.voovan.network;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.log.Logger;

/**
 * SSL 解析器
 * 		1.握手信息 
 * 		2.报文信息
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SSLParser {
	private SSLEngine engine;
	private ByteBuffer appData;
	private ByteBuffer netData;
	private IoSession session;
	boolean handShakeDone = false;
	/**
	 * 构造函数
	 * @param engine
	 * @param session
	 */
	public SSLParser(SSLEngine engine,IoSession session) {
		this.engine = engine;
		this.session = session;
		session.setSSLParser(this);
		this.appData= buildAppDataBuffer();
		this.netData = buildNetDataBuffer();
	}
	
	/**
	 * 判断握手是否完成
	 * @return
	 */
	public boolean isHandShakeDone() {
		return handShakeDone;
	}

	/**
	 * 获取 SSLEngine
	 * @return
	 */
	public SSLEngine getSSLEngine(){
		return engine;
	}
	
	protected ByteBuffer buildNetDataBuffer() {
		SSLSession sslSession = engine.getSession();
		int newBufferMax = sslSession.getPacketBufferSize();
		return ByteBuffer.allocate(newBufferMax);
	}
	
	protected ByteBuffer buildAppDataBuffer() {
		SSLSession sslSession = engine.getSession();
		int newBufferMax = sslSession.getPacketBufferSize();
		return ByteBuffer.allocate(newBufferMax);
	}
	
	/**
	 * 清理缓冲区
	 */
	private void clearBuffer(){
		appData.clear();
		netData.clear();
	}
	
	/**
	 * 打包并发送数据
	 * @param buffer       需要的数据缓冲区
	 * @return 			   返回成功执行的最后一个或者失败的那个 SSLEnginResult
	 * @throws IOException
	 */
	public SSLEngineResult warpData(ByteBuffer buffer) throws IOException{
		netData.clear();
		SSLEngineResult engineResult = null;
		do{
			engineResult = engine.wrap(buffer, netData);
			netData.flip();
			if(session.isConnect() && engineResult.bytesProduced()>0 && netData.limit()>0){
				session.send(netData);
			}
			netData.clear();
		}while(engineResult.getStatus() == Status.OK && buffer.hasRemaining());
		return engineResult;
	}
	
	/**
	 * 处理握手 Warp;
	 * @return
	 * @throws IOException 
	 * @throws Exception
	 */
	private HandshakeStatus doHandShakeWarp() throws IOException {
		clearBuffer();
		appData.flip();
		warpData(appData);
		//如果有 HandShake Task 则执行
		HandshakeStatus handshakeStatus = runDelegatedTasks();
		return handshakeStatus;
	}
	
	/**
	 * 解包数据
	 * @param buffer    	接受解包数据的缓冲区
	 * @return
	 * @throws SSLException 
	 * @throws Exception
	 */
	public SSLEngineResult unwarpData(ByteBuffer netBuffer,ByteBuffer appBuffer) throws SSLException{
		SSLEngineResult engineResult = null;
		engineResult = engine.unwrap(netBuffer, appBuffer);
		return engineResult;
	}
	
	/**
	 * 处理握手 Unwarp;
	 * @return
	 * @throws IOException 
	 * @throws Exception
	 */
	private HandshakeStatus doHandShakeUnwarp() throws IOException{
			HandshakeStatus handshakeStatus =engine.getHandshakeStatus();
			ByteBufferChannel netDataChannel = new ByteBufferChannel();
			SSLEngineResult engineResult = null;
			do{
				clearBuffer();
				session.read(netData);
				netDataChannel.write(netData);
				do{
					engineResult = unwarpData(netDataChannel.getBuffer(),appData);
					//如果有 HandShake Task 则执行
					handshakeStatus = runDelegatedTasks();
				}while(engineResult!=null && engineResult.getStatus()==Status.OK && netDataChannel.getBuffer().hasRemaining());
			}while(engineResult!=null && engineResult.getStatus()!=Status.OK);
			netDataChannel.close();
			return handshakeStatus;
	}
	
	/**
	 * 执行委派任务
	 * @param result
	 * @param engine
	 * @throws Exception
	 */
	private HandshakeStatus runDelegatedTasks()  {
		if(handShakeDone==false){
			if (engine.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
				Runnable runnable;
				while ((runnable = engine.getDelegatedTask()) != null) {
					runnable.run();
				}
			}
			return engine.getHandshakeStatus();
		}
		return null;
	}
	
	public boolean doHandShake() throws IOException{
		
		engine.beginHandshake();
		int handShakeCount = 0;
		HandshakeStatus handshakeStatus = engine.getHandshakeStatus();
		while(!handShakeDone && handShakeCount<20){
			handShakeCount++;
			switch (handshakeStatus) {
				case NEED_TASK:
					handshakeStatus = runDelegatedTasks();
					break;
				case NEED_WRAP:
					handshakeStatus = doHandShakeWarp();
					break;
				case NEED_UNWRAP:
					handshakeStatus = doHandShakeUnwarp();
 					break;
				case FINISHED:
					handshakeStatus = engine.getHandshakeStatus();
					break;
				case NOT_HANDSHAKING:
					handShakeDone = true;
					Logger.debug("HandShake is done!");
					break;
				default:
					break;
			}
		}
		return handShakeDone;
	}
}
