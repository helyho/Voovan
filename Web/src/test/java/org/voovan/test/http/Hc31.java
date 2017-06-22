package org.voovan.test.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Hc31 {
    public static void main(String[] args) throws IOException {
        HttpClient hc = new HttpClient();
        PostMethod post = new PostMethod("http://127.0.0.1:28080/test/");
        post.setRequestHeader("Connection", "keep-alive");
        NameValuePair[] params = new NameValuePair[2];
        params[0] = new NameValuePair("head", "dafadf");
        params[1] = new NameValuePair("data", "123123123");
        post.addParameters(params);
        int sc = hc.executeMethod(post);
        System.out.println(sc);
        InputStream is = post.getResponseBodyAsStream();
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        int chari;
        while ((chari = br.read()) != -1) {
            sb.append((char) chari);
        }
        br.close();
        String src = new String(sb.toString());
//            byte[] bts = post.getResponseBody();
//            String src = new String(bts);
        System.out.println(src);
    }
}
