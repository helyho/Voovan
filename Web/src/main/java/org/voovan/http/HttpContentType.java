package org.voovan.http;

import org.voovan.http.message.HttpStatic;

/**
 * Some description
 *
 * @author: helyho
 * Project: BlockLink
 * Create: 2017/9/21 19:41
 */
public enum HttpContentType {
    HTML("text/HTML"),
    PLAIN("text/plain"),
    JSON("application/json"),
    XML("text/xml"),
    IMAGE_JPG("image/jpg"),
    IMAGE_GIF("image/gif"),
    IMAGE_PNG("image/png"),
    IMAGE_SVG("image/svg+xml");

    String contentType = null;

    HttpContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
