package org.voovan.tools.collection;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.voovan.tools.json.JSON;

public class SimpleDalayQueue<T> extends AbstractQueue implements BlockingQueue {
    private int lifeTime;
    private SortType sortType = SortType.ASC;
    private DelayQueue<DelayedWrap<T>> data;

    /**
     * @param windowSize
     * @param data
     */
    public SimpleDalayQueue(int lifeTime) {
        this.lifeTime = lifeTime;
        this.data = new DelayQueue<DelayedWrap<T>>();
    }

    public SimpleDalayQueue(int lifeTime, SortType sortType) {
        this.lifeTime = lifeTime;
        this.data = new DelayQueue<DelayedWrap<T>>();
        this.sortType = sortType;
    }

    /**
     * @return the lifeTime
     */
    public int getLifeTime() {
        return lifeTime;
    }

    /**
     * @return the datas
     */
    public DelayQueue<DelayedWrap<T>> getData() {
        return data;
    }

    public boolean addObject(T t) {
        return data.offer(new DelayedWrap<T>(t, lifeTime, sortType));
    }

    @Override
    public Object peek() {
        DelayedWrap<T> obj = data.peek();
        return obj == null ? null : obj.getData();
    }

    @Override
    public Object poll() {
        DelayedWrap<T> obj = data.poll();
        return obj == null ? null : obj.getData();
    }

    @Override
    public int drainTo(Collection arg0) {
        List<DelayedWrap> wrapedDatas = (List<DelayedWrap>) arg0.stream()
                .map(a -> new DelayedWrap(a, lifeTime, sortType)).collect(Collectors.toList());
        return data.drainTo(wrapedDatas);
    }

    @Override
    public int drainTo(Collection arg0, int arg1) {
        List<DelayedWrap> wrapedDatas = (List<DelayedWrap>) arg0.stream()
                .map(a -> new DelayedWrap(a, lifeTime, sortType)).collect(Collectors.toList());
        return data.drainTo(wrapedDatas, arg1);
    }

    @Override
    public boolean offer(Object arg0) {
        return data.offer(new DelayedWrap(arg0, lifeTime, sortType));
    }

    @Override
    public boolean offer(Object arg0, long arg1, TimeUnit arg2) throws InterruptedException {
        return data.offer(new DelayedWrap(arg0, lifeTime, sortType), arg1, arg2);
    }

    @Override
    public Object poll(long arg0, TimeUnit arg1) throws InterruptedException {
        DelayedWrap<T> obj = data.poll(arg0, arg1);
        return obj == null ? null : obj.getData();
    }

    @Override
    public void put(Object arg0) throws InterruptedException {
        data.put(new DelayedWrap(arg0, lifeTime, sortType));
    }

    @Override
    public int remainingCapacity() {
        return data.remainingCapacity();
    }

    @Override
    public Object take() throws InterruptedException {
        DelayedWrap<T> obj = data.take();
        return obj == null ? null : obj.getData();
    }

    @Override
    public Iterator iterator() {
        return new innerIterator<>(data);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Object[] toArray(IntFunction arg0) {
        Object[] arrs = data.toArray(arg0);
        return Arrays.stream(arrs).map(i -> ((DelayedWrap<T>) i).getData()).toArray(arg0);
    }

    @Override
    public String toString() {
        return JSON.toJSON(data.toArray(new Object[0]));
    }

    public class innerIterator<T> implements Iterator<T> {
        private DelayQueue<DelayedWrap<T>> data;
        private Iterator<DelayedWrap<T>> parentIterator;

        /**
         * @param data
         */
        public innerIterator(DelayQueue<DelayedWrap<T>> data) {
            this.data = data;
            this.parentIterator = data.iterator();
        }

        @Override
        public boolean hasNext() {
            return parentIterator.hasNext();
        }

        @Override
        public T next() {
            return (T) parentIterator.next();
        }

    }

    public enum SortType {
        ASC, DESC
    }

    public class DelayedWrap<T> implements Delayed {

        private T data;

        private SortType sortType = SortType.ASC;
        private Long createTime;
        private int lifeTime;

        /**
         * @param data
         * @param lifeTime
         */
        public DelayedWrap(T data, int lifeTime, SortType sortType) {
            this.createTime = System.currentTimeMillis();
            this.data = data;
            this.lifeTime = lifeTime;
            this.sortType = sortType;
        }

        /**
         * @return the data
         */
        public T getData() {
            return data;
        }

        /**
         * @param data the data to set
         */
        public void setData(T data) {
            this.data = data;
        }

        /**
         * @return the objTimestamp
         */
        public Long getCreateTime() {
            return createTime;
        }

        /**
         * @param objTimestamp the objTimestamp to set
         */
        public void setCreateTime(Long objTimestamp) {
            this.createTime = objTimestamp;
        }

        /**
         * @return the objTimestamp
         */
        public int getLifeTime() {
            return lifeTime;
        }

        /**
         * @param objTimestamp the objTimestamp to set
         */
        public void setLifeTime(int objTimestamp) {
            this.lifeTime = objTimestamp;
        }

        @Override
        public int compareTo(Delayed arg0) {
            DelayedWrap delayedWrap = (DelayedWrap) arg0;
            int compareResult = createTime.compareTo(delayedWrap.getCreateTime());
            if (sortType == SortType.DESC) {
                compareResult = compareResult * -1;
            }

            return compareResult;
        }

        @Override
        public long getDelay(TimeUnit timeUnit) {
            return timeUnit.convert((createTime + lifeTime) - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
    }

}
