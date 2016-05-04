package org.voovan.network;

import org.voovan.tools.TString;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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
	 * @throws NoSuchAlgorithmException  无可用协议异常
	 */
	public SSLManager(String protocol) throws NoSuchAlgorithmException{
		this.useClientAuth = true;
		this.protocol = protocol;
	}
	
	/**
	 * 构造函数
	 * @param protocol  	协议类型
	 * @param useClientAuth	是否使用客户端认证
	 * @throws SSLException  SSL 异常
	 */
	public SSLManager(String protocol,boolean useClientAuth) throws SSLException{
			this.useClientAuth = useClientAuth;
			this.protocol = protocol;
	}
	
	/**
	 * 读取管理证书
	 * @param manageCertFile   证书地址
	 * @param certPassword	   证书密码
	 * @param keyPassword	   密钥
	 * @throws SSLException SSL 异常
	 */
	public void loadCertificate(String manageCertFile, String certPassword,String keyPassword) throws SSLException{
		try{
			keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
			
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
	 * @throws SSLException SSL 异常
	 */
	private synchronized void init(String protocol) throws SSLException {

		if(TString.isNullOrEmpty(protocol) || protocol == null){
			this.protocol = "SSL";
		}
		try {
			context = SSLContext.getInstance(protocol);
			if(keyManagerFactory!=null && trustManagerFactory!=null){
				context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
			}else{
				context.init(new KeyManager[0],new TrustManager[]{new DefaultTrustManager()},new SecureRandom());
			}
			//NoSuchAlgorithmException | KeyManagementException |
		} catch ( Exception e) {
			
			throw new SSLException("Init SSLContext Error: "+e.getMessage(),e);
		}
		
	}
	
	/**
	 * 构造SSLEngine
	 * @throws SSLException SSL 异常
	 */
	private synchronized void createSSLEngine(String protocol) throws SSLException {
		init(protocol);
		engine = context.createSSLEngine();
	}
	
	/**
	 * 获取SSLParser
	 * @param session session 对象
	 * @return SSLParser 对象
	 * @throws SSLException SSL 异常
	 */
	public SSLParser createClientSSLParser(IoSession session) throws SSLException {
		createSSLEngine(protocol);
		engine.setUseClientMode(true);
		return new SSLParser(engine, session);
	}
	
	/**
	 * 获取Server 模式 SSLParser
	 * @param session session 对象
	 * @return SSLParser对象
	 * @throws SSLException SSL 异常
	 */
	public SSLParser createServerSSLParser(IoSession session) throws SSLException{
		createSSLEngine(protocol);
		engine.setUseClientMode(false);
		engine.setNeedClientAuth(useClientAuth);
		return new SSLParser(engine, session);
	}
	
	private static class DefaultTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
			
		}

		@Override
		public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
			
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}  
		
	}
}
