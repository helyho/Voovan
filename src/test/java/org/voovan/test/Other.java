package org.voovan.test;

import org.voovan.http.client.HttpClient;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.json.JSONDecode;

import java.io.UnsupportedEncodingException;
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
