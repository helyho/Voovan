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
 *
 * SSL - Supports some version of SSL; may support other versions
 * SSLv2 - Supports SSL version 2 or later; may support other versions
 * SSLv3 - Supports SSL version 3; may support other versions
 * TLS - Supports some version of TLS; may support other versions
 * TLSv1 - Supports RFC 2246: TLS version 1.0 ; may support other versions
 * TLSv1.1 - Supports RFC 4346: TLS version 1.1 ; may support other versions
 * TLSv1.2 - Supports RFC 5246: TLS version 1.2 ; may support other versions
 *
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
	private boolean needClientAuth;
	private String protocol;
	
	/**
	 * 构造函数
	 * 		默认使用客户端认证
	 * @param protocol    协议类型
	 * @throws NoSuchAlgorithmException  无可用协议异常
	 */
	public SSLManager(String protocol) throws NoSuchAlgorithmException{
		this.needClientAuth = true;
		this.protocol = protocol;
	}
	
	/**
	 * 构造函数
	 * @param protocol  	协议类型
	 * @param useClientAuth	 是否使用客户端认证, true:双向认证, false: 单向认证
	 * @throws SSLException  SSL 异常
	 */
	public SSLManager(String protocol,boolean useClientAuth) throws SSLException{
			this.needClientAuth = useClientAuth;
			this.protocol = protocol;
	}

	/**
	 * 获取 SSLEngine
	 * @return SSLEngine 对象
     */
	public SSLEngine getSSLEngine(){
		return engine;
	}
	
	/**
	 * 读取管理证书
	 * @param manageCertFile   证书地址
	 * @param certPassword	   证书密码
	 * @param keyPassword	   密钥
	 * @throws SSLException SSL 异常
	 */
	public void loadCertificate(String manageCertFile, String certPassword,String keyPassword) throws SSLException{

		FileInputStream certFIS = null;
		try{
			keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
			
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			certFIS = new FileInputStream(manageCertFile);
			keystore.load(certFIS, certPassword.toCharArray());

			keyManagerFactory.init(keystore , keyPassword.toCharArray());
			trustManagerFactory.init(keystore );
		} catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
			throw new SSLException("Init SSLContext Error: "+e.getMessage(),e);
		}finally {
			if(certFIS!=null) {
				try {
					certFIS.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 初始化
	 * @param protocol		协议名称 SSL/TLS
	 * @throws SSLException SSL 异常
	 */
	private synchronized void init(String protocol) throws SSLException {

		if(TString.isNullOrEmpty(protocol)){
			this.protocol = "SSL";
		}
		try {
			context = SSLContext.getInstance(protocol, "SunJSSE");
			if(keyManagerFactory!=null && trustManagerFactory!=null){
				context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
			}else{
				context.init(null, new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
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
	private synchronized void createSSLEngine(String protocol, String ipAddress, int port) throws SSLException {
		init(protocol);
		engine = context.createSSLEngine(ipAddress, port);
	}
	
	/**
	 * 获取SSLParser
	 * @param session session 对象
	 * @return SSLParser 对象
	 * @throws SSLException SSL 异常
	 */
	public SSLParser createClientSSLParser(IoSession session) throws SSLException {
		createSSLEngine(protocol, session.socketContext().getHost(), session.socketContext().getPort());
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
		createSSLEngine(protocol, session.socketContext().getHost(), session.socketContext().getPort());
		engine.setUseClientMode(false);
		engine.setNeedClientAuth(needClientAuth);
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
