package org.voovan.http.server.module.annontationRouter.swagger.entity;

import org.voovan.http.server.context.WebContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Swagger root
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Swagger {
    private String swagger = "2.0";
    private Info info;
    private String host;
    private String basePath = "/";      //TODO: 配置文件读取
    private List<Tag> tags = new ArrayList<Tag>();
    private List<String> schemes = new ArrayList<String>();
    //<url,<method, pathinfo>>
    private Map<String, Map<String, Path>> paths = new TreeMap<String, Map<String, Path>>();
    private Security securityDefinitions;

    public Swagger(String basePath, String description, String version) {
        schemes.add("http");
        this.basePath = basePath == null ? "/" : basePath;
        info = new Info(WebContext.getWebServerConfig().getServerName(), description, version == null ? "Voovan " + WebContext.VERSION : version);
        host = WebContext.getWebServerConfig().getHost() + ":" + WebContext.getWebServerConfig().getPort();
    }

    public String getSwagger() {
        return swagger;
    }

    public void setSwagger(String swagger) {
        this.swagger = swagger;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<String> getSchemes() {
        return schemes;
    }

    public void setSchemes(List<String> schemes) {
        this.schemes = schemes;
    }

    public Map<String, Map<String, Path>> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, Map<String, Path>> paths) {
        this.paths = paths;
    }

    public Security getSecurityDefinitions() {
        return securityDefinitions;
    }

    public void setSecurityDefinitions(Security securityDefinitions) {
        this.securityDefinitions = securityDefinitions;
    }
}
