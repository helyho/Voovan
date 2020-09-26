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
public class Schema extends Property{


    private String $ref;



    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        this.$ref = $ref;
    }


}
