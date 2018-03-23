package org.voovan.tools.security;

import org.voovan.tools.TObject;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class TBase58 {
    public static final BigInteger BASE = BigInteger.valueOf(58);

    public static final int BLOCK_LENGTH_BYTES = 29;

    public static final int BLOCK_LENGTH_DIGITS = 128;

    public static final List<Character> ALPHABET = TObject.asList('1','2','3','4','5','6','7','8','9',
            'a','b','c','d','e','f','g','h','i','j','k','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
            'A','B','C','D','E','F','G','H','J','K','L','M','N','P','Q','R','S','T','U','V','W','X','Y','Z');


    public static String encode(byte[] source) {
        if (source.length == 0)
            return "";

        BigInteger dividend;

        if (source[0] >= 0) {
            dividend = new BigInteger(source);
        } else {
            byte paddedSource[] = new byte[source.length + 1];
            System.arraycopy(source, 0, paddedSource, 1, source.length);
            dividend = new BigInteger(paddedSource);
        }

        if (dividend.equals(BigInteger.ZERO))
            return "1";

        BigInteger qr[]; // quotient and remainder

        StringBuilder sb = new StringBuilder();

        while (dividend.compareTo(BigInteger.ZERO) > 0) {
            qr = dividend.divideAndRemainder(BASE);
            int base58DigitValue = qr[1].intValue();

            // this tacks each successive digit on at the end, so it's LSD first
            sb.append(ALPHABET.get(base58DigitValue));

            dividend = qr[0];
        }

        // so we reverse the string before returning it
        return sb.reverse().toString();
    }

    public static byte[] decode(final String source) {
        BigInteger value = BigInteger.ZERO;

        Iterator<Character> it = stringIterator(source);
        while (it.hasNext()) {
            value = value.add(BigInteger.valueOf(ALPHABET.indexOf(it.next())));
            if (it.hasNext())
                value = value.multiply(BASE);
        }

        return value.toByteArray();
    }


    private static Iterator<Character> stringIterator(final String string) {
        if (string == null)
            throw new NullPointerException();

        return new Iterator<Character>() {
            private int index = 0;

            public boolean hasNext() {
                return index < string.length();
            }

            public Character next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return string.charAt(index++);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
