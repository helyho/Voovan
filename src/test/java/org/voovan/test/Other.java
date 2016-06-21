package org.voovan.test;

import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.tools.TStream;
import org.voovan.tools.TString;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.util.*;


public class Other {

    public static Map<String,List> alist = new HashMap<String,List>();

    public static void main(String[] args) throws Exception {

        FileInputStream fis = new FileInputStream("/Users/helyho/Downloads/CN-20160619.txt");
        if(fis!=null) {
            while (fis.available() > 0) {
                String line = TStream.readLine(fis);
                String ipMaskStr = TString.searchByRegex(line, "\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+")[0];
                String[] ipMaskArray = ipMaskStr.split("/");
                System.out.println("{\"IP\":\""+ipMaskArray[0] + "\", \"mask\":\"" + convertMask(Integer.parseInt(ipMaskArray[1]))+"\"},");
            }
        }
    }

    /**
     * 将十进制的子网掩码转换为 xxx.xxx.xxx.xxx 形式的子网掩码
     * @param maskNum
     * @return
     */
    public static String convertMask(int maskNum){
        String binMask = "";
        for(int i =0; i<maskNum; i++){
            binMask=binMask+1;
        }
        binMask = TString.rightPad(binMask, 32, '0');

        String mask = "";
        for(int m=0; m<4; m++) {
            Integer segmValue = Integer.parseInt(binMask.substring(0, 8), 2);
            binMask = binMask.substring(8, binMask.length());
            mask = mask+segmValue.toString()+(m!=3?".":"");
        }
        return mask;
    }
}
