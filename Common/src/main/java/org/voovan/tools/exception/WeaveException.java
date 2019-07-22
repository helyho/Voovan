package org.voovan.tools.exception;

/**
 * 内存已被释放的异常
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WeaveException extends RuntimeException{

    public WeaveException() {
        super();
    }


    public WeaveException(String message) {
        super(message);
    }


    public WeaveException(String message, Throwable cause) {
        super(message, cause);
    }


    public WeaveException(Throwable cause) {
        super(cause);
    }
}
