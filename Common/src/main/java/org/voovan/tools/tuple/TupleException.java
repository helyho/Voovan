package org.voovan.tools.tuple;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TupleException extends RuntimeException {
    public TupleException() {
        super();
    }

    public TupleException(String message) {
        super(message);
    }

    public TupleException(String message, Throwable cause) {
        super(message, cause);
    }

    public TupleException(Throwable cause) {
        super(cause);
    }

    protected TupleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
