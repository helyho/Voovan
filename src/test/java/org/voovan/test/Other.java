package org.voovan.test;

import org.voovan.tools.log.Logger;

import java.lang.reflect.ParameterizedType;
import java.util.*;


public class Other {

    public static Map<String,List> alist = new HashMap<String,List>();

    public static void main(String[] args) throws Exception {
        System.out.println(alist.getClass().getGenericSuperclass().getTypeName());
        ParameterizedType pt = (ParameterizedType) alist.getClass().getGenericSuperclass();
        System.out.println(pt.getActualTypeArguments().length);
        System.out.println(pt.getActualTypeArguments()[0]);
        System.out.println(pt.getActualTypeArguments()[1]);

        Properties x =  System.getProperties();
        for(Map.Entry<Object,Object> m: x.entrySet()){
            if(!m.getKey().equals("line.separator")) {
                System.out.println(m.getKey() + "=" + m.getValue());
            }else{
                byte[] bytes = m.getValue().toString().getBytes();
                System.out.println(m.getKey() + "=" + (bytes.length>=1?bytes[0]:"") + "" +(bytes.length>=2?bytes[1]:"") );
            }
        }
    }

}
