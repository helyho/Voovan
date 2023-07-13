package org.voovan.tools.serialize;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.voovan.tools.TObject;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.JSONPath;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

/**
 * 默认框架内 JSON 序列化的实现
 *
 * @author helyho
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

            Integer[] genericClazzsHash = null;
            if(genericClazzs!=null) {
                genericClazzsHash = new Integer[genericClazzs.length];
                for (int i = 0; i < genericClazzs.length; i++) {
                    genericClazzsHash[i] = TSerialize.getHashByClass(genericClazzs[i]);
                }
            }
            Map result = TObject.asMap("T", TSerialize.getHashByClass(clazz), "V", obj);

            if(genericClazzsHash!=null) {
                result.put("G", genericClazzsHash);
            }

            return JSON.toJSON(result).getBytes();
        } catch (Exception e){
            Logger.error("TSerialize.serializeJDK error: ", e);
            return null;
        }
    }

    @Override
    public <T> T unserialize(byte[] bytes) {
        try {
            JSONPath jsonPath = new JSONPath(new String(bytes));

            Class mainClazz = TSerialize.getClassByHash(jsonPath.value("/T", Integer.class)); 

            List<Integer> genericClazzStrs = jsonPath.listObject("/G", Integer.class);
            Class[] genericClazzs = null;
            if(genericClazzStrs!=null) {
                genericClazzs = new Class[genericClazzStrs.size()];

                for (int i = 0; i < genericClazzStrs.size(); i++) {
                    genericClazzs[i] = TSerialize.getClassByHash(genericClazzStrs.get(i));
                }
            }

            Object ret = null;
            if(TReflect.isSuper(mainClazz, Map.class) || TReflect.isSuper(mainClazz, Collection.class)) {
                ret = TReflect.getObjectFromMap(mainClazz, jsonPath.mapObject("/V", genericClazzs), true);
            } else {
                ret = jsonPath.value("/V", mainClazz);
            }
            return (T)ret;
        } catch (Exception e){
            Logger.error("TSerialize.serializeJDK error: ", e);
            return null;
        }
    }
}
