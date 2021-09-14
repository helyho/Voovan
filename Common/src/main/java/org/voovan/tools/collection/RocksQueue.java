package org.voovan.tools.collection;

import org.rocksdb.RocksIterator;
import org.voovan.tools.serialize.ProtoStuffSerialize;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocksDB 的 Queue 封装
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RocksQueue<E> implements Queue<E> {
    private static long BASE_SEQ = 1000000000000000000L;

    private RocksMap root;
    private RocksMap<Long, E> container;

    private volatile long firstSeq;
    private volatile long  lastSeq;

    public RocksQueue(RocksMap rocksMap, String name) {
        this.root = rocksMap;
        this.container = rocksMap.duplicate(name);
        this.container.getReadOptions().setTotalOrderSeek(true);
        this.firstSeq = container.firstKey() == null ? BASE_SEQ + 1 : container.firstKey(); //加 1 的目的是初始化到 isEmpty() == true 的状态
        this.lastSeq = container.lastKey() == null ? BASE_SEQ : container.lastKey();
    }

    public RocksMap getRoot() {
        return root;
    }

    public RocksMap<Long, E> getContainer() {
        return container;
    }

    @Override
    public synchronized boolean add(E e) {
        if(e == null) {
            throw new NullPointerException();
        }
        boolean isEmpty = isEmpty();
        Long newLast = BASE_SEQ;
        if(isEmpty) {
            firstSeq = newLast + 1;
            lastSeq = newLast;
        }
        lastSeq = lastSeq + 1;
        container.put(lastSeq, e);

        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        c.stream().forEach(e->add(e));
        return true;
    }


    @Override
    public int size() {
        return (int)(lastSeq - firstSeq + 1);
    }

    @Override
    public boolean isEmpty() {
        return firstSeq > lastSeq;
    }

    @Override
    public boolean offer(E e) {
        return add(e);
    }

    @Override
    public E remove() {
        return poll();
    }

    @Override
    public synchronized E poll() {
        E e = null;

        if(!isEmpty()) {
            e = container.remove(firstSeq);
            firstSeq = firstSeq + 1;
        } else {
            return null;
        }

        return e;
    }

    @Override
    public E element() {
        E e = poll();
        if(e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    public E get(int offset) {
        return container.get(firstSeq + offset);
    }

    @Override
    public E peek() {
        return container.get(container.get(firstSeq));
    }

    @Override
    public void clear() {
        container.clear();
        this.firstSeq = BASE_SEQ + 1; //加 1 的目的是初始化到 isEmpty() == true 的状态
        this.lastSeq = BASE_SEQ;
    }

    @Override
    public Iterator<E> iterator() {
        return new RocksQueueIterator(this);
    }

    @Override
    public Object[] toArray() {
        Map map = container.subMap(firstSeq, lastSeq);
        return map.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return container.subMap(firstSeq, lastSeq).values().toArray(a);
    }


    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void flush(boolean sync, boolean allowStall){
        container.flush(sync, allowStall);
    }

    public void flush(boolean allowStall){
        flush(true, allowStall);
    }

    public void flush(){
        flush(true, false);
    }

    @Override
    public String toString() {
        return "RocksQueue{" +
                "firstSeq=" + firstSeq +
                ", lastSeq=" + lastSeq +
                ", size=" + size() +
                ", container_size=" + container.size() +
                '}';
    }


    public class RocksQueueIterator<E> implements Iterator<E>, Closeable {
        RocksMap.RocksMapIterator rocksMapIterator;

        protected RocksQueueIterator(RocksQueue<E> queue) {
            this.rocksMapIterator = queue.getContainer().iterator();
        }

        @Override
        public void close() throws IOException {
            rocksMapIterator.close();
        }

        @Override
        public boolean hasNext() {
            return rocksMapIterator.hasNext();
        }

        public byte[] nextBytes() {
            return rocksMapIterator.valueBytes();
        }

        public E next() {
            return (E) rocksMapIterator.value();
        }

        public boolean isValid(){
            return rocksMapIterator.isValid();
        }
    }
}
