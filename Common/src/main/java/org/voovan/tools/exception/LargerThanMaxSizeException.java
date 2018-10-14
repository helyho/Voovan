package org.voovan.tools.exception;

import java.io.IOException;

/**
 * 内存已被释放的异常
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class LargerThanMaxSizeException extends IOException{

    public LargerThanMaxSizeException() {
        super();
    }


    public LargerThanMaxSizeException(String message) {
        super(message);
    }


    public LargerThanMaxSizeException(String message, Throwable cause) {
        super(message, cause);
    }


    public LargerThanMaxSizeException(Throwable cause) {
        super(cause);
    }
}
