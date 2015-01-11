package org.hocate.network;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

public class SSLManager {
	private KeyManagerFactory keyManagerFactory;
	private TrustManagerFactory trustManagerFactory;
	private SSLContext context;
	private SSLEngine engine;
	private boolean useClientAuth;
	private String protocol;
	
	/**
	 * 构造函数
	 * 		默认使用客户端认证
	 * @param protocol    协议类型
	 * @throws Exception
	 */
	public SSLManager(String protocol) throws Exception{
		this.useClientAuth = true;
		this.protocol = protocol;
		keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
	}
	
	/**
	 * 构造函数
	 * @param protocol  	协议类型
	 * @param useClientAuth	是否使用客户端认证
	 * @throws Exception
	 */
	public SSLManager(String protocol,boolean useClientAuth) throws Exception{
		this.useClientAuth = useClientAuth;
		this.protocol = protocol;
		keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
	}
	
	/**
	 * 读取管理证书
	 * @param manageCertFile   证书地址
	 * @param certPassword	   证书密码
	 * @param keyPassword	   密钥
	 * @throws Exception
	 */
	public void loadCertificate(String manageCertFile, String certPassword,String keyPassword) throws Exception{
		KeyStore manageKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
		manageKeystore.load(new FileInputStream(manageCertFile), certPassword.toCharArray());
		keyManagerFactory.init(manageKeystore, keyPassword.toCharArray());
		trustManagerFactory.init(manageKeystore);
	}
	
	/**
	 * 初始化
	 * @param protocol		协议名称 SSL/TLS
	 * @throws Exception
	 */
	private void init(String protocol) throws Exception{

		if(protocol.isEmpty() || protocol == null){
			protocol = "SSL";
		}
		context = SSLContext.getInstance(protocol);
		context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
	}
	
	/**
	 * 获取Client 模式 SSLEngine
	 * @return
	 * @throws Exception
	 */
	private void createSSLEngine(String protocol) throws Exception{
		init(protocol);
		engine = context.createSSLEngine();
	}
	
	/**
	 * 获取SSLEngine
	 * @return
	 * @throws Exception
	 */
	public SSLParser createClientSSLParser(IoSession session) throws Exception{
		createSSLEngine(protocol);
		engine.setUseClientMode(true);
		return new SSLParser(engine, session);
	}
	
	/**
	 * 获取Server 模式 SSLEngine
	 * @return
	 * @throws Exception
	 */
	public SSLParser createServerSSLParser(IoSession session) throws Exception{
		createSSLEngine(protocol);
		engine.setUseClientMode(false);
		engine.setNeedClientAuth(useClientAuth);
		return new SSLParser(engine, session);
	}
}
