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
    TEXT, JSON, XML, IMAGE_JPG, IMAGE_GIF, IMAGE_PNG, IMAGE_SVG;

    /**
     * 根据标识获得标准Content-Type类型
     * @param httpContentType  标记
     * @return 标准Content-Type类型
     */

    public static String getHttpContentType(HttpContentType httpContentType){
        if(httpContentType == HttpContentType.TEXT){
            return HttpStatic.TEXT_PLAIN_STRING;
        }
        else if(httpContentType == HttpContentType.XML){
            return "text/xml";
        }
        else if(httpContentType == HttpContentType.JSON){
            return HttpStatic.APPLICATION_JSON_STRING;
        }
        else if(httpContentType == HttpContentType.IMAGE_GIF){
            return "image/gif";
        }
        else if(httpContentType == HttpContentType.IMAGE_JPG){
            return "image/jpeg";
        }
        else if(httpContentType == HttpContentType.IMAGE_PNG){
            return "image/png";
        }
        else if(httpContentType == HttpContentType.IMAGE_SVG){
            return "image/svg+xml";
        }
        else {
            return HttpStatic.TEXT_HTML_STRING;
        }
    }
}
