package org.voovan.tools.serialize;

import org.voovan.tools.TObject;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.JSONPath;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.List;

/**
 * 默认框架内 JSON 序列化的实现
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class DefaultJSONSerialize implements Serialize {
    @Override
    public byte[] serialize(Object obj) {
        try{
            Class clazz = obj.getClass();
            Class[] genericClazzs = TReflect.getGenericClass(obj);

            return JSON.toJSON(TObject.asMap("T",clazz.getCanonicalName(),  "G", genericClazzs, "V", obj)).getBytes();
        } catch (Exception e){
            Logger.error("TSerialize.serializeJDK error: ", e);
            return null;
        }
    }

    @Override
    public <T> T unserialize(byte[] bytes, Class<T> clazz) {
        try {

            Class mainClazz = null;

            JSONPath jsonPath = new JSONPath(new String(bytes));

            mainClazz = Class.forName(jsonPath.value("/T", String.class));
            List<String> genericClazzStrs = jsonPath.listObject("/G", String.class);
            Class[] genericClazzs = new Class[genericClazzStrs.size()];

            for (int i = 0; i < genericClazzStrs.size(); i++) {
                genericClazzs[i] = Class.forName(genericClazzStrs.get(i));
            }

            genericClazzs = genericClazzs.length == 0 ? null : genericClazzs;


            return TReflect.getObjectFromMap(mainClazz, jsonPath.mapObject("/V", genericClazzs), true);
        } catch (Exception e){
            Logger.error("TSerialize.serializeJDK error: ", e);
            return null;
        }
    }
}
