package org.voovan.http.server.module.annontationRouter.swagger.entity;

import org.voovan.tools.reflect.annotation.Serialization;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Parameter {
    private String name;
    private String in;
    private String description;
    private Boolean required;
    private String type;
    private String format;
    private Schema schema;
    @Serialization("default")
    private String defaultVal;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description.isEmpty() ? null : description;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
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

    public String getDefaultVal() {
        return defaultVal;
    }

    public void setDefaultVal(String defaultVal) {
        this.defaultVal = defaultVal.isEmpty() ? null : defaultVal;
    }

    public Schema getSchema() {
        if(schema == null) {
            schema = new Schema();
        }
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }
}
