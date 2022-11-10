package org.voovan.http.server.module.annontationRouter.swagger.entity;

import java.util.*;

/**
 * Swagger path
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Path {
    private String summary;
    private String description;
    private String operationId;
    private String[] consumes;
    private String[] produces;
    private List<Parameter> parameters;
    private Boolean deprecated;
    private List<String> tags;
    private Map<String, Response> responses;

    public Path(String operationId, String summary, String description, String[] consumes, String[] produces, Boolean deprecated) {
        this.summary = summary.isEmpty() ? null : summary;
        this.description = description.isEmpty() ? null : description;
        this.operationId = operationId.isEmpty() ? null : operationId;
        this.consumes = consumes;
        this.produces = produces;
        this.deprecated = deprecated;
        this.tags = new ArrayList<String>();
        this.responses = new TreeMap<String, Response>();
        responses.put("200", new Response("200", "successful"));
        responses.put("404", new Response("404", "Resource not found"));
        responses.put("500", new Response("5XX", "error"));
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String[] getConsumes() {
        return consumes;
    }

    public void setConsumes(String[] consumes) {
        this.consumes = consumes;
    }

    public String[] getProduces() {
        return produces;
    }

    public void setProduces(String[] produces) {
        this.produces = produces;
    }

    public List<Parameter> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<Parameter>();
        }

        return parameters;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Map<String, Response> getResponses() {
        return responses;
    }

    public void setResponses(Map<String, Response> responses) {
        this.responses = responses;
    }

    public Parameter getParameter(String name) {
        Optional<Parameter> optional = parameters.parallelStream().filter(parameter->parameter.getName().equals(name)).findAny();
        return optional.isPresent() ? optional.get() : null;

    }
}
