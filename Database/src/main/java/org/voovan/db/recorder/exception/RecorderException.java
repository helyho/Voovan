package org.voovan.db.recorder.exception;

/**
 * Recorder 异常
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class RecorderException extends RuntimeException {
    public RecorderException(String message){
        super(message);
    }

    public RecorderException() {
    }

    public RecorderException(String message, Throwable e){
        super(message, e);
    }

    public RecorderException(Throwable cause) {
        super(cause);
    }

    public RecorderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
