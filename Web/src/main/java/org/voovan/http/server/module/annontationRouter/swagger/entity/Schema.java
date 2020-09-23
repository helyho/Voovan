package org.voovan.http.server.module.annontationRouter.swagger.entity;

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
    private SchemaItem items;

    //properties 属性名->SchemaItem
    Map<String, SchemaItem> properties;


    public Map<String, SchemaItem> getProperties() {
        if(properties == null) {
            properties = new TreeMap<String, SchemaItem>();
        }

        return properties;
    }

    public void setProperties(Map<String, SchemaItem> properties) {
        this.properties = properties;
    }
}
