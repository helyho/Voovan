package org.voovan.http.server.module.annontationRouter.swagger;

import org.voovan.Global;
import org.voovan.http.message.HttpStatic;
import org.voovan.http.server.*;
import org.voovan.http.server.module.annontationRouter.annotation.*;
import org.voovan.http.server.module.annontationRouter.router.AnnotationRouter;
import org.voovan.http.server.module.annontationRouter.router.RouterInfo;
import org.voovan.http.server.module.annontationRouter.swagger.annotation.ApiGenericType;
import org.voovan.http.server.module.annontationRouter.swagger.annotation.ApiModel;
import org.voovan.http.server.module.annontationRouter.swagger.annotation.ApiProperty;
import org.voovan.http.server.module.annontationRouter.swagger.annotation.ApiGeneric;
import org.voovan.http.server.module.annontationRouter.swagger.entity.*;
import org.voovan.http.server.module.annontationRouter.swagger.entity.Properties;
import org.voovan.http.server.module.annontationRouter.swagger.entity.Schema;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.reflect.annotation.NotSerialization;

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
        Map<String, Object> swaggerConfig = (Map<String, Object>) httpModule.getModuleConfig().getParameter("Swagger");
        if(swaggerConfig == null) {
            swaggerConfig = TObject.asMap();
        }

        String  moduleName      = httpModule.getModuleConfig().getName();
        String  modulePath      = httpModule.getModuleConfig().getPath();
        Boolean enable          = (Boolean) swaggerConfig.getOrDefault("Enable", true);

        if(enable == false) {
            return;
        }

        String  routePath       = (String)  swaggerConfig.getOrDefault("RoutePath", "/swagger");
        Integer refreshInterval = (Integer) swaggerConfig.getOrDefault("RefreshInterval", -1);
        String  description     = (String)  swaggerConfig.getOrDefault("Description", "");
        String  version         = (String)  swaggerConfig.get("Version");

        MODULE_SWAGGER.put(moduleName, new Swagger(httpModule.getModuleConfig().getPath(), description, version));

        if(enable) {
            SwaggerApi.buildModuleSwagger(moduleName);

            if(refreshInterval > 0) {
                Global.getHashWheelTimer().addTask(() -> {
                    MODULE_SWAGGER.put(moduleName, new Swagger(modulePath, description, version));

                    SwaggerApi.buildModuleSwagger(moduleName);
                }, refreshInterval);
            }

            String swaggerPath = routePath + (modulePath.startsWith("/") ? modulePath : ("/" + modulePath));
            swaggerPath = HttpDispatcher.fixRoutePath(swaggerPath);

            webserver.get(swaggerPath+"-ui", new HttpRouter() {
                @Override
                public void process(HttpRequest request, HttpResponse response) throws Exception {
                    response.header().put(HttpStatic.CONTENT_TYPE_STRING, HttpStatic.TEXT_HTML_STRING);
                    String content = new String(TFile.loadResource("org/voovan/http/server/conf/swagger.html"));
                    content = content.replace("${Title}", moduleName + " - voovan" );
                    response.write(content);
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

        Collections.sort(swagger.getTags(), Swagger.TAG_COMPARATOR);

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
                            parameter.setExample(((Param) paramAnnotation).example());
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
                            parameter.setExample(((Header) paramAnnotation).example());
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
                            parameter.setExample(((Cookie) paramAnnotation).example());
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
                            String example = ((BodyParam) paramAnnotation).example();
                            createSchema(swagger, parameter.getSchema(), paramTypes[i], name, description, defaultVal, isRequire, example, true);

                            if(method.getAnnotationsByType(ApiGeneric.class).length > 0) {
                                if (parameter.getSchema().getType().equals("object")) {
                                    for (Schema propertySchema : parameter.getSchema().getProperties().values()) {
                                        generic(ApiGenericType.REQUEST, swagger, propertySchema, propertySchema.getClazz(), method);
                                    }
                                }
                            }

                        } else if(paramAnnotation instanceof Body) {
                            Parameter parameter = new Parameter();
                            parameter.setIn("body");
                            parameter.setName("body");

                            String description = ((Body) paramAnnotation).description();
                            String defaultVal = ((Body) paramAnnotation).defaultVal();
                            String example = ((Body) paramAnnotation).example();

                            parameter.setDescription(description);
                            parameter.setDefaultVal(defaultVal);

                            Schema schema = parameter.getSchema();
                            createSchema(swagger, parameter.getSchema(), paramTypes[i], null, null, null, true, example, true);
                            path.getParameters().add(parameter);

                            if(method.getAnnotationsByType(ApiGeneric.class).length > 0) {
                                generic(ApiGenericType.REQUEST, swagger, parameter.getSchema(), paramTypes[i], method);

                                if (parameter.getSchema().getType().equals("object")) {
                                    for (Schema propertySchema : parameter.getSchema().getProperties().values()) {
                                        generic(ApiGenericType.REQUEST, swagger, propertySchema, propertySchema.getClazz(), method);
                                    }
                                }
                            }

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

            Response response = buildResponse(swagger, method);
            path.getResponses().put("200", response);


            swagger.getPaths().put(url, TObject.asMap(routeMethod.toLowerCase(), path));
        }

        return swagger;
    }

    /**
     * 构造响应
     * @param swagger Swagger 对象
     * @param method 方法对象
     * @return Response 对象
     */
    public static Response buildResponse(Swagger swagger, Method method) {
        Class returnType = method.getReturnType();

        Response response = new Response();

        createSchema(swagger, response.getSchema(), returnType, null, null, null, null, null, true);

        if(response.getSchema().getRef() == null) {
            String schemaDescription = response.getSchema().getDescription();
            if (schemaDescription != null && !schemaDescription.isEmpty()) {
                response.setDescription(response.getSchema().getDescription());
            }
        } else {
            response.getSchema().setType(null);
            response.getSchema().setDescription(null);
        }

        //范型处理
        Schema schema = response.getSchema();
        generic(ApiGenericType.RESPONSE, swagger, schema, returnType, method);

        return response;
    }

    public static void generic(ApiGenericType genericType, Swagger swagger, Schema schema, Class clazz, Method method) {
        ApiGeneric[] apiGenerics = method.getAnnotationsByType(ApiGeneric.class);
        for(ApiGeneric apiGeneric : apiGenerics) {
            if(apiGeneric.type() == ApiGenericType.ALL || apiGeneric.type() == genericType) {
                if (clazz == Object.class) {
                    schema.setType("object");
                    createSchema(swagger, schema, apiGeneric.clazz(), null, null, null, null, null, false);
                } else if (TReflect.isImpByInterface(clazz, Collection.class)) {
                    schema.setType("array");
                    createSchema(swagger, schema.getItems(), apiGeneric.clazz(), null, null, null, null, null, false);
                } else if (TReflect.isImpByInterface(clazz, Map.class)) {
                    schema.setType("object");

                    Schema keySchema = new Schema("string", null);
                    keySchema.setClazz(String.class);
                    schema.getProperties().put("key", keySchema);

                    Schema valueSchema = new Schema();
                    valueSchema.setClazz(apiGeneric.clazz());
                    createSchema(swagger, valueSchema, apiGeneric.clazz(), null, null, null, null, null, false);
                    schema.getProperties().put("value", valueSchema);
                } else {
                    // 范型无法引用, 所以重新构造 schema
                    if(schema.getRef()!=null) {
                        createSchema(swagger, schema, clazz, null, null, null, null, null, false);
                    }
                    Schema fieldSchema = schema.getProperties().get(apiGeneric.property());
                    createSchema(swagger, fieldSchema, apiGeneric.clazz(), null, null, null, null, null, false);
                }
            }
        }
    }

    public static Collection<Tag> parseAllTags() {

        Map<String, Tag> tagsMap = new HashMap<String, Tag>();
        for(RouterInfo routerInfo : AnnotationRouter.ROUTER_INFO_LIST) {
            tagsMap.putAll(parseTags(routerInfo.getClassAnnotation()));
            String classUrl = routerInfo.getClassAnnotation().value() == null ? routerInfo.getClassAnnotation().path() : routerInfo.getClassAnnotation().value();
            classUrl = HttpDispatcher.fixRoutePath(classUrl);
            tagsMap.put(classUrl, new Tag(classUrl, "Tag of Router class: " + routerInfo.getClazz().getSimpleName()));

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

    public static Schema createSchema(Swagger swagger, Schema schema, Class clazz, String name, String description, String defaultVal, Boolean required, String example, boolean ref){
        if(schema == null) {
            schema = new Schema();
        }

        if(TReflect.isSystemType(clazz)) {
            if(name == null) {
                String[] types = getParamType(clazz);
                schema.setType(types[0]);
                schema.setFormat(types[1]);
                schema.setExample(example);
            } else {
                //for @BodyParam
                String[] types = getParamType(clazz);
                schema.setType("object");
                Schema property = new Schema(types[0], types[1]);
                property.setClazz(clazz);
                property.setDefaultVal(TString.isNullOrEmpty(defaultVal) ? null : defaultVal);
                property.setDescription(TString.isNullOrEmpty(description) ? null : description);
                property.setExample(example);

                schema.getProperties().put(name, property);
                if(required == null || required) {
                    schema.getRequired().add(name);
                }
            }
        } else {
            schema.setType("object");
            if(name == null) {
                //for @BodyParam
                createProperites(swagger, schema, clazz, ref);
                schema.setExample(example);

                ApiModel apiModel = (ApiModel) clazz.getAnnotation(ApiModel.class);
                if (apiModel != null) {
                    schema.setDescription(apiModel.value());
                }
            } else {
                Schema property = new Schema();
                createProperites(swagger, property, clazz, ref);
                schema.setExample(example);

                ApiModel apiModel = (ApiModel) clazz.getAnnotation(ApiModel.class);
                if (apiModel != null) {
                    schema.setDescription(apiModel.value());
                }

                if(required == null || required) {
                    schema.getRequired().add(name);
                }

                schema.getProperties().put(name, property);
            }
        }

        schema.setClazz(clazz);

        return schema;
    }

    public static Object convertExample(String example) {
        if(JSON.isJSON(example)) {
            return JSON.parse(example);
        } else {
            return example;
        }
    }

    public static Properties createProperites(Swagger swagger, Properties properties, Class clazz, boolean ref) {
        //find created Definition
        Schema definitionSchema = null;

        if(ref) {
            definitionSchema = swagger.getDefinitions().get(clazz.getSimpleName());
            if (definitionSchema != null) {
                properties.setProperties(null);
                properties.setRef(clazz.getSimpleName());
                if(properties instanceof Schema) {
                    ((Schema)properties).setClazz(clazz);
                }
                return properties;
            }
        }

        for (Field field : TReflect.getFields(clazz)) {
            if(field.getName().startsWith("this$")){
                continue;
            }

            if(Modifier.isTransient(field.getModifiers()) ||
                    Modifier.isStatic(field.getModifiers()) ||
                    field.isAnnotationPresent(NotSerialization.class)) {
                continue;
            }

            ApiProperty apiProperty = field.getAnnotation(ApiProperty.class);

            if(apiProperty!=null && apiProperty.hidden()) {
                continue;
            }

            String[] types = getParamType(field.getType());
            Schema schema = null;
            if(types[0] == null) {
                schema = new Schema();
                schema.setClazz(field.getType());
                createProperites(swagger, schema, field.getType(), ref);
                schema.setProperties(null);
                schema.setRequired(null);
                schema.setRef(field.getType().getSimpleName());

            } else {
                schema = new Schema(types[0], types[1]);
                schema.setClazz(field.getType());

                if(apiProperty!=null) {
                    schema.setDescription(apiProperty.value());

                    if(!apiProperty.isRequire()) {
                        properties.getParent().getRequired().add(field.getName());
                    }

                    schema.setExample(apiProperty.example());
                } else {
                    properties.getParent().getRequired().add(field.getName());
                }
            }

            properties.getProperties().put(field.getName(), schema);
        }

        //create definition
        if(ref){
            definitionSchema = new Schema();
            definitionSchema.setClazz(clazz);
            definitionSchema.setType("object");
            definitionSchema.getProperties().putAll(properties.getProperties());
            swagger.getDefinitions().put(clazz.getSimpleName(), definitionSchema);

            properties.setProperties(null);
            properties.setRef(clazz.getSimpleName());
        } else {
            properties.setRef(null);
        }

        if(properties instanceof Schema) {
            ((Schema)properties).setClazz(clazz);
        }

        return properties;
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
        } else if(TReflect.isImpByInterface(clazz, Collection.class)) {
            Class[] classes = TReflect.getGenericClass(clazz);
            return new String[]{"array", classes!=null ? getParamType(classes[0])[0] : null};
        } else if(TReflect.isImpByInterface(clazz, Map.class)) {
            return new String[]{"string", "object"};
        } else if(clazz == Object.class) {
            Class[] genericClazz = TReflect.getGenericClass(clazz);
            return genericClazz!=null  ? getParamType(genericClazz[0]) : new String[]{"string", "object"};
        } else {
            return new String[]{null, null};
        }
    }
}
