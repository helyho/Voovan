package org.voovan.test;

import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TPerformance;
import org.voovan.tools.TStream;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.util.*;


public class Other {

    public static Map<String,List> alist = new HashMap<String,List>();

    public static void main(String[] args) throws Exception {
        System.out.println(new String(TFile.loadFileLastLines(new File("/Users/helyho/Work/Java/Voovan/logs/sysout.20160419.log"),20)));
    }

}
