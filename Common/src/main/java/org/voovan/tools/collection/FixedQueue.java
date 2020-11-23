package org.voovan.tools.collection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * 定长队列
 *  当队列满时,会自动清除头部元素
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class FixedQueue<E> {

    private static final long serialVersionUID = -6271813154993569614L;
    /**
     * 队列长度
     */
    private int limit;

    private LinkedList<E> queue = new LinkedList<E>();

    public FixedQueue(int limit) {
        this.limit = limit;
    }

    /**
     * 入列：当队列大小已满时，把队头的元素poll掉，将数据插入队尾
     *
     * @param e 入队的元素
     */
    public void offer(E e) {
        while (queue.size() >= limit) {
            queue.poll();
        }
        queue.offer(e);
    }

    public void set(E e, int position) {
        queue.set(position, e);
    }

    public E get(int position) {
        return queue.get(position);
    }

    /**
     * 清空队列
     */
    public void clearAll() {
        this.queue.clear();
    }

    public E getLast() {
        return queue.peekLast();
    }

    public E getFirst() {
        return queue.peekFirst();
    }

    public int getLimit() {
        return limit;
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty(){
        return queue.isEmpty();
    }

    public E removeLast() {
        return queue.removeLast();
    }

    public E removeFirst() {
        return queue.removeFirst();
    }

    public boolean removeFirst(E e) {
        return queue.remove(e);
    }

    public E removeIndex(int index) {
        return queue.remove(index);
    }

    public void addIndex(int index, E object) {
        queue.add(index, object);
        int i = 0;
        while (queue.size() > limit) {
            queue.poll();
            i++;
        }
    }

    public List<E> subList(int from, int to) {
        from = from < 0 ? 0 : from;
        to = to > size() ? size() - 1 : to;

        return queue.subList(from, to);
    }

    public List<E> asList() {
        return queue;
    }

    public FixedQueue<E> clone() {
        FixedQueue<E> newOne = new FixedQueue<E>(this.limit);
        newOne.addAll(queue);
        return newOne;
    }

    public void addAll(Collection<E> datas) {
        for(E e : datas) {
            offer(e);
        }
    }
}
