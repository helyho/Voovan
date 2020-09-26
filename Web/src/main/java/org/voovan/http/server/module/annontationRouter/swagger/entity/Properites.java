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
    public transient Property property;

    //properties 属性名->SchemaItem
    Map<String, Property> properties;


    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

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
