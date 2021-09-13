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

    private volatile AtomicLong firstSeq = new AtomicLong();
    private volatile AtomicLong lastSeq = new AtomicLong();

    private Object lock = new Object();

    public RocksQueue(RocksMap rocksMap, String name) {
        this.root = rocksMap;
        this.container = rocksMap.duplicate(name);
        this.container.getReadOptions().setTotalOrderSeek(true);
        this.firstSeq.set(container.firstKey() == null ? BASE_SEQ + 1 : container.firstKey() ); //加 1 的目的是初始化到 isEmpty() == true 的状态
        this.lastSeq.set(container.lastKey() == null ? BASE_SEQ : container.lastKey());
    }

    public RocksMap getRoot() {
        return root;
    }

    public RocksMap<Long, E> getContainer() {
        return container;
    }

    @Override
    public boolean add(E e) {
        if(e == null) {
            throw new NullPointerException();
        }

        boolean isEmpty = isEmpty();
        Long newLast = BASE_SEQ;
        if(isEmpty) {
            firstSeq.set(newLast + 1);
            lastSeq.set(newLast);
        }

        synchronized (lastSeq) {
            lastSeq.updateAndGet(old -> {
                long newSeq = old + 1;
                container.put(newSeq, e);
                return newSeq;
            });
        }

        synchronized (lock) {
            lock.notify();
        }

        return true;
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
        return firstSeq.get() > lastSeq.get();
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
        E e = poll();
        try {
            synchronized (lock) {
                if (e == null) {
                    lock.wait(unit.toMillis(timeout));

                    e = poll();
                    if (e == null) {
                        throw new TimeoutException();
                    }
                }
            }
            return e;
        } catch (InterruptedException ex) {
            throw new TimeoutException(ex.getMessage());
        }
    }

    @Override
    public E poll() {
        Object[] tmp = new Object[1];
        if(!isEmpty()) {
            synchronized (firstSeq) {
                firstSeq.getAndUpdate(old -> {
                    tmp[0] = container.remove(old);
                    return old + 1;
                });
            }
        } else {
            return null;
        }

        return tmp[0] == null ? null : (E)tmp[0];
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
        return container.get(firstSeq.get() + offset);
    }

    @Override
    public E peek() {
        return container.get(container.get(firstSeq));
    }

    @Override
    public void clear() {
        container.clear();
        this.firstSeq.set(BASE_SEQ + 1); //加 1 的目的是初始化到 isEmpty() == true 的状态
        this.lastSeq.set(BASE_SEQ);
    }

    @Override
    public Iterator<E> iterator() {
        return new RocksQueueIterator(this);
    }

    @Override
    public Object[] toArray() {
        Map map = container.subMap(firstSeq.get(), lastSeq.get());
        return map.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return container.subMap(firstSeq.get(), lastSeq.get()).values().toArray(a);
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
