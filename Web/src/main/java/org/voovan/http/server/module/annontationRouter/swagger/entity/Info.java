package org.voovan.http.server.module.annontationRouter.swagger.entity;

import org.voovan.tools.TObject;

import java.util.Map;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Info {
    private String title;
    private String description;
    private String version;
    private String termsOfService = "http://swagger.io/terms/";
    private Map<String, String> contact = TObject.asMap("email","helyho@gmail.com");
    private Map<String, String> license = TObject.asMap("name","Apache 2.0");

    public Info(String title, String description, String version) {
        this.title = title;
        this.description = description;
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
