package org.voovan.http.server.module.annontationRouter.swagger.entity;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Security {

    private String type;
    private String description;
    private String name;
    private String in;
    private String flow;
    private String authorizationUrl;
    private String tokenUrl;
    private String scopes;

    public String getType() {
        return type;
    }

    public Security setType(String type) {
        this.type = type;
        return  this;
    }

    public String getDescription() {
        return description;
    }

    public Security setDescription(String description) {
        this.description = description;
        return  this;
    }

    public String getName() {
        return name;
    }

    public Security setName(String name) {
        this.name = name;
        return  this;
    }

    public String getIn() {
        return in;
    }

    public Security setIn(String in) {
        this.in = in;
        return  this;
    }

    public String getFlow() {
        return flow;
    }

    public Security setFlow(String flow) {
        this.flow = flow;
        return  this;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public Security setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
        return  this;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public Security setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
        return  this;
    }

    public String getScopes() {
        return scopes;
    }

    public Security setScopes(String scopes) {
        this.scopes = scopes;
        return  this;
    }

}
