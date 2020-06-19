package org.voovan.tools.collection;

import java.util.ArrayList;
import java.util.List;
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
public class Chain<E> {
	public int iterator = 0;
	public int invertedIterator = 0;
	private boolean isStop;
	private List<E> contianer = new ArrayList<E>();

	/**
	 * 构造函数
	 */
	public Chain(){
		isStop = false;
		rewind();
	}

	public Chain(List<E> contianer) {
		this.contianer = contianer;
		isStop = false;
		rewind();
	}

	public Chain<E> add(E e) {
		contianer.add(e);
		return this;
	}

	public boolean contains(E e) {
		return contianer.contains(e);
	}

	public boolean remove(E e) {
		return contianer.remove(e);
	}

	public Chain<E> clear() {
		contianer.clear();
		return this;
	}

	public int size() {
		return contianer.size();
	}


	/**
	 * 获取保存对象的容器
	 * @return
	 */
	public Chain<E> getContianer() {
		return this;
	}

	/**
	 * 重置链的迭代器
	 * @return 链对象
	 */
	public Chain<E> rewind(){
		isStop = false;
		iterator = 0;
		invertedIterator = contianer.size() - 1;

		return this;
	}

	/**
	 * 迭代完成
	 */
	public void stop(){
		this.isStop = true;
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
				 E e = contianer.get(iterator++);
				return e;
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
			return iterator <= contianer.size() - 1;
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
				return contianer.get(invertedIterator--);
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
			return invertedIterator >= 0;
		}
	}

	@Override
	public Object clone() {
		return new Chain<E>(contianer);
	}
}
