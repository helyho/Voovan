package org.voovan.http.server.exception;

/**
 * 注解路由异常
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AnnotationRouterParamException extends Exception {
    private String path;
    private String paramCheckMsg;

    public AnnotationRouterParamException(String message) {
        super(message);
    }

    public AnnotationRouterParamException(String description, Exception e){
        super(description + "\r\n"+ e.getClass().getName()+ ": " + e.getMessage());
        this.setStackTrace(e.getStackTrace());
    }

    public String getParamCheckMsg() {
        return paramCheckMsg;
    }

    public AnnotationRouterParamException setParamCheckMsg(String paramCheckMsg) {
        this.paramCheckMsg = paramCheckMsg;
        return this;
    }

    public String getPath() {
        return path;
    }

    public AnnotationRouterParamException setPath(String path) {
        this.path = path;
        return this;
    }
}
