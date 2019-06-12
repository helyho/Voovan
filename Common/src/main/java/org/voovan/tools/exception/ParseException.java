package org.voovan.tools.exception;

/**
 * 类文字命名
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class ParseException extends RuntimeException {
    public ParseException() {
        super();
    }


    public ParseException(String message) {
        super(message);
    }


    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }


    public ParseException(Throwable cause) {
        super(cause);
    }
}
