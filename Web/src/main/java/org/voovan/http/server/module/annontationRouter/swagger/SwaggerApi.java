package org.voovan.http.server.module.annontationRouter.swagger;

import org.voovan.Global;
import org.voovan.http.message.HttpStatic;
import org.voovan.http.server.*;
import org.voovan.http.server.module.annontationRouter.annotation.*;
import org.voovan.http.server.module.annontationRouter.router.AnnotationRouter;
import org.voovan.http.server.module.annontationRouter.router.RouterInfo;
import org.voovan.http.server.module.annontationRouter.swagger.entity.*;
import org.voovan.http.server.module.annontationRouter.swagger.entity.Property;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TProperties;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.reflect.annotation.NotSerialization;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Swagger 配置构造类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SwaggerApi {
    public static ConcurrentHashMap<String,Swagger> MODULE_SWAGGER = new ConcurrentHashMap<String, Swagger>();

    static {
    }

    public static void init(HttpModule httpModule) {
        WebServer webserver = httpModule.getWebServer();
        Map<String, Object> swaggerConfig = (Map<String, Object>) httpModule.getModuleConfig().getParameter("swagger");
        if(swaggerConfig == null) {
            swaggerConfig = TObject.asMap();
        }

        String  moduleName      = httpModule.getModuleConfig().getName();
        String  modulePath      = httpModule.getModuleConfig().getPath();
        Boolean enable          = (Boolean) swaggerConfig.getOrDefault("enable", true);

        if(enable == false) {
            return;
        }

        String  routePath       = (String)  swaggerConfig.getOrDefault("routePath", "/swagger");
        Integer refreshInterval = (Integer) swaggerConfig.getOrDefault("refreshInterval", 30);
        String  description     = (String)  swaggerConfig.getOrDefault("description", "");
        String  version         = (String)  swaggerConfig.get("version");

        MODULE_SWAGGER.put(moduleName, new Swagger(httpModule.getModuleConfig().getPath(), description, version));

        if(enable) {
            SwaggerApi.buildModuleSwagger(moduleName);
            Global.getHashWheelTimer().addTask(()->{
                MODULE_SWAGGER.put(moduleName, new Swagger(modulePath, description, version));

                SwaggerApi.buildModuleSwagger(moduleName);
            }, refreshInterval);

            String swaggerPath = routePath + (modulePath.startsWith("/") ? modulePath : ("/" + modulePath));
            swaggerPath = HttpDispatcher.fixRoutePath(swaggerPath);

            webserver.get(swaggerPath+"-ui", new HttpRouter() {
                @Override
                public void process(HttpRequest request, HttpResponse response) throws Exception {
                    response.header().put(HttpStatic.CONTENT_TYPE_STRING, HttpStatic.TEXT_HTML_STRING);
                    response.write(TFile.loadResource("org/voovan/http/server/conf/swagger.html"));
                }
            });

            webserver.get(swaggerPath, new HttpRouter() {
                @Override
                public void process(HttpRequest request, HttpResponse response) throws Exception {
                    response.header().put(HttpStatic.CONTENT_TYPE_STRING, HttpStatic.APPLICATION_JSON_STRING);
                    response.write(JSON.removeNullNode(JSON.toJSON(MODULE_SWAGGER.get(moduleName))));
                }
            });

            Logger.simplef("[SWAGGER] module: {1}  path: {2}, refreshInterval: {3}", moduleName, swaggerPath, refreshInterval);
        }
    }

    public static Swagger buildModuleSwagger(String moduleName) {
        return buildSwagger(MODULE_SWAGGER.get(moduleName));
    }

    public static Swagger buildSwagger(Swagger swagger) {
        swagger.getTags().addAll(parseAllTags());

        Collections.sort(swagger.getTags(), new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                String tag1Name = o1.getName();
                String tag2Name = o2.getName();
                if(tag1Name.length() > tag2Name.length() && !tag1Name.equals(o2)){
                    return 1;
                } else if(tag1Name.length() < tag2Name.length() &&!tag1Name.equals(o2)){
                    return -1;
                } else if(tag1Name.equals(o2)){
                    return 0;
                } else{
                    return -1;
                }
            }
        });

        Map<String, Tag> tagsMap = new HashMap<String, Tag>();
        for(RouterInfo routerInfo : AnnotationRouter.ROUTER_INFO_LIST) {
            String classUrl = routerInfo.getClassAnnotation().value() == null ? routerInfo.getClassAnnotation().path() : routerInfo.getClassAnnotation().value();
            classUrl = HttpDispatcher.fixRoutePath(classUrl);
            String url = routerInfo.getUrl();
            //转换路径中的参数未 swagger 的格式
            while(url.indexOf("/:") >0 ) {
                int startIndex = url.indexOf(":");
                url = url.substring(0, startIndex) + "{" + url.substring(startIndex + 1);
                int endIndex = url.indexOf("/", startIndex + 1);
                if(endIndex > 0) {
                    url = TString.insert(url, endIndex, "}");
                } else {
                    url = url + "}";
                }
            }

            url = HttpDispatcher.fixRoutePath(url);

            String routeMethod = routerInfo.getRouteMethod();
            Router classAnnotation = routerInfo.getClassAnnotation();
            Class clazz = routerInfo.getClass();
            Router methodAnnotation = routerInfo.getMethodAnnotation();
            Method method = routerInfo.getMethod();

            if(methodAnnotation.hide()) {
                continue;
            }

            String operationId = routerInfo.getClazz().getSimpleName() + "." + routerInfo.getMethod().getName();
            Path path = new Path(operationId, methodAnnotation.summary(), methodAnnotation.description(),
                    new String[]{HttpStatic.APPLICATION_JSON_STRING}, new String[]{HttpStatic.APPLICATION_JSON_STRING},
                    methodAnnotation.deprecated());

            //处理 Tag
            path.getTags().addAll(TObject.asList(methodAnnotation.tags()));
            for(Tag tag : parseTags(classAnnotation).values()) {
                path.getTags().add(tag.getName());
            }
            path.getTags().add(classUrl);

            Annotation[][] paramAnnotationsArrary = method.getParameterAnnotations();
            Class[] paramTypes = method.getParameterTypes();
            int unNamedParamCount = 1;
            for (int i = 0; i < paramAnnotationsArrary.length; i++) {
                Annotation[] paramAnnotations = paramAnnotationsArrary[i];

                if (paramTypes[i] == HttpRequest.class ||
                        paramTypes[i] == HttpResponse.class ||
                        paramTypes[i] == HttpSession.class) {

                    continue;
                }

                if (paramAnnotations.length > 0) {
                    for (Annotation paramAnnotation : paramAnnotations) {
                        if (paramAnnotation instanceof Param) {
                            Parameter parameter = new Parameter();
                            parameter.setIn("path");
                            String[] types = getParamType(paramTypes[i]);
                            parameter.setType(types[0]);
                            parameter.setFormat(types[1]);
                            parameter.setName(((Param) paramAnnotation).value());
                            parameter.setDescription(((Param) paramAnnotation).description());
                            parameter.setRequired(((Param) paramAnnotation).isRequire());
                            parameter.setDefaultVal(((Param) paramAnnotation).defaultVal());
                            path.getParameters().add(parameter);
                        } else if (paramAnnotation instanceof Header) {
                            Parameter parameter = new Parameter();
                            parameter.setIn("header");
                            String[] types = getParamType(paramTypes[i]);
                            parameter.setType(types[0]);
                            parameter.setFormat(types[1]);
                            parameter.setName(((Header) paramAnnotation).value());
                            parameter.setDescription(((Header) paramAnnotation).description());
                            parameter.setRequired(((Header) paramAnnotation).isRequire());
                            parameter.setDefaultVal(((Header) paramAnnotation).defaultVal());
                            path.getParameters().add(parameter);
                        }  else if (paramAnnotation instanceof Cookie) {
                            Parameter parameter = new Parameter();
                            parameter.setIn("cookie");
                            String[] types = getParamType(paramTypes[i]);
                            parameter.setType(types[0]);
                            parameter.setFormat(types[1]);
                            parameter.setName(((Cookie) paramAnnotation).value());
                            parameter.setDescription(((Cookie) paramAnnotation).description());
                            parameter.setRequired(((Cookie) paramAnnotation).isRequire());
                            parameter.setDefaultVal(((Cookie) paramAnnotation).defaultVal());
                            path.getParameters().add(parameter);
                        }else if (paramAnnotation instanceof BodyParam) {
                            if(path.getParameters().size() == 0) {
                                Parameter parameter = new Parameter();
                                parameter.setIn("body");
                                parameter.setName("body");
                                path.getParameters().add(parameter);
                            }

                            Parameter parameter = path.getParameters().get(0);

                            Schema schema = parameter.getSchema();
                            String name = ((BodyParam) paramAnnotation).value();
                            String description = ((BodyParam) paramAnnotation).description();
                            String defaultVal = ((BodyParam) paramAnnotation).defaultVal();
                            boolean isRequire = ((BodyParam) paramAnnotation).isRequire();
                            createSchema(parameter.getSchema(), paramTypes[i], name, description, defaultVal, isRequire);
                        } else if(paramAnnotation instanceof Body) {
                            Parameter parameter = new Parameter();
                            parameter.setIn("body");
                            parameter.setName("body");

                            String description = ((Body) paramAnnotation).description();
                            String defaultVal = ((Body) paramAnnotation).defaultVal();

                            parameter.setDescription(description);
                            parameter.setDefaultVal(defaultVal);

                            Schema schema = parameter.getSchema();
                            createSchema(parameter.getSchema(), paramTypes[i], null, null, null, true);
                            path.getParameters().add(parameter);
                        }
                    }
                } else {
                    Parameter parameter = new Parameter();
                    parameter.setIn("path");
                    String[] types = getParamType(paramTypes[i]);
                    parameter.setType(types[0]);
                    parameter.setFormat(types[1]);
                    parameter.setName("param" + unNamedParamCount);
                    parameter.setRequired(true);
                    path.getParameters().add(parameter);
                    unNamedParamCount++;
                }
            }

            Response response = new Response();

            createSchema(response.getSchema(),method.getReturnType(),null, null, null, null);

            path.getResponses().put("200", response);


            swagger.getPaths().put(url, TObject.asMap(routeMethod.toLowerCase(), path));
        }

