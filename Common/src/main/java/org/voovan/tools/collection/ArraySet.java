package org.voovan.tools.collection;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 简单 Array 为基础的 Set 类
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ArraySet<E> extends AbstractSet<E> {

	Object[] elements;
	volatile int size;

	public ArraySet(int cap) {
		elements = new Object[cap];
	}

	public E[] getElements() {
		return (E[]) elements;
	}

	public E get(int index){
		return (E)elements[index];
	}

	public void set(int index, E e){
		elements[index] = e;
		if(index >= size){
			size++;
		}
	}

	public E getAndRemove(int index){
		E e = (E)elements[index];
		elements[index] = null;
		size--;
		return e;
	}

	@Override
	public boolean add(E objet) {
		elements[size++] = objet;
		if (elements.length == size) {
			reallocate();
		}
		return true;
	}

	public int indexOf(Object o) {
		if (o == null) {
			for (int i = 0; i < size; i++)
				if (elements[i]==null)
					return i;
		} else {
			for (int i = 0; i < size; i++)
				if (o.equals(elements[i]))
					return i;
		}
		return -1;
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) != -1;
	}

	private synchronized void reallocate() {
		elements = Arrays.copyOf(elements, elements.length << 1);
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}

	private void rangeCheck(int index) {
		if (index >= size)
			throw new IndexOutOfBoundsException();
	}

	@Override
	public boolean remove(Object o) {
		int index = indexOf(o);
		if(index==-1){
			return false;
		} else {
			remove(index);
			return true;
		}
	}

	public E remove(int index) {
		Object value = elements[index];
		int numMoved = size - index - 1;
		if (numMoved > 0)
			System.arraycopy(elements, index+1, elements, index,
					numMoved);
		elements[--size] = null;

		return (E) value;
	}

	public void reset() {
		size = 0;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		return "size: " + size();
	}

}
