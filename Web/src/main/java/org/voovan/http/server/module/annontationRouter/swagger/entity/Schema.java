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

    //item
    private Properties items;

    //properties 属性名->SchemaItem
    Map<String, Properties> properties;

    private List<String> required;


    public Map<String, Properties> getProperties() {
        if(properties == null) {
            properties = new TreeMap<String, Properties>();
        }

        return properties;
    }

    public void setProperties(Map<String, Properties> properties) {
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
