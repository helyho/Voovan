package org.voovan.network;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TEnv;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.ByteBuffer;

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
	 * @param engine  SSLEngine对象
	 * @param session session 对象
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
	 * @return 握手是否完成
	 */
	public boolean isHandShakeDone() {
		return handShakeDone;
	}

	/**
	 * 获取 SSLEngine
	 * @return  SSLEngine 对象
	 */
	public SSLEngine getSSLEngine(){
		return engine;
	}

	public ByteBuffer buildNetDataBuffer() {
		SSLSession sslSession = engine.getSession();
		int newBufferMax = sslSession.getPacketBufferSize();
		return ByteBuffer.allocateDirect(newBufferMax);
	}

	public ByteBuffer buildAppDataBuffer() {
		SSLSession sslSession = engine.getSession();
		int newBufferMax = sslSession.getPacketBufferSize();
		return ByteBuffer.allocateDirect(newBufferMax);
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
	 * @throws IOException IO 异常
	 */
	public SSLEngineResult warpData(ByteBuffer buffer) throws IOException{
		netData.clear();
		SSLEngineResult engineResult = null;
		do{
			engineResult = engine.wrap(buffer, netData);
			netData.flip();
			if(session.isConnected() && engineResult.bytesProduced()>0 && netData.limit()>0){
				session.send0(netData);
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
	 * @param netBuffer    	接受解包数据的缓冲区
	 * @param appBuffer		接受解包后数据的缓冲区
	 * @return				SSLEngineResult 对象
	 * @throws SSLException SSL 异常
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
		SSLEngineResult engineResult = null;
		while(true){
			clearBuffer();
			ByteBufferChannel byteBufferChannel = session.getByteBufferChannel();

			if(byteBufferChannel.isReleased()){
				new IOException("Socket is terminate");
			}

			if(byteBufferChannel.size() > 0)  {
				ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer();
				engineResult = unwarpData(byteBuffer, appData);
				//如果有 HandShake Task 则执行
				handshakeStatus = runDelegatedTasks();
				byteBufferChannel.compact();

				if(engineResult!=null &&
						engineResult.getStatus()==Status.OK &&
						byteBuffer.remaining() == 0){
					break;
				}

				if(engineResult!=null &&
						(engineResult.getStatus() == Status.BUFFER_OVERFLOW ||
								engineResult.getStatus() == Status.BUFFER_UNDERFLOW ||
								engineResult.getStatus() == Status.CLOSED)
						){
					break;
				}

			}

			TEnv.sleep(1);
		};
		return handshakeStatus;
	}

	/**
	 * 执行委派任务
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
					break;
				default:
					break;
			}
//			TEnv.sleep(1);
		}
		return handShakeDone;
	}

	/**
	 * 读取SSL消息到缓冲区
	 * @param session Socket 会话对象
	 * @param netByteBufferChannel Socket SSL 加密后的数据
	 * @param appByteBufferChannel Socket SSL 解密后的数据
	 * @return 接收数据大小
	 * @throws IOException  IO异常
	 */
	public int unWarpByteBufferChannel(IoSession session, ByteBufferChannel netByteBufferChannel, ByteBufferChannel appByteBufferChannel) throws IOException{
		int readSize = 0;

		if(session.isConnected() && netByteBufferChannel.size()>0){
			SSLEngineResult engineResult = null;

			while(true) {
				appData.clear();
				ByteBuffer byteBuffer = netByteBufferChannel.getByteBuffer();

				engineResult = unwarpData(byteBuffer, appData);
				netByteBufferChannel.compact();

				appData.flip();
				appByteBufferChannel.writeEnd(appData);

				if(engineResult!=null &&
						engineResult.getStatus()==Status.OK  &&
						byteBuffer.remaining() == 0){
					break;
				}

				if(engineResult!=null &&
						(engineResult.getStatus() == Status.BUFFER_OVERFLOW ||
								engineResult.getStatus() == Status.BUFFER_UNDERFLOW  ||
								engineResult.getStatus() == Status.CLOSED)
						){
					break;
				}
			}
		}
		return readSize;
	}

	public void release(){
		TByteBuffer.release(netData);
		TByteBuffer.release(appData);
	}
}
