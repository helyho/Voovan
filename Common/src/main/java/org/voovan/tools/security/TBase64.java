package org.voovan.tools.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Base64算法
 *
 * @author helyho
 * <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TBase64 {

    private static final char[] toBase64 = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    private static final int[] fromBase64 = new int[256];
    static {
        Arrays.fill(fromBase64, -1);
        for (int i = 0; i < toBase64.length; i++)
            fromBase64[toBase64[i]] = i;
        fromBase64['='] = -2;
    }

    public static String encode(byte[] binaryData) {
        int off = 0;
        int end = binaryData.length;
        int linemax = -1;
        byte newline[] = null;
        boolean doPadding = true;
        byte[] src = binaryData;
        byte[] dst = new byte[4 * ((src.length + 2) / 3)];

        char[] base64 = toBase64;
        int sp = off;
        int slen = (end - off) / 3 * 3;
        int sl = off + slen;
        if (linemax > 0 && slen  > linemax / 4 * 3)
            slen = linemax / 4 * 3;
        int dstLength = 0;
        while (sp < sl) {
            int sl0 = Math.min(sp + slen, sl);
            for (int sp0 = sp, dp0 = dstLength ; sp0 < sl0; ) {
                int bits = (src[sp0++] & 0xff) << 16 |
                        (src[sp0++] & 0xff) <<  8 |
                        (src[sp0++] & 0xff);
                dst[dp0++] = (byte)base64[(bits >>> 18) & 0x3f];
                dst[dp0++] = (byte)base64[(bits >>> 12) & 0x3f];
                dst[dp0++] = (byte)base64[(bits >>> 6)  & 0x3f];
                dst[dp0++] = (byte)base64[bits & 0x3f];
            }
            int dlen = (sl0 - sp) / 3 * 4;
            dstLength += dlen;
            sp = sl0;
            if (dlen == linemax && sp < end) {
                for (byte b : newline){
                    dst[dstLength++] = b;
                }
            }
        }
        if (sp < end) {               // 1 or 2 leftover bytes
            int b0 = src[sp++] & 0xff;
            dst[dstLength++] = (byte)base64[b0 >> 2];
            if (sp == end) {
                dst[dstLength++] = (byte)base64[(b0 << 4) & 0x3f];
                if (doPadding) {
                    dst[dstLength++] = '=';
                    dst[dstLength++] = '=';
                }
            } else {
                int b1 = src[sp++] & 0xff;
                dst[dstLength++] = (byte)base64[(b0 << 4) & 0x3f | (b1 >> 4)];
                dst[dstLength++] = (byte)base64[(b1 << 2) & 0x3f];
                if (doPadding) {
                    dst[dstLength++] = '=';
                }
            }
        }
        return new String(Arrays.copyOf(dst, dstLength));
    }

    public static byte[] decode(String encoded) {
        byte[] src = encoded.getBytes(StandardCharsets.ISO_8859_1);
        int sp = 0;
        int sl = src.length;


        if (sl%4 != 0) {
            return null;//should be divisible by four
        }

        int numberQuadruple    = (sl/4);

        if (numberQuadruple == 0)
            return new byte[0];

        byte[] dst = new byte[numberQuadruple*3];


        int[] base64 = fromBase64;
        int dstLength = 0;
        int bits = 0;
        int shiftto = 18;       // pos of first byte of 4-byte atom
        while (sp < sl) {
            int b = src[sp++] & 0xff;
            if ((b = base64[b]) < 0) {
                if (b == -2) {
                    if (shiftto == 6 && (sp == sl || src[sp++] != '=') ||
                            shiftto == 18) {
                        throw new IllegalArgumentException(
                                "Input byte array has wrong 4-byte ending unit");
                    }
                    break;
                }

				throw new IllegalArgumentException(
						"Illegal base64 character " +
								Integer.toString(src[sp - 1], 16));
            }
            bits |= (b << shiftto);
            shiftto -= 6;
            if (shiftto < 0) {
                dst[dstLength++] = (byte)(bits >> 16);
                dst[dstLength++] = (byte)(bits >>  8);
                dst[dstLength++] = (byte)(bits);
                shiftto = 18;
                bits = 0;
            }
        }
        if (shiftto == 6) {
            dst[dstLength++] = (byte)(bits >> 16);
        } else if (shiftto == 0) {
            dst[dstLength++] = (byte)(bits >> 16);
            dst[dstLength++] = (byte)(bits >>  8);
        } else if (shiftto == 12) {
            throw new IllegalArgumentException(
                    "Last unit does not have enough valid bits");
        }
        while (sp < sl) {
            throw new IllegalArgumentException(
                    "Input byte array has incorrect ending byte at " + sp);
        }
        return Arrays.copyOf(dst, dstLength);
    }


}
