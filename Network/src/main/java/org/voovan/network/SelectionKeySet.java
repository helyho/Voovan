package org.voovan.network;

import java.nio.channels.SelectionKey;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SelectionKeySet extends AbstractSet<SelectionKey> {

	SelectionKey[] selectionKeys;
	volatile int size;

	public SelectionKeySet(int cap) {
		selectionKeys = new SelectionKey[cap];
	}

	public SelectionKey[] getSelectionKeys() {
		return selectionKeys;
	}

	@Override
	public boolean add(SelectionKey selectionKey) {
		selectionKeys[size++] = selectionKey;
		if (selectionKeys.length == size) {
			reallocate();
		}
		return true;
	}

	@Override
	public boolean contains(Object o) {
		return false;
	}

	private void reallocate() {
		selectionKeys = Arrays.copyOf(selectionKeys, selectionKeys.length << 1);
	}

	@Override
	public Iterator<SelectionKey> iterator() {
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
