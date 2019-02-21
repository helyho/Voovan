package org.voovan.tools;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RingBuffer<T> {
	public static final int DEFAULT_SIZE = 48;

	public ArrayList<T> byteBufferList;
	private int readPositon = 0;
	private int writePositon = 0;
	private int capacity;

	/**
	 * 使用默认容量构造一个环形缓冲区
	 */
	public RingBuffer() {
		this(DEFAULT_SIZE);
	}

	/**
	 * 使用指定容量构造一个环形缓冲区
	 *
	 * @param capacity 分配的容量
	 */
	public RingBuffer(int capacity) {
		byteBufferList = new ArrayList<T>(capacity);
		this.capacity = capacity;

	}

	/**
	 * 获得读指针位置
	 *
	 * @return 读指针位置
	 */
	public int getReadPositon() {
		return readPositon;
	}

	/**
	 * 获得写指针位置
	 *
	 * @return 写指针位置
	 */
	public int getWritePositon() {
		return writePositon;
	}

	/**
	 * 获得容量
	 *
	 * @return 容量
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * 读指针跳过特定的偏移量
	 *
	 * @param offset 偏移量
	 */
	public void skip(int offset) {
		if (remaining() < offset) {
			throw new BufferOverflowException();
		}

		readPositon = (readPositon + offset) % capacity;
	}

	/**
	 * 缓冲区空判断
	 *
	 * @return true: 缓冲区无可用数据, false: 缓冲区有可用数据
	 */
	public Boolean isEmpty() {
		return readPositon == writePositon;
	}

	/**
	 * 缓冲区满判断
	 *
	 * @return true: 缓冲区已满, false: 缓冲区未满
	 */
	public Boolean isFull() {
		return (writePositon + 1) % capacity == readPositon;
	}

	/**
	 * 清理缓冲区
	 */
	public void clear() {
		this.readPositon = 0;
		this.writePositon = 0;
	}

	/**
	 * 获得基于索引位置的数据
	 *
	 * @param offset 偏移量
	 * @return byte 数据
	 */
	public T get(int offset) {
		if (offset < remaining()) {
			int realOffset = (readPositon + offset) % capacity;
			return byteBufferList.get(realOffset);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * 读取所有缓冲区的数据
	 * 不影响读写位置
	 *
	 * @param t      用于读取数据的 byte 数组
	 * @param offset 偏移量
	 * @param length 数据长度
	 * @return 读取数据大小
	 */
	public int get(T[] t, int offset, int length) {
		if (length > remaining()) {
			length = remaining();
		}

		for (int i = offset; i < offset + length; i++) {
			t[i] = get(i);
		}

		return length;
	}

	/**
	 * 读取所有缓冲区的数据
	 * 不影响读写位置
	 *
	 * @return 读取数据
	 */
	public Object[] toArray() {
		return byteBufferList.toArray();
	}

	/**
	 * 增加一个对象
	 *
	 * @param t 对象
	 */
	public void push(T t) {
		if (isFull()) {
			throw new BufferOverflowException();
		}

		if (isEmpty() && readPositon != 0) {
			clear();
		}
		byteBufferList.add(writePositon, t);
		writePositon = (writePositon + 1) % capacity;
	}

	/**
	 * 写入一个 byte[] 数据
	 *
	 * @param ts     T[] 对象
	 * @param offset 针对 byte[] 的偏移量
	 * @param length 写入数据的长度
	 * @return 写入数据长度
	 */
	public int push(T[] ts, int offset, int length) {
		if (length > remaining()) {
			length = remaining();
		}

		for (int i = 0; i < length; i++) {
			push(ts[offset + i]);
		}

		return length;
	}

	/**
	 * 缓冲区可用数据量
	 *
	 * @return 缓冲区可用数据量
	 */
	public int remaining() {
		if (writePositon == readPositon) {
			return 0;
		} else if (writePositon < readPositon) {
			return capacity - readPositon + writePositon;
		} else {
			return writePositon - readPositon;
		}
	}

	/**
	 * 缓冲区可写空间
	 *
	 * @return 缓冲区可写空间
	 */
	public int avaliable() {
		return capacity - remaining() - 1;
	}

	/**
	 * 读取一个 byte
	 *
	 * @return byte 数据
	 */
	public T pop() {
		if (isEmpty()) {
			throw new BufferUnderflowException();
		}
		T t = byteBufferList.get(readPositon);
		readPositon = (readPositon + 1) % capacity;

		if (isEmpty() && readPositon != 0) {
			clear();
		}
		return t;
	}


	/**
	 * 读取缓冲区的数据
	 *
	 * @param ts     用于读取数据的 byte 数组
	 * @param offset 偏移量
	 * @param length 读取数据的长度
	 * @return 读取数据大小
	 */
	public int pop(T[] ts, int offset, int length) {
		if (length > remaining()) {
			length = remaining();
		}

		for (int i = offset; i < offset + length; i++) {
			ts[i] = pop();
		}

		return length;
	}

	@Override
	public String toString() {
		return "readPositon=" + readPositon + ", writePositon=" + writePositon + ", capacity=" + capacity + ", remaining=" + remaining() + ", avaliable=" + avaliable();
	}
}
