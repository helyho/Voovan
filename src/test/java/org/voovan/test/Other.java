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
    }

}
