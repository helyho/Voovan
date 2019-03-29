package org.voovan.tools.collection;

import java.nio.channels.SelectionKey;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 简单 Array 为基础的 Set 类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SimpleArraySet<E> extends AbstractSet<E> {

	Object[] elements;
	volatile int size;

	public SimpleArraySet(int cap) {
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
	}

	public E getAndRemove(int index){
		E e = (E)elements[index];
		elements[index] = null;
		return e;
	}

	@Override
	public boolean add(E selectionKey) {
		elements[size++] = selectionKey;
		if (elements.length == size) {
			reallocate();
		}
		return true;
	}

	@Override
	public boolean contains(Object o) {
		return false;
	}

	private void reallocate() {
		elements = Arrays.copyOf(elements, elements.length << 1);
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		return false;
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
		return "SelectionKeySet.size: " + size();
	}

}
