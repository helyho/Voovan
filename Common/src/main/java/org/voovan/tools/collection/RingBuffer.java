package org.voovan.tools.collection;

import org.voovan.tools.log.Logger;

import java.nio.BufferOverflowException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 无锁环形队列
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RingBuffer<T> {
	public static final int DEFAULT_SIZE = 128;

	public Object[] elements;
	private  AtomicInteger readPositon = new AtomicInteger(0);
	private  AtomicInteger writePositon= new AtomicInteger(0);
	private  AtomicInteger size = new AtomicInteger(0);
	private  int capacity;

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
		this.capacity = nextPowerOfTwo(capacity);
		elements = new Object[this.capacity];
	}

	/**
	 * 获得读指针位置
	 *
	 * @return 读指针位置
	 */
	public int getReadPositon() {
		return readPositon.get();
	}

	/**
	 * 获得写指针位置
	 *
	 * @return 写指针位置
	 */
	public int getWritePositon() {
		return writePositon.get();
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
		if (remaining() < offset || offset < 0) {
			throw new BufferOverflowException();
		}

		readPositon.getAndUpdate( p -> {
			p = p + offset;
			if(p >= capacity) {
				p = p %capacity;
			}
			return p;
		});
	}

	/**
	 * 缓冲区空判断
	 *
	 * @return true: 缓冲区无可用数据, false: 缓冲区有可用数据
	 */
	public Boolean isEmpty() {
		return size.get() == 0;
	}

	/**
	 * 缓冲区满判断
	 *
	 * @return true: 缓冲区已满, false: 缓冲区未满
	 */
	public Boolean isFull() {
		return size.get() == capacity;
	}

	/**
	 * 清理缓冲区
	 */
	public void clear() {
		this.readPositon.set(0);
		this.writePositon.set(0);
	}

	/**
	 * 获得基于索引位置的数据
	 *
	 * @param offset 偏移量
	 * @return byte 数据
	 */
	public T get(int offset) {
		if (offset < remaining()) {
			int realOffset = (readPositon.get() + offset) % capacity;
			return (T)elements[realOffset];
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
		return elements;
	}

	/**
	 * 读取一个 byte
	 *
	 * @return byte 数据
	 */
	public T pop() {
		if (isEmpty()) {
			return null;
		}

		size.decrementAndGet();
		int readPositionV = readPositon.getAndUpdate(p->{
			p++;
			return p == capacity ? 0 : p;
		});

		T t = (T)elements[readPositionV];
		elements[readPositionV] = null;

		return t;
	}


	/**
	 * 增加一个对象
	 *
	 * @param t 对象
	 * @return true: 成功, false: 失败
	 */
	public boolean push(T t) {
		if (isFull()) {
			return false;
		}

		int writePositionV = writePositon.getAndUpdate(p->{
			p++;
			return p == capacity ? 0 : p;
		});
		elements[writePositionV] = t;
		size.incrementAndGet();

		return true;
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
		if (length > avaliable()) {
			return 0;
		}

		for (int i = 0; i < length; i++) {
			if(push(ts[offset + i])){
				length = i;
				break;
			}
		}

		return length;
	}

	/**
	 * 缓冲区可用数据量
	 *
	 * @return 缓冲区可用数据量
	 */
	public int remaining() {
		return size.get();
	}

	/**
	 * 缓冲区可写空间
	 *
	 * @return 缓冲区可写空间
	 */
	public int avaliable() {
		return capacity - size.get();
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

	private static int nextPowerOfTwo(int x) {
		if (x == 0) return 1;
		x--;
		x |= x >> 1;
		x |= x >> 2;
		x |= x >> 4;
		x |= x >> 8;
		x |= x >> 16;
		return (x | x >> 32) + 1;
	}

	@Override
	public String toString() {
		return "readPositon=" + readPositon + ", writePositon=" + writePositon + ", capacity=" + capacity + ", size=" + size.get() + ", remaining=" + remaining() + ", avaliable=" + avaliable();
	}
}
