package org.voovan.tools;

import java.nio.ByteBuffer;

/**
 * Varint
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class Varint {
    public static byte[] intToVarintBytes(int value) {
        byte[] data = new byte[5];
        int count = 0;
        do {
            data[count] = (byte) ((value & 0x7F) | 0x80);
            count++;
        } while ((value >>= 7) != 0);
        data[count - 1] &= 0x7F;
        byte[] ret = new byte[count];
        System.arraycopy(data, 0, ret, 0, count);
        return ret;
    }

    public static ByteBuffer intToVarintBuffer(int value) {
        byte[] data = new byte[5];
        int count = 0;
        do {
            data[count] = (byte) ((value & 0x7F) | 0x80);
            count++;
        } while ((value >>= 7) != 0);
        data[count - 1] &= 0x7F;
        byte[] ret = new byte[count];

        return ByteBuffer.wrap(data, 0, count);
    }

    public static int varintToInt(byte[] bytes) {
        int value = bytes[0];
        if ((value & 0x80) == 0)
            return value;
        value &= 0x7F;
        int chunk = bytes[1];
        value |= (chunk & 0x7F) << 7;
        if ((chunk & 0x80) == 0)
            return value;
        chunk = bytes[2];
        value |= (chunk & 0x7F) << 14;
        if ((chunk & 0x80) == 0)
            return value;
        chunk = bytes[3];
        value |= (chunk & 0x7F) << 21;
        if ((chunk & 0x80) == 0)
            return value;
        chunk = bytes[4];
        value |= chunk << 28;
        if ((chunk & 0xF0) == 0)
            return value;
        throw new RuntimeException("varint2int error");
    }

    public static int varintToInt(ByteBuffer byteBuffer) {
        int value = byteBuffer.get();
        if ((value & 0x80) == 0)
            return value;
        value &= 0x7F;

        int chunk = byteBuffer.get();
        value |= (chunk & 0x7F) << 7;
        if ((chunk & 0x80) == 0)
            return value;

        chunk = byteBuffer.get();
        value |= (chunk & 0x7F) << 14;
        if ((chunk & 0x80) == 0)
            return value;

        chunk = byteBuffer.get();
        value |= (chunk & 0x7F) << 21;
        if ((chunk & 0x80) == 0)
            return value;

        chunk = byteBuffer.get();
        value |= chunk << 28;
        if ((chunk & 0xF0) == 0)
            return value;
        throw new RuntimeException("varint2int error");
    }
}
