package org.voovan.test;

import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.JSONDecode;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;


public class Other {


    public static void main(String[] args) throws Exception {

        Field field = TReflect.findField(TestObject.class,"map");
        Logger.simple(TReflect.getFieldGenericType(field)[0]);

        Method m = TReflect.findMethod(TestObject.class,"setMap",new Class[]{HashMap.class});
        Logger.simple(TReflect.getMethodParameterGenericType(m,0)[0]);

        ByteBuffer bb = ByteBuffer.allocate(0);

        Map<String, Object> WEB_CONFIG = loadMapFromFile("/conf/web.json");
    }

    private static Map<String, Object> loadMapFromFile(String filePath){
        if(TFile.fileExists(TEnv.getSystemPath(filePath))) {
            String fileContent = null;
            try {
                fileContent = new String(TFile.loadFileFromContextPath(filePath),"UTF-8");
                Object configObject = JSONDecode.parse(fileContent);
                return TObject.cast(configObject);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return new HashMap<String,Object>();
    }
}
