package org.voovan.test;

import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;


public class Other {


    public static void main(String[] args) throws Exception {

        Field field = TReflect.findField(TestObject.class,"map");
        Logger.simple(TReflect.getFieldGenericType(field)[0]);

        Method m = TReflect.findMethod(TestObject.class,"setMap",new Class[]{HashMap.class});
        Logger.simple(TReflect.getMethodParameterGenericType(m,0)[0]);

        JSON.parse("\r\n //adfadfadff \r\n { \"IndexServerAddress\": \"https://index.docker.io/v1/\"}");
    }
}
