package org.voovan.test;

import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.util.regex.Matcher;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Other {

    public static void main(String[] args) throws IOException {
        Matcher matcher = TString.doRegex("http://127.0.0.1:28080/Star/%E4%B8%AD%E6%96%87%E6%B5%8B%E8%AF%95/100",
                "\\/Star\\/(?<name>.*)\\/(?<age>.*)");
        Logger.simple(matcher);
    }

}
