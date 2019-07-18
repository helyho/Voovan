package org.voovan.tools.exception;

/**
 * 内存已被释放的异常
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SerializeException extends RuntimeException{

    public SerializeException() {
        super();
    }


    public SerializeException(String message) {
        super(message);
    }


    public SerializeException(String message, Throwable cause) {
        super(message, cause);
    }


    public SerializeException(Throwable cause) {
        super(cause);
    }
}
