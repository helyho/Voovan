package org.voovan.http.server.exception;

/**
 * 注解路由异常
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AnnotationRouterException  extends Exception {
    public AnnotationRouterException(String message) {
        super(message);
    }

    public AnnotationRouterException(String description, Exception e){
        super(description + "\r\n"+ e.getClass().getName()+ ": " + e.getMessage());
        this.setStackTrace(e.getStackTrace());
    }
}
