package org.voovan.test;

import com.sun.tools.attach.VirtualMachine;
import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.http.server.MimeTools;
import org.voovan.network.SSLManager;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.*;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class Other {


    public static void main(String[] args) throws Exception {

        Field field = TReflect.findField(TestObject.class,"map");
        Logger.simple(TReflect.getFieldGenericType(field)[0]);

        Method m = TReflect.findMethod(TestObject.class,"setMap",new Class[]{HashMap.class});
        Logger.simple(TReflect.getMethodParameterGenericType(m,0)[0]);


    }
}
