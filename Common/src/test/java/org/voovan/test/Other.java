package org.voovan.test;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.TUnsafe;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
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
        long address = TUnsafe.getUnsafe().allocateMemory(1024);
        TUnsafe.getUnsafe().setMemory(address, 1024, (byte) 9);
        TUnsafe.getUnsafe().putByte(address, (byte) 51);
        TUnsafe.getUnsafe().freeMemory(address);
        System.out.println(TUnsafe.getUnsafe().getByte(address));
    }
}
