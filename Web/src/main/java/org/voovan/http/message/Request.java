package org.voovan.http.message;

import org.voovan.http.message.packet.*;
import org.voovan.http.server.context.WebContext;
import org.voovan.network.IoSession;
import org.voovan.tools.buffer.TByteBuffer;
import org.voovan.tools.TString;
import org.voovan.tools.exception.MemoryReleasedException;
import org.voovan.tools.log.Logger;
import org.voovan.tools.security.THash;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;

/**
 * HTTP 请求对象
 *
 * GET 请求获取Request-URI所标识的资源 POST 在Request-URI所标识的资源后附加新的数据 HEAD
 * 请求获取由Request-URI所标识的资源的响应消息报头 PUT 请求服务器存储一个资源，并用Request-URI作为其标识 DELETE
 * 请求服务器删除Request-URI所标识的资源 TRACE 请求服务器回送收到的请求信息，主要用于测试或诊断 CONNECT 保留将来使用
 * OPTIONS 请求查询服务器的性能，或者查询与资源相关的选项和需求
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */

public class Request {
    private RequestProtocol protocol;
    private Header 			header;
    private List<Cookie>	cookies;
    private Body 			body;
    private List<Part>		parts;
    private String 			boundary;
    private boolean         hasBody;
    protected boolean 		basicSend = false;
    private boolean         cookieParsed = false;
    private Long            mark = 0l;

    private static final String CONTENT_TYPE = "Content-Type";

    /**
     * HTTP 请求的枚举对象
     *
     * @author helyho
     *
     */
    public enum RequestType {
        BODY_URLENCODED, BODY_MULTIPART, NORMAL
    }

    /**
     * 构造函数
     *
     * @param request 请求对象
     */
    public Request(Request request) {
        init(request);
    }

    public void init(Request request){
        this.protocol = request.protocol;
        this.header = request.header;
        this.body = request.body;
        this.cookies = request.cookies;
        this.parts = request.parts;
        this.hasBody = request.hasBody;
        this.basicSend = false;
        this.cookieParsed = false;
        this.mark = request.mark;
    }

    /**
     * 构造函数
     */
    public Request() {
        protocol = new RequestProtocol();
        header = new Header();
        cookies = new Vector<Cookie>();
        body = new Body();
        parts = new Vector<Part>();
        this.basicSend = false;
    }

    /**
     * 获取协议对象
     *
     * @return 请求协议对象
     */
    public RequestProtocol protocol() {
        return protocol;
    }

    public Long getMark() {
        return mark;
    }

    public void setMark(Long mark) {
        this.mark = mark;
    }

    /**
     * 获取 Header 对象
     *
     * @return HTTP-Header 对象
     */
    public Header header() {
        return header;
    }

    /**
     * 获取所有的Cookies对象,返回一个 List
     *
     * @return Cookie 对象
     */
    public synchronized List<Cookie> cookies() {
        if(!cookieParsed) {
            String cookieStr = header.get("Cookie");
            if(cookieStr!=null) {
                HttpParser.parseCookie(cookies, 0, cookieStr);
            }
            cookieParsed = true;
        }
        return cookies;
    }

    /**
     * 获取 Body 对象
     *
     * @return Body对象
     */
    public Body body() {
        return body;
    }

    /**
     * 获取所有的 Part 对象,返回一个 List
     *
     * @return POST 请求报文对象
     */
    public List<Part> parts() {
        return parts;
    }

    public boolean isHasBody() {
        return hasBody;
    }

    public void setHasBody(boolean hasBody) {
        this.hasBody = hasBody;
    }

    /**
     * 获取请求类型
     *
     * @return RequestType枚举
     */
    public RequestType getBodyType() {
        if (header.get(CONTENT_TYPE) != null) {
            if (header.get(CONTENT_TYPE).contains("application/x-www-form-urlencoded")) {
                return RequestType.BODY_URLENCODED;
            } else if (header.get(CONTENT_TYPE).contains("multipart/form-data")) {
                return RequestType.BODY_MULTIPART;
            }
        }

        return RequestType.NORMAL;
    }

