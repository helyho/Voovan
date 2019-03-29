package org.voovan.tools.collection;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对象链
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Chain<E>  extends ArrayList<E> {
	public ThreadLocal<AtomicInteger> iteratorLocal = new ThreadLocal<AtomicInteger>();
	public ThreadLocal<AtomicInteger> invertedIteratorLocal = new ThreadLocal<AtomicInteger>();
	private boolean isStop;
	private E currentObj;

	/**
	 * 构造函数
	 */
	public Chain(){
		isStop = false;
		rewind();
	}

	/**
	 * 重置链的迭代器
	 * @return 链对象
	 */
	public Chain<E> rewind(){
		isStop = false;
		if(iteratorLocal.get()==null) {
			iteratorLocal.set(new AtomicInteger(0));
		} else {
			iteratorLocal.get().set(0);
		}

		if(invertedIteratorLocal.get() == null) {
			invertedIteratorLocal.set(new AtomicInteger(this.size() - 1));
		} else {
			invertedIteratorLocal.get().set(this.size() - 1);
		}

		return this;
	}

	/**
	 * 迭代完成
	 */
	public void stop(){
		this.isStop = true;
	}

	/**
	 * 迭代器当前元素
	 * @return 当前元素
	 */
	public E current(){
		return currentObj;
	}

	/**
	 * 迭代器下一个元素
	 * @return 下一个元素
	 */
	public E next(){
		if(isStop){
			return null;
		} else {
			if(this.hasNext()){
				return this.get(iteratorLocal.get().getAndIncrement());
			} else {
				return null;
			}
		}
	}

	/**
	 * 迭代器是否有下一个对象
	 * @return 是否有下一个对象
	 */
	public boolean hasNext(){
		if(isStop){
			return false;
		} else {
			return iteratorLocal.get().get() <= this.size() - 1;
		}
	}

	/**
	 * 迭代器上一个元素
	 * @return 上一个元素
	 */
	public E previous(){
		if(isStop){
			return null;
		} else {
			if(this.hasPrevious()){
				return this.get(invertedIteratorLocal.get().getAndDecrement());
			} else {
				return null;
			}
		}
	}

	/**
	 * 迭代器是否有上一个对象
	 * @return 是否有上一个对象
	 */
	public boolean hasPrevious(){
		if(isStop){
			return false;
		} else {
			return invertedIteratorLocal.get().get() >= 0;
		}
	}

}
