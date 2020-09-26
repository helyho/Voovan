package org.voovan.http.server.module.annontationRouter.swagger.entity;

import org.voovan.tools.reflect.annotation.Serialization;

import java.util.Map;
import java.util.TreeMap;

/**
 * Swagger properties
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Properties {
    public transient Property property;

    //properties 属性名->SchemaItem
    Map<String, Property> properties;

    @Serialization("$ref")
    private String ref;

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

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = "#/definitions/" + ref;
    }
}
