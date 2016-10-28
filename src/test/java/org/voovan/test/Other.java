package org.voovan.test;

import com.sun.tools.attach.VirtualMachine;
import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.http.server.MimeTools;
import org.voovan.network.SSLManager;
import org.voovan.tools.*;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class Other {


    public static void main(String[] args) throws Exception {
        Thread mainThread = null;
        for(Thread threadI: TEnv.getThreads()){
            if(threadI.getId()==1)
                mainThread = threadI;
        }
        Thread finalMainThread = mainThread;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    System.out.println(finalMainThread.getState());
                    TEnv.sleep(1000);
                }
            }
        });
        for(Thread threadI: TEnv.getThreads()){
            System.out.println(threadI.getId()+" "+threadI.getName());
        }
        thread.start();
    }
}
