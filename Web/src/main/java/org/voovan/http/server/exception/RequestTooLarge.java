package org.voovan.http.server.exception;

import java.io.IOException;

/**
 * Http request 请求超长异常
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RequestTooLarge extends RuntimeException {
    public RequestTooLarge(String description){
        super(description);
    }
}
