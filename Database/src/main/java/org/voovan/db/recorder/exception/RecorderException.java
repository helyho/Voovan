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

    public RecorderException(String message, Throwable e){
        super(message, e);
    }
}
