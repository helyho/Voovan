package org.voovan.http.server.module.annontationRouter.swagger.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Schema {

    //类型
    private String type = "object";

    //properties 属性名->SchemaItem
    Map<String, Property> properties;

    private List<String> required;

    public Map<String, Property> getProperties() {
        if(properties == null) {
            properties = new TreeMap<String, Property>();
        }

        return properties;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setProperties(Map<String, Property> properties) {
        this.properties = properties;
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
}
