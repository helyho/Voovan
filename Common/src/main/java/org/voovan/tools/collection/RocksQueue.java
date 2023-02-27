package org.voovan.tools.collection;

import org.voovan.tools.TByte;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * RocksDB 的 Queue 封装
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RocksQueue<E> implements Queue<E> {
    private static long BASE_SEQ = 0L;

    private RocksMap root;
    private RocksMap<byte[], E> container;

    private volatile Long firstSeq;
    private volatile Long  lastSeq;

    public RocksQueue(RocksMap rocksMap, String name) {
        this.root = rocksMap;
        this.container = rocksMap.duplicate(name);
        this.container.getReadOptions().setTotalOrderSeek(true);
        this.firstSeq = container.firstKey() == null ? BASE_SEQ + 1 : TByte.getLong(container.firstKey()); //加 1 的目的是初始化到 isEmpty() == true 的状态
        this.lastSeq = container.lastKey() == null ? BASE_SEQ : TByte.getLong(container.lastKey());
        container.setUseSingleRemove(true);
    }

    public RocksMap getRoot() {
        return root;
    }

    private void cPut(long seq, E e) {
        container.put(TByte.getBytes(seq), e);
    }

    private E cGet(long seq) {
        return container.get(TByte.getBytes(seq));
    }

    private E cRemove(long seq) {
        return container.remove(TByte.getBytes(seq));
    }

    public RocksMap<byte[], E> getContainer() {
        return container;
    }

    @Override
    public synchronized boolean add(E e) {
        if(e == null) {
            throw new NullPointerException();
        }

        if(isEmpty()) {
            firstSeq = BASE_SEQ + 1;
            lastSeq = BASE_SEQ;
        }

        lastSeq = lastSeq + 1;
        cPut(lastSeq, e);

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
            e = cRemove(firstSeq);
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
        Long seq = firstSeq + offset;
        return cGet(seq);
    }

    @Override
    public E peek() {
        return cGet(firstSeq);
    }

    @Override
    public synchronized void clear() {
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
        return container.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[])container.values().toArray(a);
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

    public void compact(){
        container.compact();
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
