package org.voovan.tools.exception;

/**
 * 类文字命名
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class EventRunnerException extends RuntimeException {
    public EventRunnerException() {
        super();
    }


    public EventRunnerException(String message) {
        super(message);
    }


    public EventRunnerException(String message, Throwable cause) {
        super(message, cause);
    }


    public EventRunnerException(Throwable cause) {
        super(cause);
    }
}
