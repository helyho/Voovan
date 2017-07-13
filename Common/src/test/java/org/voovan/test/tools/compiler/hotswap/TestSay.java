package org.voovan.test.tools.compiler.hotswap;

import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TestSay {
    public String say() {
        System.out.println("hello world");
        System.out.println(TFile.fileExists("/Users/helyho/.zshrcbak"));
        TEnv.sleep(1000);
        return TString.removePrefix("finished");
    }
}
