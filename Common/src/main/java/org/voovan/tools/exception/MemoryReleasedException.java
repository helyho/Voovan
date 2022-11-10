package org.voovan.tools.exception;

/**
 * 内存已被释放的异常
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class MemoryReleasedException extends RuntimeException{

    public MemoryReleasedException() {
        super();
    }


    public MemoryReleasedException(String message) {
        super(message);
    }


    public MemoryReleasedException(String message, Throwable cause) {
        super(message, cause);
    }


    public MemoryReleasedException(Throwable cause) {
        super(cause);
    }
}
