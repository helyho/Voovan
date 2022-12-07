package org.voovan.tools.exception;

/**
 * 解析通用异常
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class IOCException extends RuntimeException {
    public IOCException() {
        super();
    }


    public IOCException(String message) {
        super(message);
    }


    public IOCException(String message, Throwable cause) {
        super(message, cause);
    }


    public IOCException(Throwable cause) {
        super(cause);
    }
}
