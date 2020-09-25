package org.voovan.http.server.module.annontationRouter.swagger.entity;

import org.voovan.http.server.module.annontationRouter.swagger.SwaggerApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Swagger schema
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Schema extends Properites{

    //类型
    private String type = "object";
    private String format;

    private List<String> required;

    private String $ref;

    private Object example;

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


    public List<String> getRequired() {

        if(required == null) {
            required = new ArrayList<String>();
        }

        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required;
    }

    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        this.$ref = $ref;
    }

    public Object getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example == null || example.isEmpty() ? null : SwaggerApi.convertExample(example);
    }

    public void setExample(Object example) {
        this.example = example;
    }
}
