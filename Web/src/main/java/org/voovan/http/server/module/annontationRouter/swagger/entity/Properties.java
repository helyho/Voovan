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
    public transient Schema schema;

    //properties 属性名->SchemaItem
    Map<String, Schema> properties;

    @Serialization("$ref")
    private String ref;

    public Schema getProperty() {
        return schema;
    }

    public void setProperty(Schema schema) {
        this.schema = schema;
    }

    public Map<String, Schema> getProperties() {
        if(properties == null) {
            properties = new TreeMap<String, Schema>();
        }

        return properties;
    }

    public void setProperties(Map<String, Schema> properties) {
        this.properties = properties;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = "#/definitions/" + ref;
    }
}
