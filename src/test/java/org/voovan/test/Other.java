package org.voovan.test;

import org.voovan.http.client.HttpClient;
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

        HttpClient httpClient = new HttpClient("https://www.oschina.net/","UTF-8",10000);
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
