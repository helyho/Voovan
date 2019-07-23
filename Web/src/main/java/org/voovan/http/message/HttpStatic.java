package org.voovan.http.message;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpStatic {



	public static final String HEADER_SPLITER_STRING = ": ";

	// headers
	public static final String ACCEPT_STRING = "Accept";
	public static final String ACCEPT_CHARSET_STRING = "Accept-Charset";
	public static final String ACCEPT_ENCODING_STRING = "Accept-Encoding";
	public static final String ACCEPT_LANGUAGE_STRING = "Accept-Language";
	public static final String ACCEPT_RANGES_STRING = "Accept-Ranges";
	public static final String AGE_STRING = "Age";
	public static final String ALLOW_STRING = "Allow";
	public static final String AUTHENTICATION_INFO_STRING = "Authentication-Info";
	public static final String AUTHORIZATION_STRING = "Authorization";
	public static final String CACHE_CONTROL_STRING = "Cache-Control";
	public static final String COOKIE_STRING = "Cookie";
	public static final String COOKIE2_STRING = "Cookie2";
	public static final String CONNECTION_STRING = "Connection";
	public static final String CONTENT_DISPOSITION_STRING = "Content-Disposition";
	public static final String CONTENT_ENCODING_STRING = "Content-Encoding";
	public static final String CONTENT_LANGUAGE_STRING = "Content-Language";
	public static final String CONTENT_LENGTH_STRING = "Content-Length";
	public static final String CONTENT_LOCATION_STRING = "Content-Location";
	public static final String CONTENT_MD5_STRING = "Content-MD5";
	public static final String CONTENT_RANGE_STRING = "Content-Range";
	public static final String CONTENT_SECURITY_POLICY_STRING = "Content-Security-Policy";
	public static final String CONTENT_TYPE_STRING = "Content-Type";
	public static final String DATE_STRING = "Date";
	public static final String ETAG_STRING = "ETag";
	public static final String EXPECT_STRING = "Expect";
	public static final String EXPIRES_STRING = "Expires";
	public static final String FORWARDED_STRING = "Forwarded";
	public static final String FROM_STRING = "From";
	public static final String HOST_STRING = "Host";
	public static final String IF_MATCH_STRING = "If-Match";
	public static final String IF_MODIFIED_SINCE_STRING = "If-Modified-Since";
	public static final String IF_NONE_MATCH_STRING = "If-None-Match";
	public static final String IF_RANGE_STRING = "If-Range";
	public static final String IF_UNMODIFIED_SINCE_STRING = "If-Unmodified-Since";
	public static final String LAST_MODIFIED_STRING = "Last-Modified";
	public static final String LOCATION_STRING = "Location";
	public static final String MAX_FORWARDS_STRING = "Max-Forwards";
	public static final String ORIGIN_STRING = "Origin";
	public static final String PRAGMA_STRING = "Pragma";
	public static final String PROXY_AUTHENTICATE_STRING = "Proxy-Authenticate";
	public static final String PROXY_AUTHORIZATION_STRING = "Proxy-Authorization";
	public static final String RANGE_STRING = "Range";
	public static final String REFERER_STRING = "Referer";
	public static final String REFERRER_POLICY_STRING = "Referrer-Policy";
	public static final String REFRESH_STRING = "Refresh";
	public static final String RETRY_AFTER_STRING = "Retry-After";
	public static final String SEC_WEB_SOCKET_ACCEPT_STRING = "Sec-WebSocket-Accept";
	public static final String SEC_WEB_SOCKET_EXTENSIONS_STRING = "Sec-WebSocket-Extensions";
	public static final String SEC_WEB_SOCKET_KEY_STRING = "Sec-WebSocket-Key";
	public static final String SEC_WEB_SOCKET_KEY1_STRING = "Sec-WebSocket-Key1";
	public static final String SEC_WEB_SOCKET_KEY2_STRING = "Sec-WebSocket-Key2";
	public static final String SEC_WEB_SOCKET_LOCATION_STRING = "Sec-WebSocket-Location";
	public static final String SEC_WEB_SOCKET_ORIGIN_STRING = "Sec-WebSocket-Origin";
	public static final String SEC_WEB_SOCKET_PROTOCOL_STRING = "Sec-WebSocket-Protocol";
	public static final String SEC_WEB_SOCKET_VERSION_STRING = "Sec-WebSocket-Version";
	public static final String SERVER_STRING = "Server";
	public static final String SERVLET_ENGINE_STRING = "Servlet-Engine";
	public static final String SET_COOKIE_STRING = "Set-Cookie";
	public static final String SET_COOKIE2_STRING = "Set-Cookie2";
	public static final String SSL_CLIENT_CERT_STRING = "SSL_CLIENT_CERT";
	public static final String SSL_CIPHER_STRING = "SSL_CIPHER";
	public static final String SSL_SESSION_ID_STRING = "SSL_SESSION_ID";
	public static final String SSL_CIPHER_USEKEYSIZE_STRING = "SSL_CIPHER_USEKEYSIZE";
	public static final String STATUS_STRING = "Status";
	public static final String STRICT_TRANSPORT_SECURITY_STRING = "Strict-Transport-Security";
	public static final String TE_STRING = "TE";
	public static final String TRAILER_STRING = "Trailer";
	public static final String TRANSFER_ENCODING_STRING = "Transfer-Encoding";
	public static final String UPGRADE_STRING = "Upgrade";
	public static final String USER_AGENT_STRING = "User-Agent";
	public static final String VARY_STRING = "Vary";
	public static final String VIA_STRING = "Via";
	public static final String WARNING_STRING = "Warning";
	public static final String WWW_AUTHENTICATE_STRING = "WWW-Authenticate";
	public static final String X_CONTENT_TYPE_OPTIONS_STRING = "X-Content-Type-Options";
	public static final String X_DISABLE_PUSH_STRING = "X-Disable-Push";
	public static final String X_FORWARDED_FOR_STRING = "X-Forwarded-For";
	public static final String X_FORWARDED_PROTO_STRING = "X-Forwarded-Proto";
	public static final String X_FORWARDED_HOST_STRING = "X-Forwarded-Host";
	public static final String X_FORWARDED_PORT_STRING = "X-Forwarded-Port";
	public static final String X_FORWARDED_SERVER_STRING = "X-Forwarded-Server";
	public static final String X_FRAME_OPTIONS_STRING = "X-Frame-Options";
	public static final String X_XSS_PROTECTION_STRING = "X-Xss-Protection";
	public static final String X_REAL_IP_STRING = "X-Real-IP";


	// Header names
    public static final HttpItem ACCEPT = new HttpItem(ACCEPT_STRING);
    public static final HttpItem ACCEPT_CHARSET = new HttpItem(ACCEPT_CHARSET_STRING);
    public static final HttpItem ACCEPT_ENCODING = new HttpItem(ACCEPT_ENCODING_STRING);
    public static final HttpItem ACCEPT_LANGUAGE = new HttpItem(ACCEPT_LANGUAGE_STRING);
    public static final HttpItem ACCEPT_RANGES = new HttpItem(ACCEPT_RANGES_STRING);
    public static final HttpItem AGE = new HttpItem(AGE_STRING);
    public static final HttpItem ALLOW = new HttpItem(ALLOW_STRING);
    public static final HttpItem AUTHENTICATION_INFO = new HttpItem(AUTHENTICATION_INFO_STRING);
    public static final HttpItem AUTHORIZATION = new HttpItem(AUTHORIZATION_STRING);
    public static final HttpItem CACHE_CONTROL = new HttpItem(CACHE_CONTROL_STRING);
    public static final HttpItem CONNECTION = new HttpItem(CONNECTION_STRING);
    public static final HttpItem CONTENT_DISPOSITION = new HttpItem(CONTENT_DISPOSITION_STRING);
    public static final HttpItem CONTENT_ENCODING = new HttpItem(CONTENT_ENCODING_STRING);
    public static final HttpItem CONTENT_LANGUAGE = new HttpItem(CONTENT_LANGUAGE_STRING);
    public static final HttpItem CONTENT_LENGTH = new HttpItem(CONTENT_LENGTH_STRING);
    public static final HttpItem CONTENT_LOCATION = new HttpItem(CONTENT_LOCATION_STRING);
    public static final HttpItem CONTENT_MD5 = new HttpItem(CONTENT_MD5_STRING);
    public static final HttpItem CONTENT_RANGE = new HttpItem(CONTENT_RANGE_STRING);
    public static final HttpItem CONTENT_SECURITY_POLICY = new HttpItem(CONTENT_SECURITY_POLICY_STRING);
    public static final HttpItem CONTENT_TYPE = new HttpItem(CONTENT_TYPE_STRING);
    public static final HttpItem COOKIE = new HttpItem(COOKIE_STRING);
    public static final HttpItem COOKIE2 = new HttpItem(COOKIE2_STRING);
    public static final HttpItem DATE = new HttpItem(DATE_STRING);
    public static final HttpItem ETAG = new HttpItem(ETAG_STRING);
    public static final HttpItem EXPECT = new HttpItem(EXPECT_STRING);
    public static final HttpItem EXPIRES = new HttpItem(EXPIRES_STRING);
    public static final HttpItem FORWARDED = new HttpItem(FORWARDED_STRING);
    public static final HttpItem FROM = new HttpItem(FROM_STRING);
    public static final HttpItem HOST = new HttpItem(HOST_STRING);
    public static final HttpItem IF_MATCH = new HttpItem(IF_MATCH_STRING);
    public static final HttpItem IF_MODIFIED_SINCE = new HttpItem(IF_MODIFIED_SINCE_STRING);
    public static final HttpItem IF_NONE_MATCH = new HttpItem(IF_NONE_MATCH_STRING);
    public static final HttpItem IF_RANGE = new HttpItem(IF_RANGE_STRING);
    public static final HttpItem IF_UNMODIFIED_SINCE = new HttpItem(IF_UNMODIFIED_SINCE_STRING);
    public static final HttpItem LAST_MODIFIED = new HttpItem(LAST_MODIFIED_STRING);
    public static final HttpItem LOCATION = new HttpItem(LOCATION_STRING);
    public static final HttpItem MAX_FORWARDS = new HttpItem(MAX_FORWARDS_STRING);
    public static final HttpItem ORIGIN = new HttpItem(ORIGIN_STRING);
    public static final HttpItem PRAGMA = new HttpItem(PRAGMA_STRING);
    public static final HttpItem PROXY_AUTHENTICATE = new HttpItem(PROXY_AUTHENTICATE_STRING);
    public static final HttpItem PROXY_AUTHORIZATION = new HttpItem(PROXY_AUTHORIZATION_STRING);
    public static final HttpItem RANGE = new HttpItem(RANGE_STRING);
    public static final HttpItem REFERER = new HttpItem(REFERER_STRING);
    public static final HttpItem REFERRER_POLICY = new HttpItem(REFERRER_POLICY_STRING);
    public static final HttpItem REFRESH = new HttpItem(REFRESH_STRING);
    public static final HttpItem RETRY_AFTER = new HttpItem(RETRY_AFTER_STRING);
    public static final HttpItem SEC_WEB_SOCKET_ACCEPT = new HttpItem(SEC_WEB_SOCKET_ACCEPT_STRING);
    public static final HttpItem SEC_WEB_SOCKET_EXTENSIONS = new HttpItem(SEC_WEB_SOCKET_EXTENSIONS_STRING);
    public static final HttpItem SEC_WEB_SOCKET_KEY = new HttpItem(SEC_WEB_SOCKET_KEY_STRING);
    public static final HttpItem SEC_WEB_SOCKET_KEY1 = new HttpItem(SEC_WEB_SOCKET_KEY1_STRING);
    public static final HttpItem SEC_WEB_SOCKET_KEY2 = new HttpItem(SEC_WEB_SOCKET_KEY2_STRING);
    public static final HttpItem SEC_WEB_SOCKET_LOCATION = new HttpItem(SEC_WEB_SOCKET_LOCATION_STRING);
    public static final HttpItem SEC_WEB_SOCKET_ORIGIN = new HttpItem(SEC_WEB_SOCKET_ORIGIN_STRING);
    public static final HttpItem SEC_WEB_SOCKET_PROTOCOL = new HttpItem(SEC_WEB_SOCKET_PROTOCOL_STRING);
    public static final HttpItem SEC_WEB_SOCKET_VERSION = new HttpItem(SEC_WEB_SOCKET_VERSION_STRING);
    public static final HttpItem SERVER = new HttpItem(SERVER_STRING);
    public static final HttpItem SERVLET_ENGINE = new HttpItem(SERVLET_ENGINE_STRING);
    public static final HttpItem SET_COOKIE = new HttpItem(SET_COOKIE_STRING);
    public static final HttpItem SET_COOKIE2 = new HttpItem(SET_COOKIE2_STRING);
    public static final HttpItem SSL_CIPHER = new HttpItem(SSL_CIPHER_STRING);
    public static final HttpItem SSL_CIPHER_USEKEYSIZE = new HttpItem(SSL_CIPHER_USEKEYSIZE_STRING);
    public static final HttpItem SSL_CLIENT_CERT = new HttpItem(SSL_CLIENT_CERT_STRING);
    public static final HttpItem SSL_SESSION_ID = new HttpItem(SSL_SESSION_ID_STRING);
    public static final HttpItem STATUS = new HttpItem(STATUS_STRING);
    public static final HttpItem STRICT_TRANSPORT_SECURITY = new HttpItem(STRICT_TRANSPORT_SECURITY_STRING);
    public static final HttpItem TE = new HttpItem(TE_STRING);
    public static final HttpItem TRAILER = new HttpItem(TRAILER_STRING);
    public static final HttpItem TRANSFER_ENCODING = new HttpItem(TRANSFER_ENCODING_STRING);
    public static final HttpItem UPGRADE = new HttpItem(UPGRADE_STRING);
    public static final HttpItem USER_AGENT = new HttpItem(USER_AGENT_STRING);
    public static final HttpItem VARY = new HttpItem(VARY_STRING);
    public static final HttpItem VIA = new HttpItem(VIA_STRING);
    public static final HttpItem WARNING = new HttpItem(WARNING_STRING);
    public static final HttpItem WWW_AUTHENTICATE = new HttpItem(WWW_AUTHENTICATE_STRING);
    public static final HttpItem X_CONTENT_TYPE_OPTIONS = new HttpItem(X_CONTENT_TYPE_OPTIONS_STRING);
    public static final HttpItem X_DISABLE_PUSH = new HttpItem(X_DISABLE_PUSH_STRING);
    public static final HttpItem X_FORWARDED_FOR = new HttpItem(X_FORWARDED_FOR_STRING);
    public static final HttpItem X_FORWARDED_HOST = new HttpItem(X_FORWARDED_HOST_STRING);
    public static final HttpItem X_FORWARDED_PORT = new HttpItem(X_FORWARDED_PORT_STRING);
    public static final HttpItem X_FORWARDED_PROTO = new HttpItem(X_FORWARDED_PROTO_STRING);
    public static final HttpItem X_FORWARDED_SERVER = new HttpItem(X_FORWARDED_SERVER_STRING);
    public static final HttpItem X_FRAME_OPTIONS = new HttpItem(X_FRAME_OPTIONS_STRING);
    public static final HttpItem X_XSS_PROTECTION = new HttpItem(X_XSS_PROTECTION_STRING);
    public static final HttpItem X_REAL_IP = new HttpItem(X_REAL_IP_STRING);

	// Content codings
	public static final String COMPRESS_STRING = "compress";
	public static final String X_COMPRESS_STRING = "x-compress";
	public static final String DEFLATE_STRING = "deflate";
	public static final String IDENTITY_STRING = "identity";
	public static final String GZIP_STRING = "gzip";
	public static final String X_GZIP_STRING = "x-gzip";

    public static final HttpItem COMPRESS = new HttpItem(COMPRESS_STRING);
    public static final HttpItem X_COMPRESS = new HttpItem(X_COMPRESS_STRING);
    public static final HttpItem DEFLATE = new HttpItem(DEFLATE_STRING);
    public static final HttpItem IDENTITY = new HttpItem(IDENTITY_STRING);
    public static final HttpItem GZIP = new HttpItem(GZIP_STRING);
    public static final HttpItem X_GZIP = new HttpItem(X_GZIP_STRING);

	// Transfer codings
	public static final String CHUNKED_STRING = "chunked";
    public static final HttpItem CHUNKED = new HttpItem(CHUNKED_STRING);

	// Connection values
	public static final String KEEP_ALIVE_STRING = "keep-alive";
	public static final String CLOSE_STRING = "close";

    public static final HttpItem KEEP_ALIVE = new HttpItem(KEEP_ALIVE_STRING);
    public static final HttpItem CLOSE = new HttpItem(CLOSE_STRING);

	//MIME header used in multipart file uploads
	public static final String MULTIPART_FORM_DATA_STRING = "multipart/form-data;";
	public static final String FORM_DATA_STRING = "form-data;";
	public static final String CONTENT_TRANSFER_ENCODING_STRING = "Content-Transfer-Encoding";
	public static final String NAME_STRING = "name";
	public static final String FILE_NAME_STRING = "filename";

    public static final HttpItem MULTIPART_FORM_DATA = new HttpItem(MULTIPART_FORM_DATA_STRING);
    public static final HttpItem CONTENT_TRANSFER_ENCODING = new HttpItem(CONTENT_TRANSFER_ENCODING_STRING);
    public static final HttpItem NAME = new HttpItem(NAME_STRING);
    public static final HttpItem FILE_NAME = new HttpItem(FILE_NAME_STRING);


	public static final String BOUNDARY_STRING = "boundary";
    public static final HttpItem BOUNDARY = new HttpItem(BOUNDARY_STRING);

	//Cookie
	public static final String SECURE_STRING = "secure";
	public static final String HTTPONLY_STRING = "httponly";

    public static final HttpItem SECURE = new HttpItem(SECURE_STRING);
    public static final HttpItem HTTPONLY = new HttpItem(HTTPONLY_STRING);

	public static final String HTTP_STRING = "HTTP";
	public static final HttpItem HTTP = new HttpItem(HTTP_STRING);

	public static final String HTTP_11_STRING = "1.1";
	public static final String HTTP_10_STRING = "1.0";
	public static final String HTTP_09_STRING = "0.9";

	public static final String BODY_MARK_STRING = "\r\n\r\n";
	public static final String LINE_MARK_STRING	= "\r\n";
	public static final HttpItem BODY_MARK = new HttpItem(BODY_MARK_STRING);
	public static final HttpItem LINE_MARK = new HttpItem(LINE_MARK_STRING);

	public static final String TEXT_HTML_STRING = "text/html";
    public static final HttpItem TEXT_HTML = new HttpItem(TEXT_HTML_STRING);


	public static final String TEXT_PLAIN_STRING = "text/plain";
    public static final HttpItem TEXT_PLAIN = new HttpItem(TEXT_PLAIN_STRING);

	public static final String WEB_SOCKET_STRING = "websocket";
    public static final HttpItem WEB_SOCKET = new HttpItem(WEB_SOCKET_STRING);




}
