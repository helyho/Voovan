package org.voovan.http.server.exception;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AnnotationRouterException  extends Exception {
    public AnnotationRouterException(String description, Exception e){
        super(description + "\r\n"+ e.getClass().getName()+ ": " + e.getMessage(), e);
    }
}
