package org.voovan.http.server.module.annontationRouter.swagger.entity;

import org.voovan.tools.reflect.annotation.Serialization;

import java.util.Map;
import java.util.TreeMap;

/**
 * Swagger property
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Property extends Properites{
    /**
     * 必填。参数类型。”string”, “number”, “integer”, “boolean”, “array” or “file”.
     * 由于参数不在请求体，所以都是简单类型。consumes必须为multipart/form-data或者application/x-www-form-urlencoded或者两者皆有。
     * 参数的in值必须为formData。
     */
    private String type;

    /**
     * 前面提到的type的扩展格式。详情参照Data Type Formats。
     */
    private String format;

    //item
    private Property items;

    private Boolean required;

    @Serialization("default")
    private String defaultVal;

    private String description;

    public Property() {
    }

    public Property(String type, String format) {
        this.type = type;

        if(type == null) {
            return;
        }

        //数组的特殊处理
        if(type.equals("array")) {
            items = new Property(format, null);
            return;
        }
        this.format = format;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Property getItems() {
        return items;
    }

    public void setItems(Property items) {
        this.items = items;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getDefaultVal() {
        return defaultVal;
    }

    public void setDefaultVal(String defaultVal) {
        this.defaultVal = defaultVal;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
