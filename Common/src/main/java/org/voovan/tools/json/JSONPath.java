package org.voovan.tools.json;

import org.voovan.Global;
import org.voovan.tools.TObject;
import org.voovan.tools.TStream;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON 使用路径解析的工具类
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class JSONPath extends BeanVisitor{


    private String jsonStr;

    public JSONPath(String jsonStr) {
        super();
        if(jsonStr.startsWith("http")) {
            try {
                URL url = new URL(jsonStr);
                Object object = url.getContent();
                jsonStr = new String(TStream.readAll((InputStream)object));
            } catch (Exception e) {
                Logger.error("Load JSONPath error: " + jsonStr);
            }
        }

        Object result = JSON.parse(jsonStr);
        init(result);
    }

    /**
     * 构造默认的对象
     * @param jsonStr JSONPath 路径
     * @return  转换后的对象
     */
    public static JSONPath newInstance(String jsonStr){
        return new JSONPath(jsonStr);
    }

}
