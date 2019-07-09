package org.voovan.http.server.exception;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpDispatchException extends RuntimeException {
    public HttpDispatchException(String msg){
        super(msg);
    }

    public HttpDispatchException(Exception e){
        super(e.getMessage());
        this.setStackTrace(e.getStackTrace());
    }

    public HttpDispatchException(String msg, Exception e){
        super(msg);
        this.setStackTrace(e.getStackTrace());
    }
}
