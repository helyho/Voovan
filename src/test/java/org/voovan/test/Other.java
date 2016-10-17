package org.voovan.test;

import com.sun.tools.attach.VirtualMachine;
import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.http.server.MimeTools;
import org.voovan.network.SSLManager;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TStream;
import org.voovan.tools.TString;
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

        HttpClient httpc = new HttpClient("http://www.voovan.org:2735");
        Logger.info(httpc.send("/containers/c3c397d3a10e/logs?stdout=1&tail=20").body().getBodyString());

    }
}
