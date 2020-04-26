package org.voovan.http.message.exception;

import org.voovan.tools.exception.EmptyStackRuntimeException;

import java.io.IOException;

/**
 * Http 解析异常
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpParserException extends EmptyStackRuntimeException {
    public final static int NORMAL = 0;
    public final static int SOCKET_DISCONNECT = 1;
    public final static int BUFFER_RELEASED = 2;

    private int type; //0: normal, 1: socket error

    public HttpParserException(String msg){
        this(msg, NORMAL);
    }

    public HttpParserException(String msg, Exception e){
        this(msg, NORMAL, e);
    }

    public HttpParserException(String msg, int type){
        super(msg);
        this.type = type;
    }

    public HttpParserException(String msg, int type, Exception e){
        super(msg, e);
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isNormal() {
        return type == NORMAL;
    }

    public boolean isSocketDisconnect() {
        return type == SOCKET_DISCONNECT;
    }

    public boolean isBufferReleased() {
        return type == BUFFER_RELEASED;
    }

    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
