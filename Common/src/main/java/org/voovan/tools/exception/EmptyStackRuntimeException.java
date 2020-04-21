package org.voovan.tools.exception;

/**
 * 无堆栈 RuntimeException
 *
 * @author: helyho
 * voovan-framework Framework.
 * WebSite: https://github.com/helyho/voovan-framework
 * Licence: Apache v2 License
 */
public class EmptyStackRuntimeException extends RuntimeException {
    public EmptyStackRuntimeException() {
    }

    public EmptyStackRuntimeException(String message) {
        super(message);
    }

    public EmptyStackRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyStackRuntimeException(Throwable cause) {
        super(cause);
    }

    public EmptyStackRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
