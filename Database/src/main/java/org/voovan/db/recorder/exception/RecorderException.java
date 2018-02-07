package org.voovan.db.recorder.exception;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class RecorderException extends Exception {
    public RecorderException(String message){
        super(message);
    }

    public RecorderException(String message, Throwable e){
        super(message, e);
    }
}