    /**
     * 获取QueryStirng 或 将参数拼装成QueryString
     * @param charset 字符集
     * @return 请求字符串
     */
    public String getQueryString(String charset) {
        String queryString = "";
        //请求路径内包含的参数
        queryString = protocol.getQueryString();

        if(hasBody) {
            // POST_URLENCODED 请求类型的处理
            if (getBodyType() == RequestType.BODY_URLENCODED) {
                queryString = queryString + "&" + body.getBodyString();
            }
            // POST_MULTIPART 请求类型的处理
            else if (getBodyType() == RequestType.BODY_MULTIPART) {
                StringBuilder result = new StringBuilder();
                for (Part part : parts) {
                    if (part.getType() == Part.PartType.TEXT) {
                        String name = part.header().get("name");
                        String value = null;
                        if (!part.body().isFile()) {
                            value = part.body().getBodyString(charset);
                        } else {
                            value = part.header().get("filename");
                        }

                        result.append(name);
                        result.append("=");
                        result.append(value);
                        result.append("&");

                    }
                }
                queryString = TString.removeSuffix(queryString + "&" + result.toString());
            }

            if (queryString.startsWith("&")) {
                queryString = TString.removePrefix(queryString);
            }
        }

        try {
            return queryString.isEmpty()? null : URLDecoder.decode(queryString, WebContext.getWebServerConfig().getCharacterSet());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * 根据内容构造一些必要的 Header 属性
     * 		这里不按照请求方法组装必要的头信息,而是根据 Body 和 parts 对象的内容组装必要的头信息
     */
    private void initHeader() {
        //如果之前设置过 ContentType 则不自动设置 ContentType
        if(!header.contain(CONTENT_TYPE)){
            // 如果请求中包含 Part 的处理
            if (!parts.isEmpty()) {
                // 产生新的boundary备用
                String contentType = "multipart/form-data;";
                header.put(CONTENT_TYPE, contentType);
            }else if(body.size()>0){
                header.put(CONTENT_TYPE, "application/x-www-form-urlencoded");
            }
        }

        String contentType = header.get(CONTENT_TYPE);
        if(contentType!=null && contentType.startsWith("multipart/form-data;")){
            boundary = THash.encryptBASE64(TString.generateId(this));
            header.put(CONTENT_TYPE, "multipart/form-data;boundary=" + boundary);
        }

        if (body.size() > 0) {
            header.put("Content-Length", Long.toString(body.size()));
        }

        //生成 Cookie 信息
        String cookieValue = genCookie();
        if(!TString.isNullOrEmpty(cookieValue)){
            header.put("Cookie", genCookie());
        }
    }

    /**
     * 根据 Cookie 对象,生成 HTTP 请求中的 Cookie 字符串 用于报文拼装
     *
     * @return 获取 Cookie 字符串
     */
    private String genCookie() {
        StringBuilder cookieString = new StringBuilder();
        for (Cookie cookie : cookies) {
            cookieString.append(cookie.getName());
            cookieString.append("=");
            cookieString.append(cookie.getValue());
            cookieString.append("; ");
        }
        return cookieString.toString();
    }

    /**
     * 根据 Cookie 名称取 Cookie
     *
     * @param name  Cookie 名称
     * @return Cookie
     */
    public Cookie getCookie(String name){
        for(Cookie cookie : this.cookies()){
            if(cookie !=null && name !=null && name.equals(cookie.getName())){
                return cookie;
            }
        }
        return null;
    }

    /**
     * 根据对象的内容,构造 Http 请求报文
     *
     * @return Http 请求报文
     */
    private ByteBuffer readHead() {

        StringBuilder stringBuilder = new StringBuilder();

        initHeader();

        stringBuilder.append(protocol.toString());


        stringBuilder.append(header.toString());

        stringBuilder.append("\r\n");

        try {
            return ByteBuffer.wrap(stringBuilder.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Logger.error("Response.readHead io error",e);
            return null;
        }

    }

    /**
     * 发送数据
     * @param session socket 会话对象
     * @throws IOException IO异常
     */
    public void send(IoSession session) throws IOException {
        int readSize = 0;

        //发送报文头
        ByteBuffer byteBuffer = null;

        try{
            try {
                byteBuffer = readHead();
            } catch (Throwable e){
                if(!(e instanceof MemoryReleasedException)){
                    Logger.error("Response writeToChannel error: ", (Exception) e);
                }
            }

            if(byteBuffer == null){
                return;
            }

            //发送报文头
            session.send(byteBuffer);

            //发送缓冲区
            byteBuffer = TByteBuffer.allocateDirect();

            // 有 BodyBytes 时直接写入包体
            if (body.size() > 0) {
                while(true) {
                    readSize = body.read(byteBuffer);
                    if (readSize == -1) {
                        break;
                    }
                    session.send(byteBuffer);
                    byteBuffer.clear();
                }
            }

            byteBuffer.clear();

            // 有 parts 时按 parts 的格式写入 parts
            if(!parts.isEmpty()) {
                // Content-Type存在
                if (parts.size() != 0) {

                    if(boundary == null){
                        boundary = THash.encryptBASE64(TString.generateId(this));
                    }

                    // 获取 multiPart 标识
                    for (Part part : this.parts) {
                        //发送 part 报文
                        part.send(session, boundary);
                    }

                    //发送结尾标识
                    byteBuffer.put(TString.assembly("--" + boundary + "--").getBytes());
                    byteBuffer.flip();
                    session.send(byteBuffer);
                    byteBuffer.clear();
                    // POST结束不需要空行标识结尾
                }
            }

            basicSend = true;
        } catch (Throwable e){
            if(!(e instanceof MemoryReleasedException)){
                Logger.error("Request writeToChannel error: ", (Exception) e);
            }
            return;
        } finally {
            if(byteBuffer!=null) {
                TByteBuffer.release(byteBuffer);
            }
            clear();
        }
    }


    public void release(){
        for (Part part : this.parts) {
            part.body().release();
        }

        body.release();
    }

    public Request copyFrom(Request request) {
        this.protocol().setMethod(request.protocol().getMethod());
        this.protocol().setPath(request.protocol().getPath());
        this.protocol().setQueryString(request.protocol().getQueryString());
        this.header().copyFrom(request.header());
        this.body().write( request.body().getBodyBytes());
        this.cookies().addAll(request.cookies());
        this.parts.addAll(request.parts());
        this.setMark(request.getMark());
        this.setHasBody(request.hasBody);
        return this;
    }

    /**
     * 清理
     */
    public void clear(){
        this.header.clear();
        this.cookies.clear();
        this.protocol.clear();
        this.body.clear();
        this.parts.clear();
        this.cookieParsed = false;
        this.mark = 0l;
    }

    @Override
    public String toString() {
        return new String(TByteBuffer.toString(readHead()));
    }
}
