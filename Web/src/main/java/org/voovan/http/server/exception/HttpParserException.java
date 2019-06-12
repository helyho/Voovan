package org.voovan.http.server.exception;

import java.io.IOException;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpParserException extends IOException {
    public HttpParserException(String msg){
        super(msg);
    }
}
