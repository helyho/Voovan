package org.voovan.test;

import org.voovan.http.client.HttpClient;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.json.JSONDecode;
import org.voovan.tools.log.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class Other {


    public static void main(String[] args) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(30);
        byte[] arrs = byteBuffer.array();
        arrs[10] = 10;
        arrs[13] = 13;
        Arrays.copyOf(arrs,50);
        Logger.simple(byteBuffer.capacity());
    }


}
