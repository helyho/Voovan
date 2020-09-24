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
public class Properites {
    //properties 属性名->SchemaItem
    Map<String, Property> properties;


    public Map<String, Property> getProperties() {
        if(properties == null) {
            properties = new TreeMap<String, Property>();
        }

        return properties;
    }

    public void setProperties(Map<String, Property> properties) {
        this.properties = properties;
    }
}
