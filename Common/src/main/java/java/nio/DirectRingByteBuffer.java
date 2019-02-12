package java.nio;

import org.voovan.tools.DirectRingBuffer;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DirectRingByteBuffer extends ByteBuffer {

    DirectRingBuffer directRingBuffer = null;

    private DirectRingByteBuffer(DirectRingBuffer directRingBuffer) {
        super(0, 0, directRingBuffer.remaining(), directRingBuffer.getCapacity());
    }

    public static ByteBuffer newInstance(DirectRingBuffer directRingBuffer){
        return new DirectRingByteBuffer(directRingBuffer);
    }

    @Override
    public ByteBuffer slice() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer duplicate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer asReadOnlyBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte get() {
        nextGetIndex();
        return directRingBuffer.read();
    }

    @Override
    public ByteBuffer put(byte b) {
        nextPutIndex();
        directRingBuffer.write(b);
        return this;
    }

    @Override
    public byte get(int index) {
        this.checkIndex(index);
        return get(index);
    }

    @Override
    public ByteBuffer put(int index, byte b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer compact() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isDirect() {
        return true;
    }

    @Override
    byte _get(int i) {
        return 0;
    }

    @Override
    void _put(int i, byte b) {

    }

    @Override
    public char getChar() {
        return 0;
    }

    @Override
    public ByteBuffer putChar(char value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public char getChar(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putChar(int index, char value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharBuffer asCharBuffer() {
        return null;
    }

    @Override
    public short getShort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putShort(short value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getShort(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putShort(int index, short value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ShortBuffer asShortBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putInt(int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putInt(int index, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntBuffer asIntBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLong() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putLong(long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLong(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putLong(int index, long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LongBuffer asLongBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getFloat() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putFloat(float value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getFloat(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putFloat(int index, float value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FloatBuffer asFloatBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getDouble() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putDouble(double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getDouble(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer putDouble(int index, double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DoubleBuffer asDoubleBuffer() {
        throw new UnsupportedOperationException();
    }
}
