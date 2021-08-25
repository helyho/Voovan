package org.voovan.tools.collection;

import org.rocksdb.RocksIterator;
import org.voovan.tools.TByte;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RocksDB 的 DelayQueue 封装 <br>
 * 最小延时单位: 秒
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RocksDelayQueue<E extends Delayed> implements Queue<E> {

    private RocksMap root;
    private RocksMap<String, E> container;
    private LinkedBlockingDeque<String> queueCache = new LinkedBlockingDeque<>();
    private volatile String lastKey;

    public RocksDelayQueue(RocksMap rocksMap, String name) {
        this.root = rocksMap;
        this.container = rocksMap.duplicate(name);
        this.container.getReadOptions().setTotalOrderSeek(true);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                RocksMap.RocksMapIterator iterator0 = null;
                synchronized (container) {
                    String fromKey = lastKey == null ? null : container.containsKey(lastKey) ? lastKey : null;
                    String toKey = TString.radixConvert(System.currentTimeMillis()/1000, 62);
                    iterator0 = container.iterator(fromKey, toKey, fromKey == null ? 0 : 1, 0);
                }


                try (RocksMap.RocksMapIterator iterator = iterator0) {
                    RocksMap.RocksMapEntry rocksMapEntry = null;
                    while ((rocksMapEntry = iterator.next()) != null) {
                        String key = rocksMapEntry.getKey().toString();
                        lastKey = key;
                        queueCache.add(key);
                    }
                }
            }
        }, 1000, 1000);
    }

    public RocksMap getRoot() {
        return root;
    }

    public RocksMap<String, E> getContainer() {
        return container;
    }

    private synchronized String offerSeq(long delayMilliSecond){
        delayMilliSecond = delayMilliSecond<0 ? 0 : delayMilliSecond;
        String ret = TString.radixConvert((System.currentTimeMillis() + delayMilliSecond)/1000, 62) + TString.radixConvert(TDateTime.currentTimeNanos(), 62);
        return ret;
    }

    private synchronized String currentMilliTime(){
        String ret =  TString.radixConvert((System.currentTimeMillis())/1000, 62);
        return ret;
    }

    @Override
    public boolean add(E e) {
        long delay = e.getDelay(TimeUnit.MILLISECONDS);
        String seq = offerSeq(delay);
        container.put(seq, e);
        if(delay<=0) {
            queueCache.offerFirst(seq);
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
        return container.isEmpty();
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

        long time = TimeUnit.SECONDS.convert(timeout, unit);
        while(time>0) {
            if (e == null) {
                e = poll();
            }

            if(e!=null) {
                break;
            }

            time--;
            synchronized (this) {
                try {
                    this.wait(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (e == null) {
            throw new TimeoutException();
        }

        return e;

    }

    @Override
    public E poll() {
        while(true) {
            String seq = queueCache.poll();
            if (seq != null) {
                E e = container.remove(seq);
                return e;
            } else {
                return null;
            }
        }
    }

    @Override
    public E element() {
        E e = poll();
        if(e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    public Collection<E> get(long second) {
        return container.startWith(currentMilliTime()).values();
    }

    @Override
    public E peek() {
        return container.get(queueCache.peek());
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
        return container.values().toArray();

    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) container.values().toArray(a);
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
        return container.toString();
    }


    public class RocksQueueIterator<E extends Delayed> implements Iterator<E>, Closeable {
        RocksMap.RocksMapIterator rocksMapIterator;

        protected RocksQueueIterator(RocksDelayQueue<E> queue) {
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
