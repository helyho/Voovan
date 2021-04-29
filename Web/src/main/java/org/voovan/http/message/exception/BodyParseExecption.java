package org.voovan.http.message.exception;

import org.voovan.tools.exception.EmptyStackRuntimeException;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class BodyParseExecption extends RuntimeException {
    private int status;
    private String body;

    public BodyParseExecption(int status, String body) {
        super(body);
        this.status = status;
        this.body = body;
    }

    public BodyParseExecption(int status, String body, String message) {
        super(message + "\r\n" + body);
        this.status = status;
        this.body = body;
    }

    public BodyParseExecption(int status, String body, String message, Throwable cause) {
        super(message + "\r\n" + body, cause);
        this.status = status;
        this.body = body;
    }

    public BodyParseExecption(int status, String body, Throwable cause) {
        super(cause);
        this.status = status;
        this.body = body;
    }

    public BodyParseExecption(int status, String body, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message + "\r\n" + body, cause, enableSuppression, writableStackTrace);
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "BodyParseExecption->{" +
                "status=" + status +
                ", body='" + body + '\'' +
                '}';
    }
}
