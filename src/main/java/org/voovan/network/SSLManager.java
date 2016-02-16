package org.voovan.network;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.voovan.tools.TString;

/**
 * SSL管理器
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
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
	 * @throws NoSuchAlgorithmException 
	 * @throws Exception
	 */
	public SSLManager(String protocol) throws NoSuchAlgorithmException{
		this.useClientAuth = true;
		this.protocol = protocol;
		keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
	}
	
	/**
	 * 构造函数
	 * @param protocol  	协议类型
	 * @param useClientAuth	是否使用客户端认证
	 * @throws SSLException 
	 */
	public SSLManager(String protocol,boolean useClientAuth) throws SSLException{
		try{
			this.useClientAuth = useClientAuth;
			this.protocol = protocol;
			keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
		} catch (NoSuchAlgorithmException e) {
			throw new SSLException("Init SSLContext Error: "+e.getMessage(),e);
		}
	}
	
	/**
	 * 读取管理证书
	 * @param manageCertFile   证书地址
	 * @param certPassword	   证书密码
	 * @param keyPassword	   密钥
	 * @throws SSLException 
	 */
	public void loadCertificate(String manageCertFile, String certPassword,String keyPassword) throws SSLException{
		try{
			KeyStore manageKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
			manageKeystore.load(new FileInputStream(manageCertFile), certPassword.toCharArray());
			keyManagerFactory.init(manageKeystore, keyPassword.toCharArray());
			trustManagerFactory.init(manageKeystore);
		} catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
			throw new SSLException("Init SSLContext Error: "+e.getMessage(),e);
		}
	}
	
	/**
	 * 初始化
	 * @param protocol		协议名称 SSL/TLS
	 * @throws SSLException 
	 */
	private void init(String protocol) throws SSLException {

		if(TString.isNullOrEmpty(protocol) || protocol == null){
			this.protocol = "SSL";
		}
		try {
			context = SSLContext.getInstance(protocol);
			context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new SSLException("Init SSLContext Error: "+e.getMessage(),e);
		}
		
	}
	
	/**
	 * 获取Client 模式 SSLEngine
	 * @return
	 * @throws SSLException 
	 */
	private void createSSLEngine(String protocol) throws SSLException {
		init(protocol);
		engine = context.createSSLEngine();
	}
	
	/**
	 * 获取SSLParser
	 * @return
	 * @throws SSLException 
	 */
	public SSLParser createClientSSLParser(IoSession session) throws SSLException {
		createSSLEngine(protocol);
		engine.setUseClientMode(true);
		return new SSLParser(engine, session);
	}
	
	/**
	 * 获取Server 模式 SSLSSLParser
	 * @return
	 * @throws SSLException 
	 */
	public SSLParser createServerSSLParser(IoSession session) throws SSLException{
		createSSLEngine(protocol);
		engine.setUseClientMode(false);
		engine.setNeedClientAuth(useClientAuth);
		return new SSLParser(engine, session);
	}
}
