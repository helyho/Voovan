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

    private volatile Long firstSeq;
    private volatile Long lastSeq;

    private Object lock = new Object();

    public RocksQueue(RocksMap rocksMap, String name) {
        this.root = rocksMap;
        this.container = rocksMap.duplicate(name);
        this.container.getReadOptions().setTotalOrderSeek(true);
        firstSeq = container.firstKey();
        this.firstSeq = firstSeq == null ? BASE_SEQ + 1 : firstSeq; //加 1 的目的是初始化到 isEmpty() == true 的状态
        lastSeq = container.lastKey();
        this.lastSeq = lastSeq == null ? BASE_SEQ : lastSeq;
    }

    public RocksMap getRoot() {
        return root;
    }

    public RocksMap<Long, E> getContainer() {
        return container;
    }

    public synchronized Long offerSeq(){
        boolean isEmpty = isEmpty();
        Long newLast = BASE_SEQ;
        if(isEmpty) {
            lastSeq = newLast;
            firstSeq = newLast;
        } else {
//            newLast = lastSeq.incrementAndGet();
            newLast = ++lastSeq;
        }

        return newLast;
    }

    public synchronized Long pollSeq() {
        if(!isEmpty()) {
//            return firstSeq.getAndIncrement();
            return firstSeq++;
        } else {
           return null;
        }
    }

    @Override
    public boolean add(E e) {
        container.put(offerSeq(), e);

        synchronized (lock) {
            lock.notify();
        }

        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        c.stream().forEach(e->add(e));
        return true;
    }


    @Override
    public int size() {
        return container.size();
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

    public E take(long timeout, TimeUnit unit) throws TimeoutException {
        Long seq = pollSeq();
        try {
            synchronized (lock) {
                if (seq == null) {
                    lock.wait(unit.toMillis(timeout));

                    seq = pollSeq();
                    if (seq == null) {
                        throw new TimeoutException();
                    }
                }
            }
            return seq == null ? null : container.remove(seq);
        } catch (InterruptedException e) {
            throw new TimeoutException(e.getMessage());
        }
    }

    @Override
    public E poll() {
        Long seq = pollSeq();
        return seq == null ? null : container.remove(seq);
    }

    @Override
    public E element() {
        E e = container.remove(pollSeq());
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
        return container.get(pollSeq());
    }

    @Override
    public void clear() {
        container.clear();
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
