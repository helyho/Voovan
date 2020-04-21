package org.voovan.tools.exception;

/**
 * 无堆栈 Exception
 *
 * @author: helyho
 * voovan-framework Framework.
 * WebSite: https://github.com/helyho/voovan-framework
 * Licence: Apache v2 License
 */
public class EmptyStackException extends Exception {
    public EmptyStackException() {
    }

    public EmptyStackException(String message) {
        super(message);
    }

    public EmptyStackException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyStackException(Throwable cause) {
        super(cause);
    }

    public EmptyStackException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