//        try {
//            TFile.writeFile(new File("/Users/helyho/swagger.json"), false, JSON.formatJson(JSON.removeNullNode(JSON.toJSON(swagger))).getBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return swagger;
    }

    public static Collection<Tag> parseAllTags() {

        Map<String, Tag> tagsMap = new HashMap<String, Tag>();
        for(RouterInfo routerInfo : AnnotationRouter.ROUTER_INFO_LIST) {
            tagsMap.putAll(parseTags(routerInfo.getClassAnnotation()));
            String URL = routerInfo.getClassAnnotation().value() == null ? routerInfo.getClassAnnotation().path() : routerInfo.getClassAnnotation().value();
            tagsMap.put(URL, new Tag(URL, "Tag of Router class: " + routerInfo.getClazz().getSimpleName()));

        }
        return tagsMap.values();
    }

    public static Map<String, Tag> parseTags(Router router) {
        Map<String, Tag> tagsMap = new HashMap<String, Tag>();
        for(String tag : router.tags()) {
            tag = tag.trim();

            if (tag.length() > 0) {
                int descStart = tag.indexOf("[");
                int descEnd = tag.lastIndexOf("]");
                String name = descStart > 0 ? tag.substring(0, descStart) : tag;
                String description = descStart > 0 && descEnd > 0 && descEnd > descStart ? tag.substring(descStart + 1, descEnd) : "";
                tagsMap.put(name, new Tag(name, description));
            }
        }

        return tagsMap;
    }

    public static Schema createSchema(Schema schema, Class clazz, String name, String description, String defaultVal, Boolean required){
        if(schema == null) {
            schema = new Schema();
        }

        if(TReflect.isSystemType(clazz)) {
            if(name == null) {
                String[] types = getParamType(clazz);
                schema.setType(types[0]);
                schema.setFormat(types[1]);

            } else {
                String[] types = getParamType(clazz);
                schema.setType("object");
                Property property = new Property(types[0], types[1]);
                property.setDefaultVal(TString.isNullOrEmpty(defaultVal) ? null : defaultVal);
                property.setDescription(TString.isNullOrEmpty(description) ? null : description);
                schema.getProperties().put(name, property);
                if(required == null || required) {
                    schema.getRequired().add(name);
                }
            }
        } else {
            createProperites(schema, clazz);
        }

        return schema;
    }

    public static Properites createProperites(Properites properites, Class clazz) {
        for (Field field : TReflect.getFields(clazz)) {
            if(field.getName().startsWith("this$")){
                continue;
            }

            if(Modifier.isTransient(field.getModifiers()) ||
                    Modifier.isStatic(field.getModifiers()) ||
                    field.isAnnotationPresent(NotSerialization.class)) {
                continue;
            }

            String[] types = getParamType(field.getType());
            Property property = null;
            if(types[0] == null) {
                property = new Property();
                createProperites(property, field.getType());
            } else {
                property = new Property(types[0], types[1]);
            }
            properites.getProperties().put(field.getName(), property);
        }

        return properites;
    }

    public static String[] getParamType(Class clazz) {
        if(TReflect.getUnPackageType(clazz.getSimpleName()).equals("String")) {
            return new String[]{"string", null};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("int")) {
            return new String[]{"integer", "int32"};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("long")) {
            return new String[]{"integer", "int64"};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("float")) {
            return new String[]{"number", "float"};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("double")) {
            return new String[]{"number", "double"};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("String")) {
            return new String[]{"string", null};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("byte")) {
            return new String[]{"string", "byte"};
        } else if(TReflect.getUnPackageType(clazz.getName()).equals("boolean")) {
            return new String[]{"boolean", null};
        } else if(TReflect.getUnPackageType(clazz.getSimpleName()).equals("Date")) {
            return new String[]{"string", "date"};
        } else if(TReflect.getUnPackageType(clazz.getSimpleName()).equals("BigDecimal")) {
            return new String[]{"number", null};
        } else if(clazz.isArray()) {
            return new String[]{"array", clazz.getComponentType().getName().toLowerCase()};
        } else if(clazz == List.class) {
            Class[] classes = TReflect.getGenericClass(clazz);
            return new String[]{"array", classes!=null ? getParamType(classes[0])[0] : null};
        } else if(clazz == Map.class) {
            return new String[]{"string", "object"};
        } else if(clazz == Object.class) {
            Class[] genericClazz = TReflect.getGenericClass(clazz);
            return genericClazz!=null  ? getParamType(genericClazz[0]) : new String[]{"string", "object"};
        } else {
            return new String[]{null, null};
        }
    }
}
